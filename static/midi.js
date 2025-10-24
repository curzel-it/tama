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
            return { pitch: null, duration, waveform: 'square', volume: 1.0, arpeggioNotes: [], adsr: false, vibrato: false };
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

        return { pitch: midiNote, duration, waveform, volume, arpeggioNotes: [], adsr: false, vibrato: false };
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

        return { pitch: null, duration, waveform, volume, arpeggioNotes, adsr: false, vibrato: false };
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

    parseChannels(input) {
        const tokens = input.trim().split(/\s+/);

        if (tokens.length === 0) {
            throw new Error('Empty input');
        }

        const globalFlags = { volume: null, adsr: false, vibrato: false };
        let currentFlags = { volume: null, adsr: false, vibrato: false };
        let bpm = null;
        const channels = [];
        let i = 0;
        let foundChannel = false;

        while (i < tokens.length) {
            const token = tokens[i];

            if (token === '--volume') {
                if (i + 1 >= tokens.length) {
                    throw new Error('Missing value for --volume');
                }
                const vol = parseFloat(tokens[i + 1]);
                if (isNaN(vol)) {
                    throw new Error(`Invalid volume value: '${tokens[i + 1]}'`);
                }
                if (vol < 0.0 || vol > 1.0) {
                    throw new Error(`Volume must be between 0.0 and 1.0, got ${vol}`);
                }

                if (foundChannel) {
                    currentFlags.volume = vol;
                } else {
                    globalFlags.volume = vol;
                }
                i += 2;
            } else if (token === '--bpm') {
                if (i + 1 >= tokens.length) {
                    throw new Error('Missing value for --bpm');
                }
                const bpmVal = parseInt(tokens[i + 1]);
                if (isNaN(bpmVal)) {
                    throw new Error(`Invalid BPM value: '${tokens[i + 1]}'`);
                }
                if (bpmVal < 1 || bpmVal > 300) {
                    throw new Error(`BPM must be between 1 and 300, got ${bpmVal}`);
                }
                bpm = bpmVal;
                i += 2;
            } else if (token === '--adsr') {
                if (foundChannel) {
                    currentFlags.adsr = true;
                } else {
                    globalFlags.adsr = true;
                }
                i += 1;
            } else if (token === '--vibrato') {
                if (foundChannel) {
                    currentFlags.vibrato = true;
                } else {
                    globalFlags.vibrato = true;
                }
                i += 1;
            } else if (token === '--channel') {
                if (foundChannel && channels.length === 0) {
                    throw new Error('--channel found but no composition provided');
                }
                foundChannel = true;
                currentFlags = { volume: null, adsr: false, vibrato: false };
                i += 1;
            } else {
                if (foundChannel) {
                    let composition = '';
                    while (i < tokens.length && !tokens[i].startsWith('--')) {
                        if (composition) {
                            composition += ' ';
                        }
                        composition += tokens[i];
                        i++;
                    }

                    if (!composition) {
                        throw new Error('Empty composition for channel');
                    }

                    const volume = currentFlags.volume !== null ? currentFlags.volume : globalFlags.volume;
                    const adsr = currentFlags.adsr || globalFlags.adsr;
                    const vibrato = currentFlags.vibrato || globalFlags.vibrato;

                    channels.push({ composition, volume, adsr, vibrato });
                    foundChannel = false;
                } else {
                    let composition = '';
                    while (i < tokens.length && !tokens[i].startsWith('--')) {
                        if (composition) {
                            composition += ' ';
                        }
                        composition += tokens[i];
                        i++;
                    }

                    if (composition) {
                        channels.push({
                            composition,
                            volume: globalFlags.volume,
                            adsr: globalFlags.adsr,
                            vibrato: globalFlags.vibrato
                        });
                    }
                }
            }
        }

        if (foundChannel) {
            throw new Error('--channel found but no composition provided');
        }

        if (channels.length === 0) {
            throw new Error('No channels or compositions found');
        }

        return { channels, bpm };
    }

    applyAdsrEnvelope(sampleIndex, totalSamples) {
        const t = sampleIndex / totalSamples;

        const attackTime = 0.05;
        const decayTime = 0.15;
        const sustainLevel = 0.7;
        const releaseTime = 0.20;

        if (t < attackTime) {
            return t / attackTime;
        } else if (t < attackTime + decayTime) {
            const decayProgress = (t - attackTime) / decayTime;
            return 1.0 - (1.0 - sustainLevel) * decayProgress;
        } else if (t < 1.0 - releaseTime) {
            return sustainLevel;
        } else {
            const releaseProgress = (t - (1.0 - releaseTime)) / releaseTime;
            return sustainLevel * (1.0 - releaseProgress);
        }
    }

    applyVibrato(t, frequency) {
        const vibratoRate = 5.0;
        const vibratoDepth = 0.02;
        const vibratoOffset = Math.sin(t * vibratoRate * 2.0 * Math.PI) * vibratoDepth;
        return frequency * (1.0 + vibratoOffset);
    }

    generateNoteSamples(note) {
        if (note.arpeggioNotes.length > 0) {
            const allSamples = [];
            const noteDuration = note.duration / note.arpeggioNotes.length;

            for (const arpPitch of note.arpeggioNotes) {
                const frequency = this.midiToFrequency(arpPitch);
                const samples = this.generateWaveformSamples(frequency, noteDuration, note.volume, note.waveform, note.adsr, note.vibrato);
                allSamples.push(...samples);
            }

            return allSamples;
        } else if (note.pitch !== null) {
            const frequency = this.midiToFrequency(note.pitch);
            return this.generateWaveformSamples(frequency, note.duration, note.volume, note.waveform, note.adsr, note.vibrato);
        } else {
            const numSamples = Math.floor(this.sampleRate * note.duration);
            return new Array(numSamples).fill(0);
        }
    }

    generateWaveformSamples(frequency, duration, volume, waveform, adsr, vibrato) {
        const numSamples = Math.floor(this.sampleRate * duration);
        const amplitude = 0.2 * volume;
        const samples = [];

        for (let i = 0; i < numSamples; i++) {
            const t = i / this.sampleRate;
            const freq = vibrato ? this.applyVibrato(t, frequency) : frequency;
            const envelope = adsr ? this.applyAdsrEnvelope(i, numSamples) : 1.0;

            let sample = 0;

            switch (waveform) {
                case 'square': {
                    const wave = Math.sin(t * freq * 2.0 * Math.PI);
                    sample = wave >= 0 ? amplitude : -amplitude;
                    break;
                }
                case 'triangle': {
                    const phase = (t * freq) % 1.0;
                    const wave = phase < 0.5 ? (4.0 * phase - 1.0) : (3.0 - 4.0 * phase);
                    sample = wave * amplitude;
                    break;
                }
                case 'sawtooth': {
                    const phase = (t * freq) % 1.0;
                    const wave = 2.0 * phase - 1.0;
                    sample = wave * amplitude;
                    break;
                }
                case 'pulse': {
                    const dutyCycle = 0.25;
                    const phase = (t * freq) % 1.0;
                    const wave = phase < dutyCycle ? 1.0 : -1.0;
                    sample = wave * amplitude;
                    break;
                }
            }

            samples.push(sample * envelope);
        }

        return samples;
    }

    parseComposition(composition) {
        const noteStrings = composition.trim().split(/\s+/);
        const notes = [];

        for (const noteStr of noteStrings) {
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
        if (composition.includes('--channel') || composition.includes('--bpm') ||
            composition.includes('--volume') || composition.includes('--adsr') ||
            composition.includes('--vibrato')) {

            const { channels, bpm } = this.parseChannels(composition);

            if (bpm !== null) {
                this.bpm = bpm;
            }

            if (channels.length === 1 && !composition.includes('--channel')) {
                const channel = channels[0];
                let notes = this.parseComposition(channel.composition);

                for (const note of notes) {
                    if (channel.volume !== null && note.volume === 1.0) {
                        note.volume = channel.volume;
                    }
                    note.adsr = channel.adsr;
                    note.vibrato = channel.vibrato;
                }

                const allSamples = [];
                for (const note of notes) {
                    const noteSamples = this.generateNoteSamples(note);
                    allSamples.push(...noteSamples);
                }
                return allSamples;
            } else {
                const channelSamples = [];
                let maxLength = 0;

                for (const channel of channels) {
                    let notes = this.parseComposition(channel.composition);

                    for (const note of notes) {
                        if (channel.volume !== null && note.volume === 1.0) {
                            note.volume = channel.volume;
                        }
                        note.adsr = channel.adsr;
                        note.vibrato = channel.vibrato;
                    }

                    const channelBuffer = [];
                    for (const note of notes) {
                        const noteSamples = this.generateNoteSamples(note);
                        channelBuffer.push(...noteSamples);
                    }

                    maxLength = Math.max(maxLength, channelBuffer.length);
                    channelSamples.push(channelBuffer);
                }

                const mixedSamples = new Array(maxLength).fill(0);

                for (const channelBuffer of channelSamples) {
                    for (let i = 0; i < channelBuffer.length; i++) {
                        mixedSamples[i] += channelBuffer[i];
                    }
                }

                return mixedSamples;
            }
        } else {
            const notes = this.parseComposition(composition);
            const allSamples = [];

            for (const note of notes) {
                const noteSamples = this.generateNoteSamples(note);
                allSamples.push(...noteSamples);
            }

            return allSamples;
        }
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
