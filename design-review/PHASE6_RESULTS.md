# Fase 6, números pontilhados e gaveta arredondada

Pedido de Marcel em 22/10/2026, com três referências anotadas. Base 2cfeef6, worktree free-my-tv-design, branch design-system.

## Implementação

- Relógio principal e relógio da Home sem recomendações: números de sete linhas formados por círculos. DotMatrix define os desenhos; DotDisplay mantém um Path por valor e tamanho. A cor vem do tema ativo. É uma reprodução visual original, não o arquivo da fonte da referência.
- Temperatura atual usa o mesmo desenho, inclusive o símbolo de grau e o sinal negativo. A altura diminui quando necessário para caber antes da previsão por hora. Horas da previsão e temperaturas menores permanecem texto monoespaçado.
- Barra de apps com divisórias verticais discretas, tanto na Home quanto na gaveta, em Day e Night. O foco e os logos das fases anteriores continuam ativos.
- Gaveta com títulos, nomes de apps, rótulos, clima e cotações em Typeface.MONOSPACE do Android. Nenhuma fonte externa, biblioteca ou licença adicional foi incorporada.
- Entradas e atalhos em cards individuais arredondados, separados por 12 unidades, com ícones existentes ampliados de 34 para 48 unidades. O foco preenche o card com laranja e usa texto escuro para contraste, nos dois modos.
- Clima e cotações ganham superfície arredondada e padding interno de 24 unidades. Divisórias internas pontilhadas. O cache de widgets do Android 9 continua em uso.
- A gaveta sobe mais para acomodar os cards: deslocamento de 700 unidades, linha de controles em y=450, altura 168; widgets em y=650, altura 368. Navegação, regiões acessíveis e foco usam os novos retângulos. Home com pôsteres mantém sua geometria; a saudação acompanha a largura real do relógio pontilhado.
- Nomes extensos dos controles são ajustados e, se necessário, abreviados com reticências. O resultado fica em cache. Cidade, condição e rodapé do clima também respeitam as áreas de texto.

O placar removido não retorna. A lógica de CEC, ações dos controles, timer, recomendações, preferências Day/Night e os arquivos protegidos permanecem intactos. Os assets de ícones e logos foram preservados.

## Validação local

Build com JDK 17 concluído. As quatro fontes novas/alteradas compilam contra API 28. Assinatura do APK verificada para API 28. Testes cobrem todos os 1.440 minutos em formatos de 12 e 24 horas, temperaturas de -99 a 199, sinal negativo Unicode, ausência de dados e limites verticais da gaveta. Os 15 testes existentes de modos e cores passaram. Hashes confirmam que os únicos arquivos de produção existentes alterados são HomeView.java e Theme.java; os dois renderizadores numéricos são novos.

Relatório técnico: phase6-verification.json. Teste: tools/DotMatrixTest.java. Não se trata de medição de desempenho ou prova de fidelidade de pixels à referência.

## Instalação e validação visual pendentes

APK preparado, ainda não instalado. Pedir autorização específica antes de instalar, evitando conflito com Claude Code. Capturar a Home antes, instalar e conferir Home e gaveta em Day e Night pelo controle. Conferir o relógio, divisórias dos logos, foco de entrada/atalho, temperatura, linhas pontilhadas e legibilidade de rótulos. Somente depois registrar capturas como resultado real.

Sem push ou merge. A branch sony-bravia-tweaks e o checkout do Claude Code não foram alterados.
