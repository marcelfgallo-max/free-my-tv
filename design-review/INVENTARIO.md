# Inventário visual, Sony BRAVIA

Data: 21/10/2026, Brasília. Branch de trabalho: `design-system`. Base: `origin/sony-bravia-tweaks`.

Status: auditoria e proposta somente. Nenhuma alteração de Java, assets, build ou comportamento. `Theme.java` e `DESIGN_SYSTEM.md` ainda não foram criados. Nenhum build, instalação, print da TV, commit ou push nesta etapa.

## Como ler

As dimensões de HomeView e SettingsActivity são unidades do canvas de 1920 × 1080, multiplicadas por `k = largura / 1920`. Não são dp nem sp. Valores iguais podem ter significados diferentes; a migração inicial deverá preservar essa distinção.

`LITERAIS-COM-LOCALIZACAO.csv` contém todas as linhas com literais numéricos nos 15 arquivos visuais e de suporte listados em BASELINE.json, com localização, trecho e categorias automáticas. Inclui parâmetros de algoritmos, IDs e limites que NÃO são tokens de design. Serve de rede de conferência, não de lista de substituição automática. A classificação semântica resumida abaixo foi conferida no código. As cores em strings têm tabela separada. Imagens de fundo e pôsteres não têm suas cores internas enumeradas como tokens.

Os quatro arquivos protegidos HomeMode.java, Adb.java, ModeJob.java e Watchdog.java não estão no escopo. CLAUDE.md e WISHLIST.md foram lidos e não foram alterados. config.properties e freemytv.keystore não foram lidos para este relatório.

## Constatações que afetam a migração

1. O visual ativo é Modern 80s. `night = 1` e `checkNight()` mantém o modo escuro, em HomeView:142 e 1000. Ativar Day é mudança de comportamento e deve acontecer só na segunda fase.
2. A home usa `fields/ember.png` e a gaveta/ajustes usam `fields/ultra.png`. Os dois bitmaps são decodificados fora da UI. Fundo liso só entra após provar equivalência da migração inicial.
3. `drawPlatter()` desenha duas linhas, não uma cápsula. `drawerCard()` está vazio. Apps, posters, entradas e botões ativos são retangulares, embora existam raios e rotinas antigas no arquivo.
4. HomeView conserva tokens e paints de caminhos antigos, de All Apps, toasts e estados sem recomendações. Não considerar todo raio ou sombra encontrado como efeito ativo no Explore.
5. SettingsActivity usa linhas encostadas, foco branco com texto preto e fundo azul em imagem. A tela de ajustes não tem mockup aprovado; a proposta limita-se a aplicar o sistema aos componentes existentes.
6. O ponto CEC, timer, Continue, Catalog, placar, fonte de dados, foco por controle remoto, tradução e acessibilidade precisam continuar funcionando. Não substituir dados vivos pelos números das prévias.

## Tabela do visual atual

| Grupo | Valores e expressões atuais | Local principal |
|---|---|---|
| Canvas e margens | 1920 × 1080; MARGIN 96; CONTENT_R 1824 | HomeView:65; SettingsActivity:122 |
| Explore | 11 colunas; pôster 138 × 207; gap 21; cabeçalhos nas baselines 300 e 588; início dos pôsteres baseline + 30 | HomeView:119-120; layoutShowcase |
| Apps | padding 28; gap 36; altura 120; largura máxima 270; topo 848; borda de foco 3; afastamento 8 + 1.5 | HomeView:72-73; layoutShelf; drawShelfTile |
| Cabeçalho | relógio baseline 155; tabs top 176 / altura 63 / baseline 214 / linha 239 / gap 64; botões 72, gap 8, centro y108; saudação após relógio +48 | HomeView:96-100; drawLightTop |
| Gaveta | deslocamento 604; cabeçalho 482; top 510; altura entradas 137; intervalo entre grupos 48; cards top 684 / altura 310 | HomeView:79; layoutDrawer |
| Clima | título +28; linha +53; temperatura baseline +231; H/L e sol +288; previsão começa em x+304; horas baseline +104, ícone +158, graus +228; separadores y+76 até +251 | drawWeather |
| CEC | círculo raio 6; inset 16; ciano, azul quando focado | drawDrawerTile |
| All Apps | 5 colunas; 3 linhas visíveis; item 300 × 169; gap40; passo vertical239; top196; raio22 | HomeView:81-82 |
| Ajustes | margem96; lista x1000 até1824; top186; linha principal74; listas68; escolha104; gap0; padding horizontal30 | SettingsActivity:118-122; rowBox; drawMain |
| Ajustes, bloco esquerdo | título baseline470; linha y500 atéx900; subtítulo548; versão588 | SettingsActivity:onDraw |
| Raios ativos | 0 nos apps, pôsteres, entradas e botões do Modern 80s; componentes antigos ainda têm 22 e raios calculados | drawShelfTile; drawShowcase; drawDrawerTile; drawLightButtons |
| Linhas | hairline max(1, round(k)); foco3 × k; rim2.5; borda2; sparkline2.5; spinner3.5 | makePaints; hairline; drawSpark; SettingsActivity:onSizeChanged |
| Sombras declaradas | blur26/30; soft12; halo22; texto22 com offset(0,2), alpha0x59; tooltip16 e quotes14 com offset(0,1), alpha0x99; ajustes blur22 | HomeView:91,628,654,657,682-685; SettingsActivity:333 |
| Fonte | InterVariable.ttf; pesos200/300/400/500/600; eixo óptico14 para texto e32 para display | Ui:47-53 |
| Tracking home | -0.055, -0.05, -0.035, -0.03, -0.02, -0.01, +0.005, +0.014, +0.1 em | makePaints |
| Tracking ajustes | -0.05, -0.02, -0.01 em | SettingsActivity:onSizeChanged |
| Toast | permanência6000ms; alpha entra300ms/sai400ms; janela de redraw350/450ms | HomeView:89; tickToast; drawToast |
| Imagem/hero | crossfade800ms | HomeView:89; fadeEase |
| Reordenação | segurar500ms; wiggle300ms, 1.5°; lift+8% | HomeView:201-202 |
| Render | dt limitado50ms; frame nominal1/60s; idle1000ms; fallback dos ajustes atualiza100ms | HomeView:794,844; SettingsActivity:290,400,413 |

## Tipografia atual

| Uso em HomeView | Tamanho no canvas | Peso/família |
|---|---:|---|
| Relógio principal |124|Inter Display500|
| Saudação |40|Inter400|
| Abas |28|Inter Display500|
| Título de card |34|Inter Display500|
| Metadata |23|Inter400|
| Cabeçalhos de seção |26|Inter Display500|
| Rótulo de entrada |21, reduz até17 se não couber|Inter Display500|
| Mercado, nome e preço |31|Inter Display500|
| Variação |25|Inter Display500|
| Texto secundário da gaveta |22|Inter400|
| Hora da previsão |24|Inter400|
| Temperatura horária |38|Inter Display500|
| Temperatura grande / grau |184 / 84|Inter Display500|
| Placar / próxima partida / tag |26 / 21 / 21|Inter500 / 400 / 400|
| Badge |20|Inter600|
| Outros paints e caminhos herdados |20,21,22,23,24,25,26,28,29,31,34,38,40,76,84,100,124,176,184,200|Ver ocorrências abaixo|

| Uso em SettingsActivity | Tamanho | Peso/família |
|---|---:|---|
| Título |92|Inter Display500|
| Subtítulo e valor da linha |26|Inter400|
| Versão / header |22|Inter400 / Display500|
| Rótulo principal |28|Inter Display500|
| Subtexto, aviso e hint |23|Inter400|
| Célula e valor |25|Inter400|
| Título da seção |34|Inter Display500|

## Springs atuais

Resposta em segundos e damping são parâmetros físicos, não duração exata da animação.

| Uso | Resposta | Damping |
|---|---:|---:|
| Abas e botões superiores |0.26|0.85|
| Cards |0.32|0.78|
| Apps |0.34|0.74|
| Deslizamento de apps |0.32|0.86|
| Lift de reordenação |0.30|0.72|
| Hero, entradas e linhas de ajustes |0.28|0.85|
| Gaveta |0.46|0.92|
| All Apps |0.38|0.90|
| Scroll |0.34|1.00|
| Grade All Apps |0.30|0.82|

Spring subdivide dt em passos de até0.004s; termina com distância<0.0008 e velocidade<0.004. Esses limiares são do algoritmo e ficam fora da primeira migração de estilo.

## Cores literais e alpha

Formato de código Android: 0xAARRGGBB. A tabela contém também cores herdadas e máscaras opacas; não significa que todas estejam visíveis hoje. Cores dos logos de terceiros devem ser diferenciadas das cores semânticas da interface.

| Literal | RGB | Alpha | Ocorrências (arquivo:linha) |
|---|---|---:|---|
| `0x00000000` | `#000000` | 0.000 | HomeApps:839 |
| `0x00FFFFFF` | `#FFFFFF` | 0.000 | HomeView:1938 |
| `0x14FFFFFF` | `#FFFFFF` | 0.078 | HomeView:1955, HomeView:2966, HomeView:3025 |
| `0x1AFFFFFF` | `#FFFFFF` | 0.102 | HomeView:1938 |
| `0x33FFFFFF` | `#FFFFFF` | 0.200 | Scene:62 |
| `0x59000000` | `#000000` | 0.349 | HomeView:628, SettingsActivity:980 |
| `0x59FFFFFF` | `#FFFFFF` | 0.349 | SettingsActivity:980 |
| `0x73000000` | `#000000` | 0.451 | SettingsActivity:981 |
| `0x73FFFFFF` | `#FFFFFF` | 0.451 | HomeView:1589, SettingsActivity:981 |
| `0x80000000` | `#000000` | 0.502 | HomeView:3113, SettingsActivity:916 |
| `0x80FFFFFF` | `#FFFFFF` | 0.502 | SettingsActivity:916, DreamView:232, DreamView:652 |
| `0x8CFFFFFF` | `#FFFFFF` | 0.549 | HomeView:87, SettingsActivity:124, SettingsActivity:306, DreamView:49 |
| `0x99000000` | `#000000` | 0.600 | HomeView:654, HomeView:657 |
| `0x99FFFFFF` | `#FFFFFF` | 0.600 | HomeView:658, HomeView:666, HomeView:679, HomeView:1037, HomeView:1916 |
| `0xA8000000` | `#000000` | 0.659 | HomeView:1894 |
| `0xADFFFFFF` | `#FFFFFF` | 0.678 | HomeView:665, HomeView:669 |
| `0xB3000000` | `#000000` | 0.702 | SettingsActivity:569 |
| `0xB3FFFFFF` | `#FFFFFF` | 0.702 | HomeView:660, HomeView:663, HomeView:1704 |
| `0xB8FFFFFF` | `#FFFFFF` | 0.722 | HomeView:87, HomeView:656, SettingsActivity:124, DreamView:49 |
| `0xC7FFFFFF` | `#FFFFFF` | 0.780 | HomeView:620 |
| `0xD1FFFFFF` | `#FFFFFF` | 0.820 | HomeView:624 |
| `0xE6FFFFFF` | `#FFFFFF` | 0.902 | SettingsActivity:314, SettingsActivity:315, SettingsActivity:527 |
| `0xEBFFFFFF` | `#FFFFFF` | 0.922 | HomeView:661, HomeView:2005 |
| `0xF2FFFFFF` | `#FFFFFF` | 0.949 | HomeView:651, HomeView:652, HomeView:664, HomeView:671, HomeView:673, HomeView:681, SettingsActivity:440, HomeApps:828 |
| `0xFF000000` | `#000000` | 1.000 | HomeView:184, HomeView:190, HomeView:1062, HomeView:1589, HomeView:1931, HomeView:1952, HomeView:2849, HomeView:2894, HomeView:3139, SettingsActivity:124, Frost:50, Backdrop:166, Backdrop:177, Backdrop:217, Shadows:74, Scene:257, HomeApps:144, DreamView:134, Shaders:217 |
| `0xFF08143F` | `#08143F` | 1.000 | HomeView:188 |
| `0xFF09090A` | `#09090A` | 1.000 | HomeView:185 |
| `0xFF0A0A0A` | `#0A0A0A` | 1.000 | HomeView:184 |
| `0xFF0A68D4` | `#0A68D4` | 1.000 | HomeView:187 |
| `0xFF0B12E6` | `#0B12E6` | 1.000 | HomeView:1601 |
| `0xFF111113` | `#111113` | 1.000 | HomeView:84 |
| `0xFF141416` | `#141416` | 1.000 | HomeView:992 |
| `0xFF15161C` | `#15161C` | 1.000 | HomeApps:544, HomeApps:775 |
| `0xFF161618` | `#161618` | 1.000 | HomeView:1353 |
| `0xFF17191F` | `#17191F` | 1.000 | HomeApps:853 |
| `0xFF173584` | `#173584` | 1.000 | HomeView:188 |
| `0xFF1C1C1E` | `#1C1C1E` | 1.000 | HomeView:84, HomeView:185, HomeView:190 |
| `0xFF1D1D1F` | `#1D1D1F` | 1.000 | HomeView:103 |
| `0xFF221684` | `#221684` | 1.000 | HomeView:189 |
| `0xFF2AA8FF` | `#2AA8FF` | 1.000 | HomeView:187 |
| `0xFF2B2E3A` | `#2B2E3A` | 1.000 | HomeApps:853 |
| `0xFF30D158` | `#30D158` | 1.000 | HomeView:85 |
| `0xFF5C30E8` | `#5C30E8` | 1.000 | HomeView:189 |
| `0xFF5EDCFF` | `#5EDCFF` | 1.000 | HomeView:88, DreamView:249 |
| `0xFF6E6E73` | `#6E6E73` | 1.000 | HomeView:103 |
| `0xFF8E8E93` | `#8E8E93` | 1.000 | HomeView:103 |
| `0xFF98989D` | `#98989D` | 1.000 | HomeView:1033, HomeView:1035, HomeView:1036 |
| `0xFFEBEBF0` | `#EBEBF0` | 1.000 | HomeView:992 |
| `0xFFF5F5F7` | `#F5F5F7` | 1.000 | HomeView:84 |
| `0xFFFF2D5A` | `#FF2D5A` | 1.000 | HomeView:186 |
| `0xFFFF453A` | `#FF453A` | 1.000 | HomeView:85 |
| `0xFFFF5A3C` | `#FF5A3C` | 1.000 | HomeView:88 |
| `0xFFFF7A12` | `#FF7A12` | 1.000 | SettingsActivity:124 |
| `0xFFFF8A1F` | `#FF8A1F` | 1.000 | HomeView:186 |
| `0xFFFFFFFF` | `#FFFFFF` | 1.000 | HomeView:87, HomeView:193, HomeView:622, HomeView:626, HomeView:641, HomeView:668, HomeView:675, HomeView:677, HomeView:1540, HomeView:1916, HomeView:1959, SettingsActivity:303, SettingsActivity:307, SettingsActivity:312, SettingsActivity:319, SettingsActivity:331, SettingsActivity:455, SettingsActivity:568, SettingsActivity:574, SettingsActivity:916, SettingsActivity:955, SettingsActivity:959, SettingsActivity:978, HomeApps:680, DreamView:49 |

