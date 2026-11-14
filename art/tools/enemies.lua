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

-- ------------------------------------------------------------------ outils communs

local function xEye(c, x, y, size, color)
  c:line(x, y, x + size, y + size, color, 2); c:line(x + size, y, x, y + size, color, 2)
end

local function impact(c)
  c:line(2, 8, 6, 12, "6"); c:line(6, 8, 2, 12, "6"); c:px(4, 6, "Y"); c:px(4, 14, "Y")
end

-- Animations habituelles à partir d'un seul dessin draw(canvas, pose) :
--   idle   : pose {} puis { alt = true }
--   attack : { attack = 1 } décalé de 3 px vers le joueur, puis { attack = 2 } décalé de 6 px
--   hurt   : { hurt = true } en recul, avec un éclat
--   death  : la pose hurt écrasée vers le sol, deux fois
local function standard(id, draw)
  local function frame(dx, pose, extra)
    return function(lib)
      return lib.drawn(64, function(c)
        draw(c:shift(dx, 0), pose)
        if extra then extra(c) end
      end)
    end
  end
  local function fallen(factor)
    return function(lib)
      return lib.squashed(lib.drawn(64, function(c) draw(c, { hurt = true, dead = true }) end), factor)
    end
  end
  return { id = id, animations = {
    { name = "idle", frames = { frame(0, {}), frame(0, { alt = true }) } },
    { name = "attack", frames = { frame(-3, { attack = 1 }), frame(-6, { attack = 2 }) } },
    { name = "hurt", frames = { frame(3, { hurt = true }, impact) } },
    { name = "death", frames = { fallen(0.7), fallen(0.4) } },
  } }
end

-- ------------------------------------------------------------------ Camé (feu)
-- Capuche rouge, yeux rouges mi-clos, clope au bec ; à l'attaque il tend la main (la Taxe).

local function came(c, pose)
  local bob = pose.alt and 1 or 0
  -- corps
  c:ellipse(32, 46, 19, 9, "r"); c:rect(14, 46, 37, 17, "r")
  c:rect(44, 44, 7, 19, "d"); c:rect(24, 53, 16, 6, "d"); c:hline(24, 39, 53, "1")
  c:vline(29, 42, 49, "6"); c:vline(35, 42, 49, "6")
  -- bras tendu (attaque)
  if pose.attack then
    local reach = pose.attack * 4
    c:rect(14 - reach, 43, reach + 6, 6, "r"); c:hline(14 - reach, 19, 48, "d")
    c:disc(12 - reach, 45, 3, "s"); c:px(9 - reach, 43, "s"); c:px(9 - reach, 47, "s")
  end
  local h = c:shift(0, bob)
  -- capuche et visage
  h:disc(32, 24, 17, "r"); h:arc(32, 24, 17, 17, 300, 420, "d"); h:arc(32, 24, 16, 16, 300, 420, "d")
  h:ellipse(31, 27, 11, 12, "s"); h:arc(31, 27, 11, 12, 180, 360, "n")
  h:rect(25, 15, 13, 3, "N")
  -- yeux
  for _, x in ipairs({ 23, 33 }) do
    if pose.hurt then
      xEye(h, x, 22, 4, "1")
    else
      h:rect(x, 23, 6, 4, "6"); h:rect(x + 1, 24, 3, 3, "R"); h:px(x + 2, 25, "1")
      h:hline(x, x + 5, 22, "n"); h:hline(x, x + 5, 23, pose.attack and "6" or "n")
      h:hline(x, x + 5, 28, "p")
    end
  end
  h:rect(30, 29, 2, 3, "n")
  -- bouche et clope
  if pose.attack == 2 or pose.hurt then
    h:ellipse(31, 35, 3, 2, "1")
  else
    h:hline(27, 35, 35, "N"); h:px(36, 34, "N")
  end
  if not pose.dead then
    h:rect(17, 34, 10, 2, "6"); h:rect(16, 34, 2, 2, "o"); h:px(15, 34, "R")
    local drift = pose.alt and 1 or 0
    h:px(14 - drift, 31, "4"); h:px(13 + drift, 28, "4"); h:px(15 - drift, 25, "3"); h:px(13 + drift, 22, "3")
  end
