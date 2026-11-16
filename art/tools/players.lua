-- Premiers jets des personnages jouables et du familier (64×64). Voir generate.lua.
--
-- PLACEHOLDERS : une silhouette commune, une couleur par personnage, une pose par animation, pour que
-- chaque fichier existe avec le bon nom (images/player/<id>.<animation>.<n>.png). Louis les redessine
-- dans art/player/<id>.aseprite ; generate.lua ne touche jamais un tag existant.
-- Personnages tournés vers la droite (l'adversaire est à droite de l'écran).

-- ------------------------------------------------------------------ Personnages

-- look : shirt, dark (ombre du haut), hair, pants. pose : arm ("up" frappe), lean, eyes ("x"), bob, flash.
local function hero(c, look, pose)
  pose = pose or {}
  local b = c:shift(pose.lean or 0, pose.bob or 0)
  -- jambes et chaussures
  b:rect(24, 44, 6, 14, look.pants); b:rect(34, 44, 6, 14, look.pants)
  b:rect(23, 57, 8, 3, "1"); b:rect(33, 57, 9, 3, "1")
  -- torse
  b:rect(21, 27, 22, 18, look.shirt); b:rect(21, 41, 22, 4, look.dark)
  b:vline(32, 28, 44, look.dark)
  -- bras arrière
  b:rect(17, 28, 5, 14, look.dark); b:disc(19, 43, 2, "s")
  -- bras avant : le long du corps, ou tendu vers l'adversaire
  if pose.arm == "up" then
    b:rect(42, 27, 14, 5, look.shirt); b:disc(57, 29, 3, "s")
    b:line(60, 24, 63, 21, "Y"); b:line(60, 34, 63, 37, "Y"); b:hline(61, 63, 29, "Y")
  else
    b:rect(42, 28, 5, 14, look.shirt); b:disc(44, 43, 2, "s")
  end
  -- cou, tête, cheveux
  b:rect(29, 23, 6, 5, "s")
  b:ellipse(32, 15, 9, 9, "s")
  b:ellipse(31, 9, 10, 5, look.hair); b:rect(22, 8, 4, 8, look.hair)
  -- visage (de profil vers la droite)
  if pose.eyes == "x" then
    b:line(34, 13, 37, 16, "1"); b:line(37, 13, 34, 16, "1")
  else
    b:rect(35, 13, 2, 3, "1")
  end
  b:hline(34, 38, 19, pose.eyes == "x" and "d" or "N")
  if pose.flash then
    for _, p in ipairs({ { 6, 10 }, { 10, 4 }, { 4, 20 } }) do c:line(p[1], p[2], p[1] + 4, p[2] + 4, "6") end
  end
end

local function heroSprite(id, look)
  local function frame(pose) return function(lib) return lib.drawn(64, function(c) hero(c, look, pose) end) end end
  return {
    id = id,
    animations = {
      { name = "idle", frames = { frame({}), frame({ bob = 1 }) } },
      { name = "attack", frames = { frame({ lean = 2 }), frame({ lean = 4, arm = "up" }) } },
      { name = "hurt", frames = { frame({ lean = -4, eyes = "x", flash = true }) } },
      { name = "death", frames = {
        frame({ lean = -2, eyes = "x" }),
        function(lib) return lib.squashed(lib.drawn(64, function(c) hero(c, look, { eyes = "x" }) end), 0.35) end,
      } },
    },
  }
end

-- ------------------------------------------------------------------ Alien de Sassy (familier)
-- Petit alien vert, antennes, grands yeux noirs. pose : beam (il tire), joint (il fume), ko (assommé), bob.

local function alien(c, pose)
  pose = pose or {}
  local b = c:shift(0, pose.bob or 0)
  if pose.ko then
    -- allongé, yeux en croix, étoiles
    b:ellipse(32, 52, 18, 6, "G"); b:ellipse(32, 52, 18, 6, "G")
    b:ellipse(20, 46, 9, 8, "G"); b:hline(14, 50, 58, "g")
    b:line(16, 43, 19, 46, "1"); b:line(19, 43, 16, 46, "1")
    b:line(22, 43, 25, 46, "1"); b:line(25, 43, 22, 46, "1")
    for _, p in ipairs({ { 14, 32 }, { 22, 28 }, { 30, 32 } }) do b:px(p[1], p[2], "Y"); b:px(p[1] + 1, p[2], "Y"); b:px(p[1], p[2] + 1, "Y") end
    return
  end
  -- antennes
  b:line(25, 14, 21, 6, "g"); b:disc(21, 5, 2, "C")
  b:line(39, 14, 43, 6, "g"); b:disc(43, 5, 2, "C")
  -- tête et yeux
  b:ellipse(32, 22, 13, 11, "G"); b:ellipse(32, 25, 12, 8, "G")
  b:ellipse(27, 22, 4, 5, "1"); b:ellipse(38, 22, 4, 5, "1")
  b:px(26, 20, "6"); b:px(37, 20, "6")
  b:hline(29, 35, 30, "g")
  -- corps, bras, jambes
  b:ellipse(32, 44, 9, 10, "G"); b:ellipse(32, 46, 6, 6, "g")
  b:rect(27, 53, 4, 8, "g"); b:rect(34, 53, 4, 8, "g")
  b:rect(20, 40, 5, 3, "G")
  if pose.beam then
    b:rect(40, 38, 9, 3, "G"); b:rect(48, 37, 3, 5, "3")
    b:hline(51, 63, 39, "C"); b:hline(51, 63, 38, "c"); b:hline(51, 63, 40, "c")
  else
    b:rect(40, 40, 5, 3, "G")
  end
  if pose.joint then
    b:hline(35, 42, 30, "6"); b:px(43, 30, pose.alt and "Y" or "R")
    local y = pose.alt and 20 or 24
    b:px(45, y, "4"); b:px(46, y - 2, "4"); b:px(45, y - 4, "4"); b:px(47, y - 6, "4"); b:px(46, y - 8, "4")
  end
end

local function alienFrame(pose) return function(lib) return lib.drawn(64, function(c) alien(c, pose) end) end end

return {
  heroSprite("louis", { shirt = "R", dark = "r", hair = "N", pants = "u" }),
  heroSprite("dave", { shirt = "B", dark = "b", hair = "n", pants = "2" }),
  heroSprite("sassy", { shirt = "M", dark = "m", hair = "o", pants = "u" }),
  heroSprite("frederic-blc", { shirt = "4", dark = "3", hair = "y", pants = "2" }),
  {
    id = "alien",
    animations = {
      { name = "idle", frames = { alienFrame({}), alienFrame({ bob = 1 }) } },
      { name = "attack", frames = { alienFrame({ beam = false, bob = -1 }), alienFrame({ beam = true }) } },
      { name = "smoke", frames = { alienFrame({ joint = true }), alienFrame({ joint = true, alt = true }) } },
      { name = "ko", frames = { alienFrame({ ko = true }) } },
    },
  },
}
