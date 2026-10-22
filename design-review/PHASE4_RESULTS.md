# Fase 4, logos monocromáticos e cantos arredondados

Base: 5fb9043. Trabalho exclusivo no worktree free-my-tv-design, branch design-system.

## O que muda

- Logos claros #F5F5F7 sobre o dock contínuo #2B2C2F; os fundos coloridos por app deixam de ser desenhados.
- Conversão usa as artes existentes e preserva o recorte do triângulo do YouTube. Máscaras calculadas no carregador de segundo plano e guardadas em memória.
- Apps personalizados também usam o extrator existente. Sem extração válida, o fallback é o banner em escala de cinza, com cantos suaves.
- Pôsteres e cards de conteúdo com raio 12; contorno laranja arredondado acompanha a imagem. Contorno dos apps com raio 24.
- Botões do cabeçalho circulares dentro das regiões existentes de 72 × 72, superfície grafite e foco laranja.
- Layout, seleção, navegação, ações, tamanhos dos pôsteres e molas mantidos. Sem alteração de preferências, lógica de home ou arquivos protegidos.

## Validação local

Build JDK 17 concluído. Theme, LogoMask, HomeApps e HomeView compilaram contra android-28.jar, usando core-lambda-stubs.jar do SDK para as lambdas existentes. Assinatura validada para mínimo API 28. Nove testes do conversor de máscara passaram. Sessenta e nove classes fora dos grupos alterados permaneceram binariamente idênticas ao build anterior. Assets e recursos preservados.

Nove métodos de layout e navegação foram comparados diretamente com a fase 3, sem alterações. Os tokens Legacy não mudaram. Relatório completo: phase4-verification.json. Teste reproduzível: tools/LogoMaskTest.java, compilado com LogoMask.java e executado em Java 17.

## Limites e checkpoint

Ainda não instalado na TV. Nenhum print desta fase foi fabricado ou apresentado como captura real. Pedir autorização antes de adb install -r. Depois capturar a Home com o mesmo pôster, conferir os sete logos (especialmente YouTube e BladeTV), os cantos e o foco sobre um app.

A transformação depende da arte dos apps instalados. A rotina do YouTube pressupõe o botão vermelho reconhecível na arte existente. O teste cobre a conversão e transparência, não substitui a inspeção do logo real. Não há medição de desempenho na Sony.

O dock compartilhado também muda na gaveta. Cards de clima, cotações e entradas, grade Todos os Apps, ajustes e Day continuam para outras etapas. Sem push ou merge.