end

-- ------------------------------------------------------------------ Paresseux (plante)
-- Accroché à sa branche ; « Badoing » : il se balance. À l'attaque, un bras lâche et griffe.

local function paresseux(c, pose)
  local sag = pose.alt and 2 or 0
  -- branche et feuilles (ne bougent pas)
  local branch = pose.dead and c or c:shift(-c.dx, 0)
  branch:rect(0, 6, 64, 4, "n"); branch:hline(0, 63, 10, "N"); branch:hline(0, 63, 6, "y")
  branch:ellipse(6, 4, 5, 2, "G"); branch:ellipse(55, 3, 6, 2, "g"); branch:ellipse(58, 11, 4, 2, "G")
  local b = c:shift(0, sag)
  -- bras accrochés
  c:rect(41, 10, 6, 22 + sag, "n"); c:vline(46, 10, 30 + sag, "N")
  c:rect(40, 7, 2, 4, "5"); c:rect(43, 7, 2, 4, "5"); c:rect(46, 7, 2, 4, "5")
  if pose.attack then
    local reach = pose.attack * 5
    b:line(22, 30, 14 - reach, 36, "n", 5)
    for i = 0, 2 do b:line(13 - reach, 36 + i * 2, 8 - reach, 38 + i * 3, "6") end
  else
    c:rect(17, 10, 6, 22 + sag, "n"); c:vline(17, 10, 30 + sag, "N")
    c:rect(16, 7, 2, 4, "5"); c:rect(19, 7, 2, 4, "5"); c:rect(22, 7, 2, 4, "5")
  end
  -- corps
  b:ellipse(32, 40, 16, 15, "n"); b:ellipse(36, 42, 11, 11, "N"); b:ellipse(31, 43, 9, 9, "n")
  b:ellipse(30, 45, 7, 6, "y")
  -- pattes
  b:rect(22, 52, 6, 8, "n"); b:rect(37, 52, 6, 8, "n")
  b:rect(21, 59, 2, 3, "5"); b:rect(24, 59, 2, 3, "5"); b:rect(27, 59, 2, 3, "5")
  b:rect(36, 59, 2, 3, "5"); b:rect(39, 59, 2, 3, "5"); b:rect(42, 59, 2, 3, "5")
  -- tête
  b:ellipse(30, 29, 11, 9, "n"); b:ellipse(30, 30, 9, 7, "5")
  b:ellipse(25, 29, 3, 2, "N"); b:ellipse(35, 29, 3, 2, "N")
  if pose.hurt then
    xEye(b, 23, 27, 3, "1"); xEye(b, 33, 27, 3, "1")
  elseif pose.attack then
    b:rect(24, 28, 2, 2, "6"); b:rect(34, 28, 2, 2, "6")
  else
    b:hline(23, 26, 29, "1"); b:hline(33, 36, 29, "1")
  end
  b:rect(29, 31, 3, 2, "1")
  b:arc(30, 32, 4, 3, 20, 160, "N")
end

-- ------------------------------------------------------------------ Le Peneu (neutre)
-- Un gros pneu qui rebondit, le visage dans la jante.