## Cores RGB em strings

| Arquivo:linha | Declaração |
|---|---|
| HomeApps:84 | `{"netflix", "Netflix", "com.netflix.ninja", "E50914", "", "1"},` |
| HomeApps:85 | `{"youtube", "YouTube", "com.google.android.youtube.tv", "FF0033", "play", "1"},` |
| HomeApps:86 | `{"prime", "Prime Video", "com.amazon.amazonvideo.livingroom", "1A98FF", "tint", "0.96"},` |
| HomeApps:87 | `{"disney", "Disney+", "com.disney.disneyplus", "1838C8", "", "1"},` |
| HomeApps:88 | `{"max", "Max", "com.wbd.stream", "6C3BE0", "", "1.32"},` |
| HomeApps:89 | `{"appletv", "Apple TV", "com.apple.atve.androidtv.appletv", "B8BCC6", "", "1.1"},` |
| HomeApps:90 | `{"hulu", "Hulu", "com.hulu.livingroomplus", "1CE783", "", "1"},` |
| HomeApps:91 | `{"paramount", "Paramount+", "com.cbs.ott", "0064FF", "", "1"},` |
| HomeApps:92 | `{"peacock", "Peacock", "com.peacocktv.peacockandroid", "6E55DC", "", "1"},` |
| HomeApps:93 | `{"globoplay", "Globoplay", "com.globo.globotv", "FF5A1F", "tint", "1"},` |
| HomeApps:94 | `{"spotify", "Spotify", "com.spotify.tv.android", "1DB954", "", "1"},` |

## Ocorrências por categoria

As expressões abaixo são a evidência completa para os 15 arquivos inspecionados. Contadores e parâmetros algorítmicos permanecem no CSV, sem indicação de migração. Valores calculados conservam a expressão original.

### tipografia

| Arquivo:linha | Expressão original |
|---|---|
| HomeView:620 | `labelPaint = Ui.text(Ui.medium, 25 * k, 0xC7FFFFFF);` |
| HomeView:621 | `labelPaint.setLetterSpacing(0.1f);` |
| HomeView:622 | `titlePaint = new TextPaint(Ui.text(Ui.displayMedium, 76 * k, 0xFFFFFFFF));` |
| HomeView:623 | `titlePaint.setLetterSpacing(0.005f);` |
| HomeView:624 | `metaPaint = Ui.text(Ui.medium, 29 * k, 0xD1FFFFFF);` |
| HomeView:625 | `metaPaint.setLetterSpacing(0.014f);` |
| HomeView:626 | `clockPaint = Ui.numbers(Ui.thin, 176 * k, 0xFFFFFFFF);` |
| HomeView:627 | `clockPaint.setLetterSpacing(-0.01f);` |
| HomeView:630 | `lTitle = Ui.text(Ui.displayMedium, 124 * k, M_INK);` |
| HomeView:631 | `lTitle.setLetterSpacing(-0.05f);` |
| HomeView:632 | `lGreeting = Ui.text(Ui.regular, 40 * k, M_MUTED);` |
| HomeView:633 | `lGreeting.setLetterSpacing(-0.02f);` |
| HomeView:634 | `lTab = Ui.text(Ui.displayMedium, 28 * k, M_MUTED);` |
| HomeView:635 | `lTab.setLetterSpacing(-0.01f);` |
| HomeView:636 | `lTabOn = Ui.text(Ui.displayMedium, 28 * k, M_INK);` |
| HomeView:637 | `lTabOn.setLetterSpacing(-0.01f);` |
| HomeView:638 | `lCardTitle = Ui.text(Ui.displayMedium, 34 * k, M_INK);` |
| HomeView:639 | `lCardTitle.setLetterSpacing(-0.03f);` |
| HomeView:640 | `lCardMeta = Ui.text(Ui.regular, 23 * k, M_MUTED);` |
| HomeView:641 | `badgePaint = Ui.text(Ui.semibold, 20 * k, 0xFFFFFFFF);` |
| HomeView:642 | `lQuote = Ui.numbers(Ui.regular, 24 * k, M_INK);` |
| HomeView:643 | `lTip = Ui.text(Ui.displayMedium, 22 * k, M_INK);` |
| HomeView:645 | `lDate = Ui.text(Ui.semibold, 34 * k, L_DIM);` |
| HomeView:646 | `lClock = Ui.numbers(Ui.light, 200 * k, L_INK);` |
| HomeView:647 | `lClock.setLetterSpacing(-0.01f);` |
| HomeView:648 | `lWeather = Ui.text(Ui.medium, 34 * k, L_DIM);` |
| HomeView:649 | `lClockQuote = Ui.numbers(Ui.medium, 28 * k, L_DIM);` |
| HomeView:650 | `buttonPaint = Ui.text(Ui.semibold, 29 * k, INK);` |
| HomeView:651 | `capsuleText = Ui.numbers(Ui.medium, 29 * k, 0xF2FFFFFF);` |
| HomeView:652 | `tipPaint = Ui.text(Ui.semibold, 25 * k, 0xF2FFFFFF);` |
| HomeView:656 | `quotePaint = Ui.numbers(Ui.regular, 21 * k, 0xB8FFFFFF);` |
| HomeView:658 | `staleQuote = Ui.text(Ui.medium, 21 * k, 0x99FFFFFF);` |
| HomeView:660 | `rowTitle = Ui.text(Ui.semibold, 25 * k, 0xB3FFFFFF);` |
| HomeView:661 | `drawerLabel = Ui.text(Ui.medium, 23 * k, 0xEBFFFFFF);` |
| HomeView:663 | `cardTitle = Ui.text(Ui.semibold, 25 * k, 0xB3FFFFFF);` |
| HomeView:664 | `cardText = Ui.text(Ui.medium, 29 * k, 0xF2FFFFFF);` |
| HomeView:665 | `cardDim = Ui.numbers(Ui.medium, 23 * k, 0xADFFFFFF);` |
| HomeView:666 | `cardDimRight = Ui.text(Ui.medium, 21 * k, 0x99FFFFFF);` |
| HomeView:668 | `bigTemp = Ui.numbers(Ui.light, 100 * k, 0xFFFFFFFF);` |
| HomeView:669 | `hourPaint = Ui.numbers(Ui.medium, 22 * k, 0xADFFFFFF);` |
| HomeView:671 | `hourTemp = Ui.numbers(Ui.medium, 26 * k, 0xF2FFFFFF);` |
| HomeView:673 | `pricePaint = Ui.numbers(Ui.medium, 26 * k, 0xF2FFFFFF);` |
| HomeView:675 | `changePaint = Ui.numbers(Ui.semibold, 21 * k, 0xFFFFFFFF);` |
| HomeView:677 | `sheetTitle = Ui.text(Ui.displaySemibold, 40 * k, 0xFFFFFFFF);` |
| HomeView:679 | `gridLabel = new TextPaint(Ui.text(Ui.medium, 24 * k, 0x99FFFFFF));` |
| HomeView:681 | `toastPaint = new TextPaint(Ui.text(Ui.medium, 26 * k, 0xF2FFFFFF));` |
| HomeView:691 | `mLabel = Ui.text(Ui.displayMedium, 26 * k, M_INK);` |
| HomeView:692 | `mLabel.setLetterSpacing(-0.01f);` |
| HomeView:695 | `mCell = Ui.text(Ui.displayMedium, 21 * k, M_INK);` |
| HomeView:696 | `mCell.setLetterSpacing(-0.01f);` |
| HomeView:698 | `mName = Ui.text(Ui.displayMedium, 31 * k, M_INK);` |
| HomeView:699 | `mName.setLetterSpacing(-0.02f);` |
| HomeView:700 | `mPrice = Ui.numbers(Ui.displayMedium, 31 * k, M_INK);` |
| HomeView:701 | `mPrice.setLetterSpacing(-0.02f);` |
| HomeView:703 | `mChange = Ui.numbers(Ui.displayMedium, 25 * k, M_UP);` |
| HomeView:704 | `mChange.setLetterSpacing(-0.01f);` |
| HomeView:706 | `mMuted = Ui.numbers(Ui.regular, 22 * k, M_MUTED);` |
| HomeView:709 | `mHour = Ui.numbers(Ui.regular, 24 * k, M_MUTED);` |
| HomeView:711 | `mHourTemp = Ui.numbers(Ui.displayMedium, 38 * k, M_INK);` |
| HomeView:712 | `mHourTemp.setLetterSpacing(-0.035f);` |
| HomeView:714 | `mBig = Ui.text(Ui.displayMedium, 184 * k, M_INK);   // proportional digits: a "1" isn't as wide as an "8"` |
| HomeView:715 | `mBig.setLetterSpacing(-0.055f);` |
| HomeView:716 | `mBigDegree = Ui.text(Ui.displayMedium, 84 * k, M_INK);` |
| HomeView:717 | `teamBig = Ui.text(Ui.displayMedium, 26 * k, M_INK);` |
| HomeView:718 | `teamBig.setLetterSpacing(-0.01f);` |
| HomeView:719 | `teamSmall = Ui.numbers(Ui.regular, 21 * k, M_MUTED);` |
| HomeView:720 | `teamTag = Ui.text(Ui.regular, 21 * k, M_UP);` |
| HomeView:1595 | `if (wide > room) mCell.setTextSize(Math.max(17 * k, 21 * k * room / wide));` |
| HomeView:1597 | `mCell.setTextSize(21 * k);` |
| SettingsActivity:303 | `title = Ui.text(Ui.displayMedium, 92 * k, 0xFFFFFFFF);` |
| SettingsActivity:304 | `title.setLetterSpacing(-0.05f);` |
| SettingsActivity:305 | `subtitle = Ui.text(Ui.regular, 26 * k, MUTED);` |
| SettingsActivity:306 | `version = Ui.text(Ui.regular, 22 * k, 0x8CFFFFFF);` |
| SettingsActivity:307 | `rowLabel = Ui.text(Ui.displayMedium, 28 * k, 0xFFFFFFFF);` |
| SettingsActivity:308 | `rowLabel.setLetterSpacing(-0.01f);` |
| SettingsActivity:309 | `rowValue = Ui.text(Ui.regular, 26 * k, MUTED);` |
| SettingsActivity:311 | `rowSub = Ui.text(Ui.regular, 23 * k, MUTED);` |
| SettingsActivity:312 | `header = Ui.text(Ui.displayMedium, 22 * k, 0xFFFFFFFF);` |
| SettingsActivity:313 | `header.setLetterSpacing(-0.01f);` |
| SettingsActivity:314 | `cellLabel = Ui.text(Ui.regular, 25 * k, 0xE6FFFFFF);` |
| SettingsActivity:315 | `cellValue = Ui.numbers(Ui.regular, 25 * k, 0xE6FFFFFF);` |
| SettingsActivity:317 | `warn = Ui.text(Ui.regular, 23 * k, AMBER);` |
| SettingsActivity:318 | `notice = new TextPaint(Ui.text(Ui.regular, 23 * k, MUTED));` |
| SettingsActivity:319 | `heading = Ui.text(Ui.displayMedium, 34 * k, 0xFFFFFFFF);` |
| SettingsActivity:320 | `heading.setLetterSpacing(-0.02f);` |
| SettingsActivity:321 | `hint = Ui.text(Ui.regular, 23 * k, MUTED);` |
| Ui:47 | `thin = font(a, 32, 200);` |
| Ui:48 | `light = font(a, 32, 300);` |
| Ui:49 | `displayMedium = font(a, 32, 500);` |
| Ui:50 | `displaySemibold = font(a, 32, 600);` |
| Ui:51 | `regular = font(a, 14, 400);` |
| Ui:52 | `medium = font(a, 14, 500);` |
| Ui:53 | `semibold = font(a, 14, 600);` |
| DreamView:223 | `datePaint = Ui.text(Ui.displayMedium, 30 * k, INK);` |
| DreamView:224 | `datePaint.setLetterSpacing(-0.01f);` |
| DreamView:226 | `clockPaint = Ui.text(Ui.displayMedium, 300 * k, INK);` |
| DreamView:228 | `clockPaint.setLetterSpacing(-0.05f);` |
| DreamView:229 | `weatherPaint = Ui.numbers(Ui.displayMedium, 30 * k, INK);` |
| DreamView:230 | `weatherDim = Ui.numbers(Ui.regular, 28 * k, MUTED);` |
| DreamView:231 | `quotePaint = Ui.numbers(Ui.regular, 24 * k, MUTED);` |
| DreamView:232 | `stalePaint = Ui.text(Ui.regular, 20 * k, 0x80FFFFFF);` |
| DreamView:235 | `cornerDate = Ui.numbers(Ui.displayMedium, 26 * k, INK);` |
| DreamView:236 | `cornerDate.setLetterSpacing(-0.01f);` |
| DreamView:237 | `cornerQuote = Ui.numbers(Ui.regular, 24 * k, INK);` |
| DreamView:238 | `cornerStale = Ui.text(Ui.regular, 21 * k, MUTED);` |
| DreamView:240 | `cornerClock.setLetterSpacing(-0.05f);` |
| DreamView:241 | `labelPaint = Ui.text(Ui.displayMedium, 26 * k, INK);` |
| DreamView:242 | `labelPaint.setLetterSpacing(-0.01f);` |
| DreamView:243 | `titlePaint = new TextPaint(Ui.text(Ui.displayMedium, 76 * k, INK));` |
| DreamView:244 | `titlePaint.setLetterSpacing(-0.035f);` |
| DreamView:245 | `metaPaint = Ui.text(Ui.regular, 26 * k, MUTED);` |
| DreamView:246 | `teamBig = Ui.text(Ui.displayMedium, 26 * k, INK);` |
| DreamView:247 | `teamBig.setLetterSpacing(-0.01f);` |
| DreamView:248 | `teamSmall = Ui.numbers(Ui.regular, 21 * k, MUTED);` |
| DreamView:249 | `teamTag = Ui.text(Ui.regular, 21 * k, 0xFF5EDCFF);` |

