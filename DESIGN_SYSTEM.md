# Design system do Free My TV

## Estado da implementação

Fase 1 concluída para o primeiro lote de 293 tokens, com validação visual da Home. Fase 2 instalada com autorização de Marcel: fundo grafite e superfície da barra de apps na Home e na gaveta. Cores confirmadas nas capturas reais; aguardar o próximo OK antes de aplicar foco laranja.

Base: 8569d2127ac1577921764e6ea541dd798f085cb9. Trabalho exclusivo no worktree free-my-tv-design, branch design-system.
Build e comparação de instruções: design-review/phase1-verification.json. Home validada com capturas reais antes/depois: zero diferença em 1.911.600 pixels fora do cabeçalho dinâmico de relógio e saudação. Gaveta e ajustes ainda não possuem capturas pareadas. Build sozinho não comprova equivalência de pixels.

## Contrato dos tokens

- Tokens Legacy: constantes primitivas static final, inlined pelo Java. Os novos papéis semânticos usam um switch simples, sem alocação no caminho normal, novos shaders ou dependências.
- Coordenadas e tamanhos usam o Canvas lógico 1920 × 1080; multiplicar por k como no código original. Não converter para dp/sp.
- ARGB contém alpha exato. WHITE_Axx e BLACK_Axx usam o byte hexadecimal xx, não uma porcentagem arredondada.
- Tamanhos de texto são textSize lógico; tracking é em. Inter, pesos e optical sizes existentes são preservados.
- Tempos MS são milissegundos. RESPONSE_S é parâmetro da mola em segundos, não duração fixa; DAMPING é adimensional.
- Valores iguais de componentes diferentes podem permanecer separados. Não normalizar margens, raios ou cores parecidas durante a extração.
- HomeView continua com night = 1 e checkNight fixo. Na fase 2, apenas o fundo e a superfície arredondada do dock usam os novos tokens. Day, foco laranja e as demais formas serão aplicados em etapas posteriores.
- Os helpers tone/blend atuais continuam no mesmo local, preservando interpolação e arredondamento.

## Componentes

| Componente | Consumidores atuais | Contrato da fase 1 | Alvo posterior aprovado |
|---|---|---|---|
| Card de app | HomeLayout, AppColor, Palette, HomeMotion | Manter arte colorida, launch, ordem, foco e reorder | Dock contínuo, logos monocromáticos, foco laranja |
| Pôster | HomeLayout.SHOW_*, HomeText, HomeColor | 11 colunas, artes e deep links originais, foco atual | Raio 12, contorno laranja |
| Entrada HDMI | HomeLayout.ROW_*, HomeText.M_CELL_*, Palette.CEC_FOCUSED | Não mudar estado CEC, ações ou nomes | Superfície suave; indicador CEC separado do foco |
| Botão de utilidade | HomeLayout.L_BUTTON*, HomeColor | Manter forma atual, geometria e acessibilidade | Botão redondo de 88, raio 44 |
| Clima e cotações | HomeText.M_*, HomeLayout.CARDS_*, HomeColor.M_* | Mesmo conteúdo, sinais, cache e desenho | Cards arredondados, números pontilhados apenas onde aprovados |
| Linha de ajuste | SettingsLayout, SettingsText, SettingsColor | Mesma lógica de preferências e troca de home | Herdar tokens, sem inventar nova tela |
| Aba | HomeLayout.L_TAB*, HomeText.L_TAB*, HomeColor | Mesma seleção, foco e navegação | Seleção laranja, distinta do foco atual |

## Paleta alvo, aplicação por etapa

| Papel | Day | Night |
|---|---|---|
| BACKGROUND | #E6E5E8 | #202124 |
| SURFACE | #FAF9FB | #2B2C2F |
| TEXT_PRIMARY | #292B2E | #F5F5F7 |
| TEXT_SECONDARY | #62636A | #B8B9BF |
| ACCENT / FOCUS | #FF5A20 | #FF5A20 |

A especificação completa de valores alvo, estados, contrastes e sequência está em [PROPOSTA-TOKENS.md](design-review/PROPOSTA-TOKENS.md). Os 293 tokens Legacy preservam os valores extraídos. Os novos valores ficam fora de Legacy.

### Tokens semânticos da fase 2

