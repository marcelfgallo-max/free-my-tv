# Fase 2, fundo e barra de apps

Base validada: 9497a90. Worktree free-my-tv-design, branch design-system.

## Mudança preparada

- Fundo sólido #202124 na Home e na gaveta.
- Barra de apps opaca #2B2C2F, com raio de 48 unidades lógicas nos cantos.
- Mesmos bounds da barra e posições de apps e pôsteres. Foco e navegação preservados.
- HomeView deixa de carregar os bitmaps Ember e Ultramarine, e não redesenha o fundo a cada frame de abertura da gaveta.
- Os assets permanecem disponíveis para outros consumidores. Ajustes e screensaver não foram redesenhados.
- Day está apenas declarado como endpoint de fundo/superfície. A interface continua em Night.
- Abas e foco laranja, logos monocromáticos e demais formas serão trabalhados em checkpoints posteriores.

## Validação local

Build com JDK 17 concluído. Theme.java compila contra API 28. Assinatura do APK verificada para mínimo API 28. As 71 classes fora dos grupos alterados permaneceram idênticas ao build anterior. Assets e arquivos protegidos preservados.

Nenhum valor de Theme.Legacy foi trocado pelos valores novos. A verificação de identidade da fase 1 é histórica e não deve ser usada para exigir igualdade após este redesenho intencional.

## Checkpoint na TV

Instalação ainda não realizada. Aguardar autorização de Marcel antes de instalar.
Capturar a Home da fase 1 antes da instalação, repetir o mesmo foco depois e conferir o fundo e a barra. Conferir também a gaveta pelo controle, pois o fundo acompanha as duas telas.

Não há medição de desempenho na TV. A redução de carga de bitmaps é uma mudança de implementação, não uma promessa de ganho de FPS.