### geometria/constante

| Arquivo:linha | Expressão original |
|---|---|
| HomeView:66 | `private static final float MARGIN = 96, CONTENT_R = 1824;` |
| HomeView:68 | `private static final float HERO_TITLE_W = 900;` |
| HomeView:72 | `private static final float SHELF_PAD = 28, TILE_GAP = 36;` |
| HomeView:73 | `private static final float TILE_RADIUS = 22, TILE_MAX_W = 270;` |
| HomeView:75 | `private static final float FOCUS_GROW = 0.2f;` |
| HomeView:78 | `private static final float CAPSULE_TOP = 46, CAPSULE_H = 68, BUTTON = 56;` |
| HomeView:80 | `private static final float SHIFT = 604, ROW_TITLE = 482, ROW_TOP = 510, ROW_H = 137, CARDS_TOP = 684, CARDS_H = 310;` |
| HomeView:82 | `private static final int COLUMNS = 5, VISIBLE_ROWS = 3;` |
| HomeView:83 | `private static final float GRID_W = 300, GRID_H = 169, GRID_GAP = 40, GRID_ROW = 239, GRID_TOP = 196, GRID_RADIUS = 22;` |
| HomeView:84 | `private static final int SMOKE = 0xFF111113, INK = 0xFF1C1C1E, WHITE = 0xFFF5F5F7;` |
| HomeView:85 | `private static final int UP_COLOR = 0xFF30D158, DOWN_COLOR = 0xFFFF453A;` |
| HomeView:87 | `private static final int M_INK = 0xFFFFFFFF, M_MUTED = 0xB8FFFFFF, M_HAIRLINE = 0x8CFFFFFF;` |
| HomeView:88 | `private static final int M_UP = 0xFF5EDCFF, M_DOWN = 0xFFFF5A3C;` |
| HomeView:91 | `private static final float SHADOW_BLUR = 26, DEEP_BLUR = 30;` |
| HomeView:92 | `private static final int SKY = 0, HERO = 1, STATUS = 2, PLATTER = 3, TILES = 4, DRAWER = 5, SHEET = 6, TOAST = 7;` |
| HomeView:93 | `private static final int Z_STATUS = 0, Z_HERO = 1, Z_SHELF = 2, Z_DRAWER = 3;` |
| HomeView:97 | `private static final float L_TITLE_BASE = 155, L_TABBAR_TOP = 176, L_TABBAR_H = 63, L_CARDS_TOP = 280, L_CARD_H = 420;` |
| HomeView:99 | `private static final float L_TAB_BASE = 214, L_TAB_RULE = 239, L_TAB_GAP = 64;` |
| HomeView:100 | `private static final float L_TALL_W = 328, L_CARD_GAP = 32, L_BUTTON = 72, L_BUTTON_GAP = 8, L_BUTTON_CY = 108;` |
| HomeView:101 | `private static final float L_PLATTER_TOP = 848, L_TILE_H = 120;` |
| HomeView:102 | `private static final float L_DATE_BASE = 330, L_CLOCK_BASE = 540, L_WEATHER_BASE = 626, L_QUOTES_BASE = 694;` |
| HomeView:103 | `private static final int L_INK = 0xFF1D1D1F, L_DIM = 0xFF6E6E73, L_FAINT = 0xFF8E8E93;` |
| HomeView:111 | `private static final int TAB_COUNT = TABS.length, NEXT_TAB = 3, CATALOG_TAB = 4;` |
| HomeView:112 | `private static final int Z_TOP = 4, Z_CARDS = 5;` |
| HomeView:114 | `private static final int SHOW_COLS = 11;` |
| HomeView:115 | `private static final float SHOW_W = 138, SHOW_H = 207, SHOW_GAP = 21, SHOW_LABEL_1 = 300, SHOW_LABEL_2 = 588;` |
| HomeView:200 | `private static final float LIFT_GROW = 0.08f, WIGGLE_DEG = 1.5f;` |
| HomeView:602 | `screen.set(0, 0, w, h);` |
| HomeView:620 | `labelPaint = Ui.text(Ui.medium, 25 * k, 0xC7FFFFFF);` |
| HomeView:622 | `titlePaint = new TextPaint(Ui.text(Ui.displayMedium, 76 * k, 0xFFFFFFFF));` |
| HomeView:624 | `metaPaint = Ui.text(Ui.medium, 29 * k, 0xD1FFFFFF);` |
| HomeView:626 | `clockPaint = Ui.numbers(Ui.thin, 176 * k, 0xFFFFFFFF);` |
| HomeView:628 | `for (Paint p : new Paint[] {labelPaint, titlePaint, metaPaint, clockPaint}) p.setShadowLayer(22 * k, 0, 2 * k, 0x59000000);` |
| HomeView:630 | `lTitle = Ui.text(Ui.displayMedium, 124 * k, M_INK);` |
| HomeView:632 | `lGreeting = Ui.text(Ui.regular, 40 * k, M_MUTED);` |
| HomeView:634 | `lTab = Ui.text(Ui.displayMedium, 28 * k, M_MUTED);` |
| HomeView:636 | `lTabOn = Ui.text(Ui.displayMedium, 28 * k, M_INK);` |
| HomeView:638 | `lCardTitle = Ui.text(Ui.displayMedium, 34 * k, M_INK);` |
| HomeView:640 | `lCardMeta = Ui.text(Ui.regular, 23 * k, M_MUTED);` |
| HomeView:641 | `badgePaint = Ui.text(Ui.semibold, 20 * k, 0xFFFFFFFF);` |
| HomeView:642 | `lQuote = Ui.numbers(Ui.regular, 24 * k, M_INK);` |
| HomeView:643 | `lTip = Ui.text(Ui.displayMedium, 22 * k, M_INK);` |
| HomeView:645 | `lDate = Ui.text(Ui.semibold, 34 * k, L_DIM);` |
| HomeView:646 | `lClock = Ui.numbers(Ui.light, 200 * k, L_INK);` |
| HomeView:648 | `lWeather = Ui.text(Ui.medium, 34 * k, L_DIM);` |
| HomeView:649 | `lClockQuote = Ui.numbers(Ui.medium, 28 * k, L_DIM);` |
| HomeView:650 | `buttonPaint = Ui.text(Ui.semibold, 29 * k, INK);` |
| HomeView:651 | `capsuleText = Ui.numbers(Ui.medium, 29 * k, 0xF2FFFFFF);` |
| HomeView:652 | `tipPaint = Ui.text(Ui.semibold, 25 * k, 0xF2FFFFFF);` |
| HomeView:654 | `tipPaint.setShadowLayer(16 * k, 0, 1 * k, 0x99000000);` |
| HomeView:656 | `quotePaint = Ui.numbers(Ui.regular, 21 * k, 0xB8FFFFFF);` |
| HomeView:657 | `quotePaint.setShadowLayer(14 * k, 0, 1 * k, 0x99000000);` |
| HomeView:658 | `staleQuote = Ui.text(Ui.medium, 21 * k, 0x99FFFFFF);` |
| HomeView:660 | `rowTitle = Ui.text(Ui.semibold, 25 * k, 0xB3FFFFFF);` |
| HomeView:661 | `drawerLabel = Ui.text(Ui.medium, 23 * k, 0xEBFFFFFF);` |
| HomeView:663 | `cardTitle = Ui.text(Ui.semibold, 25 * k, 0xB3FFFFFF);` |
| HomeView:664 | `cardText = Ui.text(Ui.medium, 29 * k, 0xF2FFFFFF);` |
| HomeView:665 | `cardDim = Ui.numbers(Ui.medium, 23 * k, 0xADFFFFFF);` |
| HomeView:666 | `cardDimRight = Ui.text(Ui.medium, 21 * k, 0x99FFFFFF);` |
| HomeView:668 | `bigTemp = Ui.numbers(Ui.light, 100 * k, 0xFFFFFFFF);` |
| HomeView:669 | `hourPaint = Ui.numbers(Ui.medium, 22 * k, 0xADFFFFFF);` |
| HomeView:671 | `hourTemp = Ui.numbers(Ui.medium, 26 * k, 0xF2FFFFFF);` |
| HomeView:673 | `pricePaint = Ui.numbers(Ui.medium, 26 * k, 0xF2FFFFFF);` |
| HomeView:675 | `changePaint = Ui.numbers(Ui.semibold, 21 * k, 0xFFFFFFFF);` |
| HomeView:677 | `sheetTitle = Ui.text(Ui.displaySemibold, 40 * k, 0xFFFFFFFF);` |
| HomeView:679 | `gridLabel = new TextPaint(Ui.text(Ui.medium, 24 * k, 0x99FFFFFF));` |
| HomeView:681 | `toastPaint = new TextPaint(Ui.text(Ui.medium, 26 * k, 0xF2FFFFFF));` |
| HomeView:683 | `shadowSoft.setMaskFilter(new BlurMaskFilter(12 * k, BlurMaskFilter.Blur.NORMAL));` |
| HomeView:685 | `halo.setMaskFilter(new BlurMaskFilter(22 * k, BlurMaskFilter.Blur.NORMAL));` |
| HomeView:687 | `border.setStrokeWidth(2 * k);` |
| HomeView:689 | `rim.setStrokeWidth(2.5f * k);` |
| HomeView:691 | `mLabel = Ui.text(Ui.displayMedium, 26 * k, M_INK);` |
| HomeView:695 | `mCell = Ui.text(Ui.displayMedium, 21 * k, M_INK);` |
| HomeView:698 | `mName = Ui.text(Ui.displayMedium, 31 * k, M_INK);` |
| HomeView:700 | `mPrice = Ui.numbers(Ui.displayMedium, 31 * k, M_INK);` |
| HomeView:703 | `mChange = Ui.numbers(Ui.displayMedium, 25 * k, M_UP);` |
| HomeView:706 | `mMuted = Ui.numbers(Ui.regular, 22 * k, M_MUTED);` |
| HomeView:709 | `mHour = Ui.numbers(Ui.regular, 24 * k, M_MUTED);` |
| HomeView:711 | `mHourTemp = Ui.numbers(Ui.displayMedium, 38 * k, M_INK);` |
| HomeView:714 | `mBig = Ui.text(Ui.displayMedium, 184 * k, M_INK);   // proportional digits: a "1" isn't as wide as an "8"` |
| HomeView:716 | `mBigDegree = Ui.text(Ui.displayMedium, 84 * k, M_INK);` |
| HomeView:717 | `teamBig = Ui.text(Ui.displayMedium, 26 * k, M_INK);` |
| HomeView:719 | `teamSmall = Ui.numbers(Ui.regular, 21 * k, M_MUTED);` |
| HomeView:720 | `teamTag = Ui.text(Ui.regular, 21 * k, M_UP);` |
| HomeView:771 | `return Math.round(L_TAB_RULE * k) + Math.max(1, Math.round(k));` |
| HomeView:779 | `c.drawRect(Math.round(x0), y, Math.round(x1), y + Math.max(1, Math.round(k)), fill);` |
| HomeView:787 | `c.drawRect(x, Math.round(y0), x + Math.max(1, Math.round(k)), Math.round(y1), fill);` |
| HomeView:965 | `h.titleLayout = StaticLayout.Builder.obtain(h.title, 0, h.title.length(), titlePaint, Math.max(1, Math.round(HERO_TITLE_W * k)))` |
| HomeView:1124 | `float b = BUTTON * k, gap = 4 * k, pad = 6 * k;` |
| HomeView:1127 | `info = 26 * k + capsuleText.measureText(clockText);` |
| HomeView:1130 | `info += 22 * k + Symbols.width(getContext(), Symbols.weather(wx.code, wx.isDay), 24 * k) + 8 * k` |
| HomeView:1133 | `info += 22 * k;` |
| HomeView:1138 | `float left = right - pad - buttons - (info > 0 ? info + 12 * k : pad);` |
| HomeView:1142 | `statusRects[i].set(x, cy - b / 2, x + b, cy + b / 2);` |
| HomeView:1185 | `appsLine = TextUtils.ellipsize(appsLine, fitPaint, (CONTENT_R - MARGIN - 300) * k, TextUtils.TruncateAt.END).toString();` |
| HomeView:1194 | `float tw = Math.min(TILE_MAX_W * k, (full - Math.max(0, n - 1) * gap) / Math.max(1, n));` |
| HomeView:1197 | `platterRect.set(left - pad, top, left + width + pad, top + th + 2 * pad);` |
| HomeView:1273 | `float cap = q.cap, gap = (clock ? 8 : 6) * k, spacing = (clock ? 32 : 24) * k;` |
| HomeView:1287 | `if (clock) c.drawText(stale, x - spacing + 24 * k + staleQuote.measureText(stale), base, staleQuote);` |
| HomeView:1288 | `else c.drawText(stale, right - total - 24 * k, base, staleQuote);` |
| HomeView:1332 | `cell.set(cx - bw * s / 2, cy - bh * s / 2, cx + bw * s / 2, cy + bh * s / 2);` |
| HomeView:1347 | `c.drawRect(-bw / 2, -bh / 2, bw / 2, bh / 2, tileFill);` |
| HomeView:1360 | `float out = 8 * k + 1.5f * k;` |
| HomeView:1364 | `rim.setStrokeWidth(3 * k);` |
| HomeView:1435 | `w = Math.max(1, w);` |
| HomeView:1436 | `h = Math.max(1, h);` |
| HomeView:1496 | `float groupGap = 48 * k, top = ROW_TOP * k;` |
| HomeView:1502 | `float x0 = left + (second ? half + groupGap : 0), cw = half / Math.max(1, count);` |
| HomeView:1503 | `drawerRects[i].set(Math.round(x0 + at * cw), Math.round(top), Math.round(x0 + (at + 1) * cw), Math.round(top + ROW_H * k));` |
| HomeView:1505 | `float cardsTop = CARDS_TOP * k, split = left + (right - left) * 0.585f;` |
| HomeView:1506 | `float weatherRight = split - 16 * k, quotesLeft = split + 16 * k;` |
| HomeView:1551 | `float base = tileRects[0].top - SHIFT * k - 54 * k;` |
| HomeView:1557 | `c.drawText(caps(Ui.tr("Entradas", "Inputs")), drawerRects[0].left, titleBase, mLabel);` |
| HomeView:1590 | `Symbols.drawSharp(getContext(), c, it.symbol, cx, cellRect.top + cellRect.height() * 0.36f, 34 * k, ink);` |
| HomeView:1593 | `float room = cellRect.width() - 16 * k, wide = mCell.measureText(label);` |
| HomeView:1595 | `if (wide > room) mCell.setTextSize(Math.max(17 * k, 21 * k * room / wide));` |
| HomeView:1596 | `c.drawText(label, cx, cellRect.top + cellRect.height() * 0.74f, mCell);` |
| HomeView:1597 | `mCell.setTextSize(21 * k);` |
| HomeView:1602 | `c.drawCircle(cellRect.right - 16 * k, cellRect.top + 16 * k, 6 * k, fill);` |
| HomeView:1615 | `float x = r.left, labelBase = r.top + 28 * k, rule = r.top + 53 * k;` |
| HomeView:1620 | `c.drawText(Ui.tr("Sem dados do clima agora.", "No weather data right now."), x, r.top + 120 * k, mMuted);` |
| HomeView:1625 | `float sw = Symbols.width(getContext(), symbol, 26 * k);` |
| HomeView:1626 | `Symbols.drawSharp(getContext(), c, symbol, r.right - sw / 2, labelBase - Ui.halfCap(mLabel), 26 * k, M_INK);` |
| HomeView:1627 | `c.drawText(condition, r.right - sw - 12 * k, labelBase, mLabelRight);` |
| HomeView:1629 | `float tempBase = r.top + 231 * k, tx = x - Ui.leftBearing(mBig, nowDigits);` |
| HomeView:1632 | `c.drawText("°", tx + mBig.measureText(nowDigits) + 4 * k, tempBase - Ui.cap(mBig) + Ui.cap(mBigDegree) * 0.9f, mBigDegree);` |
| HomeView:1634 | `c.drawText(highLow, x, r.top + 288 * k, mMuted);` |
| HomeView:1636 | `float hx = r.left + 304 * k, hw = (r.right - hx) / hours;` |
| HomeView:1639 | `if (i > 0) vline(c, hx + hw * i, r.top + 76 * k, r.top + 251 * k);` |
| HomeView:1640 | `c.drawText(hourText[i], hc, r.top + 104 * k, mHour);` |
| HomeView:1641 | `Symbols.drawSharp(getContext(), c, Symbols.weather(wx.hourCode.get(i), wx.hourDay.get(i)), hc, r.top + 158 * k, 32 * k, M_INK);` |
| HomeView:1642 | `c.drawText(hourTempText[i], hc, r.top + 228 * k, mHourTemp);` |
| HomeView:1644 | `c.drawText(sunLine, hx, r.top + 288 * k, mMuted);` |
| HomeView:1703 | `float size = 22 * k, sw = Symbols.width(getContext(), symbol, size);` |
| HomeView:1705 | `c.drawText(label, x + sw + 10 * k, base, cardDim);` |
| HomeView:1706 | `return x + sw + 10 * k + cardDim.measureText(label);` |
| HomeView:1735 | `int w = Math.max(1, Math.round(r.right) - left), h = Math.max(1, Math.round(r.bottom) - top);` |
| HomeView:1756 | `float labelBase = r.top + 28 * k;` |
| HomeView:1759 | `hairline(c, r.left, r.right, r.top + 53 * k);` |
| HomeView:1761 | `float priceRight = right - 150 * k;` |
| HomeView:1763 | `float sparkRight = priceRight - priceColumn - 32 * k;` |
| HomeView:1764 | `float sparkLeft = Math.max(x + 44 * k + nameColumn + 32 * k, sparkRight - 170 * k);` |
| HomeView:1768 | `float rule = r.top + (117 + 64 * j) * k, base = rule - 21 * k, mid = base - cap / 2;` |
| HomeView:1769 | `Glyphs.coin(getContext(), c, HomeData.COINS[i], x + 8 * k, mid, cap * 0.85f, M_MUTED);` |
| HomeView:1770 | `c.drawText(caps(HomeData.coinName(i)), x + 44 * k, base, mName);` |
| HomeView:1775 | `if (line != null && sparkRight - sparkLeft > 48 * k) drawSpark(c, line, sparkLeft, sparkRight, mid, color);` |
| HomeView:1791 | `float h = 28 * k, span = hi - lo;` |
| HomeView:1810 | `sparkPaint.setStrokeWidth(2.5f * k);` |
| HomeView:1813 | `c.drawLines(sparkPoints, 0, n, sparkPaint);` |
| HomeView:1896 | `c.drawText(Ui.tr("Todos os Apps", "All Apps"), w / 2f, 124 * k - shift, sheetTitle);` |
| HomeView:1901 | `gridLabels[i] = TextUtils.ellipsize(others.get(i).label, gridLabel, (GRID_W - 10) * k, TextUtils.TruncateAt.END).toString();` |
| HomeView:1905 | `float left = (1920 - gridWidth) / 2 * k;` |
| HomeView:1910 | `if (y > h \|\| y + GRID_ROW * k < 0) continue;` |
| HomeView:1914 | `drawCard(c, others.get(i).tile, gridCell, GRID_RADIUS * k, f, 0.08f);` |
| HomeView:1917 | `c.drawText(gridLabels[i], gridCell.centerX(), gridCell.bottom + (40 + 6 * f) * k, gridLabel);` |
| HomeView:1925 | `cell.set(cx - base.width() * s / 2, cy - base.height() * s / 2, cx + base.width() * s / 2, cy + base.height() * s / 2);` |
| HomeView:1930 | `tmp.offset(0, 16 * k * f);` |
| HomeView:1944 | `tmp.set(-base.width() / 2, -bh / 2, base.width() / 2, bh / 2);` |
| HomeView:1960 | `rim.setStrokeWidth(1.5f * k);` |
| HomeView:1962 | `rim.setStrokeWidth(2.5f * k);` |
| HomeView:1999 | `float padX = 28 * k, padY = 18 * k, icon = 26 * k, iconW = Symbols.width(getContext(), "bell", icon);` |
| HomeView:2000 | `float tw = toast.getWidth() + iconW + 16 * k, th = toast.getHeight();` |
| HomeView:2001 | `float toastTop = 64 * k - (1 - a) * 16 * k;` |
| HomeView:2002 | `tmp.set((w - tw) / 2f - padX, toastTop, (w + tw) / 2f + padX, toastTop + th + 2 * padY);` |
| HomeView:2004 | `scene.glass(c, tmp, Math.min(tmp.height() / 2, 32 * k), SMOKE, 0.55f, 0, 0, 0.02f, 1);` |
| HomeView:2006 | `c.translate(tmp.left + padX + iconW + 16 * k, tmp.top + padY);` |
| HomeView:2012 | `int max = Math.round(720 * k);` |
| HomeView:2510 | `private static final int GRID = 9, WIDGETS = 8;` |
| HomeView:2566 | `float x = ((1920 - gridWidth) / 2 + (i % COLUMNS) * (GRID_W + GRID_GAP)) * k;` |
| HomeView:2594 | `a11yRect.set(i == 0 ? weatherCard : quotesCard);` |
| HomeView:2600 | `a11yRect.offset(0, dy);` |
| HomeView:2601 | `float edge = 24 * k;` |
| HomeView:2603 | `else if (a11yRect.bottom <= edge) a11yRect.set(a11yRect.left, 0, a11yRect.right, edge);` |
| HomeView:2736 | `float pad = 16 * k, tabTop = L_TABBAR_TOP * k, tabBottom = tabRuleBottom();` |
| HomeView:2745 | `topRects[TAB_COUNT + i].set(Math.round(right - b), Math.round(cy - b / 2), Math.round(right), Math.round(cy + b / 2));` |
| HomeView:2755 | `float wide = (CONTENT_R - MARGIN) * k - 2 * tall - 2 * g;` |
| HomeView:2756 | `cardRects[0].set(Math.round(left), Math.round(top), Math.round(left + wide), Math.round(top + ch));` |
| HomeView:2757 | `cardRects[1].set(cardRects[0].right + Math.round(g), cardRects[0].top, cardRects[0].right + Math.round(g + tall), cardRects[0].bottom);` |
| HomeView:2758 | `cardRects[2].set(cardRects[1].right + Math.round(g), cardRects[0].top, cardRects[1].right + Math.round(g + tall), cardRects[0].bottom);` |
| HomeView:2767 | `float top = ((second ? SHOW_LABEL_2 : SHOW_LABEL_1) + 30) * k, x = (MARGIN + col * (SHOW_W + SHOW_GAP)) * k;` |
| HomeView:2872 | `c.drawText(greetingText, MARGIN * k + lTitle.measureText(clockText) + 48 * k, L_TITLE_BASE * k, lGreeting);` |
| HomeView:2874 | `Team.draw(c, topRects[TAB_COUNT].left - 40 * k, 104 * k, 142 * k, k, teamBig, teamSmall, teamTag, teamImage, rule());` |
| HomeView:2882 | `float lx = r.left + 16 * k;` |
| HomeView:2887 | `c.drawRect(lx, tabRuleBottom() - Math.round(3 * k), lx + lTabOn.measureText(label), tabRuleBottom(), fill);` |
| HomeView:2910 | `MARGIN * k, (L_CARDS_TOP + 60) * k, lCardMeta);` |
| HomeView:2916 | `MARGIN * k, (L_CARDS_TOP + 60) * k, lCardMeta);` |
| HomeView:2941 | `float size = 40 * k, base = L_WEATHER_BASE * k;` |
| HomeView:2945 | `c.drawText(weatherLine, x + sw + 16 * k, base, lWeather);` |
| HomeView:2970 | `float out = 8 * k + 1.5f * k;` |
| HomeView:2974 | `rim.setStrokeWidth(3 * k);` |
| HomeView:2981 | `c.drawText(TextUtils.ellipsize(caps(p.title), fitPaint, bw, TextUtils.TruncateAt.END).toString(), base.left, base.bottom + 57 * k, lCardTitle);` |
| HomeView:2985 | `c.drawText(TextUtils.ellipsize(p.meta, fitPaint, bw, TextUtils.TruncateAt.END).toString(), base.left, base.bottom + 87 * k, lCardMeta);` |
| HomeView:3000 | `float base = (row == 0 ? SHOW_LABEL_1 : SHOW_LABEL_2) * k;` |
| HomeView:3002 | `hairline(c, left, right, base + 14 * k);` |
| HomeView:3011 | `c.drawText(TextUtils.ellipsize(title, fitPaint, x - left - 520 * k, TextUtils.TruncateAt.END).toString(), x - 18 * k, base, mLabelRight);` |
| HomeView:3016 | `"Nada de este género en tus apps ahora."), left, base + 120 * k, lCardMeta);` |
| HomeView:3030 | `float out = 8 * k + 1.5f * k;` |
| HomeView:3034 | `rim.setStrokeWidth(3 * k);` |
| HomeView:3040 | `float cy = ((SHOW_LABEL_1 + 30 + SHOW_LABEL_2 + 30 + SHOW_H) / 2) * k, size = 34 * k;` |
| HomeView:3041 | `if (genrePage < Genres.pages() - 1) Symbols.drawSharp(getContext(), c, "chevron_right", (CONTENT_R + MARGIN / 2) * k, cy, size, M_MUTED);` |
| HomeView:3043 | `float cx = MARGIN / 2 * k;` |
| HomeView:3055 | `float cy = (L_CARDS_TOP + L_CARD_H / 2) * k, size = 34 * k;` |
| HomeView:3057 | `if (page < pages - 1) Symbols.draw(getContext(), c, "chevron_right", (CONTENT_R + MARGIN / 2) * k, cy, size, color);` |
| HomeView:3059 | `float cx = MARGIN / 2 * k;` |
| HomeView:3071 | `float pad = 16 * k * s, h = 36 * k * s, w = badgePaint.measureText(name) * s + 28 * k * s;` |
| HomeView:3078 | `c.drawText(name, tmp.centerX() - badgePaint.measureText(name) / 2, tmp.centerY() + Ui.halfCap(badgePaint), badgePaint);` |
| HomeView:3110 | `float pad = 24 * k * s, bh = 7 * k * s, y = card.bottom - pad - bh;` |
| HomeView:3138 | `Symbols.drawSharp(getContext(), c, lightButtons.get(i).symbol, r.centerX(), r.centerY(), 27 * k,` |
| HomeView:3145 | `c.drawText(label, x, r.bottom + 40 * k, lTip);` |
| SettingsActivity:117 | `private static final int MAIN = 0, HOME_CHOICE = 1, HOME_SETTINGS = 2, REMOTE_KEYS = 3, PICKER = 4, SHELF_EDIT = 5;` |
| SettingsActivity:119 | `private static final float LIST_ROW_H = 68, LIST_ROW_GAP = 0;` |
| SettingsActivity:120 | `private static final int PICK_VISIBLE = 9;` |
| SettingsActivity:121 | `private static final float LIST_X = 1000, LIST_R = 1824, LIST_TOP = 186, ROW_H = 74, ROW_GAP = 0, CHOICE_H = 104;` |
| SettingsActivity:122 | `private static final float MARGIN = 96;` |
| SettingsActivity:124 | `private static final int AMBER = 0xFFFF7A12, INK = 0xFF000000, HAIRLINE = 0x8CFFFFFF, MUTED = 0xB8FFFFFF;` |
| SettingsActivity:137 | `private static final int PICK_CITY = 0;` |
| SettingsActivity:301 | `screenRect.set(0, 0, w, h);` |
| SettingsActivity:303 | `title = Ui.text(Ui.displayMedium, 92 * k, 0xFFFFFFFF);` |
| SettingsActivity:305 | `subtitle = Ui.text(Ui.regular, 26 * k, MUTED);` |
| SettingsActivity:306 | `version = Ui.text(Ui.regular, 22 * k, 0x8CFFFFFF);` |
| SettingsActivity:307 | `rowLabel = Ui.text(Ui.displayMedium, 28 * k, 0xFFFFFFFF);` |
| SettingsActivity:309 | `rowValue = Ui.text(Ui.regular, 26 * k, MUTED);` |
| SettingsActivity:311 | `rowSub = Ui.text(Ui.regular, 23 * k, MUTED);` |
| SettingsActivity:312 | `header = Ui.text(Ui.displayMedium, 22 * k, 0xFFFFFFFF);` |
| SettingsActivity:314 | `cellLabel = Ui.text(Ui.regular, 25 * k, 0xE6FFFFFF);` |
| SettingsActivity:315 | `cellValue = Ui.numbers(Ui.regular, 25 * k, 0xE6FFFFFF);` |
| SettingsActivity:317 | `warn = Ui.text(Ui.regular, 23 * k, AMBER);` |
| SettingsActivity:318 | `notice = new TextPaint(Ui.text(Ui.regular, 23 * k, MUTED));` |
| SettingsActivity:319 | `heading = Ui.text(Ui.displayMedium, 34 * k, 0xFFFFFFFF);` |
| SettingsActivity:321 | `hint = Ui.text(Ui.regular, 23 * k, MUTED);` |
| SettingsActivity:329 | `spinner.setStrokeWidth(3.5f * k);` |
| SettingsActivity:333 | `shadow.setMaskFilter(new BlurMaskFilter(22 * k, BlurMaskFilter.Blur.NORMAL));` |
| SettingsActivity:369 | `c.drawRect(Math.round(x0), y, Math.round(x1), y + Math.max(1, Math.round(k)), rule);` |
| SettingsActivity:398 | `if (w == 0) return;` |
| SettingsActivity:420 | `c.drawText("FREE MY TV", x - Ui.leftBearing(title, "FREE MY TV"), 470 * k, title);` |
| SettingsActivity:421 | `hairline(c, x, 900 * k, 500 * k);` |
| SettingsActivity:422 | `c.drawText(Ui.tr("Início e tela de descanso da TV", "Your TV's home screen and screensaver"), x, 548 * k, subtitle);` |
| SettingsActivity:423 | `c.drawText(versionName(getContext()), x, 588 * k, version);` |
| SettingsActivity:443 | `c.drawText(caps(labels[i]), rowRect.left + 30 * k, base, rowLabel);` |
| SettingsActivity:444 | `float right = rowRect.right - 30 * k;` |
| SettingsActivity:446 | `float cw = Symbols.width(getContext(), "chevron_right", 22 * k);` |
| SettingsActivity:447 | `Symbols.drawSharp(getContext(), c, "chevron_right", right - cw / 2, rowRect.centerY(), 22 * k, blend(MUTED, INK, f));` |
| SettingsActivity:452 | `String shown = TextUtils.ellipsize(text, new TextPaint(rowValue), 470 * k, TextUtils.TruncateAt.END).toString();` |
| SettingsActivity:454 | `float sx = right - rowValue.measureText(shown) - 26 * k;` |
| SettingsActivity:456 | `Ui.spinner(c, sx, rowRect.centerY(), 12 * k, seconds * 1.3f, spinner);` |
| SettingsActivity:463 | `float y = (LIST_TOP + 6 * (ROW_H + ROW_GAP) + 44) * k;` |
| SettingsActivity:467 | `LIST_X * k + 30 * k, y, warn);` |
| SettingsActivity:468 | `y += 46 * k;` |
| SettingsActivity:471 | `int width = Math.round(760 * k);` |
| SettingsActivity:478 | `c.translate(LIST_X * k + 30 * k, y - errorLayout.getLineBaseline(0));` |
| SettingsActivity:481 | `y += errorLayout.getLineBaseline(errorLayout.getLineCount() - 1) - errorLayout.getLineBaseline(0) + 46 * k;` |
| SettingsActivity:484 | `y = drawRecs(c, y + 30 * k);` |
| SettingsActivity:485 | `drawNotices(c, y + 44 * k);` |
| SettingsActivity:490 | `float x = LIST_X * k + 30 * k;` |
| SettingsActivity:492 | `hairline(c, LIST_X * k, LIST_R * k, y + 14 * k);` |
| SettingsActivity:501 | `c.drawText(TextUtils.ellipsize(line, notice, 790 * k, TextUtils.TruncateAt.END).toString(), x, y + 42 * k, notice);` |
| SettingsActivity:503 | `"This product uses the TMDB API but is not endorsed or certified by TMDB."), x, y + 78 * k, version);` |
| SettingsActivity:504 | `return y + 78 * k;` |
| SettingsActivity:515 | `c.drawRect(rowRect.left, rowRect.top, rowRect.right, rowRect.bottom + Math.max(1, Math.round(k)), white);` |
| SettingsActivity:521 | `float x = LIST_X * k + 30 * k, colA = LIST_R * k - 220 * k, colB = LIST_R * k - 30 * k;` |
| SettingsActivity:523 | `hairline(c, LIST_X * k, LIST_R * k, y + 14 * k);` |
| SettingsActivity:534 | `float base = y + (46 + 40 * i) * k;` |
| SettingsActivity:535 | `c.drawText(rows[i][0], x, base, cellLabel);` |
| SettingsActivity:536 | `c.drawText(metric(a, rows[i][1]), colA, base, cellValue);` |
| SettingsActivity:537 | `c.drawText(metric(b, rows[i][1]), colB, base, cellValue);` |
| SettingsActivity:539 | `return y + (46 + 40 * rows.length) * k;` |
| SettingsActivity:543 | `float x = LIST_X * k + 30 * k;` |
| SettingsActivity:545 | `hairline(c, LIST_X * k, LIST_R * k, y + 14 * k);` |
| SettingsActivity:552 | `c.drawText(TextUtils.ellipsize(line, notice, 790 * k, TextUtils.TruncateAt.END).toString(), x, y + (42 + 38 * shown) * k, notice);` |
| SettingsActivity:555 | `if (shown == 0) c.drawText(Ui.tr("Nenhum ainda.", "None yet."), x, y + 42 * k, notice);` |
| SettingsActivity:560 | `c.drawText(caps(Ui.tr("Início", "Home Screen")), LIST_X * k + 30 * k, (LIST_TOP - 24) * k, heading);` |
| SettingsActivity:565 | `float y = (LIST_TOP + 20 + i * (CHOICE_H + ROW_GAP)) * k;` |
| SettingsActivity:570 | `c.drawText(caps(options[i][0]), rowRect.left + 30 * k, rowRect.top + 44 * k, rowLabel);` |
| SettingsActivity:571 | `c.drawText(options[i][1], rowRect.left + 30 * k, rowRect.top + 80 * k, rowSub);` |
| SettingsActivity:573 | `float cw = Symbols.width(getContext(), "check", 26 * k);` |
| SettingsActivity:574 | `Symbols.drawSharp(getContext(), c, "check", rowRect.right - 30 * k - cw / 2, rowRect.centerY(), 26 * k, blend(0xFFFFFFFF, INK, f));` |
| SettingsActivity:578 | `"Switching takes a few seconds; the screen blinks while it measures."), LIST_X * k + 30 * k,` |
| SettingsActivity:579 | `(LIST_TOP + 20 + 2 * (CHOICE_H + ROW_GAP) + 40) * k, hint);` |
| SettingsActivity:679 | `private static final int WEATHER_ROW = 2, CITY_ROW = 3, QUOTES_ROW = 4, COIN_ROWS = 5;` |
| SettingsActivity:908 | `c.drawText(caps(Ui.tr("Prateleira", "Shelf")), LIST_X * k + 30 * k, (LIST_TOP - 24) * k, heading);` |
| SettingsActivity:913 | `float y = (LIST_TOP + 20 + i * (LIST_ROW_H + LIST_ROW_GAP)) * k;` |
| SettingsActivity:919 | `c.drawText(TextUtils.ellipsize(caps(app[1]), new TextPaint(rowLabel), 520 * k, TextUtils.TruncateAt.END).toString(), rowRect.left + 30 * k, base, rowLabel);` |
| SettingsActivity:920 | `c.drawText(place >= 0 ? "#" + (place + 1) : Ui.tr("Fora", "Off"), rowRect.right - 30 * k, base, rowValue);` |
| SettingsActivity:922 | `float hy = (LIST_TOP + 20 + rows * (LIST_ROW_H + LIST_ROW_GAP) + 36) * k;` |
| SettingsActivity:925 | `"OK pone o quita del estante (hasta " + HomeApps.MAX_SHELF + "). ← → cambian el orden."), LIST_X * k + 30 * k, hy, hint);` |
| SettingsActivity:948 | `c.drawText(caps(title), LIST_X * k + 30 * k, (LIST_TOP - 24) * k, heading);` |
| SettingsActivity:952 | `float y = (LIST_TOP + 20 + i * (LIST_ROW_H + LIST_ROW_GAP)) * k;` |
| SettingsActivity:956 | `c.drawText(caps(pickLabels.get(at)), rowRect.left + 30 * k, rowRect.centerY() + Ui.halfCap(rowLabel), rowLabel);` |
| SettingsActivity:958 | `float cw = Symbols.width(getContext(), "check", 26 * k);` |
| SettingsActivity:959 | `Symbols.drawSharp(getContext(), c, "check", rowRect.right - 30 * k - cw / 2, rowRect.centerY(), 26 * k, blend(0xFFFFFFFF, INK, f));` |
| SettingsActivity:962 | `float hy = (LIST_TOP + 20 + rows * (LIST_ROW_H + LIST_ROW_GAP) + 36) * k;` |
| SettingsActivity:963 | `c.drawText((pickScroll + focus + 1) + " / " + pickLabels.size() + "  ·  " + Ui.tr("OK escolhe, Voltar cancela.", "OK picks, Back cancels."),` |
| SettingsActivity:964 | `LIST_X * k + 30 * k, hy, hint);` |
| SettingsActivity:972 | `LIST_X * k + 30 * k, (LIST_TOP - 24) * k, heading);` |
| SettingsActivity:975 | `float y = (LIST_TOP + 20 + i * (rowH + LIST_ROW_GAP)) * k;` |
| SettingsActivity:983 | `c.drawText(caps(labels[i]), rowRect.left + 30 * k, base, rowLabel);` |
| SettingsActivity:984 | `c.drawText(values[i], rowRect.right - 30 * k, base, rowValue);` |
| SettingsActivity:986 | `float hy = (LIST_TOP + 20 + labels.length * (rowH + LIST_ROW_GAP) + 36) * k;` |
| SettingsActivity:987 | `c.drawText(Ui.tr("OK ou as setas mudam o valor.", "OK or the arrows change the value."), LIST_X * k + 30 * k, hy, hint);` |
| SettingsActivity:994 | `"These are the TCL remote's buttons: on this TV, the buttons work as they always did."), LIST_X * k + 30 * k, hy + 38 * k, hint);` |
| Frost:14 | `static final int W = 48, H = 27;` |
| Frost:53 | `col = col + (c[6 + ch] - col) * k;` |
| Frost:61 | `x = fract(x * 123.34f);` |
| Frost:62 | `y = fract(y * 456.21f);` |
| Backdrop:25 | `static final int W = 1920, H = 1080, FW = 192, FH = 108, GW = 480, GH = 270;` |
| Backdrop:28 | `static final int WIDE_W = 1008, WIDE_H = 420, TALL_W = 328, TALL_H = 420;` |
| Backdrop:72 | `c.drawRect(0, 0, W, H, scrim);` |
| Backdrop:77 | `c.drawRect(0, H * 0.4f, W, H, scrim);` |
| Backdrop:83 | `c.drawCircle(W * 0.8f, H * 0.65f, W * 0.24f, scrim);` |
| Backdrop:87 | `c.drawRect(0, 0, W, H * 0.24f, scrim);` |
| Backdrop:217 | `out[base + i * step] = 0xFF000000 \| (Math.round(sr * k) << 16) \| (Math.round(sg * k) << 8) \| Math.round(sb * k);` |
| Backdrop:232 | `int x = Math.round((x0 + (x1 - x0) * (i + 0.5f) / 32) * (b.getWidth() - 1));` |
| Backdrop:233 | `int y = Math.round((y0 + (y1 - y0) * (j + 0.5f) / 24) * (b.getHeight() - 1));` |
| Shadows:44 | `src.set(sx[j], sy[i], sx[j + 1], sy[i + 1]);` |
| Shadows:45 | `dst.set(xs[j], ys[i], xs[j + 1], ys[i + 1]);` |
| Scene:34 | `private int w = 1, h = 1, lw = 1, lh = 1;` |
| Scene:67 | `w = Math.max(1, width);` |
| Scene:68 | `h = Math.max(1, height);` |
| Scene:182 | `c.drawRect(r.left - 1, r.top - 1, r.right + 1, r.bottom + 1, liquidPaint);` |
| Scene:226 | `c.drawRect(0, 0, w, h, fallback);` |
| Scene:275 | `c.drawRect(r.left - 1, r.top - 1, r.right + 1, r.bottom + 1, glassPaint);` |
| Symbols:26 | `private static final float PX_PER_POINT = 96 * 1.5f;` |
| Symbols:65 | `RECT.set(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2);` |
| Symbols:79 | `int w = Math.max(1, Math.round(b.getWidth() * pointSize / PX_PER_POINT));` |
| Symbols:80 | `int h = Math.max(1, Math.round(b.getHeight() * pointSize / PX_PER_POINT));` |
| Team:41 | `private static final int CREST_PX = 64;` |
| Team:244 | `float left = right, crest = 32 * k, gap = 12 * k, cap = Ui.cap(big);` |
| Team:247 | `x = crestAt(c, top.away.crest, x, y1 - cap / 2, crest, image) - gap;` |
| Team:250 | `x -= 18 * k + big.measureText(top.score);` |
| Team:252 | `x -= 18 * k + big.measureText(top.home.name);` |
| Team:254 | `x = crestAt(c, top.home.crest, x - gap, y1 - cap / 2, crest, image);` |
| Team:260 | `x -= 16 * k + tag.measureText(next.tag);` |
| Team:265 | `float y = Math.round(y2 + 14 * k);` |
| Team:266 | `c.drawRect(Math.round(left), y, Math.round(right), y + Math.max(1, Math.round(k)), rule);` |
| Team:277 | `CREST_BOX.set(right - w, cy - size / 2, right, cy + size / 2);` |
| HomeApps:57 | `static final int APP = 0, LIVE_TV = 1, INPUT = 2, ALL_APPS = 3, SETTINGS = 4, ANDROID_SETTINGS = 5;` |
| HomeApps:59 | `static final int INPUTS = 7;` |
| HomeApps:61 | `static final int SLEEP = 10;` |
| HomeApps:63 | `static final int TILE_W = 384, TILE_H = 216;` |
| HomeApps:96 | `static final int MAX_SHELF = 8;` |
| HomeApps:465 | `private static final int LOGO_VERSION = 2;` |
| HomeApps:522 | `int w = logo.getWidth(), h = logo.getHeight(), hits = 0, total = 0;` |
| HomeApps:523 | `for (int y = 0; y < h; y += Math.max(1, h / 48)) {` |
| HomeApps:524 | `for (int x = 0; x < w; x += Math.max(1, w / 96)) {` |
| HomeApps:541 | `final int w = 768, h = 432;` |
| HomeApps:553 | `for (int y = 0; y < h; y++) {` |
| HomeApps:554 | `for (int x = 0; x < band; x++) {` |
| HomeApps:559 | `for (int x = 0; x < w; x++) {` |
| HomeApps:560 | `for (int y = 0; y < band; y++) {` |
| HomeApps:576 | `for (int y = 0; y < h; y++) {` |
| HomeApps:578 | `for (int x = 0; x < w; x++) {` |
| HomeApps:637 | `for (int x = 0; x < w; x++) {` |
| HomeApps:643 | `for (int y = 0; y < h; y++) {` |
| HomeApps:694 | `acc[0] += ((p >> 16) & 255) / 255f * k;` |
| HomeApps:695 | `acc[1] += ((p >> 8) & 255) / 255f * k;` |
| HomeApps:696 | `acc[2] += (p & 255) / 255f * k;` |
| HomeApps:840 | `canvas.drawRect(0, 0, TILE_W, TILE_H, glow);` |
| HomeApps:854 | `new Canvas(b).drawRect(0, 0, TILE_W, TILE_H, bg);` |
| HomeApps:900 | `for (int y = 0; y < h; y++) {` |
| HomeApps:902 | `for (int x = 0; x < w; x++) {` |
| DreamView:43 | `private static final float ZOOM = 0.1f;` |
| DreamView:45 | `private static final int PW = 2112, PH = 1188;` |
| DreamView:46 | `private static final float MARGIN = 96, RIGHT = 1824, TITLE_W = 900, META_BASE = 1000;` |
| DreamView:48 | `private static final float CORNER_W = 720, DATE_BASE = 96, RULE_Y = 120, CLOCK_TOP = 150, CLOCK_SIZE = 220, QUOTES_GAP = 56;` |
| DreamView:49 | `private static final int INK = 0xFFFFFFFF, MUTED = 0xB8FFFFFF, HAIRLINE = 0x8CFFFFFF;` |
| DreamView:223 | `datePaint = Ui.text(Ui.displayMedium, 30 * k, INK);` |
| DreamView:226 | `clockPaint = Ui.text(Ui.displayMedium, 300 * k, INK);` |
| DreamView:229 | `weatherPaint = Ui.numbers(Ui.displayMedium, 30 * k, INK);` |
| DreamView:230 | `weatherDim = Ui.numbers(Ui.regular, 28 * k, MUTED);` |
| DreamView:231 | `quotePaint = Ui.numbers(Ui.regular, 24 * k, MUTED);` |
| DreamView:232 | `stalePaint = Ui.text(Ui.regular, 20 * k, 0x80FFFFFF);` |
| DreamView:235 | `cornerDate = Ui.numbers(Ui.displayMedium, 26 * k, INK);` |
| DreamView:237 | `cornerQuote = Ui.numbers(Ui.regular, 24 * k, INK);` |
| DreamView:238 | `cornerStale = Ui.text(Ui.regular, 21 * k, MUTED);` |
| DreamView:241 | `labelPaint = Ui.text(Ui.displayMedium, 26 * k, INK);` |
| DreamView:243 | `titlePaint = new TextPaint(Ui.text(Ui.displayMedium, 76 * k, INK));` |
| DreamView:245 | `metaPaint = Ui.text(Ui.regular, 26 * k, MUTED);` |
| DreamView:246 | `teamBig = Ui.text(Ui.displayMedium, 26 * k, INK);` |
| DreamView:248 | `teamSmall = Ui.numbers(Ui.regular, 21 * k, MUTED);` |
| DreamView:249 | `teamTag = Ui.text(Ui.regular, 21 * k, 0xFF5EDCFF);` |
| DreamView:250 | `fieldRect.set(0, 0, w, h);` |
| DreamView:398 | `if (w == 0) return;` |
| DreamView:413 | `else canvas.drawRect(0, 0, w, h, black);` |
| DreamView:450 | `c.drawRect(0, h * 0.42f, w, h, p);` |
| DreamView:458 | `c.drawRect(0, h - w * 0.7f * 0.62f, w * 0.7f, h, p);` |
| DreamView:461 | `c.drawRect(0, 0, w, h * 0.3f, p);` |
| DreamView:470 | `c.drawRect(w * 0.5f, 0, w, cy + w * 0.36f * 0.72f, p);` |
| DreamView:497 | `float size = 28 * k, sw = Symbols.width(getContext(), symbol, size);` |
| DreamView:498 | `Symbols.drawSharp(getContext(), c, symbol, right - tw - 12 * k - sw / 2, base - Ui.halfCap(cornerDate), size, INK);` |
| DreamView:501 | `c.drawRect(Math.round(left), y, Math.round(right), y + Math.max(1, Math.round(k)), rule);` |
| DreamView:509 | `float cap = Ui.cap(cornerQuote), gap = 8 * k, spacing = 28 * k;` |
| DreamView:528 | `if (!stale.isEmpty() && n > 0) c.drawText(stale, right - total - 24 * k - cornerStale.measureText(stale), qbase, cornerStale);` |
| DreamView:530 | `float ty = n > 0 ? qbase + 56 * k : (CLOCK_TOP + 20) * k + Ui.cap(cornerClock) + 50 * k;` |
| DreamView:531 | `Team.draw(c, right, ty, ty + 38 * k, k, teamBig, teamSmall, teamTag, crestPaint, rule);` |
| DreamView:541 | `float x = MARGIN * k, metaBase = META_BASE * k, titleBase = metaBase - 52 * k;` |
| DreamView:543 | `StaticLayout t = StaticLayout.Builder.obtain(title, 0, title.length(), titlePaint, Math.round(TITLE_W * k))` |
| DreamView:549 | `float ruleY = Math.round(capTop - 32 * k);` |
| DreamView:550 | `c.drawText(Recs.periodLabel().toUpperCase(Ui.locale()), x, ruleY - 14 * k, labelPaint);` |
| DreamView:551 | `c.drawRect(Math.round(x), ruleY, Math.round(x + TITLE_W * k), ruleY + Math.max(1, Math.round(k)), rule);` |
| DreamView:568 | `current.caption.setTranslationY(14 * k * (1 - a));` |
| DreamView:582 | `incoming.caption.setTranslationY(14 * k * (1 - in));` |
| DreamView:607 | `content.setTranslationX((float) (40 * k * Math.sin(phase)));` |
| DreamView:608 | `content.setTranslationY((float) (26 * k * Math.sin(2 * phase + 0.7)));` |
| DreamView:616 | `float clockBase = h * 0.5f + clockCap / 2 - 10 * k;` |
| DreamView:617 | `c.drawText(date.toUpperCase(Ui.locale()), cx, clockBase - clockCap - 40 * k, datePaint);` |
| DreamView:623 | `float base = clockBase + 76 * k;` |
| DreamView:630 | `float sw = Symbols.width(getContext(), symbol, 32 * k);` |
| DreamView:631 | `float width = sw + 12 * k + weatherPaint.measureText(now) + 18 * k + weatherDim.measureText(range);` |
| DreamView:632 | `float x = cx - width / 2;` |
| DreamView:633 | `Symbols.drawSharp(getContext(), c, symbol, x + sw / 2, base - Ui.halfCap(weatherPaint), 32 * k, INK);` |
| DreamView:634 | `x += sw + 12 * k;` |
| DreamView:636 | `x += weatherPaint.measureText(now) + 18 * k;` |
| DreamView:641 | `float cap = Ui.cap(quotePaint), spacing = 56 * k, gap = 10 * k;` |
| DreamView:649 | `float x = cx - total / 2, base = h - 92 * k;` |
| DreamView:658 | `if (!stale.isEmpty() && values.length > 0) c.drawText(Ui.tr("cotações de ", "prices from ") + stale, cx, base + 38 * k, stalePaint);` |
| Glyphs:63 | `int w = b.getWidth(), h = b.getHeight(), left = w, right = -1;` |
| Glyphs:65 | `for (int x = 0; x < w; x++) {` |
| Shaders:235 | `private static final int[] NIGHT = {0x070B1E, 0x16225A, 0x4B2F8F};` |
| Shaders:236 | `private static final int[] DAWN = {0x24183F, 0xE89A55, 0xF6D59A};` |
| Shaders:237 | `private static final int[] DAY = {0x1E57C8, 0x4FB9EC, 0x9EE2EE};` |
| Shaders:238 | `private static final int[] SUNSET = {0x2E1650, 0xE2546C, 0xF39A4F};` |
| Shaders:239 | `private static final int[] DUSK = {0x10173A, 0x36348A, 0x8A4BB0};` |
| Shaders:248 | `cal.set(java.util.Calendar.HOUR_OF_DAY, 0);` |
| Shaders:249 | `cal.set(java.util.Calendar.MINUTE, 0);` |
| Shaders:250 | `cal.set(java.util.Calendar.SECOND, 0);` |
| Shaders:251 | `cal.set(java.util.Calendar.MILLISECOND, 0);` |
| Shaders:259 | `out.set(NIGHT[0], NIGHT[1], NIGHT[2]);` |
| Shaders:265 | `out.set(mix(key[i][0], key[i + 1][0], f), mix(key[i][1], key[i + 1][1], f), mix(key[i][2], key[i + 1][2], f));` |
| Shaders:281 | `out.set(NIGHT[0], NIGHT[1], NIGHT[2]);` |

