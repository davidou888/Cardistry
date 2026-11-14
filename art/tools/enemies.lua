-- Sprites d'adversaires, 64×64, tournés vers la gauche (le joueur est à gauche de l'écran).
-- Chaque adversaire : des animations (idle, attack, hurt, death) = des tags dans
-- art/enemies/<id>.aseprite. generate.lua n'ajoute que les animations dont le tag est absent.
-- Ébauches dessinées par formes ; Choomah et Clarence ne sont pas ici (dessinés à la main).

-- ------------------------------------------------------------------ Caddie énervé

local function caddieLeft(y) return math.floor(6 + (y - 16) * 6 / 24 + 0.5) end
local function caddieRight(y) return math.floor(52 - (y - 16) * 4 / 24 + 0.5) end

local function wheel(c, x, y)
  c:disc(x, y, 4, "3"); c:disc(x, y, 2, "2"); c:px(x, y, "6")
  c:px(x - 3, y - 3, "4")
end

-- pose : bob (le panier descend), eyes ("angry", "wide", "x"), mouth ("teeth", "open"), speed
local function caddie(c, pose)
  pose = pose or {}
  local bob = pose.bob or 0
  local b = c:shift(0, bob)
  -- châssis et roues (ne bougent pas)
  c:line(14, 41 + bob, 12, 52, "4"); c:line(46, 41 + bob, 50, 52, "4")
  c:hline(12, 50, 48, "4"); c:hline(12, 50, 49, "3")
  wheel(c, 12, 57); wheel(c, 50, 57)
  -- panier : fond, grillage, bords
  for y = 16, 40 do b:hline(caddieLeft(y), caddieRight(y), y, "1") end
  for i = 0, 6 do b:line(6 + i * 46 / 6, 16, 12 + i * 6, 40, "3") end
  for _, y in ipairs({ 22, 28, 34 }) do b:hline(caddieLeft(y), caddieRight(y), y, "3") end
  b:line(6, 16, 12, 40, "4"); b:line(52, 16, 48, 40, "4")
  b:hline(12, 48, 40, "4"); b:hline(13, 47, 41, "3")
  b:hline(5, 53, 15, "6"); b:hline(5, 53, 16, "4")
  -- poignée
  b:line(53, 14, 59, 8, "4", 2); b:rect(57, 5, 6, 3, "R"); b:rect(57, 7, 6, 1, "r")
  -- yeux
  local eyes = pose.eyes or "angry"
  for _, x in ipairs({ 16, 33 }) do
    if eyes == "x" then
      b:rect(x, 22, 7, 6, "1")
      b:line(x, 22, x + 5, 27, "R", 2); b:line(x + 5, 22, x, 27, "R", 2)
    else
      local top = eyes == "wide" and 20 or 22
      b:rect(x, top, 7, 28 - top, "Y"); b:rect(x, 27, 7, 1, "y")
      b:rect(x + 1, 24, 3, eyes == "wide" and 3 or 4, "1")
    end
  end
  if eyes ~= "x" then
    b:line(13, 18, 24, 22, "R", 2); b:line(42, 18, 31, 22, "R", 2)
  end
  -- gueule
  if pose.mouth == "open" then
    b:rect(17, 30, 23, 8, "d"); b:rect(18, 31, 21, 6, "1")
    for x = 18, 36, 4 do b:rect(x, 31, 2, 2, "6"); b:rect(x + 2, 35, 2, 2, "6") end
  else
    for x = 17, 37, 4 do b:line(x, 32, x + 2, 35, "6"); b:line(x + 2, 35, x + 4, 32, "6") end
  end
  if pose.speed then
    for _, y in ipairs({ 20, 27, 34 }) do c:hline(57, 63, y + bob, "R"); c:hline(60, 63, y + 2 + bob, "r") end
  end
end

local caddieEnerve = {
  id = "caddie-enerve",
  animations = {
    { name = "idle", frames = {
      function(lib) return lib.drawn(64, function(c) caddie(c) end) end,
      function(lib) return lib.drawn(64, function(c) caddie(c, { bob = 1 }) end) end,
    } },
    { name = "attack", frames = {
      function(lib) return lib.drawn(64, function(c) caddie(c:shift(-3, 0), { eyes = "wide", speed = true }) end) end,
      function(lib) return lib.drawn(64, function(c)
        caddie(c:shift(-6, 0), { eyes = "wide", mouth = "open", speed = true })
      end) end,
    } },
    { name = "hurt", frames = {
      function(lib) return lib.drawn(64, function(c)
        caddie(c:shift(3, 0), { eyes = "x" })
        c:line(2, 8, 6, 12, "6"); c:line(6, 8, 2, 12, "6"); c:px(4, 6, "Y"); c:px(4, 14, "Y")
      end) end,
    } },
    { name = "death", frames = {
      function(lib) return lib.squashed(lib.drawn(64, function(c) caddie(c, { eyes = "x", bob = 2 }) end), 0.7) end,
      function(lib) return lib.squashed(lib.drawn(64, function(c) caddie(c, { eyes = "x", bob = 3 }) end), 0.38) end,
    } },
  },
}

-- ------------------------------------------------------------------ Seau de fuite

local function seauLeft(y) return math.floor(14 + (y - 20) * 6 / 32 + 0.5) end
local function seauRight(y) return math.floor(50 - (y - 20) * 6 / 32 + 0.5) end