local function peneu(c, pose)
  local lift = pose.alt and 3 or 0
  c:ellipse(32, 61, 18 - lift, 2, "2")
  local t = c:shift(0, -lift)
  t:disc(32, 33, 26, "3"); t:disc(32, 33, 24, "2")
  for a = 0, 340, 20 do
    local rad = math.rad(a + (pose.attack and pose.attack * 10 or 0))
    t:rect(32 + 25 * math.cos(rad) - 1, 33 + 25 * math.sin(rad) - 1, 3, 3, "1")
  end
  t:arc(32, 33, 20, 20, 200, 290, "3")
  t:disc(32, 33, 15, "3"); t:disc(32, 33, 14, "4"); t:disc(32, 33, 12, "5")
  t:arc(32, 33, 13, 13, 20, 110, "3")
  for a = 30, 330, 60 do
    local rad = math.rad(a)
    t:disc(32 + 9 * math.cos(rad), 33 + 9 * math.sin(rad), 1, "3")
  end
  -- visage
  if pose.hurt then
    xEye(t, 25, 28, 4, "1"); xEye(t, 35, 28, 4, "1")
    t:ellipse(32, 39, 3, 2, "1")
  else
    t:rect(25, 28, 4, 5, "1"); t:rect(35, 28, 4, 5, "1"); t:px(25, 28, "6"); t:px(35, 28, "6")
    t:line(23, 25, 29, 27, "1", 2); t:line(41, 25, 35, 27, "1", 2)
    if pose.attack then
      t:rect(27, 37, 11, 4, "1"); t:hline(28, 36, 37, "6")
    else
      t:hline(28, 36, 39, "1"); t:px(27, 38, "1"); t:px(37, 38, "1")
    end
  end
  if pose.attack then
    for _, y in ipairs({ 18, 30, 42 }) do c:hline(58, 63, y, "4"); c:hline(60, 63, y + 2, "3") end
  end
end

-- ------------------------------------------------------------------ T-Rex défoncé (plante)
-- De profil, tourné vers la gauche : œil rouge mi-clos, bras trop courts, un joint au coin de la gueule.

local function trex(c, pose)
  local open = (pose.attack == 2 or pose.hurt) and 4 or (pose.attack and 2 or 0)
  -- queue, corps, pattes
  c:tri(46, 34, 63, 52, 48, 50, "g"); c:tri(50, 42, 63, 52, 50, 50, "v")
  c:ellipse(38, 40, 14, 13, "g"); c:ellipse(34, 44, 8, 9, "G")
  c:rect(31, 50, 7, 10, "g"); c:rect(42, 50, 7, 10, "g"); c:vline(48, 50, 59, "v")
  c:rect(27, 59, 11, 3, "v"); c:rect(38, 59, 11, 3, "v")
  for _, x in ipairs({ 27, 30, 38, 41 }) do c:px(x, 62, "6") end
  for _, p in ipairs({ { 42, 30 }, { 47, 35 }, { 40, 36 }, { 45, 42 } }) do c:rect(p[1], p[2], 2, 2, "v") end
  -- petits bras
  local arm = pose.alt and 1 or 0
  c:rect(23, 38 + arm, 7, 2, "v"); c:px(22, 39 + arm, "6"); c:px(22, 37 + arm, "6")
  c:rect(25, 43 - arm, 6, 2, "v"); c:px(24, 44 - arm, "6")
  -- cou et tête
  c:tri(22, 20, 36, 20, 34, 36, "g"); c:rect(24, 22, 10, 12, "g")
  c:ellipse(22, 17, 13, 8, "g"); c:ellipse(11, 19, 9, 5, "g")
  c:hline(4, 20, 14, "G"); c:px(4, 17, "v"); c:px(6, 16, "v")
  -- mâchoire
  c:rect(4, 23 + open, 20, 4, "g"); c:hline(4, 23, 26 + open, "v")
  if open > 0 then c:rect(5, 23, 18, open, "d") end
  for x = 5, 21, 3 do c:px(x, 23, "6"); c:px(x + 1, 22 + open, "6") end
  -- œil
  if pose.hurt then
    xEye(c, 20, 12, 4, "1")
  else
    c:rect(19, 12, 7, 5, "6"); c:rect(20, 14, 3, 3, "R"); c:px(21, 15, "1")
    c:rect(19, 12, 7, pose.attack and 1 or 2, "v"); c:hline(19, 25, 17, "p")
  end
  -- joint
  if not pose.dead then
    c:rect(0, 24 + open, 6, 2, "6"); c:px(0, 24 + open, "o")
    local drift = pose.alt and 1 or 0
    c:px(1 + drift, 21 + open, "4"); c:px(0 + drift, 18 + open, "4"); c:px(2 - drift, 15, "3")
  end
end

-- ------------------------------------------------------------------ Étudiant post-examen (eau)
-- En larmes, sa copie ratée dans les mains. À l'attaque, les larmes giclent vers le joueur.

