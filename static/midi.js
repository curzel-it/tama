class MidiSynthesizer {
    constructor() {
        this.sampleRate = 48000;
        this.bpm = 120;
    }

    parseNote(input) {
        input = input.trim();
        if (!input) {
            throw new Error('Empty note string');
        }

        let i = 0;
        let durationStr = '';

        while (i < input.length && /\d/.test(input[i])) {
            durationStr += input[i];
            i++;
        }

        if (!durationStr) {
            throw new Error(`Missing duration in note: '${input}'`);
        }

        const durationValue = parseInt(durationStr);

        if (input[i] === '(') {
            return this.parseArpeggio(durationValue, input.substring(i));
        }

        let isSharp = false;
        if (input[i] === '#') {
            isSharp = true;
            i++;
        }

        const noteChar = input[i];
        if (!noteChar) {
            throw new Error(`Missing note letter in: '${input}'`);
        }

        if (noteChar === '-') {
            const duration = this.durationToSeconds(durationValue);
            return { pitch: null, duration, waveform: 'square', volume: 1.0, arpeggioNotes: [] };
        }

        const noteLower = noteChar.toLowerCase();
        if (!'abcdefg'.includes(noteLower)) {
            throw new Error(`Invalid note letter: '${noteChar}'`);
        }

        i++;

        let octave = 4;
        let waveform = 'square';
        let volume = 1.0;

        let octaveStr = '';
        while (i < input.length && /\d/.test(input[i])) {
            octaveStr += input[i];
            i++;
        }
        if (octaveStr) {
            octave = parseInt(octaveStr);
        }

        if (i < input.length && 'qtsp'.includes(input[i])) {
            const waveformChar = input[i];
            waveform = { 'q': 'square', 't': 'triangle', 's': 'sawtooth', 'p': 'pulse' }[waveformChar];
            i++;
        }

        if (i < input.length && input[i] === '.') {
            i++;
            if (i < input.length && /\d/.test(input[i])) {
                volume = parseInt(input[i]) / 10.0;
            }
        }

        const midiNote = this.noteToMidiNumber(noteLower, octave, isSharp);
        const duration = this.durationToSeconds(durationValue);

        return { pitch: midiNote, duration, waveform, volume, arpeggioNotes: [] };
    }

    parseArpeggio(durationValue, input) {
        let i = 1;
        let content = '';
        let foundClose = false;

        while (i < input.length) {
            if (input[i] === ')') {
                foundClose = true;
                i++;
                break;
            }
            content += input[i];
            i++;
        }

        if (!foundClose) {
            throw new Error('Missing closing parenthesis in arpeggio');
        }

        const noteStrings = content.trim().split(/\s+/);
        if (noteStrings.length === 0) {
            throw new Error('Empty arpeggio');
        }

        const arpeggioNotes = [];
        for (const noteStr of noteStrings) {
            let idx = 0;
            let isSharp = false;

            if (noteStr[idx] === '#') {
                isSharp = true;
                idx++;
            }

            const noteChar = noteStr[idx];
            const noteLower = noteChar.toLowerCase();

            if (!'abcdefg'.includes(noteLower)) {
                throw new Error(`Invalid arpeggio note: '${noteChar}'`);
            }

            idx++;
            let octaveStr = '';
            while (idx < noteStr.length && /\d/.test(noteStr[idx])) {
                octaveStr += noteStr[idx];
                idx++;
            }

            const octave = octaveStr ? parseInt(octaveStr) : 4;
            const midiNote = this.noteToMidiNumber(noteLower, octave, isSharp);
            arpeggioNotes.push(midiNote);
        }

        let waveform = 'square';
        let volume = 1.0;

        if (i < input.length && 'qtsp'.includes(input[i])) {
            const waveformChar = input[i];
            waveform = { 'q': 'square', 't': 'triangle', 's': 'sawtooth', 'p': 'pulse' }[waveformChar];
            i++;
        }

        if (i < input.length && input[i] === '.') {
            i++;
            if (i < input.length && /\d/.test(input[i])) {
                volume = parseInt(input[i]) / 10.0;
            }
        }

        const duration = this.durationToSeconds(durationValue);

        return { pitch: null, duration, waveform, volume, arpeggioNotes };
    }

    noteToMidiNumber(note, octave, sharp) {
        const baseNotes = { 'c': 0, 'd': 2, 'e': 4, 'f': 5, 'g': 7, 'a': 9, 'b': 11 };
        const base = baseNotes[note] || 0;
        const sharpOffset = sharp ? 1 : 0;
        const midiNote = 12 + (octave * 12) + base + sharpOffset;
        return Math.max(0, Math.min(127, midiNote));
    }

    durationToSeconds(duration) {
        const quarterNoteDuration = 60.0 / this.bpm;
        return (4.0 / duration) * quarterNoteDuration;
    }

    midiToFrequency(midiNote) {
        return 440.0 * Math.pow(2, (midiNote - 69) / 12.0);
    }

    generateNoteSamples(note) {
        if (note.arpeggioNotes.length > 0) {
            const allSamples = [];
            const noteDuration = note.duration / note.arpeggioNotes.length;

            for (const arpPitch of note.arpeggioNotes) {
                const frequency = this.midiToFrequency(arpPitch);
                const samples = this.generateWaveformSamples(frequency, noteDuration, note.volume, note.waveform);
                allSamples.push(...samples);
            }

            return allSamples;
        } else if (note.pitch !== null) {
            const frequency = this.midiToFrequency(note.pitch);
            return this.generateWaveformSamples(frequency, note.duration, note.volume, note.waveform);
        } else {
            const numSamples = Math.floor(this.sampleRate * note.duration);
            return new Array(numSamples).fill(0);
        }
    }

    generateWaveformSamples(frequency, duration, volume, waveform) {
        const numSamples = Math.floor(this.sampleRate * duration);
        const amplitude = 0.2 * volume;
        const samples = [];

        for (let i = 0; i < numSamples; i++) {
            const t = i / this.sampleRate;
            let sample = 0;

            switch (waveform) {
                case 'square': {
                    const wave = Math.sin(t * frequency * 2.0 * Math.PI);
                    sample = wave >= 0 ? amplitude : -amplitude;
                    break;
                }
                case 'triangle': {
                    const phase = (t * frequency) % 1.0;
                    const wave = phase < 0.5 ? (4.0 * phase - 1.0) : (3.0 - 4.0 * phase);
                    sample = wave * amplitude;
                    break;
                }
                case 'sawtooth': {
                    const phase = (t * frequency) % 1.0;
                    const wave = 2.0 * phase - 1.0;
                    sample = wave * amplitude;
                    break;
                }
                case 'pulse': {
                    const dutyCycle = 0.25;
                    const phase = (t * frequency) % 1.0;
                    const wave = phase < dutyCycle ? 1.0 : -1.0;
                    sample = wave * amplitude;
                    break;
                }
            }

            samples.push(sample);
        }

        return samples;
    }

    parseComposition(composition) {
        const noteStrings = composition.trim().split(/\s+/);
        const notes = [];

        for (const noteStr of noteStrings) {
            if (noteStr.startsWith('--')) {
                continue;
            }

            try {
                const note = this.parseNote(noteStr);
                notes.push(note);
            } catch (error) {
                console.warn(`Failed to parse note '${noteStr}':`, error);
            }
        }

        return notes;
    }

    generateAudioBuffer(composition) {
        const notes = this.parseComposition(composition);
        const allSamples = [];

        for (const note of notes) {
            const noteSamples = this.generateNoteSamples(note);
            allSamples.push(...noteSamples);
        }

        return allSamples;
    }
}

