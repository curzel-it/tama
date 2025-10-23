#!/usr/bin/env python3
"""Generate a static noise ASCII art sheet for Tama"""

import random

def generate_static_sheet(width=30, height=9, frames=10):
    """Generate random braille characters to simulate TV static"""
    output = f"Ascii Art Animation, {width}x{height}\n"
    
    for _ in range(frames):
        for _ in range(height):
            for _ in range(width):
                # Braille characters range from U+2800 to U+28FF
                braille_char = chr(random.randint(0x2800, 0x28FF))
                output += braille_char
            output += "\n"
    
    return output

if __name__ == "__main__":
    static_art = generate_static_sheet()
    
    with open("sprites/static.txt", "w", encoding="utf-8") as f:
        f.write(static_art)
    
    print("Generated sprites/static.txt")
    print("Size: 30x9, 10 frames of random braille static")

