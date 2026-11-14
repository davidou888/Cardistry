-- Premiers jets des personnages non joueurs (64×64). Voir generate.lua.
--
-- Chaque dessin est une fonction draw(canvas, pose) ; pose.alt = deuxième frame idle.
-- Le marchand du shop est tourné vers la droite (vers ses articles). Le bout de sa clope reste
-- au même endroit sur toutes les frames : la fumée animée du shop part de là
-- (ShopScreen.CIGARETTE_X / CIGARETTE_Y, à garder d'accord avec ce dessin).

-- ------------------------------------------------------------------ Marchand
-- Bonnet violet, barbe, manteau vert, accoudé à son comptoir, clope au bec.

local function marchand(c, pose)
  -- manteau
  c:ellipse(27, 45, 17, 10, "v"); c:rect(10, 45, 35, 8, "v")
  c:vline(13, 42, 51, "g"); c:vline(14, 40, 51, "g")
  c:tri(23, 36, 33, 36, 28, 49, "2"); c:line(23, 36, 28, 49, "g"); c:line(33, 36, 28, 49, "g")
  -- bras posé sur le comptoir
  c:rect(36, 45, 14, 6, "v"); c:hline(36, 49, 45, "g"); c:disc(51, 48, 3, "s")
  -- cou et tête
  c:rect(25, 31, 8, 6, "s")
  c:ellipse(29, 21, 11, 12, "s")
  c:rect(40, 19, 3, 5, "s"); c:px(42, 24, "n"); c:px(41, 24, "n")
  -- barbe
  c:rect(20, 25, 17, 6, "N"); c:ellipse(28, 31, 8, 3, "N"); c:rect(18, 20, 3, 7, "N")
  c:hline(22, 34, 33, "n")
  -- bonnet
  c:ellipse(29, 10, 11, 5, "m"); c:rect(18, 9, 23, 6, "m")
  c:rect(17, 13, 25, 3, "M"); c:px(29, 3, "M"); c:px(30, 3, "M"); c:px(29, 4, "M"); c:px(30, 4, "M")
  -- œil mi-clos (fermé sur la deuxième frame), sourcil
  c:hline(32, 38, 16, "N")
  c:hline(33, 37, 19, "1")
  if not pose.alt then c:px(35, 20, "1"); c:px(36, 20, "1") end
  -- bouche et clope : le bout est en (51, 27)
  c:hline(35, 39, 27, "1")
  c:hline(40, 49, 27, "6"); c:hline(40, 49, 28, "4")
  c:rect(50, 27, 2, 2, pose.alt and "Y" or "R")
  if pose.alt then c:px(52, 27, "o") end
  -- comptoir et marchandises
  c:rect(0, 52, 64, 12, "n"); c:hline(0, 63, 52, "o"); c:hline(0, 63, 53, "N")
  c:hline(0, 63, 58, "N"); c:vline(20, 54, 57, "N"); c:vline(44, 59, 63, "N")
  c:rect(4, 43, 5, 9, "g"); c:rect(5, 39, 3, 4, "G"); c:px(6, 38, "n"); c:hline(4, 8, 46, "6")
  c:rect(56, 49, 6, 3, "Y"); c:hline(56, 61, 50, "y"); c:rect(57, 47, 4, 2, "Y")
end

return {
  { id = "marchand", animations = { { name = "idle", frames = {
    function(lib) return lib.drawn(64, function(c) marchand(c, {}) end) end,
    function(lib) return lib.drawn(64, function(c) marchand(c, { alt = true }) end) end,
  } } } },
}