local function etudiant(c, pose)
  local sob = pose.alt and 1 or 0
  -- jambes et corps
  c:rect(23, 52, 7, 10, "u"); c:rect(34, 52, 7, 10, "u"); c:rect(21, 61, 9, 2, "1"); c:rect(34, 61, 9, 2, "1")
  c:ellipse(32, 36, 14, 6, "b"); c:rect(19, 36, 27, 18, "b"); c:rect(40, 36, 6, 18, "u")
  -- copie
  c:rect(22, 40, 20, 14, "6"); c:frame(22, 40, 20, 14, "4")
  c:hline(25, 38, 47, "3"); c:hline(25, 38, 50, "3")
  c:line(26, 42, 30, 45, "R"); c:line(30, 42, 26, 45, "R"); c:disc(36, 43, 2, "R"); c:px(36, 43, "6")
  c:disc(21, 46, 3, "s"); c:disc(43, 46, 3, "s")
  local h = c:shift(0, sob)
  -- tête
  h:disc(32, 20, 12, "s"); h:ellipse(32, 11, 12, 6, "N"); h:rect(20, 12, 3, 9, "N"); h:rect(42, 12, 3, 8, "N")
  h:rect(24, 15, 4, 2, "N")
  if pose.hurt then
    xEye(h, 24, 18, 4, "1"); xEye(h, 35, 18, 4, "1")
  else
    h:hline(24, 29, 20, "1"); h:hline(35, 40, 20, "1"); h:px(24, 19, "1"); h:px(40, 19, "1")
    h:line(23, 16, 29, 17, "N"); h:line(41, 16, 35, 17, "N")
  end
  h:ellipse(32, 27, pose.attack and 4 or 3, pose.attack and 3 or 2, "1"); h:hline(30, 34, 28, "d")
  -- larmes
  if pose.attack then
    local reach = pose.attack * 7
    for i = 0, 3 do
      h:line(25, 21 + i, 25 - reach, 14 + i * 5, "C", 2)
      h:rect(22 - reach - i * 2, 12 + i * 6, 2, 2, "6")
    end
    h:line(37, 21, 37, 36, "C", 2)
  elseif not pose.dead then
    h:rect(26, 21, 2, 12 + sob * 2, "C"); h:rect(37, 21, 2, 14 - sob * 2, "C")
    h:px(26, 21, "6"); h:px(37, 21, "6")
    c:ellipse(14, 61, 6, 1, "c"); c:ellipse(49, 61, 5, 1, "c")
    c:px(18, 40 + sob * 4, "C"); c:px(47, 44 - sob * 3, "C")
  end
end

-- ------------------------------------------------------------------ Vafi (feu)
-- Le prof : lunettes, cravate, la feuille « 3b » à la main. À l'attaque, le stylo rouge (Correction).

