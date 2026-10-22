# Fase 6, números pontilhados e gaveta arredondada

Pedido de Marcel em 22/10/2026, com três referências anotadas. Base 2cfeef6, worktree free-my-tv-design, branch design-system.

## Implementação

- Relógio principal e relógio da Home sem recomendações: números de sete linhas formados por círculos. DotMatrix define os desenhos; DotDisplay mantém um Path por valor e tamanho. A cor vem do tema ativo. É uma reprodução visual original, não o arquivo da fonte da referência.
- Temperatura atual usa o mesmo desenho, inclusive o símbolo de grau e o sinal negativo. O algarismo 1 é mais estreito apenas na temperatura. A altura máxima é 158 unidades, reduzida quando necessário para caber em 280 unidades antes da previsão por hora. O relógio continua com avanços fixos por dígito. Horas da previsão e temperaturas menores permanecem texto monoespaçado.
- Barra de apps com divisórias verticais discretas, tanto na Home quanto na gaveta, em Day e Night. O foco e os logos das fases anteriores continuam ativos.
- Gaveta com títulos, nomes de apps, rótulos, clima e cotações em Typeface.MONOSPACE do Android. Nenhuma fonte externa, biblioteca ou licença adicional foi incorporada.
- Entradas e atalhos em cards individuais arredondados, separados por 12 unidades, com pictogramas vetoriais originais em grade de 24 unidades, traço de 1,65 e tamanho 56. O foco preenche o card com laranja, com ícone branco e texto escuro para contraste, nos dois modos. Nomes de símbolos não cobertos usam os assets anteriores como fallback.
- Clima e cotações ganham superfície arredondada e padding interno de 24 unidades. Divisórias internas pontilhadas. O cache de widgets do Android 9 continua em uso.
- Ícones de clima em 40 unidades para a condição atual e 48 para a previsão. Nuvem, chuva, sol, lua, neblina e trovoada têm desenho vetorial; demais condições continuam com os assets originais. Cotações positivas usam cinza nos dois modos, mantendo o sinal +; quedas continuam vermelhas. O indicador CEC mantém sua cor independente.
- A gaveta sobe mais para acomodar os cards: deslocamento de 700 unidades, linha de controles em y=450, altura 168; widgets em y=650, altura 368. Navegação, regiões acessíveis e foco usam os novos retângulos. Home com pôsteres mantém sua geometria; a saudação acompanha a largura real do relógio pontilhado.
- Nomes extensos dos controles são ajustados e, se necessário, abreviados com reticências. O resultado fica em cache. Cidade, condição e rodapé do clima também respeitam as áreas de texto.

O placar removido não retorna. A lógica de CEC, ações dos controles, timer, recomendações, preferências Day/Night e os arquivos protegidos permanecem intactos. Os assets de ícones e logos foram preservados.

## Validação local

Build com JDK 17 concluído. As cinco fontes novas/alteradas compilam contra API 28. Assinatura do APK verificada para API 28. Testes cobrem todos os 1.440 minutos em formatos de 12 e 24 horas, temperaturas de -99 a 199, sinal negativo Unicode, ausência de dados e limites verticais da gaveta. Os 17 testes de modos e cores passaram. Hashes confirmam que os únicos arquivos de produção existentes alterados são HomeView.java e Theme.java; os dois renderizadores numéricos e DrawerSymbols.java são novos.

Relatório técnico: phase6-verification.json. Teste: tools/DotMatrixTest.java. Não se trata de medição de desempenho ou prova de fidelidade de pixels à referência.

## Prévia local reproduzível

O script tools/render_design_preview.sh executa os próprios fontes DotDisplay e DrawerSymbols com adaptadores de Canvas que geram SVG. Verificou reutilização do Path ao mudar a cor, reconstrução no minuto seguinte, fallback, equilíbrio do estado Canvas, 14 nomes de ícones e limites da temperatura compacta. Esses adaptadores estão somente em tools e não entram no APK.