### raio/borda

| Arquivo:linha | Expressão original |
|---|---|
| HomeView:687 | `border.setStrokeWidth(2 * k);` |
| HomeView:689 | `rim.setStrokeWidth(2.5f * k);` |
| HomeView:1364 | `rim.setStrokeWidth(3 * k);` |
| HomeView:1810 | `sparkPaint.setStrokeWidth(2.5f * k);` |
| HomeView:1960 | `rim.setStrokeWidth(1.5f * k);` |
| HomeView:1962 | `rim.setStrokeWidth(2.5f * k);` |
| HomeView:2974 | `rim.setStrokeWidth(3 * k);` |
| HomeView:3034 | `rim.setStrokeWidth(3 * k);` |
| SettingsActivity:329 | `spinner.setStrokeWidth(3.5f * k);` |
| Shadows:33 | `int radius = Math.max(0, Math.round(r)), b = Math.max(1, Math.round(blur));` |
| Shadows:35 | `int corner = radius + 2 * m;                  // the slice that holds the curve and the blur on both sides of it` |
| Shadows:56 | `if (to - from >= 2 * corner + 1) {` |
| Shadows:57 | `int size = 2 * corner + 1;` |
| Shadows:58 | `out[0] = from; out[1] = from + corner; out[2] = to - corner; out[3] = to;` |
| Shadows:59 | `src[0] = 0; src[1] = corner; src[2] = corner + 1; src[3] = size;` |
| Shadows:69 | `long key = (long) radius << 48 \| (long) b << 32 \| (long) w << 16 \| h;` |
| Scene:63 | `flatRim.setStrokeWidth(1.5f);` |
| Scene:170 | `glass(c, r, radius, tint, Math.max(0.5f, tintAlpha), 0, 0, lift, 1, alpha);` |
| Scene:175 | `liquid.setFloatUniform("radius", Math.min(radius, Math.min(r.width(), r.height()) / 2));` |
| Scene:249 | `glass(c, r, radius, tint, tintAlpha, glow, glowAlpha, lift, body, 1);` |
| HomeApps:805 | `clip.addRoundRect(new RectF(left, top, left + size, top + size), size * 0.2237f, size * 0.2237f, Path.Direction.CW);` |
| DreamView:235 | `cornerDate = Ui.numbers(Ui.displayMedium, 26 * k, INK);` |
| DreamView:236 | `cornerDate.setLetterSpacing(-0.01f);` |
| DreamView:237 | `cornerQuote = Ui.numbers(Ui.regular, 24 * k, INK);` |
| DreamView:238 | `cornerStale = Ui.text(Ui.regular, 21 * k, MUTED);` |
| DreamView:240 | `cornerClock.setLetterSpacing(-0.05f);` |
| DreamView:446 | `float caption = scrimStrength(lightCaption, 0.5f), corner = scrimStrength(lightClock, 0.42f);` |
| DreamView:464 | `new int[] {black(corner), black(0.6f * corner), black(0.15f * corner), 0},` |
| DreamView:498 | `Symbols.drawSharp(getContext(), c, symbol, right - tw - 12 * k - sw / 2, base - Ui.halfCap(cornerDate), size, INK);` |
| DreamView:504 | `cornerClock.getTextBounds(clock, 0, clock.length(), ink);` |
| DreamView:509 | `float cap = Ui.cap(cornerQuote), gap = 8 * k, spacing = 28 * k;` |
| DreamView:528 | `if (!stale.isEmpty() && n > 0) c.drawText(stale, right - total - 24 * k - cornerStale.measureText(stale), qbase, cornerStale);` |
| DreamView:530 | `float ty = n > 0 ? qbase + 56 * k : (CLOCK_TOP + 20) * k + Ui.cap(cornerClock) + 50 * k;` |