local function vafi(c, pose)
  -- jambes et veste
  c:rect(20, 55, 8, 7, "2"); c:rect(33, 55, 8, 7, "2"); c:rect(18, 61, 10, 2, "1"); c:rect(33, 61, 10, 2, "1")
  c:ellipse(30, 32, 15, 6, "3"); c:rect(15, 32, 31, 24, "3"); c:rect(40, 32, 6, 24, "2")
  c:tri(24, 28, 37, 28, 30, 44, "6"); c:rect(29, 29, 3, 13, "R"); c:tri(28, 41, 32, 41, 30, 45, "r")
  c:line(24, 28, 29, 44, "2"); c:line(37, 28, 32, 44, "2")
  -- feuille « 3b »
  local sheet = pose.alt and 1 or 0
  c:rect(40, 36, 9, 5, "3"); c:disc(48, 38, 3, "s")
  c:rect(47, 22 - sheet, 15, 19, "6"); c:frame(47, 22 - sheet, 15, 19, "4")
  local y = 25 - sheet
  c:hline(49, 52, y, "1"); c:vline(52, y, y + 6, "1"); c:hline(50, 52, y + 3, "1"); c:hline(49, 52, y + 6, "1")
  c:vline(55, y, y + 6, "1"); c:hline(55, 58, y + 3, "1"); c:vline(58, y + 3, y + 6, "1"); c:hline(55, 58, y + 6, "1")
  c:hline(49, 59, y + 10, "3"); c:hline(49, 57, y + 13, "3")
  -- stylo rouge
  if pose.attack then
    local reach = pose.attack * 4
    c:rect(15 - reach, 36, reach + 4, 5, "3"); c:disc(13 - reach, 38, 3, "s")
    c:line(12 - reach, 37, 3 - reach, 31, "R", 2); c:px(2 - reach, 30, "6")
    if pose.attack == 2 then c:line(0, 22, 6, 28, "R"); c:line(6, 22, 0, 28, "R") end
  else
    c:rect(13, 34, 5, 14, "3"); c:disc(15, 49, 3, "s"); c:line(15, 50, 12, 57, "R", 2)
  end
  -- tête
  c:ellipse(30, 16, 10, 12, "s"); c:rect(26, 26, 8, 4, "s")
  c:arc(30, 16, 10, 12, 190, 350, "4"); c:rect(19, 12, 3, 8, "4"); c:rect(39, 12, 3, 8, "4")
  c:hline(24, 36, 5, "4")
  if pose.hurt then
    xEye(c, 23, 13, 4, "1"); xEye(c, 32, 13, 4, "1")
    c:line(20, 20, 27, 22, "1"); c:line(29, 21, 38, 18, "1")
    c:ellipse(29, 24, 3, 2, "1")
  else
    c:rect(22, 13, 7, 6, "6"); c:rect(31, 13, 7, 6, "6")
    c:frame(21, 12, 9, 8, "1"); c:frame(30, 12, 9, 8, "1"); c:hline(38, 41, 14, "1")
    c:rect(23, 15, 3, 3, "1"); c:rect(32, 15, 3, 3, "1")
    c:line(21, 9, 28, 11, "4", 2); c:line(38, 9, 31, 11, "4", 2)
    if pose.attack then c:rect(26, 23, 8, 3, "1"); c:hline(27, 32, 23, "6") else c:hline(26, 33, 24, "N") end
  end
  c:rect(28, 19, 2, 3, "n")
end

-- ------------------------------------------------------------------ placeholders
-- Choomah, Clarence et le boss sont dessinés à la main : une silhouette « ? » en attendant, avec le
-- seul tag idle. Remplacer le dessin dans le .aseprite (le script n'y touchera plus).

local function placeholder(id, crown)
  return { id = id, animations = { { name = "idle", frames = { function(lib)
    return lib.drawn(64, function(c)
      c:disc(32, 17, 10, "2"); c:rect(16, 29, 32, 30, "2")
      for a = 0, 350, 12 do c:px(32 + 10 * math.cos(math.rad(a)), 17 + 10 * math.sin(math.rad(a)), "3") end
      for i = 16, 47, 3 do c:px(i, 29, "3"); c:px(i, 58, "3") end
      for i = 29, 58, 3 do c:px(16, i, "3"); c:px(47, i, "3") end
      c:arc(32, 39, 5, 5, 180, 380, "3"); c:arc(32, 39, 4, 5, 180, 380, "3")
      c:vline(33, 44, 48, "3"); c:vline(32, 44, 48, "3"); c:rect(32, 51, 2, 2, "3")
      if crown then
        c:rect(24, 4, 17, 4, "y"); c:tri(24, 4, 27, 4, 24, 0, "Y"); c:tri(30, 4, 34, 4, 32, 0, "Y")
        c:tri(37, 4, 40, 4, 40, 0, "Y"); c:hline(24, 40, 7, "Y")
      end
    end)
  end } } } }
end

return {
  caddieEnerve, seauDeFuite,
  standard("came", came), standard("paresseux", paresseux), standard("le-peneu", peneu),
  standard("t-rex-defonce", trex), standard("etudiant-post-examen", etudiant), standard("vafi", vafi),
  placeholder("choomah", false), placeholder("clarence", false), placeholder("boss", true),
}

