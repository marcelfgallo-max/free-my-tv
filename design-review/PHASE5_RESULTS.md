# Fase 5, restaurar Day e Night

Solicitação de Marcel em 22/10/2026. Base: 6ed6121. Worktree free-my-tv-design, branch design-system.

## Implementação

A Home estava fixa em Night e não havia preferência de tema neste branch. Agora Ajustes do Início > Tema oferece Day e Night, com Night como padrão. A escolha usa o SharedPreferences existente, chave theme. Valores desconhecidos voltam para Night. OK e as setas alternam os modos; a Home lê a preferência ao abrir ou retomar.

Day aplica fundo #E6E5E8, superfície #FAF9FB, logos/texto #292B2E e texto secundário #62636A. Night preserva os endpoints atuais. Foco e Explore continuam #FF5A20. Home, gaveta, clima, cotações e placeholders recebem tintas correspondentes. Caches de widgets são separados por modo. Não há transição animada de tema, novos assets ou dependências.

Ajustes mantém seu visual atual, com a nova linha de seleção. Screensaver e sobreposição Todos os Apps conservam suas próprias tintas. Não há automatismo por horário. Não altera configurações do sistema ou lógica de troca de home.

## Validação local

Build JDK 17 concluído. Sete fontes alteradas compilam contra API 28 com core-lambda-stubs.jar. Assinatura do APK verificada. Quinze testes host passaram: padrão seguro, restauração dos valores, endpoints Night preservados, contraste de texto/logos, foco laranja em ambos e rejeição de papel desconhecido. O teste host não simula persistência Android. A escolha salva foi conferida por Marcel ao reabrir os ajustes e alternar os modos na TV, conforme registro abaixo.

Assets, recursos, arquivos protegidos e tokens Legacy preservados. Relatório: phase5-verification.json. Teste: tools/ThemeModeTest.java.

## Instalação e capturas reais

Instalação autorizada por Marcel e concluída com retorno Success. O APK instalado contém f2d381a (Day/Night) e 233083f (remoção do placar). Nenhuma alteração adicional de código foi necessária durante a conferência visual.

| Captura | Resultado |
|---|---|
| phase5-night-before.png | Home Night anterior, com o placar |
| phase5-night-after.png | Home Night instalada, John Candy selecionado, sem placar |
| phase5-day-home.png | Home Day, textos e logos escuros, dock claro, foco laranja |
| phase5-day-drawer.png | Gaveta Day, entrada Apple TV selecionada, clima e cotações legíveis |
| phase5-day-restored.png | Home Day após a alternância relatada, Netflix em foco; catálogo renovado |

Trinta e duas verificações de pixels passaram: fundos, superfícies, accent, tinta dos sete logos em cada modo, região antes ocupada pelo placar e cores dos widgets na gaveta Day. As capturas são originais, sem retoques. Não há alegação de identidade integral entre temas ou de medição de desempenho.

Duas capturas intermediárias não corresponderam ao estado solicitado pelo roteiro: uma ainda estava em Night e outra na Home Day em vez da gaveta. Foram substituídas na comparação pelos estados conferidos visualmente. Não foi possível atribuir uma causa a essa diferença de momento/navegação; ela não demonstrou defeito na persistência.

## Remoção do futebol

A pedido de Marcel, o placar e o próximo jogo foram removidos do cabeçalho. A Home deixa de iniciar/atualizar os dados de futebol e registrar seu listener. Posições do relógio, saudação e botões preservadas. A remoção acompanha a instalação autorizada dos temas e foi confirmada nas capturas.

## Alternância e escolha salva

Marcel confirmou ter voltado para Night e depois para Day, com ambos os valores salvos corretamente. O estado final ficou em Day por sua escolha. A captura phase5-day-restored.png confirma esse estado final e o foco laranja no Netflix. Não foi feita captura intermediária do retorno ao Night nem da linha Theme, portanto a confirmação de alternância e escolha salva nesses passos é do usuário, não uma medição automatizada. Não houve teste após reiniciar a TV ou encerrar o processo. Ajustes, screensaver e Todos os Apps mantêm suas próprias cores. A seleção é manual, sem horário automático.

Trabalho no worktree separado, branch design-system. Nenhum push ou merge.
