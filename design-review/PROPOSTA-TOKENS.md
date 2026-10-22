# Proposta de tokens e migração

Status: PROPOSTA, aguardando o OK de Marcel. Data: 21/10/2026, Brasília.

Alvo: Sony BRAVIA XBR-55X905E, Android TV 9/API28, Java/Canvas, JDK17, build.sh, sem bibliotecas novas. Base local: commit8569d21. Nada desta proposta já foi aplicado ao aplicativo.

## Direção consolidada

A referência vigente é a interface inspirada na Nothing, nas variantes Day e Night. A principal Day tem Explore laranja. A principal Night tem fundo grafite e dock só um tom mais claro, também com Explore laranja. As referências verdes e as versões de vidro anteriores não são alvo desta migração.

As imagens são prévias geradas, não telas com metadados de Figma ou fontes identificáveis. Os números abaixo serão o contrato determinístico após aprovação. A comparação com imagens orientará geometria e hierarquia, mas não se deve copiar ruído, deformações de logos, falhas de texto ou variações de cor dos pixels gerados.

A gaveta Night aprovada antecede o ajuste final do grafite. Ela fornece a estrutura; os tokens Night da principal atualizada prevalecem no fundo e nas superfícies. O laranja #FF5A20 prevalece sobre o vermelho mais intenso que aparece nas primeiras gavetas.

## Cores propostas

Hex nesta tabela é RGB. Na implementação Java deve receber alphaFF para cores opacas.

| Token | Day | Night | Uso |
|---|---|---|---|
| BACKGROUND | #E6E5E8 | #202124 | Fundo liso da home, gaveta e ajustes |
| SURFACE | #FAF9FB | #2B2C2F | Dock, cards de informação, botões e cápsula do placar |
| SURFACE_PRESSED | #DEDEE2 | #36373B | Pressionado, sem sombra |
| TEXT_PRIMARY | #292B2E | #F5F5F7 | Texto principal e ícones |
| TEXT_SECONDARY | #62636A | #B8B9BF | Metadata, rótulos secundários |
| TEXT_DISABLED | #88898F | #77797F | Controle indisponível, com estado semântico |
| DIVIDER | #D7D7DD | #414246 | Separadores finos somente onde ajudam |
| ACCENT | #FF5A20 | #FF5A20 | Aba ativa, preenchimento de controle focado |
| ON_ACCENT | #FFFFFF | #FFFFFF | Ícone e texto grande sobre laranja |
| ON_ACCENT_SMALL | #292B2E | #292B2E | Texto pequeno sobre laranja, se necessário |
| FOCUS | #FF5A20 | #FF5A20 | Contorno de pôster ou logo focado |
| NEGATIVE | #C43B24 | #FF6B4A | Variação negativa, sempre com sinal menos |
| POSITIVE | #62636A | #D3D4D8 | Variação positiva monocromática, sempre com sinal mais |
| WARNING | #A84A00 | #FFAD66 | Avisos dos ajustes, distinto de seleção |
| CEC_ACTIVE | #21854C | #55CA85 | Ponto de dispositivo HDMI ligado, preserva o significado de ligado |

O CEC é um indicador funcional já pedido na WISHLIST, não uma nova cor decorativa. Se o foco estiver laranja, usar ponto CEC com pequeno contorno na cor da superfície para separar as cores. Não confundir HDMI ligado com HDMI focado.

Contraste calculado: branco sobre #FF5A20 ≈3.12:1, destinado a rótulos grandes; texto pequeno usa ON_ACCENT_SMALL, ≈4.55:1. Na Night, TEXT_PRIMARY sobre BACKGROUND ≈14.79:1 e TEXT_SECONDARY ≈8.23:1. Isso é cálculo de cor, não uma certificação de acessibilidade da TV. O tamanho real e a leitura à distância entram na validação física.

## Tipografia proposta

Reutilizar `app/assets/fonts/InterVariable.ttf`, com licença já existente. Não substituir a fonte da aplicação inteira antes da etapa de tipografia. Os títulos pontilhados serão desenhados com círculos e glifos precomputados, sem biblioteca ou fonte proprietária. Só relógio e temperatura grande usam pontilhado. Garantir dígitos0-9, dois-pontos, grau e sinal de menos, sem piscadas. Atualizar o cache apenas quando valor, escala ou tema mudar.

