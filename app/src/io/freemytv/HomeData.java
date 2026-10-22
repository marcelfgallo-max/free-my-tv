package io.freemytv;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Optional crypto prices (Binance, every 5 min) and weather/sunrise/sunset (Open-Meteo, every 30 min), cached
 * so the screen opens with numbers. Only fetched while the home screen or the screensaver is visible (the screen asks).
 * In reais also the dollar and the euro (AwesomeAPI) and the Ibovespa (Yahoo Finance, unofficial).
 * No source needs a key. Location and currency come from config.properties.
 */
final class HomeData {
    static final String[] COINS = {"BTC", "ETH", "SOL", "USD", "EUR", "IBOV"};
    static final String[] COIN_NAMES = {"Bitcoin", "Ethereum", "Solana", "Dollar", "Euro", "Ibovespa"};
    static final int CRYPTO = 3, USD = 3, EUR = 4, IBOV = 5;
    private static final String CURRENCY = Config.CURRENCY.trim().toUpperCase(Locale.ROOT);
    /** Order on screen and in Home Settings: in reais the dollar, the euro and the Ibovespa come first. */
    static final int[] ORDER = CURRENCY.equals("BRL") ? new int[] {USD, EUR, IBOV, 0, 1, 2} : new int[] {0, 1, 2};
    /** The drawer's quotes card has room for this many. */
    static final int CARD_ROWS = 4;
    /** Binance quotes dollars as USDT. */
    private static final String QUOTE = CURRENCY.equals("USD") ? "USDT" : CURRENCY;
    private static final String WEATHER_QUERY = "&current=temperature_2m,weather_code,is_day&daily=temperature_2m_max,temperature_2m_min,sunrise,sunset"
            + "&hourly=temperature_2m,weather_code,is_day&timezone=auto&forecast_days=2";

    private static String weatherPlace() {
        return Prefs.latitude() + "," + Prefs.longitude();
    }

    private static String weatherUrl() {
        return "https://api.open-meteo.com/v1/forecast?latitude=" + Prefs.latitude() + "&longitude=" + Prefs.longitude() + WEATHER_QUERY;
    }
    private static final long PRICES_EVERY = 5 * 60_000L, WEATHER_EVERY = 30 * 60_000L;
    interface Listener { void onData(); }

    /** How many quotes the top row and the screensaver show (none when the quotes are only in the drawer). */
    static int coinCount() {
        return Prefs.quotes() && Prefs.quotesTop() ? active().length : 0;
    }

    /** The drawer's quotes card is on and has something to show. */
    static boolean quotesCard() {
        return Prefs.quotes() && active().length > 0;
    }

    private static volatile int[] activeCoins;
    private static volatile int activeVersion = -1;

    /** Indexes in COINS of the quotes the user has on, in ORDER. */
    static int[] active() {
        int[] a = activeCoins;
        if (a != null && activeVersion == Prefs.version) return a;
        int n = 0;
        int[] tmp = new int[COINS.length];
        for (int i : ORDER) if (Prefs.coin(i)) tmp[n++] = i;
        a = java.util.Arrays.copyOf(tmp, n);
        activeCoins = a;
        activeVersion = Prefs.version;
        return a;
    }

    /** Settings changed (city, coins): fetch again now. */
    void settingsChanged() {
        pricesAt = 0;
        sparkAt = 0;
        weatherAt = 0;
        pricesFailedAt = 0;
        weatherFailedAt = 0;
    }

    static String coinName(int i) {
        return i == USD ? Ui.tr("Dólar", "Dollar") : i == EUR ? Ui.tr("Euro", "Euro") : COIN_NAMES[i];
    }

    /** True if any of the quotes that are on is not a coin (their change is the day's, not the last 24 h). */
    static boolean anyMarket() {
        for (int i : active()) if (i >= CRYPTO) return true;
        return false;
    }