### sombra/blur

| Arquivo:linha | Expressão original |
|---|---|
| HomeView:628 | `for (Paint p : new Paint[] {labelPaint, titlePaint, metaPaint, clockPaint}) p.setShadowLayer(22 * k, 0, 2 * k, 0x59000000);` |
| HomeView:654 | `tipPaint.setShadowLayer(16 * k, 0, 1 * k, 0x99000000);` |
| HomeView:657 | `quotePaint.setShadowLayer(14 * k, 0, 1 * k, 0x99000000);` |
| HomeView:683 | `shadowSoft.setMaskFilter(new BlurMaskFilter(12 * k, BlurMaskFilter.Blur.NORMAL));` |
| HomeView:685 | `halo.setMaskFilter(new BlurMaskFilter(22 * k, BlurMaskFilter.Blur.NORMAL));` |
| HomeView:1931 | `shadow.setColor(Ui.alpha(0xFF000000, 0.55f * fc));` |
| SettingsActivity:333 | `shadow.setMaskFilter(new BlurMaskFilter(22 * k, BlurMaskFilter.Blur.NORMAL));` |
| Shadows:33 | `int radius = Math.max(0, Math.round(r)), b = Math.max(1, Math.round(blur));` |

### opacidade/composicao

| Arquivo:linha | Expressão original |
|---|---|
| HomeView:884 | `v.setVisibility(alpha > 0.002f ? VISIBLE : INVISIBLE);` |
| HomeView:992 | `int color = blend(0xFFEBEBF0, 0xFF141416, night);` |
| HomeView:1072 | `if (image == null \|\| alpha <= 0.004f) return;` |
| HomeView:1073 | `imagePaint.setAlpha(Math.round(255 * Math.min(1, alpha)));` |
| HomeView:1346 | `tileFill.setAlpha(255);` |
| HomeView:1540 | `return Ui.alpha(tone(L_INK, 0xFFFFFFFF), alpha);` |
| HomeView:1589 | `int ink = blend(off ? 0x73FFFFFF : M_INK, 0xFF000000, f);` |
| HomeView:1916 | `gridLabel.setColor(blend(0x99FFFFFF, 0xFFFFFFFF, Ui.clamp(f)));` |
| HomeView:1931 | `shadow.setColor(Ui.alpha(0xFF000000, 0.55f * fc));` |
| HomeView:1946 | `sheen.setAlpha(Math.round(255 * fc));` |
| HomeView:1948 | `sheen.setAlpha(255);` |
| HomeView:1952 | `veil.setColor(Ui.alpha(0xFF000000, 0.08f * (1 - fc)));` |
| HomeView:1959 | `rim.setColor(Ui.alpha(0xFFFFFFFF, 0.4f * fc));` |
| HomeView:2003 | `int save = c.saveLayerAlpha(tmp.left - 4, tmp.top - 4, tmp.right + 4, tmp.bottom + 4, Math.round(255 * a));` |
| HomeView:2853 | `imagePaint.setAlpha(255);` |
| HomeView:2857 | `imagePaint.setAlpha(Math.round(255 * d));` |
| HomeView:2859 | `imagePaint.setAlpha(255);` |
| HomeView:2894 | `if (i == tab) lTabOn.setColor(blend(M_INK, 0xFF000000, focus));` |
| HomeView:3139 | `blend(M_INK, 0xFF000000, f));` |
| SettingsActivity:440 | `int ink = blend(0xF2FFFFFF, INK, f);` |
| SettingsActivity:447 | `Symbols.drawSharp(getContext(), c, "chevron_right", right - cw / 2, rowRect.centerY(), 22 * k, blend(MUTED, INK, f));` |
| SettingsActivity:455 | `spinner.setColor(blend(0xFFFFFFFF, INK, f));` |
| SettingsActivity:514 | `white.setAlpha(Math.round(255 * f));` |
| SettingsActivity:568 | `rowLabel.setColor(blend(0xFFFFFFFF, INK, f));` |
| SettingsActivity:569 | `rowSub.setColor(blend(MUTED, 0xB3000000, f));` |
| SettingsActivity:574 | `Symbols.drawSharp(getContext(), c, "check", rowRect.right - 30 * k - cw / 2, rowRect.centerY(), 26 * k, blend(0xFFFFFFFF, INK, f));` |
| SettingsActivity:916 | `rowLabel.setColor(blend(place >= 0 ? 0xFFFFFFFF : 0x80FFFFFF, place >= 0 ? INK : 0x80000000, f));` |
| SettingsActivity:955 | `rowLabel.setColor(blend(0xFFFFFFFF, INK, f));` |
| SettingsActivity:959 | `Symbols.drawSharp(getContext(), c, "check", rowRect.right - 30 * k - cw / 2, rowRect.centerY(), 26 * k, blend(0xFFFFFFFF, INK, f));` |
| SettingsActivity:978 | `rowLabel.setColor(blend(0xFFFFFFFF, INK, f));` |
| SettingsActivity:980 | `rowValue.setColor(blend(locked ? 0x59FFFFFF : MUTED, locked ? 0x59000000 : INK, f));` |
| SettingsActivity:981 | `if (locked) rowLabel.setColor(blend(0x73FFFFFF, 0x73000000, f));` |
| Frost:48 | `float scrim = 1 - 0.22f * (1 - smoothstep(0, 0.42f, v));` |
| Frost:49 | `float gain = vig * scrim * (1 - dim);` |
| Backdrop:69 | `scrim.setShader(new LinearGradient(0, 0, W * 0.68f, 0,` |
| Backdrop:72 | `c.drawRect(0, 0, W, H, scrim);` |
| Backdrop:74 | `scrim.setShader(new LinearGradient(0, H, 0, H * 0.4f,` |
| Backdrop:77 | `c.drawRect(0, H * 0.4f, W, H, scrim);` |
| Backdrop:79 | `scrim.setShader(new android.graphics.RadialGradient(W * 0.8f, H * 0.65f, W * 0.24f,` |
| Backdrop:83 | `c.drawCircle(W * 0.8f, H * 0.65f, W * 0.24f, scrim);` |
| Backdrop:85 | `scrim.setShader(new LinearGradient(0, 0, 0, H * 0.24f, new int[] {black(0.34f), black(0.1f), 0},` |
| Backdrop:87 | `c.drawRect(0, 0, W, H * 0.24f, scrim);` |
| Scene:168 | `if (alpha <= 0.004f) return;` |
| Scene:170 | `glass(c, r, radius, tint, Math.max(0.5f, tintAlpha), 0, 0, lift, 1, alpha);` |
| Scene:176 | `liquid.setFloatUniform("tint", Color.red(tint) / 255f, Color.green(tint) / 255f, Color.blue(tint) / 255f, tintAlpha);` |
| Scene:181 | `liquidPaint.setAlpha(Math.round(255 * Math.min(1, alpha)));` |
| Scene:183 | `liquidPaint.setAlpha(255);` |
| Scene:249 | `glass(c, r, radius, tint, tintAlpha, glow, glowAlpha, lift, body, 1);` |
| Scene:254 | `if (alpha <= 0.004f) return;` |
| Scene:257 | `flatFill.setColor(Ui.alpha(tint \| 0xFF000000, Math.max(0.35f, tintAlpha)));` |
| Scene:270 | `glass.setFloatUniform("tint", Color.red(tint) / 255f, Color.green(tint) / 255f, Color.blue(tint) / 255f, tintAlpha);` |
| Scene:271 | `glass.setFloatUniform("glow", Color.red(glow) / 255f, Color.green(glow) / 255f, Color.blue(glow) / 255f, glowAlpha);` |
| Scene:274 | `glassPaint.setAlpha(Math.round(255 * Math.min(1, alpha)));` |
| Scene:276 | `glassPaint.setAlpha(255);` |
| HomeApps:596 | `} else if (tint && bgLum <= 0.6f && bgLum > 0.1f && bgSat > 0.35f && l > 0.7f && s < 0.25f) {` |
| HomeApps:839 | `glow.setShader(new RadialGradient(TILE_W / 2f, TILE_H * 1.15f, TILE_W * 0.75f, Ui.alpha(brand, 0.38f), 0x00000000, Shader.TileMode.CLAMP));` |
| DreamView:300 | `image = Backdrop.gpu(withScrims(Backdrop.photo(file, PW, PH, light), light[0], light[1]));` |
| DreamView:371 | `if (alpha <= 0.004f) return;` |
| DreamView:374 | `photoPaint.setAlpha(Math.round(255 * Math.min(1, alpha)));` |
| DreamView:446 | `float caption = scrimStrength(lightCaption, 0.5f), corner = scrimStrength(lightClock, 0.42f);` |
| Recording:47 | `private float alpha = 1, tx, ty;` |
| Recording:64 | `@Override void setAlpha(float a) { alpha = Math.max(0, Math.min(1, a)); }` |
| Recording:69 | `if (alpha <= 0.004f) return;` |
| Recording:70 | `int save = alpha < 1` |
| Recording:71 | `? c.saveLayerAlpha(left + tx, top + ty, right + tx, bottom + ty, Math.round(alpha * 255))` |

