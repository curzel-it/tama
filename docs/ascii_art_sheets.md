# ASCII Art Sheet Converter

Converts images, sprite sheets, and GIFs into ASCII Art using Braille characters. 

Output is **always 32×10 characters** (or 64×40 pixels), as that is the size used by the app.

## Usage

### PNG

```bash
cargo run --bin ascii_art_converter -- png -i input.png -o output.txt
```

Flags:
- `--transparency-as-black`: Treat transparent pixels as black (default: white)

### Sprite Sheet
Extract all frames:
```bash
cargo run --bin ascii_art_converter -- sprite \
  -i sheet.png \
  -o output.txt \
  --sprite-width 32 \
  --sprite-height 32
```

Extract specific row (e.g., second row, 4 frames):
```bash
cargo run --bin ascii_art_converter -- sprite \
  -i sheet.png -o output.txt \
  --sprite-width 32 \
  --sprite-height 32 \
  --start-x 0 \
  --start-y 32 \
  --num-frames 4
```

Flags:
- `--sprite-width <PIXELS>`: Width of each sprite
- `--sprite-height <PIXELS>`: Height of each sprite
- `--start-x <PIXELS>`: Starting X position (default: 0)
- `--start-y <PIXELS>`: Starting Y position (default: 0)
- `--num-frames <COUNT>`: Number of frames to extract (default: all in row)
- `--transparency-as-black`: Treat transparent pixels as black

### GIF

```bash
cargo run --bin ascii_art_converter -- gif -i animation.gif -o output.txt
```

Flags:
- `--transparency-as-black`: Treat transparent pixels as black
- `--sprite-width`: Width of each sprite frame in pixels (output will be width/2 characters)
- `--sprite-height`: Height of each sprite frame in pixels (output will be height/4 characters)
- `--start-x`, `--start-y`: Starting position in the sprite sheet
- `--num-frames`: Number of frames to extract (default: all frames in row)

### Examples
Extract idle animation from `neko.png`:
```bash
cargo run --bin ascii_art_converter -- sprite \
  -i sprites/neko.png \
  -o sprites/neko_idle.txt \
  --sprite-width 40 \
  --sprite-height 40
```

Extract sleep animation from `neko.png`:
```bash
cargo run --bin ascii_art_converter -- sprite \
  -i sprites/neko.png \
  -o sprites/neko_sleep.txt \
  --num-frames 4 \
  --sprite-width 40 \
  --sprite-height 40 \
  --start-x 0 \
  --start-y 40
```

Note: Since each braille character represents 2x4 pixels, a 40x40 pixel sprite will produce a 20x10 character ASCII art output.