    private static boolean on(int[] coins, int i) {
        for (int c : coins) if (c == i) return true;
        return false;
    }

    /** Binance, for the coins that are on; null with none. */
    private static String pricesUrl() {
        StringBuilder symbols = new StringBuilder();
        for (int i : active()) {
            if (i >= CRYPTO) continue;
            symbols.append(symbols.length() == 0 ? "" : ",").append("%22").append(COINS[i]).append(QUOTE).append("%22");
        }
        return symbols.length() == 0 ? null : "https://api.binance.com/api/v3/ticker/24hr?type=MINI&symbols=%5B" + symbols + "%5D";
    }

    /** AwesomeAPI, for the dollar and the euro in reais; null with neither on. */
    private static String fxUrl() {
        int[] coins = active();
        String pairs = (on(coins, USD) ? "USD-BRL," : "") + (on(coins, EUR) ? "EUR-BRL" : "");
        if (pairs.endsWith(",")) pairs = pairs.substring(0, pairs.length() - 1);
        return pairs.isEmpty() ? null : "https://economia.awesomeapi.com.br/json/last/" + pairs;
    }

    /** Yahoo Finance (unofficial, no key): the last days' closes, for the Ibovespa; null when it's off. */
    private static String ibovUrl() {
        return on(active(), IBOV) ? "https://query1.finance.yahoo.com/v8/finance/chart/%5EBVSP?range=5d&interval=1d" : null;
    }

    static final class Weather {
        double temp = Double.NaN, max = Double.NaN, min = Double.NaN;
        int code = -1;
        boolean isDay = true;
        long sunrise, sunset;   // today's, in epoch ms
        // Next hours (starting at the next full hour): time, temperature, code and day/night.
        final List<Long> hours = new ArrayList<>();
        final List<Double> hourTemp = new ArrayList<>();
        final List<Integer> hourCode = new ArrayList<>();
        final List<Boolean> hourDay = new ArrayList<>();
    }

    private static HomeData instance;

    static synchronized HomeData get(Context c) {
        if (instance == null) instance = new HomeData(c.getApplicationContext());
        return instance;
    }

    final double[] prices = {Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN};
    /** Change (%), in the same order: the coins' in 24 h, the others' since the last close. */
    final double[] changes = {Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN};
    volatile long pricesAt, weatherAt;
    volatile Weather weather = new Weather();

    private final SharedPreferences cache;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Listener> listeners = new ArrayList<>();
    private volatile boolean fetchingPrices, fetchingWeather;
    private volatile long pricesFailedAt, weatherFailedAt;

    private HomeData(Context c) {
        cache = c.getSharedPreferences("homedata", Context.MODE_PRIVATE);
        try {
            if (Prefs.quotes()) {
                String p = cache.getString("prices", null);
                if (p != null) parsePrices(p);
                String fx = cache.getString("fx", null);
                if (fx != null) parseFx(fx);
                String ibov = cache.getString("ibov", null);
                if (ibov != null) parseIbov(ibov);
                pricesAt = cache.getLong("pricesAt", 0);
            }
            String w = cache.getString("weather", null);
            // The cached forecast is for the city it was fetched for; another city means fetch again.
            if (w != null && weatherPlace().equals(cache.getString("weatherPlace", ""))) {
                weather = parseWeather(w);
                weatherAt = cache.getLong("weatherAt", 0);
            }
        } catch (Exception ignored) {}
    }

    void addListener(Listener l) { if (!listeners.contains(l)) listeners.add(l); }
    void removeListener(Listener l) { listeners.remove(l); }