### movimento/tempo

| Arquivo:linha | Expressão original |
|---|---|
| HomeView:89 | `private static final long TOAST_MS = 6000, FADE_MS = 800;` |
| HomeView:199 | `private static final long HOLD_MS = 500, WIGGLE_MS = 300;` |
| HomeView:202 | `private int liftIndex = -1;` |
| HomeView:206 | `private final Spring moveLift = new Spring(0.3f, 0.72f, 0);` |
| HomeView:223 | `private final Spring heroFocus = new Spring(0.28f, 0.85f, 0);` |
| HomeView:232 | `private final Spring drawer = new Spring(0.46f, 0.92f, 0);` |
| HomeView:240 | `private final Spring sheet = new Spring(0.38f, 0.9f, 0);` |
| HomeView:241 | `private final Spring scroll = new Spring(0.34f, 1f, 0);` |
| HomeView:364 | `handler.postDelayed(reloadSoon, 500);` |
| HomeView:382 | `handler.postDelayed(this, 15_000);` |
| HomeView:419 | `topFocus[i] = new Spring(0.26f, 0.85f, 0);` |
| HomeView:423 | `cardFocus[i] = new Spring(0.32f, 0.78f, 0);` |
| HomeView:446 | `lastStep = 0;` |
| HomeView:460 | `handler.postDelayed(tick, 15_000);` |
| HomeView:794 | `float dt = lastNanos == 0 ? 1 / 60f : Math.min(0.05f, (frameTimeNanos - lastNanos) / 1e9f);` |
| HomeView:843 | `else Choreographer.getInstance().postFrameCallbackDelayed(this, 1000);` |
| HomeView:863 | `float home = 1 - s, top = home * (1 - smoothstep(0, 0.32f, dc));` |
| HomeView:870 | `float da = home * smoothstep(0.1f, 0.6f, dc);` |
| HomeView:1042 | `if (fadeEase(heroFadeAt, up) >= 1) {` |
| HomeView:1105 | `statusFocus[i] = new Spring(0.26f, 0.85f, 0);` |
| HomeView:1162 | `tileFocus[i] = new Spring(0.34f, 0.74f, 0);` |
| HomeView:1163 | `tileSlide[i] = new Spring(0.32f, 0.86f, 0);` |
| HomeView:1257 | `float fade = 1;` |
| HomeView:1258 | `for (int i = TAB_COUNT; i < TAB_COUNT + 3 && !clock; i++) fade = Math.min(fade, 1 - Ui.clamp(topFocus[i].value));` |
| HomeView:1259 | `if (fade <= 0.01f) return;` |
| HomeView:1301 | `if (liftIndex >= 0 && liftIndex < shelf.size() && moveLift.value > 0.02f) top = liftIndex;` |
| HomeView:1308 | `float angle = i == liftIndex ? wiggle * moveLift.value : 0;` |
| HomeView:1328 | `float l = i == liftIndex ? Math.max(0, moveLift.value) : 0;` |
| HomeView:1482 | `drawerFocus[i] = new Spring(0.28f, 0.85f, 0);` |
| HomeView:1793 | `float step = (line.length - 1) / (float) (points - 1);` |
| HomeView:1854 | `for (int i = 0; i < gridFocus.length; i++) gridFocus[i] = new Spring(0.3f, 0.82f, 0);` |
| HomeView:1989 | `boolean fading = age < 350 \|\| age > TOAST_MS - 450;` |
| HomeView:1997 | `float a = Math.min(Ui.clamp(age / 0.3f), Ui.clamp((TOAST_MS / 1000f - age) / 0.4f));` |
| HomeView:2352 | `moveLift.target = 1;` |
| HomeView:2428 | `moveLift.target = 0;` |
| HomeView:2460 | `moveLift.target = 0;` |
| HomeView:2483 | `moveLift.snap(0);` |
| HomeView:2485 | `wiggle = 0;` |
| HomeView:2486 | `liftIndex = -1;` |
| HomeView:2497 | `if (reordering \|\| moveLift.value > 0.001f) {` |
| HomeView:2498 | `wiggle = WIGGLE_DEG * (float) Math.sin((up - reorderAt) * (2 * Math.PI / WIGGLE_MS));` |
| HomeView:2501 | `wiggle = 0;` |
| SettingsActivity:175 | `handler.postDelayed(this, busy() ? 500 : 2000);` |
| SettingsActivity:182 | `for (int i = 0; i < pop.length; i++) pop[i] = new Spring(0.28f, 0.85f, 0);` |
| SettingsActivity:400 | `float dt = lastNanos == 0 ? 1 / 60f : Math.min(0.05f, (now - lastNanos) / 1e9f);` |
| SettingsActivity:407 | `float seconds = (up - startedAt) / 1000f;` |
| SettingsActivity:456 | `Ui.spinner(c, sx, rowRect.centerY(), 12 * k, seconds * 1.3f, spinner);` |
| SettingsActivity:840 | `if (!message.isEmpty()) handler.postDelayed(() -> { cityStatus = ""; invalidate(); }, 4000);` |
| Ui:214 | `c.drawArc(OVAL, (seconds * 360f) % 360f, 270f, false, stroke);` |
| Frost:44 | `float band = smoothstep(0.02f, 0.38f, v) * (1 - smoothstep(0.5f, 0.92f, v));` |
| Frost:45 | `float curtain = smoothstep(0.35f, 0.85f, n) * (0.35f + 0.65f * band);` |
| Frost:46 | `float m = smoothstep(0.15f, 0.85f, n), k = curtain * 0.85f;` |
| Frost:47 | `float vig = 1 - 0.38f * smoothstep(0.35f, 1, v) - 0.2f * smoothstep(0.45f, 0.95f, Math.abs(u - 0.5f) * 1.6f);` |
| Frost:48 | `float scrim = 1 - 0.22f * (1 - smoothstep(0, 0.42f, v));` |
| Backdrop:205 | `int n = horizontal ? w : h, lines = horizontal ? h : w, step = horizontal ? 1 : w, lineStep = horizontal ? w : 1;` |
| Backdrop:211 | `int p = in[base + Math.max(0, Math.min(n - 1, i)) * step];` |
| Backdrop:217 | `out[base + i * step] = 0xFF000000 \| (Math.round(sr * k) << 16) \| (Math.round(sg * k) << 8) \| Math.round(sb * k);` |
| Backdrop:218 | `int add = in[base + Math.min(n - 1, i + r + 1) * step], sub = in[base + Math.max(0, i - r) * step];` |
| Spring:28 | `int steps = Math.max(1, (int) Math.ceil(dt / 0.004f));` |
| Spring:30 | `for (int i = 0; i < steps; i++) {` |
| Scene:170 | `glass(c, r, radius, tint, Math.max(0.5f, tintAlpha), 0, 0, lift, 1, alpha);` |
| Scene:249 | `glass(c, r, radius, tint, tintAlpha, glow, glowAlpha, lift, body, 1);` |
| HomeApps:590 | `float a = smoothstep(0.07f, 0.22f, dist);` |
| DreamView:42 | `private static final long SHOW_MS = 30_000, FADE_MS = 3_000, FIRST_MS = 1_600;` |
| DreamView:121 | `handler.postDelayed(this, 60_000);` |
| DreamView:155 | `handler.postDelayed(tick, 60_000);` |
| DreamView:176 | `if (!gone.isEmpty()) handler.postDelayed(() -> { for (Bitmap b : gone) b.recycle(); }, 1000);` |
| DreamView:353 | `handler.postDelayed(gone::recycle, 1000);` |
| DreamView:410 | `float first = current == null ? 0 : fadeIn(up - firstAt, FIRST_MS);` |
| DreamView:419 | `float e = incoming == null ? 0 : fadeIn(up - incoming.startedAt, FADE_MS);` |
| DreamView:566 | `float a = current.startedAt == firstAt ? fadeIn(up - firstAt - 700, 900) : 1;` |
| DreamView:579 | `float in = fadeIn(t - 1300, 900);` |

