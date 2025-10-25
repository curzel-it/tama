# MIDI Composer Implementation Plan

## Overview
Implement full MIDI composition features in KMP app matching web/CLI functionality including:
- Complete note syntax parsing (duration, sharp, note, octave, waveform, volume)
- All waveforms (square, triangle, sawtooth, pulse)
- Arpeggios
- Flags (--bpm, --volume, --adsr, --vibrato)
- Multi-channel/polyphonic support

## Requirements (from user)
1. **Platform Support**: Feature parity on ALL platforms (JVM, Android, iOS)
2. **Audio Library**:
   - First: Search for a KMP library that provides audio playback
   - If not available: Use per-platform implementation with dependency injection
3. **Scope**: Only focus on KMP app (CLI and web already have feature parity)
4. **Existing Code**: MidiComposerScreen implements the UI, nothing else MIDI-related exists yet
5. **Sample Rate**: Use 48kHz (same as web implementation)
6. **Implementation Strategy**: Start with simple playback, get user feedback BEFORE implementing advanced features

## Current State
- Web implementation exists in `static/midi.js` (reference)
- CLI implementation exists in Rust (reference)
- KMP app has `MidiComposerScreen.kt` UI only

## Architecture (One Feature = One File)
Following the project's architecture:

### New Files to Create
1. **MidiSynthesizer.kt** (commonMain) - Core synthesis engine
   - Parse note syntax
   - Generate waveform samples
   - ADSR envelope
   - Vibrato effect

2. **MidiParser.kt** (commonMain) - Composition parsing
   - Parse individual notes
   - Parse arpeggios
   - Parse flags
   - Parse multi-channel syntax

3. **MidiWaveforms.kt** (commonMain) - Waveform generation
   - Square wave
   - Triangle wave
   - Sawtooth wave
   - Pulse wave

4. **MidiPlayer.kt** - Audio playback
   - **Interface** in commonMain
   - Platform implementations (jvmMain, androidMain, iosMain)
   - Use dependency injection pattern from CLAUDE.md

5. **MidiComposerViewModel.kt** - State management (if not exists)
   - Playback state
   - Volume control
   - Error handling

6. **MidiComposerUseCase.kt** - Business logic
   - Coordinate parser, synthesizer, and player
   - Handle composition playback

### Files to Modify
1. **MidiComposerScreen.kt** - Wire up new implementation

## Data Structures

### Note Data Class
```kotlin
data class MidiNote(
    val pitch: Int?, // MIDI note number, null for rest
    val duration: Double, // in seconds
    val waveform: Waveform,
    val volume: Float,
    val arpeggioNotes: List<Int>,
    val adsr: Boolean,
    val vibrato: Boolean
)

enum class Waveform { SQUARE, TRIANGLE, SAWTOOTH, PULSE }
```

### Channel Data Class
```kotlin
data class MidiChannel(
    val composition: String,
    val volume: Float?,
    val adsr: Boolean,
    val vibrato: Boolean
)

data class ParsedComposition(
    val channels: List<MidiChannel>,
    val bpm: Int?
)
```

### Audio Player Interface (Dependency Injection)
```kotlin
// In commonMain/MidiPlayer.kt
interface MidiPlayerBackend {
    fun play(samples: FloatArray, sampleRate: Int)
    fun stop()
    fun setVolume(volume: Float)
    fun isPlaying(): Boolean
}

object MidiPlayer {
    lateinit var backend: MidiPlayerBackend // Set during app init by native code

    fun play(composition: String) {
        val samples = MidiSynthesizer.generateAudioBuffer(composition)
        backend.play(samples, 48000)
    }
}
```

## Implementation Phases

### Phase 1: Simple Playback ✅ COMPLETED
- [x] Research KMP audio libraries
- [x] Create MidiParser.kt with basic note parsing
  - Support: `4c`, `8e`, `16g` (duration + note)
  - Support: octave modifiers `4c2`, `4e3`
  - Support: sharp `8#f`