| Token/API | Valor | Consumidor |
|---|---|---|
| BACKGROUND | papel 0 | HomeView.drawLightSky |
| SURFACE | papel 1 | HomeView.drawPlatter |
| BACKGROUND_NIGHT | 0xFF202124 | Fundo sólido da Home e da gaveta |
| SURFACE_NIGHT | 0xFF2B2C2F | Superfície opaca da barra de apps |
| BACKGROUND_DAY | 0xFFE6E5E8 | Endpoint reservado; Day ainda não está ativo |
| SURFACE_DAY | 0xFFFAF9FB | Endpoint reservado; Day ainda não está ativo |
| DOCK_RADIUS | 48 unidades lógicas | Cantos da barra; bounds e posições dos apps preservados |
| color(role, night) | int ARGB | Retorna endpoint Day/Night sem alocação no caminho normal |

A seleção de endpoint usa night >= 0.5f, com night fixo em 1 no comportamento atual. Não há transição Day/Night implementada. O helper rejeita papéis desconhecidos. drawRect e drawRoundRect são compatíveis com API 28. Não há blur ou sombra no novo fundo/dock.

A Home não decodifica mais fields/ember.png e fields/ultra.png. Os assets continuam no projeto para consumidores existentes, inclusive ajustes. Pôsteres, logos, ícones, foco, ações e layout permanecem nesta etapa com os valores anteriores.


## Catálogo implementado

Cada linha abaixo é uma constante de Theme.Legacy. O prefixo da seção mais o nome identifica o token completo. A fonte executável dos valores é Theme.java; phase1-token-map.json registra a extração.
Os nomes dos paints foram mantidos para facilitar a auditoria contra o código anterior. L_TITLE é o relógio principal; L_GREETING é a saudação; M_BIG é a temperatura; M_NAME/M_PRICE/M_CHANGE são as cotações.
Há parâmetros visuais locais e caminhos auxiliares ainda não migrados. O inventário original permanece em design-review/INVENTARIO.md; a fase 1 não transforma índices, máscaras binárias ou constantes de algoritmos em tokens de design.
Frost, Backdrop, Shadows, Scene e os assets não são alterados nesta etapa. A presença de um token de raio antigo não significa que o componente ativo já esteja arredondado.

### Theme.Legacy.HomeColor

| Token | Tipo | Valor atual | Consumidor |
|---|---|---|---|
| SMOKE | int | 0xFF111113 | HomeView.SMOKE |
| INK | int | 0xFF1C1C1E | HomeView.INK |
| WHITE | int | 0xFFF5F5F7 | HomeView.WHITE |
| UP_COLOR | int | 0xFF30D158 | HomeView.UP_COLOR |
| DOWN_COLOR | int | 0xFFFF453A | HomeView.DOWN_COLOR |
| M_INK | int | 0xFFFFFFFF | HomeView.M_INK |
| M_MUTED | int | 0xB8FFFFFF | HomeView.M_MUTED |
| M_HAIRLINE | int | 0x8CFFFFFF | HomeView.M_HAIRLINE |
| M_UP | int | 0xFF5EDCFF | HomeView.M_UP |
| M_DOWN | int | 0xFFFF5A3C | HomeView.M_DOWN |
| L_INK | int | 0xFF1D1D1F | HomeView.L_INK |
| L_DIM | int | 0xFF6E6E73 | HomeView.L_DIM |
| L_FAINT | int | 0xFF8E8E93 | HomeView.L_FAINT |

### Theme.Legacy.AppColor

| Token | Tipo | Valor atual | Consumidor |
|---|---|---|---|
| NETFLIX_TOP | int | 0xFF0A0A0A | HomeView.TILE_STYLE |
| NETFLIX_BOTTOM | int | 0xFF000000 | HomeView.TILE_STYLE |
| YOUTUBE_TOP | int | 0xFF1C1C1E | HomeView.TILE_STYLE |
| YOUTUBE_BOTTOM | int | 0xFF09090A | HomeView.TILE_STYLE |
| GLOBOPLAY_TOP | int | 0xFFFF8A1F | HomeView.TILE_STYLE |
| GLOBOPLAY_BOTTOM | int | 0xFFFF2D5A | HomeView.TILE_STYLE |
| PRIME_TOP | int | 0xFF2AA8FF | HomeView.TILE_STYLE |
| PRIME_BOTTOM | int | 0xFF0A68D4 | HomeView.TILE_STYLE |
| DISNEY_TOP | int | 0xFF173584 | HomeView.TILE_STYLE |
| DISNEY_BOTTOM | int | 0xFF08143F | HomeView.TILE_STYLE |
| MAX_TOP | int | 0xFF5C30E8 | HomeView.TILE_STYLE |
| MAX_BOTTOM | int | 0xFF221684 | HomeView.TILE_STYLE |
| APPLETV_TOP | int | 0xFF1C1C1E | HomeView.TILE_STYLE |
| APPLETV_BOTTOM | int | 0xFF000000 | HomeView.TILE_STYLE |