## Base e alcance

Commit: `8569d2127ac1577921764e6ea541dd798f085cb9`. 1518 linhas com literais, 55 ARGBs distintos.

- HomeView.java: 676 linhas com literais.
- SettingsActivity.java: 232 linhas com literais.
- Ui.java: 17 linhas com literais.
- Frost.java: 32 linhas com literais.
- Backdrop.java: 70 linhas com literais.
- Shadows.java: 21 linhas com literais.
- Spring.java: 7 linhas com literais.
- Scene.java: 38 linhas com literais.
- Symbols.java: 16 linhas com literais.
- Team.java: 30 linhas com literais.
- HomeApps.java: 168 linhas com literais.
- DreamView.java: 151 linhas com literais.
- Glyphs.java: 17 linhas com literais.
- Shaders.java: 37 linhas com literais.
- Recording.java: 6 linhas com literais.

Auditoria estática, sem captura da TV neste turno. Código de shaders e coeficientes de Frost/Backdrop foram registrados, mas não serão transformados indiscriminadamente em tokens. Sua extração não deve remover compatibilidade API28, afetar screensaver nem alterar funções do sistema.

Limite da extração: números dentro de strings (incluindo o código AGSL embutido em Shaders.java) não entram na varredura de literais Java. Strings que contêm somente um RGB/ARGB têm a tabela específica acima. O AGSL não executa na Sony API28 e não será migrado nesta tarefa. Valores e cores internos de imagens/ícones não são tokens extraíveis do Java.