As prévias Day e Night foram rasterizadas offline e inspecionadas. São exemplos de layout e dados, não prints da TV. O relógio e os pictogramas vêm dos renderizadores de produção; a rasterização, fonte monoespaçada do navegador, símbolos de moeda e gráficos ilustrativos não substituem a verificação Android. A abertura automática de file:// no navegador foi bloqueada pela política da ferramenta, então a inspeção usou imagens estáticas locais, sem navegação ou rede.

A pedido de Marcel antes de dormir, o trabalho ficou pronto para a próxima revisão. Não foi feita instalação, captura ou qualquer comando na TV nesta continuação.

## Instalação concluída, validação visual pendente

Marcel autorizou a instalação em 22/10/2026. O hash do APK conferiu com o build validado, e adb install -r retornou Success. O APK contém d7a30f3 e 9beb764. A captura imediatamente anterior mostrou o protetor de tela, portanto não serve de comparação da Home; serão usadas as capturas verificadas da fase 5 como referência. Aguardando abertura da Home pelo controle para conferir Home e gaveta em Day e Night. Conferir o relógio, divisórias dos logos, foco de entrada/atalho, temperatura, linhas pontilhadas e legibilidade de rótulos. Somente depois registrar capturas como resultado real.

Sem push ou merge. A branch sony-bravia-tweaks e o checkout do Claude Code não foram alterados.

## Home Day capturada na TV

Captura real em screenshots/phase6-home-day.png, após reconectar o ADB. Relógio 07:34 pontilhado, sem sobreposição com a saudação; Explore laranja, contorno laranja no pôster de Super Mario, seis divisórias no dock, logos monocromáticos e cantos arredondados confirmados visualmente. Placar ausente. A gaveta e o Night ainda aguardam captura. O catálogo mudou, portanto não é uma comparação de pixels com a fase 5.

## Gaveta Day capturada na TV

Captura real em screenshots/phase6-drawer-day.png. Confirmados cards arredondados, pictogramas novos, foco laranja na entrada Apple TV com ícone branco e texto escuro, temperatura 18° pontilhada, textos monoespaçados, painéis de clima/cotações e divisórias pontilhadas. Os seis separadores do dock também aparecem. Os widgets cabem na tela. O rótulo PlayStation está abreviado por reticências; fica registrado para melhorar o encaixe, preservando o nome configurado e a navegação. A captura anterior ainda mostrava a Home e não foi usada como evidência da gaveta. Night segue pendente.

## Gaveta Night capturada na TV

Captura real em screenshots/phase6-drawer-night.png. Fundo grafite, dock e cards um pouco mais claros, logos e pictogramas brancos, foco laranja na entrada PlayStation, temperatura e divisórias pontilhadas confirmados. Widgets e rodapés cabem na tela. O rótulo PlayStation também aparece abreviado neste modo. Resta a captura da Home Night. Nenhuma reinstalação foi necessária para alternar o tema.

## Conferência visual final, 22/10/2026

Home Night capturada em screenshots/phase6-home-night.png: relógio 07:44 branco pontilhado, fundo grafite, dock um pouco mais claro, seis divisórias, logos monocromáticos, Explore laranja e contorno no pôster. Sem sobreposição visível com a saudação. As quatro telas foram conferidas na TV: Home e gaveta em Day e Night. A TV ficou em Night.

Verificação adicional em phase6-pixel-check.json: oito amostras de fundo e dock coincidem exatamente com os tokens. Quatro amostras nas divisórias não coincidem com a tinta opaca, pois o traço de 1,5 pixel tem antialiasing; são compatíveis com cobertura de 75%, com tolerância de um nível RGB. Não é comparação integral de pixels: catálogo, foco, horário e cotações mudam. Não houve medição de desempenho ou legibilidade à distância. Refinamento pendente: encaixe do rótulo PlayStation, abreviado nos dois temas. Qualquer APK corrigido precisa de nova autorização antes de instalar.
