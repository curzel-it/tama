use rodio::{Decoder, OutputStream, OutputStreamHandle, Sink, Source};
use std::fs::File;
use std::io::BufReader;

pub struct AudioManager {
    _stream: OutputStream,
    stream_handle: OutputStreamHandle,
    current_sink: Option<Sink>,
}

impl AudioManager {
    pub fn new() -> Result<Self, String> {
        let (stream, stream_handle) = OutputStream::try_default()
            .map_err(|e| format!("Failed to create audio output stream: {e}"))?;

        Ok(Self {
            _stream: stream,
            stream_handle,
            current_sink: None,
        })
    }

    pub fn play(&mut self, path: &str) -> Result<(), String> {
        self.stop();

        let file = File::open(path)
            .map_err(|e| format!("Failed to open audio file '{path}': {e}"))?;

        let source = Decoder::new(BufReader::new(file))
            .map_err(|e| format!("Failed to decode audio file '{path}': {e}"))?;

        let sink = Sink::try_new(&self.stream_handle)
            .map_err(|e| format!("Failed to create audio sink: {e}"))?;

        sink.append(source.repeat_infinite());
        sink.play();

        self.current_sink = Some(sink);

        Ok(())
    }

    pub fn stop(&mut self) {
        if let Some(sink) = self.current_sink.take() {
            sink.stop();
        }
    }
}

impl Drop for AudioManager {
    fn drop(&mut self) {
        self.stop();
    }
}