class MidiPlayer {
    constructor() {
        this.audioContext = null;
        this.sourceNode = null;
        this.gainNode = null;
        this.currentVolume = 1.0;
        this.isMuted = false;
        this.volumeBeforeMute = 1.0;
        this.synthesizer = new MidiSynthesizer();
    }

    initAudioContext() {
        if (!this.audioContext) {
            this.audioContext = new (window.AudioContext || window.webkitAudioContext)();
            this.gainNode = this.audioContext.createGain();
            this.gainNode.connect(this.audioContext.destination);
            this.gainNode.gain.value = this.isMuted ? 0 : this.currentVolume;
        }
    }

    async play(midiComposition) {
        this.stop();

        if (!midiComposition) {
            return;
        }

        try {
            this.initAudioContext();

            const samples = this.synthesizer.generateAudioBuffer(midiComposition);

            if (samples.length === 0) {
                console.log('No valid notes to play');
                return;
            }

            const audioBuffer = this.audioContext.createBuffer(1, samples.length, this.synthesizer.sampleRate);
            const channelData = audioBuffer.getChannelData(0);

            for (let i = 0; i < samples.length; i++) {
                channelData[i] = samples[i];
            }

            this.sourceNode = this.audioContext.createBufferSource();
            this.sourceNode.buffer = audioBuffer;
            this.sourceNode.loop = true;
            this.sourceNode.connect(this.gainNode);
            this.sourceNode.start();

        } catch (error) {
            console.log('MIDI playback error:', error);
        }
    }

    stop() {
        if (this.sourceNode) {
            try {
                this.sourceNode.stop();
                this.sourceNode.disconnect();
            } catch (e) {
                // Already stopped
            }
            this.sourceNode = null;
        }
    }

    isPlaying() {
        return this.sourceNode !== null;
    }

    setVolume(volume) {
        this.currentVolume = Math.max(0, Math.min(1, volume));
        if (this.gainNode && !this.isMuted) {
            this.gainNode.gain.value = this.currentVolume;
        }
    }

    getVolume() {
        return this.currentVolume;
    }

    mute() {
        if (!this.isMuted) {
            this.volumeBeforeMute = this.currentVolume;
            this.isMuted = true;
            if (this.gainNode) {
                this.gainNode.gain.value = 0;
            }
        }
    }

    unmute() {
        if (this.isMuted) {
            this.isMuted = false;
            if (this.gainNode) {
                this.gainNode.gain.value = this.currentVolume;
            }
        }
    }

    toggleMute() {
        if (this.isMuted) {
            this.unmute();
        } else {
            this.mute();
        }
        return this.isMuted;
    }

    getMuted() {
        return this.isMuted;
    }
}

export const midiPlayer = new MidiPlayer();

export function setVolume(volume) {
    midiPlayer.setVolume(volume);
}

export function getVolume() {
    return midiPlayer.getVolume();
}

export function mute() {
    midiPlayer.mute();
}

export function unmute() {
    midiPlayer.unmute();
}

export function toggleMute() {
    return midiPlayer.toggleMute();
}

export function isMuted() {
    return midiPlayer.getMuted();
}

window.midiSetVolume = setVolume;
window.midiGetVolume = getVolume;
window.midiMute = mute;
window.midiUnmute = unmute;
window.midiToggleMute = toggleMute;
window.midiIsMuted = isMuted;
