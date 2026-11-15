-- Illustrations de cartes, 24×24, dessinées par formes (voir Canvas dans generate.lua).
-- Ébauches : generate.lua ne crée que les tags absents de art/cards.aseprite.
-- Aucun logo ni marque : l'« EPFL » est un bâtiment de campus générique.

local function flame(c, x, y, s)
  c:tri(x - s, y, x + s, y, x + s + 2, y - 3 * s, "R")
  c:disc(x, y, s, "R")
  c:disc(x, y, s - 1, "o")
  if s > 2 then c:disc(x, y + 1, s - 2, "Y") end
end

local function flask(c, cx, cy, r, liquid, shade)
  c:rect(cx - 2, cy - r - 5, 5, 6, "4")
  c:rect(cx - 1, cy - r - 4, 3, 5, "1")
  c:rect(cx - 3, cy - r - 6, 7, 2, "n")
  c:disc(cx, cy, r, "4")
  c:disc(cx, cy, r - 1, liquid)
  c:ellipse(cx + 1, cy + 2, r - 2, r - 3, shade)
  c:ellipse(cx, cy + 1, r - 2, r - 3, liquid)
  c:rect(cx - r + 3, cy - 3, 1, 3, "6")
  c:px(cx - r + 4, cy - 4, "6")
end