### Theme.Legacy.Palette

| Token | Tipo | Valor atual | Consumidor |
|---|---|---|---|
| WHITE_AFF | int | 0xFFFFFFFF | HomeView drawing paints and blend endpoints |
| WHITE_AC7 | int | 0xC7FFFFFF | HomeView drawing paints and blend endpoints |
| WHITE_AD1 | int | 0xD1FFFFFF | HomeView drawing paints and blend endpoints |
| BLACK_A59 | int | 0x59000000 | HomeView drawing paints and blend endpoints |
| WHITE_AF2 | int | 0xF2FFFFFF | HomeView drawing paints and blend endpoints |
| BLACK_A99 | int | 0x99000000 | HomeView drawing paints and blend endpoints |
| WHITE_AB8 | int | 0xB8FFFFFF | HomeView drawing paints and blend endpoints |
| WHITE_A99 | int | 0x99FFFFFF | HomeView drawing paints and blend endpoints |
| WHITE_AB3 | int | 0xB3FFFFFF | HomeView drawing paints and blend endpoints |
| WHITE_AEB | int | 0xEBFFFFFF | HomeView drawing paints and blend endpoints |
| WHITE_AAD | int | 0xADFFFFFF | HomeView drawing paints and blend endpoints |
| GLASS_DAY | int | 0xFFEBEBF0 | HomeView drawing paints and blend endpoints |
| GLASS_NIGHT | int | 0xFF141416 | HomeView drawing paints and blend endpoints |
| TEXT_DIM_NIGHT | int | 0xFF98989D | HomeView drawing paints and blend endpoints |
| APP_FALLBACK | int | 0xFF161618 | HomeView drawing paints and blend endpoints |
| WHITE_A73 | int | 0x73FFFFFF | HomeView drawing paints and blend endpoints |
| BLACK_AFF | int | 0xFF000000 | HomeView drawing paints and blend endpoints |
| CEC_FOCUSED | int | 0xFF0B12E6 | HomeView drawing paints and blend endpoints |
| BLACK_AA8 | int | 0xA8000000 | HomeView drawing paints and blend endpoints |
| WHITE_A1A | int | 0x1AFFFFFF | HomeView drawing paints and blend endpoints |
| WHITE_A00 | int | 0x00FFFFFF | HomeView drawing paints and blend endpoints |
| WHITE_A14 | int | 0x14FFFFFF | HomeView drawing paints and blend endpoints |
| BLACK_A80 | int | 0x80000000 | HomeView drawing paints and blend endpoints |
| WHITE_A8C | int | 0x8CFFFFFF | SettingsView drawing paints and blend endpoints |
| WHITE_AE6 | int | 0xE6FFFFFF | SettingsView drawing paints and blend endpoints |
| BLACK_AB3 | int | 0xB3000000 | SettingsView drawing paints and blend endpoints |
| WHITE_A80 | int | 0x80FFFFFF | SettingsView drawing paints and blend endpoints |
| WHITE_A59 | int | 0x59FFFFFF | SettingsView drawing paints and blend endpoints |
| BLACK_A73 | int | 0x73000000 | SettingsView drawing paints and blend endpoints |

### Theme.Legacy.HomeText