- [x] Create MidiWaveforms.kt with ALL waveforms
- [x] Create MidiSynthesizer.kt
  - Parse composition
  - Generate samples for basic notes
- [x] Create MidiPlayer interface and implementations
  - JVM implementation (javax.sound.sampled)
  - Android implementation (AudioTrack)
  - iOS implementation (AVAudioEngine)
- [x] Wire up to MidiComposerScreen
  - Play button
  - Stop button
  - Simple composition input
- [x] Test with simple melodies

### Phase 2: Waveforms ✅ COMPLETED
- [x] Implement triangle wave
- [x] Implement sawtooth wave
- [x] Implement pulse wave
- [x] Add waveform modifier parsing: `4ct`, `4es`, `4gp`

### Phase 3: Volume & Tempo ✅ COMPLETED
- [x] Parse volume modifiers: `4c.5`, `4e.9`
- [x] Parse --bpm flag
- [x] Parse --volume flag

### Phase 4: Effects ✅ COMPLETED
- [x] Implement ADSR envelope
- [x] Implement vibrato effect
- [x] Parse --adsr flag
- [x] Parse --vibrato flag

### Phase 5: Arpeggios ✅ COMPLETED
- [x] Parse arpeggio syntax: `4(c e g)`
- [x] Generate arpeggio samples

### Phase 6: Multi-Channel ✅ COMPLETED
- [x] Parse --channel flag
- [x] Implement channel mixing
- [x] Test multi-channel compositions
- [x] Flag cascading behavior verified

### Phase 7: Feed Integration ✅ COMPLETED
- [x] Add audio playback to FeedViewModel
- [x] Auto-play MIDI on item change
- [x] Stop audio when showing static
- [x] Stop audio when leaving feed

### Phase 8: Testing & Polish ✅ COMPLETED
- [x] Test with all example songs from docs
- [x] Run ./gradlew build
- [x] Verify JVM and Android builds
- [x] iOS stub ready for future implementation

### Phase 9: iOS Implementation ✅ COMPLETED
- [x] Implement MidiComposerIos using AVAudioEngine
- [x] Convert Float32 samples to Int16 PCM format
- [x] Implement buffer scheduling with looping support
- [x] Add proper error handling with memScoped
- [x] Verify iOS framework builds successfully

## KMP Audio Library Research

### Research Results
**No suitable KMP library found for raw PCM playback.**

Existing KMP audio libraries (KMedia, BTMedia, gadulka, basic-sound) focus on audio **file** playback (MP3, WAV, OGG), not raw PCM buffer streaming which we need for synthesized audio.

### Decision: Per-platform implementation with dependency injection
- **JVM**: Use `javax.sound.sampled.AudioSystem` + `SourceDataLine`
- **Android**: Use `android.media.AudioTrack`
- **iOS**: Use `AVAudioEngine` + `AVAudioPlayerNode`

## Testing Strategy

### Unit Tests (commonMain)
- Parser: Test all note syntax variations
- Waveforms: Test sample generation correctness
- Synthesizer: Test composition parsing

### Platform Tests
- Audio playback on each platform
- Verify sample rate and format

## Sample Compositions for Testing

### Phase 1 (Simple)
```
4c 4e 4g
16e 16d 8#f 8#g
4c2 4e3 4g4
```

### Phase 2 (Waveforms)
```
4ct 4et 4gt
4cs 4es 4gs
4cp 4ep 4gp
```

### Phase 3 (Tempo/Volume)
```
--bpm 140 4c 4e 4g
4c.3 4e.6 4g.9
```

### Phase 4 (Effects)
```
--adsr 2c 2e 2g
--vibrato 1c 1e 1g
```

### Phase 5 (Arpeggios)
```
4(c e g) 4(d f a) 4(e g b)
```

### Phase 6 (Multi-channel)
```
--channel 4c 4e 4g --channel 4e 4g 4b
```

## Notes
- Follow CLAUDE.md: "One feature = One file"
- Use dependency injection for platform-specific code (no actual/expect)
- 48kHz sample rate (matching web implementation)
- Simple, clean UI matching existing style
- Unit test everything that can be tested