    /** Fetches whatever is stale. The screen calls this when it appears and every minute while it's visible. */
    void refreshIfStale() {
        long now = System.currentTimeMillis();
        // A negative age (the TV's clock went back) counts as stale: otherwise the data would freeze.
        // The mini charts too: right after a restart the prices come from the cache, the charts not yet.
        boolean stale = now - pricesAt > PRICES_EVERY || now < pricesAt || now - sparkAt > SPARK_EVERY;
        if (!fetchingPrices && quotesCard() && stale && !failedRecently(pricesFailedAt, now)) {
            fetchingPrices = true;
            io.execute(() -> {
                try {
                    // Each source on its own: one that fails leaves the others' numbers.
                    boolean got = fetch("prices", pricesUrl(), this::parsePrices);
                    got |= fetch("fx", fxUrl(), this::parseFx);
                    got |= fetch("ibov", ibovUrl(), this::parseIbov);
                    long at = System.currentTimeMillis();
                    if (got && (at - sparkAt > SPARK_EVERY || at < sparkAt)) fetchSparks();
                    if (got) {
                        pricesAt = System.currentTimeMillis();
                        cache.edit().putLong("pricesAt", pricesAt).apply();
                        notifyListeners();
                    } else {
                        pricesFailedAt = System.currentTimeMillis();
                    }
                } finally {
                    fetchingPrices = false;
                }
            });
        }
        if (!fetchingWeather && (now - weatherAt > WEATHER_EVERY || now < weatherAt) && !failedRecently(weatherFailedAt, now)) {
            fetchingWeather = true;
            io.execute(() -> {
                try {
                    String body = get(weatherUrl());
                    weather = parseWeather(body);
                    weatherAt = System.currentTimeMillis();
                    cache.edit().putString("weather", body).putLong("weatherAt", weatherAt).putString("weatherPlace", weatherPlace()).apply();
                    notifyListeners();
                } catch (Exception e) {
                    weatherFailedAt = System.currentTimeMillis();
                    AppLog.w("weather: " + e.getMessage());
                } finally {
                    fetchingWeather = false;
                }
            });
        }
    }

    private static final long SPARK_EVERY = 15 * 60_000L;
    /**
     * The drawer card's mini charts: the coins' and the currencies' last 24 h (hourly; every 30 min), the Ibovespa's
     * session. Null until fetched.
     */
    final float[][] spark = new float[COINS.length][];
    volatile long sparkAt;

    /** Binance's hourly closes for a coin, Yahoo Finance's day for the dollar, the euro and the Ibovespa. */
    private static String sparkUrl(int i) {
        if (i < CRYPTO) return "https://api.binance.com/api/v3/klines?interval=1h&limit=24&symbol=" + COINS[i] + QUOTE;
        String symbol = i == USD ? "USDBRL%3DX" : i == EUR ? "EURBRL%3DX" : "%5EBVSP";
        // The currencies trade around the clock and Yahoo's "1d" is empty for hours after its day turns: two days,
        // cut to the last 24 h below.
        return "https://query1.finance.yahoo.com/v8/finance/chart/" + symbol + "?range=" + (i == IBOV ? "1d" : "2d") + "&interval=30m";
    }

    /** The mini charts of the quotes the card shows; one that fails keeps its last line. */
    private void fetchSparks() {
        int[] coins = active();
        for (int j = 0; j < coins.length && j < CARD_ROWS; j++) {
            int i = coins[j];
            try {
                float[] line = parseSpark(i, get(sparkUrl(i)));
                if (line != null) spark[i] = line;
            } catch (Exception e) {
                AppLog.w("spark " + COINS[i] + ": " + e.getMessage());
            }
        }
        sparkAt = System.currentTimeMillis();
    }