-- pose : wave (clapotis), leak (variante des gouttes), eyes ("worried", "x", "wide"), mouth ("flat", "open"),
--        splash (0, 1, 2 : vague projetée vers la gauche), puddle (largeur de la flaque)
local function seau(c, pose)
  pose = pose or {}
  local wave = pose.wave or 0
  -- anse
  c:arc(32, 20, 21, 15, 180, 360, "3"); c:arc(32, 21, 21, 15, 180, 360, "2")
  -- flaque et ombre
  local puddle = pose.puddle or 8
  c:ellipse(10, 60, puddle, 2, "c"); c:hline(10 - puddle + 3, 10 + puddle - 4, 59, "C")
  c:hline(21, 46, 54, "2")
  -- corps
  for y = 20, 52 do
    local xl, xr = seauLeft(y), seauRight(y)
    c:hline(xl, xr, y, "b")
    c:hline(xl + 1, xl + 3, y, "B")
    c:hline(xr - 4, xr, y, "u")
    c:px(xl, y, "u")
  end
  c:hline(seauLeft(52) + 1, seauRight(52) - 1, 53, "u")
  for _, y in ipairs({ 31, 45 }) do
    c:hline(seauLeft(y), seauRight(y), y, "u"); c:hline(seauLeft(y) + 1, seauLeft(y) + 3, y, "b")
  end
  -- bord et eau
  c:rect(12, 17, 41, 4, "4"); c:hline(12, 52, 17, "6"); c:hline(12, 52, 20, "3")
  c:rect(15, 18, 35, 2, "C")
  c:hline(17 + wave, 23 + wave, 18, "6"); c:hline(33 - wave, 40 - wave, 19, "c")
  -- visage
  local eyes = pose.eyes or "worried"
  for _, x in ipairs({ 22, 36 }) do
    if eyes == "x" then
      c:line(x, 27, x + 5, 32, "1", 2); c:line(x + 5, 27, x, 32, "1", 2)
    else
      c:rect(x, 27, 6, 7, "6"); c:rect(x, 33, 6, 1, "5")
      c:rect(x + 1, eyes == "wide" and 28 or 30, 3, 3, "1")
    end
  end
  if eyes ~= "x" then
    c:line(21, 25, 27, 23, "u", 1); c:line(42, 25, 36, 23, "u", 1)
  end
  if pose.mouth == "open" then
    c:ellipse(32, 41, 4, 3, "1"); c:hline(30, 34, 43, "d")
  else
    c:hline(28, 36, 40, "1"); c:px(27, 41, "1"); c:px(37, 41, "1")
  end
  -- fuite : un filet d'eau sort d'un trou à gauche
  c:rect(seauLeft(42), 41, 2, 3, "1")
  local leak = pose.leak or 0
  local jet = { { 16, 42 }, { 14, 43 }, { 12, 45 }, { 11, 47 }, { 10, 50 }, { 9, 53 }, { 9, 56 } }
  for i, p in ipairs(jet) do
    if (i + leak) % 4 ~= 0 then c:rect(p[1], p[2], 2, 2, "C"); c:px(p[1], p[2], "6") end
  end
  c:px(6 + leak * 2, 55 + leak, "C"); c:px(14 - leak, 57, "C")
  -- vague projetée (attaque)
  local splash = pose.splash or 0
  if splash > 0 then
    local reach = splash * 6
    c:tri(14, 19, 14 - reach, 6, 20, 12, "C")
    c:ellipse(12 - reach, 8, 3 + splash, 3, "C"); c:ellipse(11 - reach, 7, 2 + splash, 2, "6")
    c:ellipse(14 - reach, 9, 2, 2, "B")
    c:px(6 - reach, 14, "C"); c:px(18 - reach, 3, "C"); c:px(9 - reach, 17, "B")
    if splash > 1 then
      c:rect(2, 20, 2, 2, "C"); c:rect(5, 26, 2, 2, "C"); c:px(1, 31, "B")
    end
  end
end

local seauDeFuite = {
  id = "seau-de-fuite",
  animations = {
    { name = "idle", frames = {
      function(lib) return lib.drawn(64, function(c) seau(c) end) end,
      function(lib) return lib.drawn(64, function(c) seau(c, { wave = 2, leak = 1 }) end) end,
    } },
    { name = "attack", frames = {
      function(lib) return lib.drawn(64, function(c)
        seau(c:shift(-1, 0), { eyes = "wide", mouth = "open", splash = 1, wave = 3 })
      end) end,
      function(lib) return lib.drawn(64, function(c)
        seau(c:shift(-2, 0), { eyes = "wide", mouth = "open", splash = 2, wave = 1, leak = 1 })
      end) end,
    } },
    { name = "hurt", frames = {
      function(lib) return lib.drawn(64, function(c)
        seau(c:shift(3, 0), { eyes = "x", mouth = "open", wave = 3 })
        c:rect(22, 9, 2, 2, "C"); c:rect(33, 5, 2, 2, "C"); c:rect(44, 8, 2, 2, "C"); c:px(28, 12, "6")
      end) end,
    } },
    { name = "death", frames = {
      function(lib) return lib.squashed(lib.drawn(64, function(c) seau(c, { eyes = "x", puddle = 9 }) end), 0.7) end,
      function(lib) return lib.squashed(lib.drawn(64, function(c) seau(c, { eyes = "x", puddle = 9 }) end), 0.4) end,
    } },
  },
}

return { caddieEnerve, seauDeFuite }
