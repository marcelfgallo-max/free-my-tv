# Fase 9, Day/Night automático

Base c20fae7. Worktree free-my-tv-design, branch design-system. 22/10/2026.

## Comportamento

- Home Settings > Theme: Day, Night ou Automatic.
- OK abre o seletor. Setas alternam os modos sem abrir o diálogo.
- Schedule / Horários: escolha Day ou Night e ajuste em passos de30min.
- Padrão Day07:00, Night19:00, hora e fuso locais da TV.
- Editar horários preserva a escolha de modo. Horários iguais são recusados.
- A preferência atual continua salva, sem ativação forçada do automático após instalar.
- Home/gaveta atualizam ao retornar e no tick existente de15s; screensaver ao abrir e no tick de60s.
- Não altera relógio, brilho, configurações do sistema ou lógica de troca de home.

## Verificações concluídas

Build JDK17 e build.sh, assinatura minSDK28, compilação dos arquivos alterados contra android-28.jar. ThemeMode18checks e ThemeSchedule3264063checks. Casos incluem meia-noite, todos os pares do editor e seus minutos, mudanças de fuso, hora avançando/retrocedendo, fallback de agenda inválida e modos manuais.

CLAUDE.md, WISHLIST.md e arquivos protegidos comparados com a base e preservados. Nenhuma biblioteca, permissão ou serviço novo. Sem instalação ou push nesta etapa.

## Conferência pendente na TV

Após autorização para instalar: abrir Theme e selecionar Automatic, conferir os horários07:00/19:00 e aparência correspondente. Testar edição e retorno às configurações para confirmar persistência. Conferir Home, gaveta e screensaver. Testar troca temporariamente com horário próximo e depois restaurar os horários desejados, sem alterar a hora do sistema.
