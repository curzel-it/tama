# MIDI Composer Syntax

A Nokia 3310-style MIDI notation system for composing retro game audio. This document describes the string syntax used to create music.

## Note Syntax

The complete syntax for a single note is:

```
[duration][#][note][octave][waveform][.volume]
```

### Components

| Component | Options | Description | Default |
|-----------|---------|-------------|---------|
| **Duration** | `1`, `2`, `4`, `8`, `16`, `32` | Note length (whole to 32nd note) | _Required_ |
| **Sharp** | `#` | Sharp modifier (e.g., `#f` = F♯) | Natural |
| **Note** | `c`, `d`, `e`, `f`, `g`, `a`, `b` or `-` | Note name or rest | _Required_ |
| **Octave** | `1`-`8` | Octave number | `4` (middle) |
| **Waveform** | `q`, `t`, `s`, `p` | Wave type (see below) | `q` (square) |
| **Volume** | `.0`-`.9` | Volume level (.0 = silent, .9 = 90%) | `1.0` (full) |

### Waveforms

|Wave|Symbol|Description|
|---|---|---|
|Square wave|`q`|Classic retro game sound, harsh and bold|
|Triangle wave|`t`|Softer, more mellow tone|
|Sawtooth wave|`s`|Buzzy, bright sound|
|Pulse wave|`p`|Similar to square but with different timbre|

### Note Examples

|Note|Description|
|---|---|
|`4c     `|Quarter note C, octave 4, square wave, full volume|
|`8#f    `|Eighth note F♯, octave 4, square wave, full volume|
|`16e2   `|16th note E, octave 2, square wave, full volume|
|`4ct    `|Quarter note C, triangle wave|
|`4e.5   `|Quarter note E at 50% volume|
|`4gs.7  `|Quarter note G, sawtooth wave at 70% volume|
|`8#f3t.5`|Eighth note F♯ octave 3, triangle wave at 50% volume|
|`2-     `|Half note rest (silence)|

### Simple Sequences

Multiple notes separated by spaces:

```
4c 4e 4g
```
Plays C-E-G quarter notes in sequence.

```
16e2 16d2 8#f4 8#g4
```
Classic Nokia ringtone opening.

## Arpeggio

Arpeggio allows you to rapidly cycle through multiple notes. Use parentheses with space-separated notes:

|Example|Effect|
|---|---|
|`4(c e g)   `|C major chord arpeggio (quarter note duration)|
|`4(#c #f a) `|Arpeggiated chord with sharps|
|`4(c2 e3 g4)`|Arpeggio across multiple octaves|
|`8(d f a)t  `|Eighth note arpeggio with triangle wave|
|`4(e g b)s.5`|Quarter note arpeggio, sawtooth wave, 50% volume|

The total duration is divided equally among all notes in the arpeggio.

## Flags

Flags modify notes and control playback. They are prefixed with `--` and placed before the notes they affect.

| Flag | Value | Example | Description |
|------|------------|---------|-------------|
| `--bpm` | 1-300 | `--bpm 140 4e 4g 4a` | Set tempo in beats per minute (default: 120) |
| `--volume` | 0.0-1.0 | `--volume 0.5 4e 4g 4a` | Set default volume for all notes |
| `--adsr` | boolean | `--adsr 4e 4g 4a` | Enable ADSR envelope (attack, decay, sustain, release) |
| `--vibrato` | boolean | `--vibrato 4e 4g 4a` | Enable vibrato effect (5Hz pitch wobble, 2% depth) |

**Combining Flags:**
```
--bpm 140 --volume 0.7 --adsr --vibrato 4e 4g 4a
```

## Polyphonic Support (Multi-Channel)

Create rich, layered music by playing multiple channels simultaneously. Each channel can have its own composition and settings.

### Basic Multi-Channel Syntax

```
--channel composition1 --channel composition2 --channel composition3
```

### Flag Cascading Rules

Flags apply to all subsequent channels until overridden:

- **Global flags** (before first `--channel`): Apply to all channels
- **Channel-specific flags** (between `--channel` markers): Apply only to the next channel
- **BPM** is always global (applies to all channels)

### Multi-Channel Examples

**Simple Two-Channel Harmony:**
```
--channel 4c 4e 4g --channel 4e 4g 4b
```
Plays C major chord (C-E-G) in one channel and E minor chord (E-G-B) in another simultaneously.

**Global Volume Applied to All Channels:**
```
--volume 0.5 --channel 4c 4e 4g --channel 4e 4g 4b
```
Both channels play at 50% volume.

**Different Volumes Per Channel:**
```
--volume 0.8 --channel 4c 4e --channel --volume 0.3 4g 4b
```
First channel at 80% volume, second channel at 30% volume.

**Global BPM with ADSR:**
```
--bpm 140 --adsr --channel 4c 4e 4g --channel 4e 4g 4b
```
Both channels play at 140 BPM with ADSR envelope enabled.

**Complex Multi-Channel:**
```
--volume 0.7 --adsr --channel 4c 4e 4g 4c --channel 8d 8f 8a 8d 8f 8a --channel --volume 0.3 --vibrato 2g2t 2b2t
```
- Channel 1: 70% volume, ADSR enabled
- Channel 2: 70% volume, ADSR enabled, faster rhythm
- Channel 3: 30% volume, ADSR + vibrato, low octaves with triangle wave

### Multi-Channel Use Cases

| Style | Example |
|-------|---------|
| **Bass + Melody** | `--channel 2c1 2c1 2c1 2c1 --channel 4e 4g 4a 4g` |
| **Chord Progression** | `--channel 1(c e g) --channel 1(d f a) --channel 1(e g b)` |
| **Rhythm + Lead** | `--channel 8c 8c 8c 8c --channel --volume 0.4 4e 4g 4a` |
| **Layered Waveforms** | `--channel 4cq --channel 4ct --channel 4cs` |

### Multi-Channel Song: "Digital Dreams"

A complete 3-channel composition showcasing bass, lead melody, and harmony:

```
--bpm 130 --adsr --channel 2c2s 2c2s 2g2s 2g2s 2a2s 2a2s 2g2s 2g2s --channel --volume 0.8 4e4t 4g4t 4a4t 4g4t 4e4t 4g4t 4a4t 4b4t --channel --volume 0.6 --vibrato 2g3t 2g3t 2d4t 2d4t 2e4t 2e4t 2d4t 2d4t
```

**Breakdown:**
- **Channel 1 (Bass)**: Deep sawtooth bass line in octave 2, following C-C-G-G-A-A-G-G pattern
- **Channel 2 (Lead)**: Main melody at 80% volume with triangle wave in octave 4
- **Channel 3 (Harmony)**: Background harmony at 60% volume with vibrato effect in octave 3

**Shorter version for testing:**
```
--bpm 130 --adsr --channel 2c2s 2g2s 2a2s 2g2s --channel --volume 0.8 4e4t 4g4t 4a4t 4b4t --channel --volume 0.6 --vibrato 2g3t 2d4t 2e4t 2d4t
```

### Rock Riff

A power-chord style riff with rhythm and lead:

```
--bpm 140 --channel 8e2s 8e2s 8g2s 8a2s 8e2s 8g2s 8#a2s 8a2s --channel --volume 0.7 8e3s 8e3s 8g3s 8a3s 8e3s 8g3s 8#a3s 8a3s --channel --volume 0.9 --adsr 4e4t 8- 4a4t 8- 4g4t 8- 4e4t 8-
```

**Breakdown:**
- **Channel 1 (Bass)**: Low octave power chord roots
- **Channel 2 (Rhythm)**: Same pattern one octave up at 70% volume
- **Channel 3 (Lead)**: High melody with rests for rhythmic punch, ADSR envelope

### Arpeggio Chord Progression

Create a rich, layered progression:

```
--bpm 100 --adsr --channel 2(c2 e2 g2) 2(a1 c2 e2) 2(f2 a2 c3) 2(g2 b2 d3) --channel --volume 0.6 1c4t 1a3t 1f4t 1g4t --channel --volume 0.4 --vibrato 1g3t 1e3t 1c4t 1d4t
```

**Breakdown:**
- **Channel 1**: Arpeggiated bass chords (C maj, A min, F maj, G maj)
- **Channel 2**: Sustained melody notes with triangle wave
- **Channel 3**: Soft harmony layer with vibrato effect

## Examples by Feature

Here's the same simple melody (C-E-G ascending) with different features applied:

| Feature | Example | Description |
|---------|---------|-------------|
| **Basic** | `4c 4e 4g` | Classic square wave at full volume in octave 4 |
| **Triangle Wave** | `4ct 4et 4gt` | Softer, mellower sound using triangle waves |
| **Sawtooth Wave** | `4cs 4es 4gs` | Bright, buzzy sound using sawtooth waves |
| **Pulse Wave** | `4cp 4ep 4gp` | Alternative retro sound with pulse waves |
| **Volume Control** | `4c.3 4e.6 4g.9` | Crescendo: 30% → 60% → 90% volume |
| **Different Octaves** | `4c2 4e3 4g4` | Rising through octaves (low to high) |
| **With Tempo** | `--bpm 180 4c 4e 4g` | Fast tempo (180 BPM) |
| **With ADSR** | `--adsr 2c 2e 2g` | Smooth attack and release envelope |
| **With Vibrato** | `--vibrato 1c 1e 1g` | Pitch wobble for expressive sound |
| **ADSR + Vibrato** | `--adsr --vibrato 2ct 2et 2gt` | Combined effects with triangle wave |
| **Everything Combined** | `4c2s.9 4e3s.6 4g4s.3` | Octaves 2→3→4, sawtooth, volume 90%→60%→30% |
| **Arpeggio** | `4(c e g) 4(d f a) 4(e g b)` | Three arpeggiated chords: C major, D minor, E minor |

## Example Songs

| Song | Composition |
|------|-------------|
| **Nokia Ringtone** | `16e 16d 8#f 8#g 16#c 16b 8d 8e 16b 16a 8#c 8e 2a 2-` |
| **Smoke on the Water (Basic)** | `4e 4g 4a 4e 4g 4#a 4a 4e 4g 4a 4g 4e` |
| **Smoke on the Water (Enhanced)** | `--bpm 150 4e3s.9 4g3s.9 4a3s.9 4e3s.8 4g3s.8 4#a3s.8 4a3s.7 32- 4e3s.9 4g3s.9 4a3s.9 4g3s.8 4e3s.7` |

## Italian Note Names Reference

For those familiar with solfège or Italian note naming:

| Italian | English | Mnemonic |
|---------|---------|----------|
| do      | c       | "dough"  |
| re      | d       | "ray"    |
| mi      | e       | "me"     |
| fa      | f       | "far"    |
| sol     | g       | "so"     |
| la      | a       | "la"     |
| si      | b       | "tea"    |

**Note:** The composer only accepts English note names (a-g) in the input.

## Technical Details

- **Sample Rate**: 48 kHz
- **MIDI Range**: Notes 12-108 (C0 to C8)
- **Default Tempo**: 120 BPM
- **Audio Backend**: rodio (Rust audio library)
- **Waveform Generation**: Pure sine-based synthesis for each waveform type

## Tips

1. **Start Simple**: Begin with basic notes like `4c 4e 4g` before adding modifiers
2. **Experiment with Waveforms**: Try the same melody with different waveforms (`q`, `t`, `s`, `p`)
3. **Use Rests**: Add `2-` or `4-` for dramatic pauses
4. **Layer Arpeggios**: Create complex sounds with `4(c e g b)` style arpeggios
5. **Change Tempo**: Speed up or slow down with `--bpm` to find the right feel
6. **Volume Dynamics**: Use volume modifiers (`.0`-`.9`) to create crescendos and diminuendos
7. **Multi-Channel Depth**: Use 2-4 channels to create rich, layered compositions
