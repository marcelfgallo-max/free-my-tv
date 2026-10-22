# Prévia SVG local

Execute `./tools/render_design_preview.sh CAMINHO_DE_SAIDA` com JDK 17. Não precisa de Android, TV, rede ou credenciais.

DotDisplay e DrawerSymbols são compilados a partir dos mesmos fontes de produção. As classes android.graphics deste diretório são adaptadores somente para teste, que traduzem comandos para SVG. Elas nunca entram no APK, pois build.sh compila apenas app/src.

O programa verifica reaproveitamento dos Paths ao mudar a tinta, reconstrução ao mudar o minuto, fallback, equilíbrio save/restore, ícones e temperatura compacta.

As superfícies e posições de exemplo são uma ilustração do layout. Fontes monoespaçadas do navegador, preços, gráficos e símbolos de moeda são demonstrativos. O resultado não equivale a screenshot Android nem comprova desempenho, alinhamento final dos textos ou navegação no controle. Não usar o SVG como evidência de instalação.