| Token | Tipo | Valor atual | Consumidor |
|---|---|---|---|
| LABEL_PAINT_SIZE | int | 25 | HomeView.labelPaint |
| TITLE_PAINT_SIZE | int | 76 | HomeView.titlePaint |
| META_PAINT_SIZE | int | 29 | HomeView.metaPaint |
| CLOCK_PAINT_SIZE | int | 176 | HomeView.clockPaint |
| L_TITLE_SIZE | int | 124 | HomeView.lTitle |
| L_GREETING_SIZE | int | 40 | HomeView.lGreeting |
| L_TAB_SIZE | int | 28 | HomeView.lTab |
| L_TAB_ON_SIZE | int | 28 | HomeView.lTabOn |
| L_CARD_TITLE_SIZE | int | 34 | HomeView.lCardTitle |
| L_CARD_META_SIZE | int | 23 | HomeView.lCardMeta |
| BADGE_PAINT_SIZE | int | 20 | HomeView.badgePaint |
| L_QUOTE_SIZE | int | 24 | HomeView.lQuote |
| L_TIP_SIZE | int | 22 | HomeView.lTip |
| L_DATE_SIZE | int | 34 | HomeView.lDate |
| L_CLOCK_SIZE | int | 200 | HomeView.lClock |
| L_WEATHER_SIZE | int | 34 | HomeView.lWeather |
| L_CLOCK_QUOTE_SIZE | int | 28 | HomeView.lClockQuote |
| BUTTON_PAINT_SIZE | int | 29 | HomeView.buttonPaint |
| CAPSULE_TEXT_SIZE | int | 29 | HomeView.capsuleText |
| TIP_PAINT_SIZE | int | 25 | HomeView.tipPaint |
| QUOTE_PAINT_SIZE | int | 21 | HomeView.quotePaint |
| STALE_QUOTE_SIZE | int | 21 | HomeView.staleQuote |
| ROW_TITLE_SIZE | int | 25 | HomeView.rowTitle |
| DRAWER_LABEL_SIZE | int | 23 | HomeView.drawerLabel |
| CARD_TITLE_SIZE | int | 25 | HomeView.cardTitle |
| CARD_TEXT_SIZE | int | 29 | HomeView.cardText |
| CARD_DIM_SIZE | int | 23 | HomeView.cardDim |
| CARD_DIM_RIGHT_SIZE | int | 21 | HomeView.cardDimRight |
| BIG_TEMP_SIZE | int | 100 | HomeView.bigTemp |
| HOUR_PAINT_SIZE | int | 22 | HomeView.hourPaint |
| HOUR_TEMP_SIZE | int | 26 | HomeView.hourTemp |
| PRICE_PAINT_SIZE | int | 26 | HomeView.pricePaint |
| CHANGE_PAINT_SIZE | int | 21 | HomeView.changePaint |
| SHEET_TITLE_SIZE | int | 40 | HomeView.sheetTitle |
| GRID_LABEL_SIZE | int | 24 | HomeView.gridLabel |
| TOAST_PAINT_SIZE | int | 26 | HomeView.toastPaint |
| M_LABEL_SIZE | int | 26 | HomeView.mLabel |
| M_CELL_SIZE | int | 21 | HomeView.mCell |
| M_NAME_SIZE | int | 31 | HomeView.mName |
| M_PRICE_SIZE | int | 31 | HomeView.mPrice |
| M_CHANGE_SIZE | int | 25 | HomeView.mChange |
| M_MUTED_SIZE | int | 22 | HomeView.mMuted |
| M_HOUR_SIZE | int | 24 | HomeView.mHour |
| M_HOUR_TEMP_SIZE | int | 38 | HomeView.mHourTemp |
| M_BIG_SIZE | int | 184 | HomeView.mBig |
| M_BIG_DEGREE_SIZE | int | 84 | HomeView.mBigDegree |
| TEAM_BIG_SIZE | int | 26 | HomeView.teamBig |
| TEAM_SMALL_SIZE | int | 21 | HomeView.teamSmall |
| TEAM_TAG_SIZE | int | 21 | HomeView.teamTag |
| LABEL_PAINT_TRACKING | float | 0.1f | HomeView.labelPaint |
| TITLE_PAINT_TRACKING | float | 0.005f | HomeView.titlePaint |
| META_PAINT_TRACKING | float | 0.014f | HomeView.metaPaint |
| CLOCK_PAINT_TRACKING | float | -0.01f | HomeView.clockPaint |
| L_TITLE_TRACKING | float | -0.05f | HomeView.lTitle |
| L_GREETING_TRACKING | float | -0.02f | HomeView.lGreeting |
| L_TAB_TRACKING | float | -0.01f | HomeView.lTab |
| L_TAB_ON_TRACKING | float | -0.01f | HomeView.lTabOn |
| L_CARD_TITLE_TRACKING | float | -0.03f | HomeView.lCardTitle |
| L_CLOCK_TRACKING | float | -0.01f | HomeView.lClock |
| M_LABEL_TRACKING | float | -0.01f | HomeView.mLabel |
| M_CELL_TRACKING | float | -0.01f | HomeView.mCell |
| M_NAME_TRACKING | float | -0.02f | HomeView.mName |
| M_PRICE_TRACKING | float | -0.02f | HomeView.mPrice |
| M_CHANGE_TRACKING | float | -0.01f | HomeView.mChange |
| M_HOUR_TEMP_TRACKING | float | -0.035f | HomeView.mHourTemp |
| M_BIG_TRACKING | float | -0.055f | HomeView.mBig |
| TEAM_BIG_TRACKING | float | -0.01f | HomeView.teamBig |
| M_CELL_MIN_SIZE | int | 17 | HomeView.drawDrawerTile |