local function pine(c, x, base, h, light, dark)
  c:rect(x - 1, base - 2, 2, 3, "n")
  for i = 0, 2 do
    local top = base - h + i * (h // 4)
    local half = 2 + i * 2
    c:tri(x, top, x - half, top + h // 3 + 1, x + half, top + h // 3 + 1, light)
    c:tri(x, top + 1, x + 1, top + h // 3 + 1, x + half, top + h // 3 + 1, dark)
  end
end

return {
  -- ---- sorts
  { id = "pluie-de-flammes", draw = function(c)
    flame(c, 6, 18, 3); flame(c, 14, 13, 3); flame(c, 8, 8, 2); flame(c, 18, 20, 2); flame(c, 19, 6, 2)
    c:line(3, 23, 21, 23, "d")
  end },
  { id = "jet-d-eau", draw = function(c)
    c:tri(2, 12, 19, 5, 19, 19, "b")
    c:tri(3, 12, 19, 7, 19, 17, "B")
    c:tri(5, 12, 19, 10, 19, 14, "C")
    c:hline(6, 16, 12, "6")
    c:disc(21, 6, 1, "C"); c:disc(22, 12, 1, "B"); c:disc(21, 18, 1, "C")
    c:px(23, 9, "6"); c:px(23, 15, "6"); c:px(20, 3, "C"); c:px(20, 21, "C")
    c:rect(0, 10, 3, 5, "3"); c:rect(0, 11, 2, 3, "4")
  end },
  { id = "tsunami", draw = function(c)
    c:rect(0, 17, 24, 7, "b")
    c:disc(10, 12, 9, "b")
    c:disc(9, 11, 7, "B")
    c:disc(14, 14, 6, ".")
    c:rect(13, 17, 11, 7, "b")
    c:arc(10, 12, 9, 9, 180, 320, "6")
    c:arc(10, 12, 8, 8, 200, 300, "C")
    c:px(17, 7, "6"); c:px(19, 9, "6"); c:px(20, 12, "C"); c:px(18, 5, "C")
    c:hline(2, 8, 20, "B"); c:hline(14, 21, 21, "B"); c:hline(6, 12, 22, "u")
  end },
  { id = "fouet-de-lianes", draw = function(c)
    for i = 0, 40 do
      local t = i / 40
      local x = 3 + t * 17
      local y = 20 - t * 15 + math.sin(t * 6.3) * 3
      c:disc(x, y, 1, "g")
      c:px(x, y - 1, "G")
    end
    c:tri(8, 15, 5, 11, 9, 12, "G"); c:tri(13, 14, 16, 17, 12, 17, "G")
    c:tri(16, 8, 13, 5, 17, 5, "G")
    c:arc(20, 5, 2, 2, 150, 420, "g")
    c:px(21, 3, "G")
  end },
  { id = "tempete-d-epines", draw = function(c)
    local thorns = { { 12, 12, 3, 2 }, { 12, 12, 21, 3 }, { 12, 12, 22, 20 }, { 12, 12, 4, 21 },
                     { 12, 12, 12, 1 }, { 12, 12, 1, 12 }, { 12, 12, 23, 12 }, { 12, 12, 12, 23 } }
    for i, t in ipairs(thorns) do
      c:line(t[1], t[2], t[3], t[4], i <= 4 and "g" or "v", 2)
      c:line(t[1], t[2], (t[1] + t[3]) / 2, (t[2] + t[4]) / 2, "G", 2)
      c:px(t[3], t[4], "6")
    end
    c:disc(12, 12, 3, "g"); c:disc(12, 12, 2, "G"); c:px(11, 11, "6")
  end },
  -- ---- soins
  { id = "potion", draw = function(c)
    flask(c, 12, 15, 7, "G", "g")
    c:px(14, 13, "6"); c:px(11, 17, "6")
  end },
  { id = "caipirinha", draw = function(c)
    c:tri(5, 8, 19, 8, 12, 30, "4")
    c:rect(6, 7, 13, 15, "4")
    c:rect(7, 8, 11, 13, "G")
    c:rect(7, 8, 11, 2, "6")
    c:rect(8, 12, 3, 3, "6"); c:rect(13, 15, 3, 3, "6"); c:rect(9, 17, 2, 2, "C")
    c:rect(5, 22, 15, 1, "4")
    c:line(15, 2, 12, 14, "R", 1)
    c:disc(18, 7, 3, "v"); c:disc(18, 7, 2, "G"); c:px(18, 7, "Y")
  end },
  { id = "petit-petou", draw = function(c)
    c:line(5, 19, 16, 8, "6", 3)
    c:line(4, 20, 6, 18, "y", 3)
    c:line(6, 21, 17, 10, "5", 1)
    c:disc(18, 7, 2, "R"); c:disc(18, 7, 1, "o"); c:px(19, 6, "Y")
    c:px(19, 3, "4"); c:px(20, 2, "4"); c:px(19, 1, "3"); c:px(21, 4, "3"); c:px(17, 2, "3")
    c:px(9, 15, "G"); c:px(12, 12, "G")
  end },
  { id = "grande-potion", draw = function(c)
    flask(c, 12, 14, 9, "G", "g")
    c:rect(11, 11, 3, 7, "6"); c:rect(9, 13, 7, 3, "6")
    c:px(3, 5, "Y"); c:px(2, 6, "Y"); c:px(4, 6, "Y"); c:px(3, 7, "Y")
    c:px(20, 3, "Y"); c:px(21, 20, "Y")
  end },
  -- ---- terrains
  { id = "epfl", draw = function(c)
    c:rect(0, 20, 24, 4, "2")
    c:rect(2, 9, 9, 11, "4"); c:rect(11, 5, 11, 15, "5")
    c:rect(2, 8, 9, 1, "R"); c:rect(11, 4, 11, 1, "R")
    for y = 0, 2 do
      for x = 0, 1 do c:rect(4 + x * 4, 11 + y * 3, 2, 2, "Y") end
      for x = 0, 2 do c:rect(13 + x * 3, 7 + y * 4, 2, 3, x == 1 and "C" or "Y") end
    end
    c:rect(15, 17, 3, 3, "N")
    c:line(0, 21, 23, 21, "3")
    c:disc(20, 2, 1, "o")
  end },
  { id = "marecage", draw = function(c)
    c:rect(0, 15, 24, 9, "u")
    c:rect(0, 15, 24, 1, "b")
    c:hline(2, 7, 18, "c"); c:hline(13, 20, 20, "c"); c:hline(5, 9, 22, "b")
    for _, x in ipairs({ 4, 7, 17, 20 }) do
      c:vline(x, 5 + (x % 3), 16, "g")
      c:rect(x, 3 + (x % 3), 1, 3, "n")
    end
    c:line(10, 9, 12, 15, "v"); c:line(14, 10, 12, 15, "v")
    c:ellipse(12, 18, 4, 1, "G"); c:px(13, 17, "p")
    c:px(19, 13, "C"); c:px(2, 12, "C")
  end },
  { id = "foret", draw = function(c)
    c:rect(0, 21, 24, 3, "v")
    pine(c, 5, 21, 14, "g", "v")
    pine(c, 18, 21, 13, "g", "v")
    pine(c, 12, 22, 19, "G", "g")
  end },
  { id = "enfer", draw = function(c)
    c:rect(0, 18, 24, 6, "d")
    c:rect(0, 20, 24, 4, "R"); c:hline(1, 6, 21, "o"); c:hline(11, 19, 22, "Y"); c:hline(15, 22, 20, "o")
    c:tri(2, 18, 6, 18, 4, 9, "2"); c:tri(17, 18, 23, 18, 20, 7, "2")
    flame(c, 11, 15, 4)
    flame(c, 6, 17, 2); flame(c, 16, 17, 2)
    c:px(9, 4, "o"); c:px(15, 2, "R"); c:px(19, 4, "o")
  end },
  { id = "atlantis", draw = function(c)
    c:rect(0, 0, 24, 24, "u")
    c:hline(0, 23, 2, "b"); c:hline(3, 9, 5, "b"); c:hline(14, 22, 7, "b")
    c:rect(0, 20, 24, 4, "c")
    c:ellipse(12, 12, 6, 5, "4"); c:rect(6, 12, 13, 8, "u")
    for _, x in ipairs({ 6, 10, 14, 17 }) do c:rect(x, 12, 2, 8, "5"); c:rect(x + 1, 12, 1, 8, "4") end
    c:rect(5, 11, 15, 1, "6"); c:rect(4, 19, 17, 1, "5")
    c:px(3, 9, "C"); c:px(2, 14, "C"); c:px(21, 11, "C"); c:px(20, 16, "C"); c:px(22, 4, "C")
  end },
  { id = "amazonie", draw = function(c)
    c:rect(0, 20, 24, 4, "b"); c:hline(2, 9, 21, "B"); c:hline(13, 21, 22, "B")
    c:line(13, 20, 11, 8, "n", 2)
    for _, leaf in ipairs({ { 3, 6 }, { 5, 3 }, { 12, 1 }, { 19, 3 }, { 21, 7 } }) do
      c:line(12, 8, leaf[1], leaf[2], "g", 2)
      c:line(12, 7, (12 + leaf[1]) / 2, (7 + leaf[2]) / 2, "G", 1)
    end
    c:ellipse(4, 17, 4, 3, "v"); c:ellipse(4, 16, 3, 2, "g")
    c:ellipse(20, 17, 4, 3, "v"); c:ellipse(20, 16, 3, 2, "G")
    c:px(17, 12, "R"); c:px(7, 12, "Y")
  end },
  -- ---- sorts neutres
  { id = "pet-de-mamouz", draw = function(c)
    -- nuage verdâtre qui s'échappe, lignes d'odeur
    c:disc(8, 15, 5, "v"); c:disc(14, 13, 6, "v"); c:disc(18, 17, 4, "v")
    c:disc(8, 14, 4, "g"); c:disc(14, 12, 5, "g"); c:disc(18, 16, 3, "g")
    c:disc(12, 10, 2, "G"); c:disc(7, 12, 1, "G")
    c:arc(6, 6, 2, 2, 180, 360, "4"); c:arc(10, 4, 2, 2, 0, 180, "4")
    c:arc(17, 4, 2, 2, 180, 360, "4"); c:arc(21, 6, 2, 2, 0, 180, "4")
    c:px(3, 20, "G"); c:px(21, 22, "G"); c:px(22, 11, "G")
  end },
  { id = "carte-basique-pas-ouf", draw = function(c)
    -- carte grise toute simple, visage blasé
    c:rect(6, 2, 12, 20, "3"); c:frame(6, 2, 12, 20, "2")
    c:rect(7, 3, 10, 18, "4")
    c:hline(9, 10, 9, "1"); c:hline(14, 15, 9, "1")
    c:hline(9, 15, 15, "1")
    c:px(19, 4, "3"); c:px(20, 6, "3")
  end },
  { id = "lancer-de-manette", draw = function(c)
    -- manette en vol, traits de vitesse
    c:hline(1, 6, 8, "4"); c:hline(0, 5, 12, "4"); c:hline(2, 7, 16, "4")
    c:ellipse(11, 12, 3, 4, "2"); c:ellipse(19, 12, 3, 4, "2")
    c:rect(11, 9, 9, 6, "2")
    c:rect(10, 11, 3, 1, "4"); c:rect(11, 10, 1, 3, "4")
    c:px(18, 10, "R"); c:px(20, 11, "B"); c:px(18, 12, "G"); c:px(16, 11, "Y")
    c:hline(12, 18, 8, "3")
  end },
  { id = "big-lez-shotgun", draw = function(c)
    -- fusil à pompe générique avec éclair de tir
    c:line(3, 19, 15, 9, "N", 3)
    c:line(9, 15, 20, 6, "3", 2)
    c:line(10, 16, 21, 7, "2", 1)
    c:rect(11, 12, 4, 2, "n")
    c:px(9, 17, "2"); c:px(8, 18, "2")
    c:disc(22, 4, 2, "Y"); c:px(22, 4, "6")
    c:px(20, 1, "o"); c:px(23, 1, "o"); c:px(23, 7, "o")
  end },
  -- ---- sorts du plan V2, lot 1
  { id = "cayouuu", draw = function(c)
    -- un grand cri : bouche ouverte et ondes sonores
    c:disc(9, 12, 7, "s"); c:disc(9, 12, 6, "s")
    c:ellipse(10, 15, 4, 3, "d"); c:ellipse(10, 15, 3, 2, "1"); c:hline(8, 12, 13, "6")
    c:disc(6, 9, 1, "1"); c:disc(12, 9, 1, "1")
    c:arc(15, 14, 3, 4, 300, 420, "4"); c:arc(15, 14, 5, 6, 300, 420, "4"); c:arc(15, 14, 7, 8, 310, 410, "3")
  end },
  { id = "pain-pita", draw = function(c)
    -- pita en demi-lune, garnie (géométrique : facettes jaunes)
    for y = 0, 9 do
      local half = math.floor(math.sqrt(100 - y * y) + 0.5)
      c:hline(12 - half, 12 + half, 18 - y, "y")
    end
    for y = 0, 7 do
      local half = math.floor(math.sqrt(64 - y * y) + 0.5)
      c:hline(12 - half, 12 + half, 18 - y, "Y")
    end
    c:hline(2, 22, 18, "n"); c:hline(3, 21, 19, "N")
    c:tri(6, 17, 9, 12, 12, 17, "o"); c:tri(13, 17, 16, 11, 19, 17, "o")
    c:hline(5, 19, 10, "G"); c:px(7, 9, "G"); c:px(15, 9, "G"); c:px(10, 10, "R"); c:px(16, 10, "R")
  end },
  { id = "matrix", draw = function(c)
    -- pluie de chiffres et crochets de matrice (algébrique : cyan)
    c:rect(0, 0, 24, 24, "0")
    c:vline(2, 2, 21, "C"); c:hline(2, 4, 2, "C"); c:hline(2, 4, 21, "C")
    c:vline(21, 2, 21, "C"); c:hline(19, 21, 2, "C"); c:hline(19, 21, 21, "C")
    for _, col in ipairs({ 6, 10, 14, 17 }) do
      for y = (col % 4), 20, 4 do c:px(col, y, "c"); c:px(col, y + 1, "C") end
    end
    c:rect(9, 9, 6, 6, "c"); c:frame(9, 9, 6, 6, "C"); c:px(11, 11, "6"); c:px(12, 12, "6")
  end },
  { id = "moindre-carre", draw = function(c)
    -- nuage de points et droite de régression
    c:rect(0, 0, 24, 24, "1")
    c:vline(2, 2, 21, "3"); c:hline(2, 21, 21, "3")
    for _, p in ipairs({ { 5, 17 }, { 7, 18 }, { 9, 14 }, { 11, 15 }, { 13, 11 }, { 15, 12 }, { 17, 8 }, { 19, 9 }, { 20, 5 } }) do
      c:px(p[1], p[2], "6")
    end
    c:line(3, 20, 21, 4, "C")
    c:px(11, 13, "M"); c:px(15, 10, "M")
  end },
  { id = "dist-player", draw = function(c)
    -- distance entre deux points : règle et flèche (géométrique : jaune)
    c:rect(0, 0, 24, 24, "1")
    c:disc(4, 18, 2, "Y"); c:disc(19, 5, 2, "y")
    c:line(5, 17, 18, 6, "Y")
    c:tri(18, 6, 14, 6, 18, 10, "Y")
    c:rect(2, 21, 20, 2, "y"); for x = 2, 21, 3 do c:vline(x, 20, 21, "N") end
    c:px(7, 11, "6"); c:px(8, 11, "6")
  end },
  -- ---- terrains (nouveaux)
  { id = "volcan", draw = function(c)
    c:rect(0, 20, 24, 4, "2")
    c:tri(1, 21, 23, 21, 12, 6, "N")
    c:tri(5, 21, 12, 6, 12, 21, "n")
    c:rect(10, 6, 5, 2, "R")
    c:line(12, 7, 9, 15, "R", 1); c:line(13, 7, 16, 13, "o", 1)
    c:disc(12, 3, 2, "3"); c:disc(15, 1, 2, "3"); c:disc(9, 2, 1, "2")
    c:px(11, 5, "Y"); c:px(14, 4, "o")
  end },
  { id = "dojo", draw = function(c)
    c:rect(0, 20, 24, 4, "s")
    c:hline(0, 23, 21, "y")
    c:tri(1, 9, 23, 9, 12, 3, "d")
    c:rect(3, 9, 18, 2, "d")
    c:rect(4, 11, 16, 9, "5")
    c:rect(4, 11, 1, 9, "N"); c:rect(19, 11, 1, 9, "N"); c:rect(11, 11, 2, 9, "N")
    c:rect(6, 13, 4, 4, "4"); c:rect(14, 13, 4, 4, "4")
  end },
  { id = "migros", draw = function(c)
    -- supermarché générique : caddie plein
    c:rect(0, 21, 24, 3, "3")
    c:line(2, 5, 5, 5, "4"); c:line(5, 5, 8, 16, "4")
    c:rect(6, 8, 15, 1, "4"); c:line(20, 8, 18, 15, "4"); c:hline(8, 18, 15, "4")
    for x = 8, 18, 3 do c:vline(x, 9, 14, "3") end
    c:rect(9, 5, 3, 3, "R"); c:rect(13, 4, 3, 4, "o"); c:rect(16, 6, 2, 2, "G")
    c:disc(9, 19, 1, "2"); c:disc(17, 19, 1, "2")
  end },
  { id = "gare", draw = function(c)
    -- quai, horloge de gare, rails
    c:rect(0, 18, 24, 6, "3")
    c:hline(0, 23, 20, "n"); c:hline(0, 23, 22, "n")
    for x = 1, 23, 4 do c:vline(x, 20, 22, "N") end
    c:vline(12, 9, 17, "2")
    c:disc(12, 6, 5, "2"); c:disc(12, 6, 4, "6")
    c:vline(12, 3, 6, "1"); c:hline(12, 14, 6, "1"); c:px(12, 6, "R")
  end },
  { id = "desert-kaleidoscopique", draw = function(c)
    -- dunes sous un soleil-kaléidoscope (spirituel : magenta)
    c:rect(0, 0, 24, 24, "1")
    c:disc(12, 8, 6, "m"); c:disc(12, 8, 4, "M"); c:disc(12, 8, 2, "p"); c:px(12, 8, "6")
    for _, d in ipairs({ { 12, 0 }, { 12, 16 }, { 4, 8 }, { 20, 8 }, { 6, 2 }, { 18, 2 }, { 6, 14 }, { 18, 14 } }) do
      c:line(12, 8, d[1], d[2], "M")
    end
    c:ellipse(6, 22, 9, 5, "y"); c:ellipse(19, 23, 8, 5, "n")
    c:ellipse(6, 21, 8, 3, "Y"); c:ellipse(19, 22, 7, 3, "y")
    c:px(3, 19, "M"); c:px(20, 20, "C")
  end },
  { id = "espace-de-dimension-n", draw = function(c)
    -- espace sombre, axes et hypercube en fil de fer (algébrique : cyan)
    c:rect(0, 0, 24, 24, "0")
    c:px(2, 3, "5"); c:px(20, 2, "5"); c:px(22, 19, "4"); c:px(3, 20, "4"); c:px(15, 22, "5")
    c:frame(4, 8, 10, 10, "c")
    c:frame(10, 4, 10, 10, "C")
    c:line(4, 8, 10, 4, "C"); c:line(13, 8, 19, 4, "C")
    c:line(4, 17, 10, 13, "c"); c:line(13, 17, 19, 13, "C")
    c:disc(19, 4, 1, "6"); c:px(8, 12, "B")
  end },
  { id = "fractale", draw = function(c)
    -- triangle de Sierpiński (géométrique : jaune) : pixel plein si x & y == 0
    c:rect(0, 0, 24, 24, "1")
    for y = 0, 15 do
      for x = 0, 15 - y do
        if (x & y) == 0 then c:px(4 + x, 19 - y, (x + y) % 4 == 0 and "y" or "Y") end
      end
    end
    c:px(20, 4, "o"); c:px(18, 7, "o"); c:px(21, 9, "y")
  end },
  -- ---- cartes mortes
  { id = "no-cards", draw = function(c)
    c:rect(5, 2, 14, 20, "2"); c:frame(5, 2, 14, 20, "3")
    c:line(7, 5, 16, 18, "R", 2); c:line(16, 5, 7, 18, "R", 2)
  end },
  { id = "exercice-3b", draw = function(c)
    c:rect(4, 1, 16, 22, "6"); c:frame(4, 1, 16, 22, "4")
    for y = 0, 3 do c:hline(6, 17, 12 + y * 3, "3") end
    -- « 3b » à la main
    c:hline(6, 9, 3, "1"); c:vline(9, 3, 9, "1"); c:hline(7, 9, 6, "1"); c:hline(6, 9, 9, "1")
    c:vline(11, 3, 9, "1"); c:hline(11, 14, 6, "1"); c:vline(14, 6, 9, "1"); c:hline(11, 14, 9, "1")
    c:line(13, 13, 18, 21, "R", 1); c:line(18, 13, 13, 21, "R", 1)
    c:disc(17, 5, 2, "R"); c:px(17, 5, "6")
  end },
}
