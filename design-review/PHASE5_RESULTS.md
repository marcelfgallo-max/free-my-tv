# Fase 5, restaurar Day e Night

Solicitação de Marcel em 22/10/2026. Base: 6ed6121. Worktree free-my-tv-design, branch design-system.

## Implementação

A Home estava fixa em Night e não havia preferência de tema neste branch. Agora Ajustes do Início > Tema oferece Day e Night, com Night como padrão. A escolha usa o SharedPreferences existente, chave theme. Valores desconhecidos voltam para Night. OK e as setas alternam os modos; a Home lê a preferência ao abrir ou retomar.

Day aplica fundo #E6E5E8, superfície #FAF9FB, logos/texto #292B2E e texto secundário #62636A. Night preserva os endpoints atuais. Foco e Explore continuam #FF5A20. Home, gaveta, clima, cotações, placar e placeholders recebem tintas correspondentes. Caches de widgets são separados por modo. Não há transição animada de tema, novos assets ou dependências.

Ajustes mantém seu visual atual, com a nova linha de seleção. Screensaver e sobreposição Todos os Apps conservam suas próprias tintas. Não há automatismo por horário. Não altera configurações do sistema ou lógica de troca de home.

## Validação local

Build JDK 17 concluído. Sete fontes alteradas compilam contra API 28 com core-lambda-stubs.jar. Assinatura do APK verificada. Quinze testes host passaram: padrão seguro, restauração dos valores, endpoints Night preservados, contraste de texto/logos, foco laranja em ambos e rejeição de papel desconhecido. O teste não simula persistência Android; ela usa o mecanismo existente e ainda requer teste na TV.

Assets, recursos, arquivos protegidos e tokens Legacy preservados. Relatório: phase5-verification.json. Teste: tools/ThemeModeTest.java.

## Checkpoint pendente

Antes de instalar, obter autorização específica. Capturar Night antes, instalar, confirmar abertura. Pelo controle: Ajustes do Início > Tema > Day, retornar à Home, capturar Home e gaveta. Reabrir os ajustes e confirmar Day salvo. Escolher Night, voltar e capturar para conferir a restauração. Não afirmar validação visual ou persistência real antes desses passos.

Nenhum push ou merge. A anotação de restauração só deve ser concluída depois do checkpoint.