### Theme.Legacy.HomeLayout

| Token | Tipo | Valor atual | Consumidor |
|---|---|---|---|
| MARGIN | float | 96f | HomeView.MARGIN |
| CONTENT_R | float | 1824f | HomeView.CONTENT_R |
| HERO_TITLE_W | float | 900f | HomeView.HERO_TITLE_W |
| SHELF_PAD | float | 28f | HomeView.SHELF_PAD |
| TILE_GAP | float | 36f | HomeView.TILE_GAP |
| TILE_RADIUS | float | 22f | HomeView.TILE_RADIUS |
| TILE_MAX_W | float | 270f | HomeView.TILE_MAX_W |
| CAPSULE_TOP | float | 46f | HomeView.CAPSULE_TOP |
| CAPSULE_H | float | 68f | HomeView.CAPSULE_H |
| BUTTON | float | 56f | HomeView.BUTTON |
| SHIFT | float | 604f | HomeView.SHIFT |
| ROW_TITLE | float | 482f | HomeView.ROW_TITLE |
| ROW_TOP | float | 510f | HomeView.ROW_TOP |
| ROW_H | float | 137f | HomeView.ROW_H |
| CARDS_TOP | float | 684f | HomeView.CARDS_TOP |
| CARDS_H | float | 310f | HomeView.CARDS_H |
| GRID_W | float | 300f | HomeView.GRID_W |
| GRID_H | float | 169f | HomeView.GRID_H |
| GRID_GAP | float | 40f | HomeView.GRID_GAP |
| GRID_ROW | float | 239f | HomeView.GRID_ROW |
| GRID_TOP | float | 196f | HomeView.GRID_TOP |
| GRID_RADIUS | float | 22f | HomeView.GRID_RADIUS |
| L_TITLE_BASE | float | 155f | HomeView.L_TITLE_BASE |
| L_TABBAR_TOP | float | 176f | HomeView.L_TABBAR_TOP |
| L_TABBAR_H | float | 63f | HomeView.L_TABBAR_H |
| L_CARDS_TOP | float | 280f | HomeView.L_CARDS_TOP |
| L_CARD_H | float | 420f | HomeView.L_CARD_H |
| L_TAB_BASE | float | 214f | HomeView.L_TAB_BASE |
| L_TAB_RULE | float | 239f | HomeView.L_TAB_RULE |
| L_TAB_GAP | float | 64f | HomeView.L_TAB_GAP |
| L_TALL_W | float | 328f | HomeView.L_TALL_W |
| L_CARD_GAP | float | 32f | HomeView.L_CARD_GAP |
| L_BUTTON | float | 72f | HomeView.L_BUTTON |
| L_BUTTON_GAP | float | 8f | HomeView.L_BUTTON_GAP |
| L_BUTTON_CY | float | 108f | HomeView.L_BUTTON_CY |
| L_PLATTER_TOP | float | 848f | HomeView.L_PLATTER_TOP |
| L_TILE_H | float | 120f | HomeView.L_TILE_H |
| L_DATE_BASE | float | 330f | HomeView.L_DATE_BASE |
| L_CLOCK_BASE | float | 540f | HomeView.L_CLOCK_BASE |
| L_WEATHER_BASE | float | 626f | HomeView.L_WEATHER_BASE |
| L_QUOTES_BASE | float | 694f | HomeView.L_QUOTES_BASE |
| SHOW_W | float | 138f | HomeView.SHOW_W |
| SHOW_H | float | 207f | HomeView.SHOW_H |
| SHOW_GAP | float | 21f | HomeView.SHOW_GAP |
| SHOW_LABEL_1 | float | 300f | HomeView.SHOW_LABEL_1 |
| SHOW_LABEL_2 | float | 588f | HomeView.SHOW_LABEL_2 |
| COLUMNS | int | 5 | HomeView grid |
| VISIBLE_ROWS | int | 3 | HomeView grid |
| SHOW_COLS | int | 11 | HomeView grid |
| DRAWER_GROUP_GAP | int | 48 | HomeView drawer metrics |
| WIDGET_LABEL_BASE | int | 28 | HomeView drawer metrics |
| WIDGET_RULE_Y | int | 53 | HomeView drawer metrics |
| TEMPERATURE_BASE | int | 231 | HomeView drawer metrics |
| WEATHER_FOOTER_BASE | int | 288 | HomeView drawer metrics |
| WEATHER_HOURS_X | int | 304 | HomeView drawer metrics |
| WEATHER_HOUR_BASE | int | 104 | HomeView drawer metrics |
| WEATHER_ICON_Y | int | 158 | HomeView drawer metrics |
| WEATHER_HOUR_TEMP_BASE | int | 228 | HomeView drawer metrics |
| CEC_INSET_X | int | 16 | HomeView drawer metrics |
| CEC_INSET_Y | int | 16 | HomeView drawer metrics |
| CEC_DOT_RADIUS | int | 6 | HomeView drawer metrics |

