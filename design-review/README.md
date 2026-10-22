# Revisão do design system e fase 1

1. Leia INVENTARIO.md para os valores existentes.
2. Abra inventario.html para filtrar as1518 linhas de evidências, ou consulte o CSV.
3. Leia PROPOSTA-TOKENS.md e confira os quatro alvos em references.
4. Proposta aprovada por Marcel. Consulte ../DESIGN_SYSTEM.md e PHASE1_RESULTS.md para a extração dos valores legados.

O inventário e a proposta são registros da revisão inicial. A implementação da fase 1 está em Theme.java e nos consumidores HomeView, SettingsActivity e Ui. Nenhum arquivo privado está incluído. O APK da fase 1 foi instalado na TV com autorização de Marcel. A Home passou na comparação de pixels, exceto o cabeçalho dinâmico excluído. Gaveta e ajustes ainda aguardam comparação pareada. Consulte phase1-build-install.json.

## Imagens de referência

- principal-day.png: última Day, Explore laranja.
- principal-night.png: última Night, grafite e dock discretamente mais claro, Explore laranja.
- gaveta-day.png: estrutura Day. Na implementação, unificar o accent para #FF5A20 conforme a proposta.
- gaveta-night.png: estrutura Night. A paleta da principal Night mais recente prevalece sobre o preto/vermelho desta imagem anterior.

As referências são imagens geradas e não screenshots de teste. Elas medem1672×941. O código usa canvas1920×1080, com a escala k existente.
