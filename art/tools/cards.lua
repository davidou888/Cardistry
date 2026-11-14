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
