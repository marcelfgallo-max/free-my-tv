# Fase 7, protetor de tela

Pedido de Marcel: remover o jogo do São Paulo e adaptar foto, relógio e demais elementos ao tema novo. Implementação d1ef931, sobre 27f61fa, exclusivamente no worktree free-my-tv-design, branch design-system.

## Mudanças

- Remoção completa do placar no DreamView, incluindo instância, listener e atualização de Team. O relógio não reserva uma linha para futebol.
- Fotos de filmes e séries da fonte atual, enquadradas em uma moldura com margem de 64 e raio de 48 unidades. BitmapShader recorta os cantos diretamente, sem clipPath, blur ou APIs novas. O enquadramento cobre a moldura durante todo o zoom e deslocamento.
- Foto Day com saturação 0,95. Night com saturação 0,85 e luminosidade 0,86. ColorMatrixColorFilter criado somente ao aplicar o tema. Fotos são decodificadas na fila de trabalho existente. Não foram baixadas fotos novas para este pacote nem integrado álbum pessoal.
- Painel superior direito, largura 680, com data, clima, relógio pontilhado de altura 132 e cotações compactas. Texto monoespaçado e ícones iguais aos da gaveta. Painel opaco usa SURFACE para garantir contraste sobre qualquer fotografia.
- Legenda inferior esquerda com título em tamanho 52, até duas linhas, metadados ajustados à largura e marcador laranja pequeno. Raio 32 e divisória pontilhada. As legendas mantêm as transições existentes.
- Sem foto disponível, relógio pontilhado de altura 220, fundo do tema, painel arredondado e clima; cotações abaixo. O fundo Ember e a animação de aurora foram retirados desse estado. O deslocamento lento do grupo continua.
- Segue a preferência Day/Night já salva. Mudanças de preferência invalidam as gravações. Não há horário automático novo.
- Mantidos 30 segundos por foto, transição de 3 segundos, entrada de 1,6 segundo e cadência existente de animação. O serviço continua encerrando pelo controle como antes. Nenhuma configuração de energia ou ativação do protetor foi alterada.

## Verificação

Build JDK 17, compilação dos três fontes alterados/novos contra API 28 e verificação da assinatura concluídos. Testes locais cobrem enquadramento em toda a trajetória, formatos 12/24 horas e três escalas. 55.010 asserções de geometria e 17 verificações de tema passaram. As prévias executam os renderizadores reais de pontos e ícones, mas usam layout ilustrativo, foto reservada e tipografia do computador. Não substituem uma captura Android.

SHA do APK e escopo em phase7-verification.json. Arquivos protegidos, HomeView, SettingsActivity, Screensaver e ScreensaverAlone iguais à base. As duas variantes do serviço usam o mesmo DreamView. Adaptadores de teste não entram no APK.

## Validação na TV pendente

Instalação autorizada por Marcel e concluída em 22/10/2026, com SHA-256 conferido e retorno Success. A captura anterior mostrou a Home Day às 08:05, portanto não é um antes do protetor. Aguardando a abertura pelo controle. Para a conferência, abrir o protetor pelo menu da TV ou aguardar sua ativação normal. Capturar foto, relógio, legenda e ausência do placar em Night e Day; observar pelo menos uma troca de foto e testar saída pelo controle. Capturar o estado sem foto somente se ele ocorrer normalmente, sem apagar dados ou configurações para forçá-lo. Build e prévias não comprovam desempenho ou renderização na TV.

Sem push ou merge. O refinamento do rótulo PlayStation da fase 6 continua separado desta etapa.

A prévia Tela de Descanso nos ajustes do próprio Free My TV usa o mesmo DreamView e permite a primeira conferência sem mudar as configurações do sistema. A ativação automática pelo DreamService ainda precisa ser observada separadamente.
