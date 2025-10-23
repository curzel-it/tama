mod remote;
mod ascii_art_player;
mod loading;

pub use remote::RemoteAnimation;
pub use ascii_art_player::AsciiArtPlayer;
pub use loading::LoadingAnimation;

use crate::ascii_art_converter::{TV_WIDTH, TV_HEIGHT};
use std::io::{self, Write};

use crossterm::{
    cursor,
    execute, queue,
    style::{Print},
    terminal::{self, ClearType},
};

pub struct UI;

impl UI {
    pub fn initialize() -> io::Result<()> {
        terminal::enable_raw_mode()?;
        execute!(
            io::stdout(),
            terminal::EnterAlternateScreen,
            cursor::Hide
        )?;
        Ok(())
    }

    pub fn cleanup() -> io::Result<()> {
        execute!(
            io::stdout(),
            cursor::Show,
            terminal::LeaveAlternateScreen
        )?;
        terminal::disable_raw_mode()?;
        Ok(())
    }

    pub fn clear_screen() -> io::Result<()> {
        execute!(
            io::stdout(),
            terminal::Clear(ClearType::All),
            cursor::MoveTo(0, 0)
        )?;
        Ok(())
    }

    pub fn display_channel_ascii(
        title: &str,
        ascii_art: &str,
        remote: Option<(&str, i16)>
    ) -> io::Result<()> {
        Self::clear_screen()?;
        let mut stdout = io::stdout();

        let art_lines: Vec<&str> = ascii_art.lines().collect();
        let content_height = art_lines.len();
        let content_width = art_lines.first().map(|l| l.chars().count()).unwrap_or(0);

        let pad_top = TV_HEIGHT.saturating_sub(content_height) / 2;
        let pad_left = TV_WIDTH.saturating_sub(content_width) / 2;

        let canvas_width = TV_WIDTH;
        let canvas_height = TV_HEIGHT;
        let inner_width = canvas_width + 2;
        let outer_width = inner_width + 2;

        let antenna_center = outer_width / 3;
        queue!(
            stdout,
            cursor::MoveTo(antenna_center as u16, 0),
            Print("╱"),
            cursor::MoveTo((antenna_center - 4) as u16, 1),
            Print("╲  ╱"),
            cursor::MoveTo((antenna_center - 3) as u16, 2),
            Print("╲╱"),
        )?;

        let outer_line = "─".repeat(outer_width - 2);
        let inner_line = "─".repeat(inner_width - 2);

        queue!(
            stdout,
            cursor::MoveTo(0, 3),
            Print(format!("╭{outer_line}╮")),
            cursor::MoveTo(0, 4),
            Print(format!("│╭{inner_line}╮│")),
        )?;

        for i in 0..canvas_height {
            let line_content = if i >= pad_top && i < pad_top + content_height {
                let art_line_idx = i - pad_top;
                if let Some(art_line) = art_lines.get(art_line_idx) {
                    let padding_left = " ".repeat(pad_left);
                    let padding_right = " ".repeat(canvas_width - pad_left - content_width);
                    format!("{padding_left}{art_line}{padding_right}")
                } else {
                    " ".repeat(canvas_width)
                }
            } else {
                " ".repeat(canvas_width)
            };

            queue!(
                stdout,
                cursor::MoveTo(0, 5 + i as u16),
                Print(format!("││{line_content}││")),
            )?;
        }

        let bottom_y = 5 + canvas_height as u16;
        queue!(
            stdout,
            cursor::MoveTo(0, bottom_y),
            Print(format!("│╰{inner_line}╯│")),
            cursor::MoveTo(0, bottom_y + 1),
        )?;

        let channel_padding = (outer_width - title.len()) / 2;
        let channel_line = format!(
            "│{}{}{}│",
            " ".repeat(channel_padding),
            title,
            " ".repeat(outer_width - channel_padding - title.len() - 2)
        );
        queue!(stdout, Print(channel_line))?;

        queue!(
            stdout,
            cursor::MoveTo(0, bottom_y + 2),
            Print(format!("╰{outer_line}╯")),
        )?;

        stdout.flush()?;

        queue!(
            stdout,
            cursor::MoveTo(0, bottom_y + 4),
            Print("[1] Previous"),
            cursor::MoveTo(0, bottom_y + 5),
            Print("[2] Next"),
            cursor::MoveTo(0, bottom_y + 6),
            Print("[S] Copy links to clipboard"),
            cursor::MoveTo(0, bottom_y + 7),
            Print("[Q] Exit"),
        )?;
        stdout.flush()?;

        if let Some((remote_art, y_offset)) = remote {
            let remote_x = (outer_width + 2) as u16;
            let remote_base_y = 5 + (canvas_height / 2) as u16;
            let remote_y = (remote_base_y as i16 + y_offset) as u16;

            for (i, line) in remote_art.lines().enumerate() {
                queue!(
                    stdout,
                    cursor::MoveTo(remote_x, remote_y + i as u16),
                    Print(line),
                )?;
            }

            stdout.flush()?;
        }

        Ok(())
    }

}