### Theme.Legacy.HomeMotion

| Token | Tipo | Valor atual | Consumidor |
|---|---|---|---|
| FOCUS_GROW | float | 0.2f | HomeView.FOCUS_GROW |
| TOAST_MS | long | 6000L | HomeView.TOAST_MS |
| FADE_MS | long | 800L | HomeView.FADE_MS |
| HOLD_MS | long | 500L | HomeView.HOLD_MS |
| WIGGLE_MS | long | 300L | HomeView.WIGGLE_MS |
| LIFT_GROW | float | 0.08f | HomeView.LIFT_GROW |
| WIGGLE_DEG | float | 1.5f | HomeView.WIGGLE_DEG |
| MOVE_LIFT_RESPONSE_S | float | 0.3f | HomeView.moveLift |
| MOVE_LIFT_DAMPING | float | 0.72f | HomeView.moveLift |
| HERO_FOCUS_RESPONSE_S | float | 0.28f | HomeView.heroFocus |
| HERO_FOCUS_DAMPING | float | 0.85f | HomeView.heroFocus |
| DRAWER_RESPONSE_S | float | 0.46f | HomeView.drawer |
| DRAWER_DAMPING | float | 0.92f | HomeView.drawer |
| SHEET_RESPONSE_S | float | 0.38f | HomeView.sheet |
| SHEET_DAMPING | float | 0.9f | HomeView.sheet |
| SCROLL_RESPONSE_S | float | 0.34f | HomeView.scroll |
| SCROLL_DAMPING | float | 1f | HomeView.scroll |
| TOP_FOCUS_RESPONSE_S | float | 0.26f | HomeView.topFocus |
| TOP_FOCUS_DAMPING | float | 0.85f | HomeView.topFocus |
| CARD_FOCUS_RESPONSE_S | float | 0.32f | HomeView.cardFocus |
| CARD_FOCUS_DAMPING | float | 0.78f | HomeView.cardFocus |
| STATUS_FOCUS_RESPONSE_S | float | 0.26f | HomeView.statusFocus |
| STATUS_FOCUS_DAMPING | float | 0.85f | HomeView.statusFocus |
| TILE_FOCUS_RESPONSE_S | float | 0.34f | HomeView.tileFocus |
| TILE_FOCUS_DAMPING | float | 0.74f | HomeView.tileFocus |
| TILE_SLIDE_RESPONSE_S | float | 0.32f | HomeView.tileSlide |
| TILE_SLIDE_DAMPING | float | 0.86f | HomeView.tileSlide |
| DRAWER_FOCUS_RESPONSE_S | float | 0.28f | HomeView.drawerFocus |
| DRAWER_FOCUS_DAMPING | float | 0.85f | HomeView.drawerFocus |
| GRID_FOCUS_RESPONSE_S | float | 0.3f | HomeView.gridFocus |
| GRID_FOCUS_DAMPING | float | 0.82f | HomeView.gridFocus |
| TOAST_ENTER_REDRAW_MS | int | 350 | HomeView.tickToast |
| TOAST_EXIT_REDRAW_MS | int | 450 | HomeView.tickToast |
| TOAST_ENTER_S | float | 0.3f | HomeView.drawToast |
| TOAST_EXIT_S | float | 0.4f | HomeView.drawToast |