    private static float[] parseSpark(int i, String body) throws Exception {
        float[] out;
        int n = 0;
        if (i < CRYPTO) {
            // [[openTime, open, high, low, close, …], …]
            JSONArray rows = new JSONArray(body);
            out = new float[rows.length()];
            for (int k = 0; k < rows.length(); k++) out[n++] = (float) Double.parseDouble(rows.getJSONArray(k).getString(4));
        } else {
            JSONArray close = new JSONObject(body).getJSONObject("chart").getJSONArray("result").getJSONObject(0)
                    .getJSONObject("indicators").getJSONArray("quote").getJSONObject(0).getJSONArray("close");
            out = new float[close.length()];
            for (int k = 0; k < close.length(); k++) if (!close.isNull(k)) out[n++] = (float) close.getDouble(k);
            if (i != IBOV && n > 48) return java.util.Arrays.copyOfRange(out, n - 48, n);   // 24 h of 30 min
        }
        return n >= 2 ? java.util.Arrays.copyOf(out, n) : null;
    }

    private interface Parser { void parse(String body) throws Exception; }

    /** Fetches one source of quotes and keeps its answer; false if it failed (or there's nothing to fetch). */
    private boolean fetch(String key, String url, Parser parser) {
        if (url == null) return false;
        try {
            String body = get(url);
            parser.parse(body);
            cache.edit().putString(key, body).apply();
            return true;
        } catch (Exception e) {
            AppLog.w(key + ": " + e.getMessage());
            return false;
        }
    }

    private static boolean failedRecently(long at, long now) {
        return at != 0 && now >= at && now - at < 2 * 60_000L;
    }

    private void notifyListeners() {
        main.post(() -> { for (Listener l : new ArrayList<>(listeners)) l.onData(); });
    }

    private void parsePrices(String body) throws Exception {
        JSONArray list = new JSONArray(body);
        String quote = QUOTE;
        int found = 0;
        for (int i = 0; i < list.length(); i++) {
            JSONObject o = list.getJSONObject(i);
            String symbol = o.getString("symbol");
            for (int k = 0; k < CRYPTO; k++) {
                if (!symbol.equals(COINS[k] + quote)) continue;
                found++;
                double last = Double.parseDouble(o.optString("lastPrice", o.optString("price", "NaN")));
                double open = Double.parseDouble(o.optString("openPrice", "NaN"));
                prices[k] = last;
                changes[k] = open > 0 ? (last - open) / open * 100 : Double.NaN;
            }
        }
        if (found == 0) pricesAt = 0;   // cached in another currency (config changed): fetch again
    }

    /** AwesomeAPI: {"USDBRL": {"bid": "5.016", "pctChange": "-0.15"}, "EURBRL": …}. */
    private void parseFx(String body) throws Exception {
        JSONObject o = new JSONObject(body);
        int[] which = {USD, EUR};
        for (int k : which) {
            JSONObject pair = o.optJSONObject(COINS[k] + "BRL");
            if (pair == null) continue;
            prices[k] = Double.parseDouble(pair.optString("bid", "NaN"));
            changes[k] = Double.parseDouble(pair.optString("pctChange", "NaN"));
        }
    }