Unidades abaixo: canvas1920×1080. São novos alvos, não substituições da fase de equivalência.

| Token | Valor alvo | Uso |
|---|---|---|
| FONT_BODY | Inter, peso400, opsz14 | Texto comum |
| FONT_LABEL | Inter, peso400/500, opsz14 | Rótulos e ações |
| FONT_DISPLAY | Inter, peso400, opsz32 | Saudação e números não pontilhados |
| TYPE_GREETING |40|Saudação|
| TYPE_SECTION |26|Cabeçalhos das fileiras|
| TYPE_NAV |26|Abas, branco em laranja|
| TYPE_BODY |26|Informação comum|
| TYPE_META |22|Ano, serviço, informação auxiliar|
| TYPE_CONTROL |24|Entradas e atalhos|
| TYPE_MARKET_VALUE |30|Nome/preço da cotação|
| TYPE_FORECAST_VALUE |34|Temperatura horária|
| TYPE_SETTINGS_ROW |28|Ajustes|
| TYPE_SETTINGS_TITLE |64|Título do painel de ajustes, proposta sem mockup específico|
| DOT_CLOCK_HEIGHT |96|Altura visual dos glifos, não tamanho tipográfico|
| DOT_TEMPERATURE_HEIGHT |152|Altura visual dos glifos|
| TRACKING_BODY |0 em|Texto comum|
| TRACKING_SECTION |0.02 em|Pequenos cabeçalhos|
| LINE_HEIGHT_FACTOR |1.2|Textos de mais de uma linha|

A aparência monoespaçada dos rótulos de algumas prévias é uma sugestão visual. Não está incluída uma fonte monoespaçada adicional nesta etapa. Usar Inter consistente antes de decidir se isso precisa de um asset licenciado novo. Cotações e horários usam tabular numbers (`tnum`).

## Espaçamento e geometria propostos

A primeira migração conserva todas as posições atuais. A tabela abaixo pertence à etapa posterior de visual novo.

| Token | Alvo | Uso |
|---|---:|---|
| DESIGN_WIDTH / DESIGN_HEIGHT |1920 /1080|Sistema de coordenadas|
| SPACE_1..SPACE_8 |4,8,12,16,24,32,48,64|Escala de espaços; não arredondar valores legados na fase1|
| CONTENT_MARGIN_X |80|Laterais de conteúdo principal|
| DOCK_MARGIN_X |48|Dock mais largo que as fileiras|
| MAIN_CLOCK_TOP |64|Topo visual do relógio|
| NAV_TOP / NAV_HEIGHT |184 /56|Cápsulas das abas|
| RAIL1_LABEL_BASE / RAIL1_TOP |298 /316|Primeira fileira|
| RAIL2_LABEL_BASE / RAIL2_TOP |598 /620|Segunda fileira|
| POSTER_COLUMNS / POSTER_GAP |11 /12|Quantidade e respiro horizontal|
| POSTER_WIDTH |(1920-2×80-10×12)/11 =149.09|Distribuir arredondamento por coordenada acumulada|
| POSTER_HEIGHT |224|Proporção próxima de2:3, não esticar imagens|
| DOCK_TOP / DOCK_HEIGHT |888 /136|Dock da principal|
| CONTROL_SIZE |88|Botões redondos do cabeçalho|
| DRAWER_GROUP_GAP |32|Entre entradas e atalhos, alvo inicial|
| CARD_PADDING |32|Clima/cotações|
| SETTINGS_ROW_HEIGHT / GAP |74 /8|Linha de ajuste; área de foco alinhada à geometria|

A composição é16:9. As prévias medem1672×941 e não são arquivos1920×1080, apesar do formato solicitado durante a geração. Medidas acima são normalizadas e arredondadas. Não esticar imagens no eixo vertical para compensar a diferença de proporção. Manter o modelo atual `k` e validar o tamanho efetivo do Canvas na Sony antes de mudar qualquer transformação global.