### Theme.Legacy.HomeEffect

| Token | Tipo | Valor atual | Consumidor |
|---|---|---|---|
| SHADOW_BLUR | float | 26f | HomeView.SHADOW_BLUR |
| DEEP_BLUR | float | 30f | HomeView.DEEP_BLUR |
| SOFT_BLUR | int | 12 | HomeView.shadowSoft |
| HALO_BLUR | int | 22 | HomeView.halo |
| BORDER_STROKE | int | 2 | HomeView.border |
| RIM_STROKE | float | 2.5f | HomeView.rim |
| FOCUS_STROKE | int | 3 | HomeView.rim |
| GRID_FOCUS_STROKE | float | 1.5f | HomeView.rim |
| SPARK_STROKE | float | 2.5f | HomeView.sparkPaint |
| HERO_TEXT_SHADOW_BLUR | int | 22 | HomeView.makePaints |
| HERO_TEXT_SHADOW_DY | int | 2 | HomeView.makePaints |
| TOOLTIP_SHADOW_BLUR | int | 16 | HomeView.makePaints |
| TOOLTIP_SHADOW_DY | int | 1 | HomeView.makePaints |
| QUOTE_SHADOW_BLUR | int | 14 | HomeView.makePaints |
| QUOTE_SHADOW_DY | int | 1 | HomeView.makePaints |
| FOCUS_GAP | int | 8 | HomeView app/card/poster outline |
| FOCUS_HALF_STROKE | float | 1.5f | HomeView app/card/poster outline |
| GRID_SHADOW_ALPHA | float | 0.55f | HomeView.drawAppCard |
| GRID_VEIL_ALPHA | float | 0.08f | HomeView.drawAppCard |
| GRID_RIM_ALPHA | float | 0.4f | HomeView.drawAppCard |

### Theme.Legacy.Canvas

| Token | Tipo | Valor atual | Consumidor |
|---|---|---|---|
| WIDTH | float | 1920f | HomeView and SettingsView scaling |

### Theme.Legacy.Frame

| Token | Tipo | Valor atual | Consumidor |
|---|---|---|---|
| FPS | float | 60f | HomeView and SettingsView frame step |
| MAX_STEP_S | float | 0.05f | HomeView and SettingsView frame step |
| IDLE_MS | int | 1000 | HomeView.doFrame |

### Theme.Legacy.Font

| Token | Tipo | Valor atual | Consumidor |
|---|---|---|---|
| DISPLAY_OPTICAL_SIZE | int | 32 | Ui.init Inter opsz |
| THIN_WEIGHT | int | 200 | Ui.init Inter wght |
| LIGHT_WEIGHT | int | 300 | Ui.init Inter wght |
| MEDIUM_WEIGHT | int | 500 | Ui.init Inter wght |
| SEMIBOLD_WEIGHT | int | 600 | Ui.init Inter wght |
| TEXT_OPTICAL_SIZE | int | 14 | Ui.init Inter opsz |
| REGULAR_WEIGHT | int | 400 | Ui.init Inter wght |

### Theme.Legacy.SettingsColor

| Token | Tipo | Valor atual | Consumidor |
|---|---|---|---|
| AMBER | int | 0xFFFF7A12 | SettingsView.AMBER |
| INK | int | 0xFF000000 | SettingsView.INK |
| HAIRLINE | int | 0x8CFFFFFF | SettingsView.HAIRLINE |
| MUTED | int | 0xB8FFFFFF | SettingsView.MUTED |

### Theme.Legacy.SettingsText

| Token | Tipo | Valor atual | Consumidor |
|---|---|---|---|
| TITLE_SIZE | int | 92 | SettingsView.title |
| SUBTITLE_SIZE | int | 26 | SettingsView.subtitle |
| VERSION_SIZE | int | 22 | SettingsView.version |
| ROW_LABEL_SIZE | int | 28 | SettingsView.rowLabel |
| ROW_VALUE_SIZE | int | 26 | SettingsView.rowValue |
| ROW_SUB_SIZE | int | 23 | SettingsView.rowSub |
| HEADER_SIZE | int | 22 | SettingsView.header |
| CELL_LABEL_SIZE | int | 25 | SettingsView.cellLabel |
| CELL_VALUE_SIZE | int | 25 | SettingsView.cellValue |
| WARN_SIZE | int | 23 | SettingsView.warn |
| NOTICE_SIZE | int | 23 | SettingsView.notice |
| HEADING_SIZE | int | 34 | SettingsView.heading |
| HINT_SIZE | int | 23 | SettingsView.hint |
| TITLE_TRACKING | float | -0.05f | SettingsView.title |
| ROW_LABEL_TRACKING | float | -0.01f | SettingsView.rowLabel |
| HEADER_TRACKING | float | -0.01f | SettingsView.header |
| HEADING_TRACKING | float | -0.02f | SettingsView.heading |

