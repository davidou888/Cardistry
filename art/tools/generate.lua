-- Génère les premiers jets des icônes et des sprites dans les sources Aseprite de art/.
--
--   ./gradlew generateArt
--   (= aseprite -b --script-param root=<dépôt> --script art/tools/generate.lua)
--
-- RÈGLE : ce script n'écrase JAMAIS un dessin existant. Dans un .aseprite, une icône = une frame
-- portant un tag (le tag = l'id). Si le tag existe déjà, l'icône est laissée telle quelle ; seuls
-- les tags absents sont ajoutés, sur de nouvelles frames à la fin. Pour un adversaire, une animation
-- (idle, attack, hurt, death) déjà taguée n'est pas touchée. Un fichier sans rien à ajouter n'est
-- même pas réenregistré.
--
-- Sources des ébauches : items.txt et trinkets.txt (grilles de lettres), cards.lua et enemies.lua
-- (dessin par formes).

local root = app.params["root"] or "."
local art = root .. "/art/"
local tools = art .. "tools/"

-- Palette commune (la même que art/palette.gpl) : lettre → couleur.
local PALETTE = {
  { "0", 0x12, 0x11, 0x17 }, { "1", 0x1C, 0x1B, 0x22 }, { "2", 0x3A, 0x38, 0x42 },
  { "3", 0x6A, 0x67, 0x72 }, { "4", 0xBF, 0xBB, 0xB2 }, { "5", 0xD8, 0xD4, 0xC8 },
  { "6", 0xFF, 0xFB, 0xF0 },
  { "r", 0xC2, 0x3B, 0x3B }, { "R", 0xFF, 0x5E, 0x57 }, { "y", 0xC9, 0x9A, 0x2E },
  { "Y", 0xFF, 0xD8, 0x4F }, { "g", 0x4F, 0x9A, 0x48 }, { "G", 0x7E, 0xD9, 0x6B },
  { "b", 0x3D, 0x6B, 0xC4 }, { "B", 0x6E, 0x9C, 0xFF }, { "m", 0xA2, 0x4D, 0xB0 },
  { "M", 0xE0, 0x7B, 0xF0 }, { "c", 0x3C, 0xA0, 0xAE }, { "C", 0x6F, 0xE0, 0xEE },
  { "d", 0x7A, 0x2E, 0x2E }, { "o", 0xE0, 0x8A, 0x3C }, { "n", 0x8A, 0x5A, 0x3C },
  { "N", 0x5A, 0x3A, 0x2A }, { "s", 0xF0, 0xB8, 0x90 }, { "u", 0x2A, 0x4A, 0x8C },
  { "v", 0x2E, 0x5E, 0x36 }, { "p", 0xF2, 0xA0, 0xB4 },
}
local COLORS = {}
for _, entry in ipairs(PALETTE) do
  COLORS[entry[1]] = app.pixelColor.rgba(entry[2], entry[3], entry[4], 255)
end
COLORS["."] = app.pixelColor.rgba(0, 0, 0, 0)

local function fail(message)
  print("ERREUR : " .. message)
  os.exit(1)
end

-- ---------------------------------------------------------------- dessin par formes

-- Toile : dessine dans une image avec un décalage (dx, dy) ; hors de l'image, rien.
local function Canvas(img, dx, dy)
  local c = { img = img, dx = dx or 0, dy = dy or 0 }
  function c:px(x, y, l)
    local color = COLORS[l]
    if color == nil then fail("lettre de palette inconnue : " .. tostring(l)) end
    x = math.floor(x + self.dx + 0.5)
    y = math.floor(y + self.dy + 0.5)
    if x >= 0 and y >= 0 and x < img.width and y < img.height then img:drawPixel(x, y, color) end
  end
  function c:rect(x, y, w, h, l)
    for j = 0, h - 1 do for i = 0, w - 1 do self:px(x + i, y + j, l) end end
  end
  function c:frame(x, y, w, h, l)
    self:rect(x, y, w, 1, l); self:rect(x, y + h - 1, w, 1, l)
    self:rect(x, y, 1, h, l); self:rect(x + w - 1, y, 1, h, l)
  end
  function c:hline(x1, x2, y, l)
    for x = math.min(x1, x2), math.max(x1, x2) do self:px(x, y, l) end
  end
  function c:vline(x, y1, y2, l)
    for y = math.min(y1, y2), math.max(y1, y2) do self:px(x, y, l) end
  end
  function c:line(x1, y1, x2, y2, l, thick)
    thick = thick or 1
    local steps = math.max(math.abs(x2 - x1), math.abs(y2 - y1), 1)
    for i = 0, steps do
      local x = x1 + (x2 - x1) * i / steps
      local y = y1 + (y2 - y1) * i / steps
      if thick == 1 then self:px(x, y, l) else self:rect(math.floor(x + 0.5), math.floor(y + 0.5), thick, thick, l) end
    end
  end
  function c:ellipse(cx, cy, rx, ry, l)
    for y = -ry, ry do
      for x = -rx, rx do
        if (x * x) / (rx * rx + 0.01) + (y * y) / (ry * ry + 0.01) <= 1.0 then self:px(cx + x, cy + y, l) end
      end
    end
  end
  function c:disc(cx, cy, r, l) self:ellipse(cx, cy, r, r, l) end
  function c:arc(cx, cy, rx, ry, from, to, l)
    for a = from, to do
      local rad = math.rad(a)
      self:px(cx + rx * math.cos(rad), cy + ry * math.sin(rad), l)
    end
  end
  function c:tri(x1, y1, x2, y2, x3, y3, l)
    local minx, maxx = math.min(x1, x2, x3), math.max(x1, x2, x3)
    local miny, maxy = math.min(y1, y2, y3), math.max(y1, y2, y3)
    local function side(ax, ay, bx, by, px, py) return (bx - ax) * (py - ay) - (by - ay) * (px - ax) end
    for y = miny, maxy do
      for x = minx, maxx do
        local a = side(x1, y1, x2, y2, x, y)
        local b = side(x2, y2, x3, y3, x, y)
        local d = side(x3, y3, x1, y1, x, y)
        if (a >= 0 and b >= 0 and d >= 0) or (a <= 0 and b <= 0 and d <= 0) then self:px(x, y, l) end
      end
    end
  end
  function c:shift(dx, dy) return Canvas(img, self.dx + dx, self.dy + dy) end
  return c
end

-- Image carrée dessinée par draw(canvas).
local function drawn(size, draw)
  local img = Image(size, size, ColorMode.RGB)
  draw(Canvas(img, 0, 0))
  return img
end

-- Image écrasée vers le bas (factor entre 0 et 1) : une chute, un tas.
local function squashed(img, factor)
  local out = Image(img.width, img.height, ColorMode.RGB)
  local h = img.height
  for y = 0, h - 1 do
    local src = h - 1 - math.floor((h - 1 - y) / factor + 0.5)
    if src >= 0 then
      for x = 0, img.width - 1 do out:drawPixel(x, y, img:getPixel(x, src)) end
    end
  end
  return out
end

local lib = { Canvas = Canvas, drawn = drawn, squashed = squashed }

-- ---------------------------------------------------------------- fichiers

local function exists(path)
  local f = io.open(path, "rb")
  if f then f:close() return true end
  return false
end

-- Ouvre le .aseprite, ou en crée un vide avec la palette commune.
local function openOrCreate(path, size)
  if exists(path) then
    local spr = app.open(path)
    if spr == nil then fail("impossible d'ouvrir " .. path) end
    if spr.width ~= size or spr.height ~= size then
      fail(path .. " fait " .. spr.width .. "×" .. spr.height .. ", attendu " .. size .. "×" .. size)
    end
    return spr, false
  end
  local spr = Sprite(size, size, ColorMode.RGB)
  local pal = Palette(#PALETTE)
  for i, entry in ipairs(PALETTE) do
    pal:setColor(i - 1, Color { r = entry[2], g = entry[3], b = entry[4], a = 255 })
  end
  spr:setPalette(pal)
  return spr, true
end

local function hasTag(spr, name)
  for _, tag in ipairs(spr.tags) do
    if tag.name == name then return true end
  end
  return false
end

-- Ajoute des frames à la fin (ou réutilise la frame vide d'un fichier tout neuf).
-- Renvoie la première et la dernière frame ajoutées. images : liste d'images ; duration en secondes.
local function appendFrames(spr, fresh, images, duration)
  local first, last
  for _, img in ipairs(images) do
    local frame
    if fresh.unused then
      frame = spr.frames[1]
      fresh.unused = false
    else
      frame = spr:newEmptyFrame(#spr.frames + 1)
    end
    frame.duration = duration
    spr:newCel(spr.layers[1], frame, img, Point(0, 0))
    first = first or frame.frameNumber
    last = frame.frameNumber
  end
  return first, last
end

-- entries : liste ordonnée de { id = ..., images = function() → liste d'images end }.
local function build(path, size, entries, duration)
  local spr, created = openOrCreate(path, size)
  local fresh = { unused = created }
  -- Aseprite étend le dernier tag quand on ajoute une frame après lui : on note les bornes des tags
  -- existants pour les remettre telles quelles, et on ne pose les nouveaux tags qu'à la fin.
  local existing = {}
  for _, tag in ipairs(spr.tags) do
    existing[#existing + 1] = { tag = tag, from = tag.fromFrame.frameNumber, to = tag.toFrame.frameNumber }
  end
  local pending, kept = {}, 0
  for _, entry in ipairs(entries) do
    if hasTag(spr, entry.id) then
      kept = kept + 1
    else
      local first, last = appendFrames(spr, fresh, entry.images(), duration)
      pending[#pending + 1] = { name = entry.id, from = first, to = last }
    end
  end
  for _, old in ipairs(existing) do
    old.tag.fromFrame = old.from
    old.tag.toFrame = old.to
  end
  for _, new in ipairs(pending) do
    local tag = spr:newTag(new.from, new.to)
    tag.name = new.name
  end
  if #pending > 0 then spr:saveAs(path) end
  spr:close()
  print(string.format("%-36s %2d ajouté(s), %2d déjà là (intacts)", path:sub(#root + 2), #pending, kept))
end

-- ---------------------------------------------------------------- grilles de lettres

local function readGrids(path, size)
  local entries, current = {}, nil
  local f = io.open(path, "r")
  if f == nil then fail("fichier absent : " .. path) end
  local number = 0
  for line in f:lines() do
    number = number + 1
    line = line:gsub("%s+$", "")
    if line:sub(1, 3) == "== " then
      current = { id = line:sub(4), rows = {} }
      entries[#entries + 1] = current
    elseif line ~= "" and line:sub(1, 1) ~= "#" then
      if current == nil then fail(path .. ":" .. number .. " : ligne avant le premier « == id »") end
      if #line ~= size then fail(path .. ":" .. number .. " : " .. #line .. " lettres au lieu de " .. size) end
      current.rows[#current.rows + 1] = line
    end
  end
  f:close()
  for _, entry in ipairs(entries) do
    if #entry.rows ~= size then fail(path .. " : " .. entry.id .. " a " .. #entry.rows .. " lignes au lieu de " .. size) end
    local rows = entry.rows
    entry.images = function()
      return { drawn(size, function(c)
        for y, row in ipairs(rows) do
          for x = 1, #row do
            local l = row:sub(x, x)
            if l ~= "." then c:px(x - 1, y - 1, l) end
          end
        end
      end) }
    end
  end
  return entries
end

-- ---------------------------------------------------------------- génération

build(art .. "items.aseprite", 16, readGrids(tools .. "items.txt", 16), 0.1)
build(art .. "trinkets.aseprite", 16, readGrids(tools .. "trinkets.txt", 16), 0.1)

-- Cartes : cards.lua renvoie une liste de { id, draw(canvas) }.
local cards = {}
for _, card in ipairs(dofile(tools .. "cards.lua")) do
  local draw = card.draw
  cards[#cards + 1] = { id = card.id, images = function() return { drawn(24, draw) } end }
end
build(art .. "cards.aseprite", 24, cards, 0.1)

-- Adversaires : enemies.lua renvoie une liste de { id, animations = { { name, frames = { f(lib) → image } } } }.
for _, enemy in ipairs(dofile(tools .. "enemies.lua")) do
  local entries = {}
  for _, animation in ipairs(enemy.animations) do
    local frames = animation.frames
    entries[#entries + 1] = { id = animation.name, images = function()
      local images = {}
      for i, frame in ipairs(frames) do images[i] = frame(lib) end
      return images
    end }
  end
  build(art .. "enemies/" .. enemy.id .. ".aseprite", 64, entries, 0.25)
end