O objetivo da migração visual é a estrutura aprovada. Rolagem, hitboxes, foco, clipping e bounds de acessibilidade devem usar a mesma geometria. Alterações de margem e densidade exigem print da Sony; não podem entrar no commit de extração de constantes.

## Raios, bordas, elevação e alpha

| Token | Alvo | Uso |
|---|---:|---|
| RADIUS_POSTER |12|Pôsteres|
| RADIUS_CARD |40|Clima, cotações e entradas|
| RADIUS_DOCK |48|Cápsula dos apps|
| RADIUS_TAB |28|Aba de altura56|
| RADIUS_ROUND_BUTTON |44|Botão88|
| RADIUS_SETTINGS_ROW |18|Linha de ajuste|
| STROKE_DIVIDER |1|Separação fina, snap à grade de pixels|
| STROKE_FOCUS |3|Foco sem halo|
| FOCUS_OUTSET |4|Contorno fora do elemento sem alterar layout|
| SHADOW_BLUR / SHADOW_ALPHA |0 /0|Sem sombra no visual novo|
| GLOW_ALPHA / BACKDROP_BLUR |0 /0|Sem brilho ou blur no visual novo|
| SURFACE_ALPHA |1|Superfície opaca, diferença pela cor|
| FOCUS_SCALE |1|Sem zoom no repouso do foco|

Em API28, pôster arredondado deve reutilizar a estratégia BitmapShader já existente em `roundImage`, sem clipPath por frame e sem criação de bitmap dentro de onDraw. Arredondar não pode introduzir renderização mais pesada que o caminho legado. Não remover as rotinas de blur que ainda servem outros componentes ou o screensaver.

## Movimento proposto

| Token | Alvo | Uso |
|---|---|---|
| FOCUS_RESPONSE_S / FOCUS_DAMPING |0.20 /1.00|Resposta de foco sem quique|
| DRAWER_RESPONSE_S / DRAWER_DAMPING |0.30 /1.00|Abrir/fechar gaveta|
| SHEET_RESPONSE_S / SHEET_DAMPING |0.30 /1.00|All Apps|
| THEME_CROSSFADE_MS |180|Troca explícita/automática Day-Night, validar custo|
| PRESS_FEEDBACK_MS |80|Feedback de OK|
| TOAST_MS |6000|Manter leitura atual|
| HOLD_REORDER_MS |500|Manter interação atual|

Springs não têm duração fixa; não chamar resposta0.20s de animação de200ms. Preservar tempos do app na primeira fase. Mudança de parâmetros físicos vem em commit próprio. Em repouso, manter invalidation por mudança, não atualizar números/bitmaps a cada frame.

## Componentes e estados

| Componente | Default | Selecionado/focado | Preservar |
|---|---|---|---|
| Aba |Texto principal sobre fundo|Aba ativa: cápsula laranja e texto branco; foco navegável adicional deve ser distinguível sem duplicar ações|Explore/Movies/Shows/Continue/Catalog, mesma ação e ordem|
| Card de app |Logo monocromático em dock contínuo|Contorno laranja3, sem escala; preenchimento só se indispensável na etapa aprovada|Launch real, ordem, reorder, apps ausentes|
| Pôster |Arte original colorida, raio12|Contorno laranja3 e metadata correspondente|22 itens, gêneros/páginas e deep links|
| Entrada HDMI |Superfície arredondada, ícone/texto|Laranja com ícone branco e rótulo grande|Estado ligado CEC separado do foco; conexão e nomes|
| Botão redondo |Superfície, ícone monoline|Laranja e ícone branco|TV/entradas/ajustes e labels de acessibilidade|
| Clima/cotações |Card opaco, conteúdo organizado|Normalmente informativo; preservar foco para leitor de tela|Cache, dados vivos, sinais+,−, indisponível/desatualizado|
| Linha de ajuste |Texto e valor, grupo estável|Linha arredondada focada; sem trocar ação de negócio|Todas as preferências, diálogos, avisos e modo original|

Manter exatamente um foco de navegação. Aba ativa e item focado são estados diferentes: a aba pode permanecer laranja quando o foco está no pôster. Não usar cor sozinha como indicação de foco, pois forma/contorno e posição também devem ajudar.