### Theme.Legacy.SettingsLayout

| Token | Tipo | Valor atual | Consumidor |
|---|---|---|---|
| LIST_ROW_H | float | 68f | SettingsView.LIST_ROW_H |
| LIST_ROW_GAP | float | 0f | SettingsView.LIST_ROW_GAP |
| LIST_X | float | 1000f | SettingsView.LIST_X |
| LIST_R | float | 1824f | SettingsView.LIST_R |
| LIST_TOP | float | 186f | SettingsView.LIST_TOP |
| ROW_H | float | 74f | SettingsView.ROW_H |
| ROW_GAP | float | 0f | SettingsView.ROW_GAP |
| CHOICE_H | float | 104f | SettingsView.CHOICE_H |
| MARGIN | float | 96f | SettingsView.MARGIN |
| PICK_VISIBLE | int | 9 | SettingsView picker visible rows |
| TITLE_BASE | int | 470 | SettingsView layout |
| TITLE_RULE_RIGHT | int | 900 | SettingsView layout |
| TITLE_RULE_BASE | int | 500 | SettingsView layout |
| SUBTITLE_BASE | int | 548 | SettingsView layout |
| VERSION_BASE | int | 588 | SettingsView layout |
| ROW_PADDING_X | int | 30 | SettingsView layout |

### Theme.Legacy.SettingsMotion

| Token | Tipo | Valor atual | Consumidor |
|---|---|---|---|
| FOCUS_RESPONSE_S | float | 0.28f | SettingsView.pop |
| FOCUS_DAMPING | float | 0.85f | SettingsView.pop |
| FALLBACK_FRAME_MS | int | 100 | SettingsView background fallback |

### Theme.Legacy.SettingsEffect

| Token | Tipo | Valor atual | Consumidor |
|---|---|---|---|
| SPINNER_STROKE | float | 3.5f | SettingsView.spinner |
| SHADOW_BLUR | int | 22 | SettingsView.shadow |

## Verificação e instalação

Compilar sempre com JDK 17 e ./build.sh. tools/verify_theme_extraction.py é uma verificação histórica exclusiva da fase 1; deve falhar após mudanças visuais intencionais. Não atualizar sua baseline para fazer a fase 2 passar. As evidências da fase 2 ficam em design-review/phase2-verification.json.
O verificador compara todas as classes existentes e todos os assets/recursos com o build original. Para classes migradas, normaliza índices e alinhamento do constant pool no javap e a codificação equivalente ldc/ldc_w. Os destinos de salto e exceções são comparados por índice da instrução, preservando fluxo, valores e assinaturas. Os hashes binários originais foram preservados; a recanonicalização foi feita apenas sobre classes que coincidiam com esses hashes. Testes de sensibilidade confirmaram a detecção de alterações de constante e destino de salto. As únicas classes novas permitidas são Theme e suas classes internas.
Capturas da Home realizadas antes e depois da instalação autorizada, em design-review/screenshots. Para novas etapas, a captura de referência precisa vir da TV antes de qualquer instalação. Posicionar a tela com o controle remoto, capturar, pedir autorização para instalar, instalar o APK deste worktree e capturar novamente no mesmo estado.
Home: comparação concluída no estado Explore, John Candy selecionado. A região x=0..899, y=0..179 foi excluída por conter o relógio e a saudação que acompanha sua largura. As diferenças medidas ficaram restritas a x=289..800, y=63..163. Gaveta e ajustes: instruções compiladas equivalentes, validação visual pareada pendente. Não generalizar o resultado da Home para todos os estados.
Nunca instalar sem autorização expressa de Marcel, nem alterar configuração do sistema da TV. Não executar navegação por ADB. Não publicar APKs ou arquivos privados em commits ou relatórios.
