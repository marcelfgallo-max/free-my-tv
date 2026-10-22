# Fase 8, alinhamento da Home e alternativas do screensaver

Feedback de Marcel em 22/10/2026. Base 8e1bec5, worktree free-my-tv-design, branch design-system.

## Home implementada, Day e Night

A cápsula da aba selecionada usa somente preenchimento laranja, inclusive quando focada pelo controle. A navegação já seleciona a aba ao mover o foco, então o anel externo não é necessário. Seu limite esquerdo foi alinhado à margem 96 do relógio e da grade. Os limites acessíveis acompanham a cápsula.

Separador superior em y=256; títulos das duas fileiras em y=296/584; divisórias 16 unidades abaixo e pôsteres 32 unidades abaixo. Os 11 pôsteres mantêm tamanho, proporção e colunas originais, fechando em x=1824. Separadores usam SOFT_DIVIDER nos dois temas, com contraste mais discreto. Título selecionado, gênero e metadados respeitam limites de largura separados.

Gaveta preservada, inclusive layout, ícones, superfícies e tipografia. Seu bloco de desenho foi comparado com a base e permanece idêntico. O pedido anterior sobre o rótulo PlayStation não foi incluído, pois Marcel aprovou a gaveta e pediu apenas Home e screensaver nesta revisão.

Build JDK 17, API 28 e assinatura passaram. Coordenadas e limites da grade foram verificados localmente. Ainda não instalado nem conferido na TV.

## Screensaver, prévias para escolher antes de aplicar

Interpretamos os retângulos anotados como cards mais compactos: relógio aproximadamente 616 × 272, legenda aproximadamente 640 × 196, mantendo suas âncoras e reduzindo o espaço vazio. Conteúdo longo deverá poder aumentar a altura da legenda até duas linhas, sem truncar o título desnecessariamente.

Comparação solicitada: A com superfícies opacas, B com 10% de transparência (90% de opacidade). Texto e ícones continuam opacos. Sugestão de accent: apenas os dois pontos do relógio em laranja, combinando com o pequeno marcador existente na legenda. Não adicionar contorno laranja ao card.

As imagens geradas são estudos de aparência. Elas não são capturas da TV e não comprovam 90% de alpha exato. O valor exato será aplicado em Canvas após a escolha. DreamView permanece igual à versão instalada nesta etapa. O primeiro lote de edição por imagem foi recusado pela ferramenta; a tentativa seguinte simplifica a edição aos cards.

Sem instalação, push ou merge. A escolha do screensaver e a autorização para instalar permanecem separadas.