## Estrutura futura de Theme.java

Somente após aprovação:

- Constantes e helpers sem alocação por frame, sem dependências, compatíveis com API28.
- Primeiro `Theme.Legacy` ou nomes explicitamente legados que copiem os valores atuais1:1. Não substituir dois valores semelhantes por um valor médio.
- Depois roles semânticos como BACKGROUND, SURFACE, TEXT_PRIMARY, TEXT_SECONDARY, ACCENT, FOCUS.
- API pretendida: `Theme.color(Theme.SURFACE, night)` para endpoints Day/Night. Para transição contínua, helper separado com float0..1 e o mesmo arredondamento atual; não trocar interpolação por boolean na fase1.
- Métricas do Canvas e métricas tipográficas distintas. Não converter px lógicos para dp/sp.
- `DESIGN_SYSTEM.md` documentará cada token, valor legado/alvo, consumidor e estado. Esta proposta não é esse arquivo final.

## Sequência de implementação, com checkpoints

0. OK de Marcel para os tokens e a fase1.
1. Criar Theme.Legacy e documentação, migrar um grupo de valores por commit sem mudar o desenho. Começar por cores/texto da HomeView; depois métricas; depois SettingsActivity. Não ativar Day nessa etapa.
2. Build com JDK17. Antes de instalar, pedir autorização para evitar concorrência com Claude Code. Captura antes e depois com os mesmos dados/estado quando possível. Comparar regiões estáticas; relógio, conteúdo remoto e placar podem mudar naturalmente.
3. Só após equivalência e OK, aplicar fundo e dock grafite. Build, instalação autorizada, print e OK antes de seguir.
4. Abas e foco laranja. Mesmo checkpoint.
5. Logos monocromáticos e formas arredondadas, preservando assets reais. Mesmo checkpoint.
6. Entradas/atalhos e cards da gaveta. Mesmo checkpoint.
7. Tipografia pontilhada e demais métricas. Mesmo checkpoint.
8. Day e seleção de tema. Reusar preferências existentes após inspecionar sua semântica; não adicionar switch sem necessidade. Mesmo checkpoint.
9. Ajustes. Não há tela alvo aprovada, aplicar tokens aos componentes atuais, apresentar print.
10. Movimento, desempenho e conferência final.

Uma etapa por vez. Se alterar geometria, atualizar foco/hitboxes/bounds junto. Se houver diferença inesperada na fase1, corrigir antes de avançar. Não usar o mockup como screenshot da TV nem declarar equivalência com base em build.

## Regras de operação

- Trabalhar só no branch design-system da worktree /Users/marcelgallo/free-my-tv-design.
- Não editar CLAUDE.md nem WISHLIST.md.
- Não tocar HomeMode.java, Adb.java, ModeJob.java ou Watchdog.java.
- Não tocar a implementação web: o alvo deste trabalho é a Sony Android API28, apesar da orientação genérica do CLAUDE.md de espelhar alterações web.
- Não imprimir, commitar ou incluir em relatório/ZIP config.properties e freemytv.keystore.
- JDK17 antes de ./build.sh. Sem Gradle, novas bibliotecas ou APIs superiores a28.
- ADB: conectar apenas conforme o fluxo fornecido; instalar somente depois de autorização explícita. Operações na TV limitadas à instalação autorizada e screencap. Não executar input/keyevent para navegação. Marcel posiciona a tela pelo controle.
- Não mudar configurações da TV nem lógica de troca de home. Nenhum merge, rebase, reset hard ou force push.
- Commits pequenos por etapa. A worktree foi criada pelo comando pedido e inicialmente rastreia origin/sony-bravia-tweaks; nunca usar push sem destino. Quando houver entrega para publicar, somente `git push -u origin design-system`, após conferir branch e arquivos staged.
- O OK para o plano não é autorização para instalar na TV. Perguntar antes de cada instalação.

## Evidência esperada

Para cada etapa: commit, arquivos alterados, saída de build sem dados privados, print antes/depois e descrição do que mudou. Primeiro print precisa ser captura real da TV no estado atual. As quatro imagens da pasta references são alvos de design, não capturas de teste.