    /**
     * Yahoo Finance: the last close against the one before. Before the market opens, today's close is null and this
     * is the last session's change.
     */
    private void parseIbov(String body) throws Exception {
        JSONObject result = new JSONObject(body).getJSONObject("chart").getJSONArray("result").getJSONObject(0);
        JSONArray close = result.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0).getJSONArray("close");
        double last = Double.NaN, before = Double.NaN;
        for (int i = 0; i < close.length(); i++) {
            if (close.isNull(i)) continue;
            before = last;
            last = close.getDouble(i);
        }
        double now = result.getJSONObject("meta").optDouble("regularMarketPrice", last);
        prices[IBOV] = now;
        changes[IBOV] = before > 0 ? (now - before) / before * 100 : Double.NaN;
    }

    private static Weather parseWeather(String body) throws Exception {
        JSONObject o = new JSONObject(body);
        TimeZone zone = TimeZone.getTimeZone(o.optString("timezone", TimeZone.getDefault().getID()));
        Weather w = new Weather();
        JSONObject current = o.getJSONObject("current");
        w.temp = current.getDouble("temperature_2m");
        w.code = current.getInt("weather_code");
        w.isDay = current.optInt("is_day", 1) == 1;
        JSONObject daily = o.getJSONObject("daily");
        w.max = daily.getJSONArray("temperature_2m_max").getDouble(0);
        w.min = daily.getJSONArray("temperature_2m_min").getDouble(0);
        w.sunrise = localTime(daily.getJSONArray("sunrise").getString(0), zone);
        w.sunset = localTime(daily.getJSONArray("sunset").getString(0), zone);
        JSONObject hourly = o.optJSONObject("hourly");
        if (hourly != null) {
            JSONArray times = hourly.getJSONArray("time");
            JSONArray temps = hourly.getJSONArray("temperature_2m");
            JSONArray codes = hourly.getJSONArray("weather_code");
            JSONArray days = hourly.optJSONArray("is_day");
            long now = System.currentTimeMillis();
            for (int i = 0; i < times.length() && w.hours.size() < 6; i++) {
                long at = localTime(times.getString(i), zone);
                if (at <= now) continue;
                w.hours.add(at);
                w.hourTemp.add(temps.getDouble(i));
                w.hourCode.add(codes.getInt(i));
                w.hourDay.add(days == null || days.optInt(i, 1) == 1);
            }
        }
        return w;
    }

    private static long localTime(String iso, TimeZone zone) throws ParseException {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
        f.setTimeZone(zone);
        return f.parse(iso).getTime();
    }

    static List<String[]> findCity(String name) throws Exception {
        JSONObject o = new JSONObject(get("https://geocoding-api.open-meteo.com/v1/search?count=10&format=json&language=" + Ui.language
                + "&name=" + URLEncoder.encode(name.trim(), "UTF-8")));
        JSONArray results = o.optJSONArray("results");
        List<String[]> out = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();   // places with the same name, state and country read the same
        for (int i = 0; results != null && i < results.length(); i++) {
            JSONObject r = results.getJSONObject(i);
            String city = r.optString("name", "");
            if (city.isEmpty() || !r.has("latitude") || !r.has("longitude")) continue;
            StringBuilder label = new StringBuilder(city);
            for (String part : new String[] {r.optString("admin1", ""), r.optString("country", "")}) {
                if (!part.isEmpty() && !part.equals(city)) label.append(", ").append(part);
            }
            if (!seen.add(label.toString())) continue;
            out.add(new String[] {label.toString(), city,
                    String.format(Locale.US, "%.4f", r.getDouble("latitude")), String.format(Locale.US, "%.4f", r.getDouble("longitude"))});
        }
        return out;
    }

    private static String get(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(6000);
        c.setReadTimeout(6000);
        c.setRequestProperty("User-Agent", "FreeMyTV");
        try {
            if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode());
            try (InputStream in = c.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
                return out.toString(StandardCharsets.UTF_8.name());
            }
        } finally {
            c.disconnect();
        }
    }

    /**
     * Night at your location: from sunset to sunrise (like Apple's "Automatic" appearance: the light design darkens).
     * Uses the last weather fetched (it moves ~1 min a day); without data, 6:00 and 18:00.
     */
    boolean isNight(long now) {
        long t = Shaders.timeOfDay(now, 0);
        long rise = Shaders.timeOfDay(weather.sunrise, 6 * 60 * 60_000L);
        long set = Shaders.timeOfDay(weather.sunset, 18 * 60 * 60_000L);
        return t < rise || t >= set;
    }

    // MARK: - Formatting

    private static Locale locale() {
        return Ui.locale();
    }

    /** 580.4k · 21.0k · 1.04k · 5.61 (as short as possible; the dollar always with cents). */
    static String price(double v) {
        if (Double.isNaN(v)) return "—";
        if (v >= 10_000) return String.format(locale(), "%.1fk", v / 1000);
        if (v >= 1000) return String.format(locale(), "%.2fk", v / 1000);
        return String.format(locale(), "%.2f", v);
    }

    /** 416.3k · 12.2k · 1.04k · 531 · 5.05: abbreviated, for the price row on the home screen. */
    static String compact(double v) {
        if (Double.isNaN(v)) return "—";
        if (v >= 10_000) return String.format(locale(), "%.1fk", v / 1000);
        if (v >= 1000) return String.format(locale(), "%.2fk", v / 1000);
        if (v >= 100) return String.format(locale(), "%.0f", v);
        return String.format(locale(), "%.2f", v);
    }

    /** $108,234 · $185.20 · R$ 581.234 · R$ 5,61 (cents only below a thousand), in the configured currency. */
    static String brl(double v) {
        String symbol = currencySymbol();
        if (Double.isNaN(v)) return symbol + "—";
        return Ui.once("brl", v, () -> v >= 1000 ? String.format(locale(), symbol + "%,.0f", v)
                : String.format(locale(), symbol + "%.2f", v));
    }

    /** A quote for the drawer card: a price in the configured currency, or the Ibovespa in points. */
    static String quote(int i, double v) {
        if (i != IBOV) return brl(v);
        if (Double.isNaN(v)) return "—";
        return Ui.once("points", v, () -> String.format(locale(), "%,.0f", v));
    }

    /** The weather in words, from Open-Meteo's WMO code. */
    static String condition(int code, boolean day) {
        if (code < 0) return "";
        if (Ui.en) {
            if (code == 0) return "Clear";
            if (code == 1) return day ? "Mostly Sunny" : "Mostly Clear";
            if (code == 2) return "Partly Cloudy";
            if (code == 3) return "Cloudy";
            if (code == 45 || code == 48) return "Fog";
            if (code >= 51 && code <= 57) return "Drizzle";
            if (code == 65 || code == 67 || code == 82) return "Heavy Rain";
            if (code >= 71 && code <= 77 || code == 85 || code == 86) return "Snow";
            if (code >= 95) return "Thunderstorms";
            return "Rain";
        }
        if (Ui.ru) {
            if (code == 0) return "Ясно";
            if (code == 1) return "Малооблачно";
            if (code == 2) return "Переменная облачность";
            if (code == 3) return "Облачно";
            if (code == 45 || code == 48) return "Туман";
            if (code >= 51 && code <= 57) return "Морось";
            if (code == 65 || code == 67 || code == 82) return "Сильный дождь";
            if (code >= 71 && code <= 77 || code == 85 || code == 86) return "Снег";
            if (code >= 95) return "Гроза";
            return "Дождь";
        }
        if (Ui.es) {
            if (code == 0) return day ? "Despejado" : "Noche despejada";
            if (code == 1) return "Mayormente despejado";
            if (code == 2) return "Parcialmente nublado";
            if (code == 3) return "Nublado";
            if (code == 45 || code == 48) return "Niebla";
            if (code >= 51 && code <= 57) return "Llovizna";
            if (code == 65 || code == 67 || code == 82) return "Lluvia fuerte";
            if (code >= 71 && code <= 77 || code == 85 || code == 86) return "Nieve";
            if (code >= 95) return "Tormenta";
            return "Lluvia";
        }
        if (Ui.de) {
            if (code == 0) return day ? "Sonnig" : "Klar";
            if (code == 1) return day ? "Überwiegend sonnig" : "Überwiegend klar";
            if (code == 2) return "Teilweise bewölkt";
            if (code == 3) return "Bewölkt";
            if (code == 45 || code == 48) return "Nebel";
            if (code >= 51 && code <= 57) return "Nieselregen";
            if (code == 65 || code == 67 || code == 82) return "Starker Regen";
            if (code >= 71 && code <= 77 || code == 85 || code == 86) return "Schnee";
            if (code >= 95) return "Gewitter";
            return "Regen";
        }
        if (code == 0) return day ? "Céu limpo" : "Noite limpa";
        if (code == 1) return "Poucas nuvens";
        if (code == 2) return "Parcialmente nublado";
        if (code == 3) return "Nublado";
        if (code == 45 || code == 48) return "Neblina";
        if (code >= 51 && code <= 57) return "Garoa";
        if (code == 65 || code == 67 || code == 82) return "Chuva forte";
        if (code >= 71 && code <= 77 || code == 85 || code == 86) return "Neve";
        if (code >= 95) return "Tempestade";
        return "Chuva";
    }

    private static String currencySymbol() {
        switch (CURRENCY) {
            case "USD": return "$";
            case "BRL": return "R$ ";
            case "EUR": return "€";
            case "GBP": return "£";
            case "JPY": return "¥";
            default: return CURRENCY + " ";
        }
    }

    /** Celsius or Fahrenheit (config.properties "temperature": C, F, or auto = Fahrenheit in the US). */
    static String degrees(double v) {
        if (Double.isNaN(v)) return "—";
        return Math.round(fahrenheit() ? v * 9 / 5 + 32 : v) + "°";
    }

    private static boolean fahrenheit() {
        String unit = Prefs.temperature();
        if (unit.equals("F")) return true;
        if (unit.equals("C")) return false;
        return "US".equals(Locale.getDefault().getCountry());
    }

    /** "+1.2%" / "−0.8%" (a real minus sign). */
    static String change(double v) {
        if (Double.isNaN(v)) return "";
        return Ui.once("change", v, () -> sign(v, 1) + String.format(locale(), "%.1f%%", Math.abs(v)));
    }

    /** Like change(v), with two decimals for the dollar and the euro (they move little: "−0.05%"). */
    static String change(int i, double v) {
        if (i != USD && i != EUR || Double.isNaN(v)) return change(v);
        return Ui.once("change2", v, () -> sign(v, 2) + String.format(locale(), "%.2f%%", Math.abs(v)));
    }

    /** True when quote i's change shows as zero ("0.00%"): too small to call up or down. */
    static boolean flat(int i, double v) {
        return !Double.isNaN(v) && Math.round(Math.abs(v) * (i == USD || i == EUR ? 100 : 10)) == 0;
    }

    /** "+" or "−", or nothing when the change rounds to zero at that many decimals (no "−0.00%"). */
    private static String sign(double v, int decimals) {
        if (Math.round(Math.abs(v) * Math.pow(10, decimals)) == 0) return "";
        return v >= 0 ? "+" : "−";
    }

    /** "18h" ("6 PM" when the TV is set to 12 hours; "18:00" in English). */
    static String hour(long at) {
        boolean h12 = Ui.h12();
        return Ui.once(h12 ? "hour12" : "hour", at, () -> h12 ? new SimpleDateFormat("h a", Locale.US).format(new java.util.Date(at))
                : new SimpleDateFormat(Ui.en || Ui.es || Ui.ru ? "HH:mm" : Ui.de ? "HH 'Uhr'" : "HH'h'", Ui.locale()).format(new java.util.Date(at)));
    }

    /** How long ago the data was fetched, if it's stale (no internet): "12 min ago". */
    static String staleness(long at, long every) {
        if (at == 0) return "";
        long age = System.currentTimeMillis() - at;
        if (age < every * 2) return "";
        long minutes = age / 60_000;
        if (Ui.en) return minutes < 60 ? minutes + " min ago" : (minutes / 60) + " h ago";
        if (Ui.ru) return minutes < 60 ? minutes + " мин назад" : (minutes / 60) + " ч назад";
        if (Ui.es) return minutes < 60 ? "hace " + minutes + " min" : "hace " + (minutes / 60) + " h";
        if (Ui.de) return minutes < 60 ? "vor " + minutes + " Min." : "vor " + (minutes / 60) + " Std.";
        return minutes < 60 ? "há " + minutes + " min" : "há " + (minutes / 60) + " h";
    }
}
