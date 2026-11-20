#!/usr/bin/env python3
"""
Wrong Button 的音效與音樂產生器。

全部用程式合成（numpy），不使用任何外部取樣或音源，所以沒有授權問題。
產生 WAV 後用 ffmpeg 編成 Ogg Vorbis，放進 app/src/main/res/raw/。

用法：python3 tools/audio/gen_audio.py
需要：numpy、ffmpeg（含 libvorbis）。

音效（單聲道）：sfx_tap、sfx_wrong、sfx_success、sfx_pop、sfx_swipe、sfx_toggle、sfx_dialog
音樂（立體聲、可無縫循環）：
  music_title    主選單：C 大調、84 BPM，電鋼琴與柔和的低音。
  music_puzzle   第一季的關卡：F 大調、104 BPM，木琴、撥弦低音、彈指。
  music_outside  第二季的關卡：D 多利安、92 BPM，顫音琴、鋪底與次低音。
"""
import os
import subprocess
import tempfile
import wave

import numpy as np

SR = 44100
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RAW = os.path.join(ROOT, "app/src/main/res/raw")
rng = np.random.default_rng(20261120)


# ---------------------------------------------------------------- 基本工具

def t_axis(dur):
    return np.arange(int(dur * SR)) / SR


def midi(n):
    return 440.0 * 2 ** ((n - 69) / 12)


def env_adsr(n, a=0.005, d=0.1, s=0.6, r=0.1, sus_len=None):
    """長度 n 個取樣的 ADSR 包絡。"""
    a_n, d_n, r_n = int(a * SR), int(d * SR), int(r * SR)
    s_n = max(0, n - a_n - d_n - r_n) if sus_len is None else int(sus_len * SR)
    e = np.concatenate([
        np.linspace(0, 1, max(a_n, 1), endpoint=False),
        np.linspace(1, s, max(d_n, 1), endpoint=False),
        np.full(s_n, s),
        np.linspace(s, 0, max(r_n, 1)),
    ])
    out = np.zeros(n)
    out[:min(n, len(e))] = e[:n]
    return out


def env_exp(n, decay):
    return np.exp(-np.arange(n) / SR / decay)


def fft_filter(x, lo=None, hi=None):
    """用頻域做簡單的帶通／低通／高通（柔和的邊緣）。"""
    n = len(x)
    size = 1 << (n - 1).bit_length()
    X = np.fft.rfft(x, size)
    f = np.fft.rfftfreq(size, 1 / SR)
    g = np.ones_like(f)
    if hi is not None:
        g *= 1 / np.sqrt(1 + (f / hi) ** 4)
    if lo is not None:
        g *= 1 / np.sqrt(1 + (lo / np.maximum(f, 1e-3)) ** 4)
    return np.fft.irfft(X * g, size)[:n]


def reverb_ir(seconds=1.4, decay=0.45, seed=1):
    r = np.random.default_rng(seed)
    n = int(seconds * SR)
    ir = r.standard_normal(n) * np.exp(-np.arange(n) / SR / decay)
    ir = fft_filter(ir, lo=200, hi=5000)
    ir[0] = 0
    return ir / np.sqrt(np.sum(ir ** 2))


def convolve(x, ir):
    n = len(x) + len(ir) - 1
    size = 1 << (n - 1).bit_length()
    return np.fft.irfft(np.fft.rfft(x, size) * np.fft.rfft(ir, size), size)[:n]


def normalize(x, peak=0.89):
    m = np.max(np.abs(x))
    return x * (peak / m) if m > 0 else x


def write_wav(path, data):
    data = np.clip(data, -1, 1)
    pcm = (data * 32767).astype(np.int16)
    ch = 1 if pcm.ndim == 1 else pcm.shape[1]
    with wave.open(path, "wb") as w:
        w.setnchannels(ch)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())


def to_ogg(name, data, quality):
    os.makedirs(RAW, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, name + ".wav")
        write_wav(wav, data)
        out = os.path.join(RAW, name + ".ogg")
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", str(quality), out], check=True)
    print(f"{name}.ogg  {os.path.getsize(out) // 1024} KB  {len(data) / SR:.1f} s")


# ---------------------------------------------------------------- 樂器

def epiano(freq, dur, vel=0.7):
    """FM 電鋼琴：調變指數隨時間衰減，帶一點敲擊聲。"""
    n = int(dur * SR)
    t = np.arange(n) / SR
    index = 2.2 * np.exp(-t / 0.35) + 0.4
    mod = np.sin(2 * np.pi * freq * t) * index
    body = np.sin(2 * np.pi * freq * t + mod)
    tine = 0.25 * np.sin(2 * np.pi * freq * 14 * t) * np.exp(-t / 0.03)
    e = env_adsr(n, a=0.004, d=0.6, s=0.35, r=min(0.25, dur * 0.4))
    return (body + tine) * e * vel


def marimba(freq, dur, vel=0.7):
    n = int(dur * SR)
    t = np.arange(n) / SR
    x = np.sin(2 * np.pi * freq * t) * np.exp(-t / 0.35)
    x += 0.35 * np.sin(2 * np.pi * freq * 3.99 * t) * np.exp(-t / 0.06)
    x += 0.12 * np.sin(2 * np.pi * freq * 9.2 * t) * np.exp(-t / 0.02)
    return x * env_adsr(n, a=0.002, d=0.05, s=1.0, r=0.03) * vel


def vibes(freq, dur, vel=0.6):
    n = int(dur * SR)
    t = np.arange(n) / SR
    trem = 1 + 0.25 * np.sin(2 * np.pi * 5.2 * t)
    x = np.sin(2 * np.pi * freq * t) + 0.2 * np.sin(2 * np.pi * freq * 4 * t) * np.exp(-t / 0.15)
    return x * trem * env_adsr(n, a=0.004, d=0.8, s=0.4, r=0.3) * vel


def pluck_bass(freq, dur, vel=0.8):
    n = int(dur * SR)
    t = np.arange(n) / SR
    saw = 2 * ((freq * t) % 1) - 1
    x = 0.6 * np.sin(2 * np.pi * freq * t) + 0.4 * fft_filter(saw, hi=1500)
    return x * env_adsr(n, a=0.004, d=0.25, s=0.5, r=0.06) * vel


def soft_bass(freq, dur, vel=0.8):
    n = int(dur * SR)
    t = np.arange(n) / SR
    x = np.sin(2 * np.pi * freq * t) + 0.15 * np.sin(2 * np.pi * freq * 2 * t)
    return x * env_adsr(n, a=0.02, d=0.3, s=0.7, r=0.12) * vel


def pad(freqs, dur, vel=0.25):
    n = int(dur * SR)
    t = np.arange(n) / SR
    x = np.zeros(n)
    for f in freqs:
        for det in (-0.12, 0.12):
            ph = rng.uniform(0, 2 * np.pi)
            x += 2 * ((f * 2 ** (det / 12) * t + ph / (2 * np.pi)) % 1) - 1
    x = fft_filter(x / (len(freqs) * 2), hi=1400)
    return x * env_adsr(n, a=0.6, d=0.2, s=0.9, r=0.8) * vel


def kick(vel=0.9):
    n = int(0.35 * SR)
    t = np.arange(n) / SR
    f = 50 + 90 * np.exp(-t / 0.04)
    ph = 2 * np.pi * np.cumsum(f) / SR
    return np.sin(ph) * np.exp(-t / 0.12) * vel


def hat(vel=0.25, decay=0.035):
    n = int(0.12 * SR)
    x = fft_filter(rng.standard_normal(n), lo=7000)
    return x * env_exp(n, decay) * vel


def snap(vel=0.5):
    n = int(0.15 * SR)
    x = fft_filter(rng.standard_normal(n), lo=1500, hi=4000)
    return x * env_exp(n, 0.03) * vel


def rim(vel=0.4):
    n = int(0.08 * SR)
    t = np.arange(n) / SR
    x = np.sin(2 * np.pi * 1700 * t) * np.exp(-t / 0.012) + 0.4 * fft_filter(rng.standard_normal(n), lo=2000) * np.exp(-t / 0.01)
    return x * vel


# ---------------------------------------------------------------- 混音

class Mix:
    """立體聲混音台。所有聲音放在 [0, length) 之內；超出長度的尾巴繞回開頭，所以可以無縫循環。"""

    def __init__(self, bpm, bars, beats_per_bar=4):
        self.beat = 60 / bpm
        self.length = int(bars * beats_per_bar * self.beat * SR)
        self.L = np.zeros(self.length)
        self.R = np.zeros(self.length)
        self.sendL = np.zeros(self.length)
        self.sendR = np.zeros(self.length)

    def add(self, x, beat, pan=0.0, gain=1.0, send=0.2, swing=0.0):
        start = int(beat * self.beat * SR)
        if swing and (beat * 2) % 2 == 1:
            start += int(swing * self.beat * SR)
        gl = gain * np.cos((pan + 1) * np.pi / 4)
        gr = gain * np.sin((pan + 1) * np.pi / 4)
        start %= self.length
        pos = 0
        while pos < len(x):
            at = (start + pos) % self.length
            k = min(len(x) - pos, self.length - at)
            seg = x[pos:pos + k]
            self.L[at:at + k] += seg * gl
            self.R[at:at + k] += seg * gr
            self.sendL[at:at + k] += seg * gl * send
            self.sendR[at:at + k] += seg * gr * send
            pos += k

    def render(self, reverb_seconds=1.6, reverb_decay=0.5):
        out = []
        for dry, wet, seed in ((self.L, self.sendL, 3), (self.R, self.sendR, 4)):
            tail = convolve(wet, reverb_ir(reverb_seconds, reverb_decay, seed))
            looped = np.zeros(self.length)
            for k in range(0, len(tail), self.length):
                seg = tail[k:k + self.length]
                looped[:len(seg)] += seg
            out.append(dry + looped)
        st = np.stack(out, axis=1)
        # 輕微的總線壓縮，讓音量穩一點。
        st = np.tanh(st * 1.2) / np.tanh(1.2)
        return normalize(st, 0.8)


CHORDS = {
    "Cmaj7": [60, 64, 67, 71], "Am7": [57, 60, 64, 67], "Dm7": [62, 65, 69, 72], "G7": [55, 59, 62, 65],
    "Fmaj7": [53, 57, 60, 64], "G6": [55, 59, 62, 64], "F": [53, 57, 60, 65], "Bb": [58, 62, 65, 70],
    "Gm7": [55, 58, 62, 65], "C7": [48, 52, 55, 58], "Dm9": [50, 53, 57, 60, 64], "G13": [43, 53, 59, 64],
    "Cmaj9": [48, 52, 55, 59, 62], "A7sus": [45, 50, 52, 55], "A7": [45, 49, 52, 55],
}
N = {n: i for i, n in enumerate(["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "Bb", "B"])}


def note(name):
    """「E5」「Bb4」→ MIDI 編號。"""
    pitch, octave = name[:-1], int(name[-1])
    return 12 * (octave + 1) + N[pitch]


def play_melody(mix, inst, notes, start_beat=0.0, pan=0.0, gain=1.0, send=0.3, octave=0, swing=0.0):
    b = start_beat
    for name, dur in notes:
        if name != "r":
            mix.add(inst(midi(note(name) + 12 * octave), dur * mix.beat * 1.05), b, pan, gain, send, swing)
        b += dur
    return b


# ---------------------------------------------------------------- 音樂

def music_title():
    mix = Mix(84, 16)
    prog = ["Cmaj7", "Am7", "Dm7", "G7", "Cmaj7", "Am7", "Fmaj7", "G6"] * 2
    for bar, ch in enumerate(prog):
        b = bar * 4
        for i, k in enumerate(CHORDS[ch]):
            mix.add(epiano(midi(k), 1.6 * mix.beat, 0.22), b + 0.01 * i, pan=-0.25, send=0.35)
            mix.add(epiano(midi(k), 1.2 * mix.beat, 0.15), b + 2.5 + 0.01 * i, pan=-0.25, send=0.35)
        root = CHORDS[ch][0] - 24 + (12 if CHORDS[ch][0] < 55 else 0)
        mix.add(soft_bass(midi(root), 1.9 * mix.beat, 0.55), b, pan=0, send=0.05)
        mix.add(soft_bass(midi(root + 7), 1.9 * mix.beat, 0.45), b + 2, pan=0, send=0.05)
        for e in range(8):
            mix.add(hat(0.06 if e % 2 else 0.1, 0.025), b + e * 0.5, pan=0.4, send=0.1)
    a = [("E5", 1.5), ("G5", .5), ("B5", 1), ("G5", 1), ("A5", 1.5), ("G5", .5), ("E5", 2),
         ("F5", 1), ("E5", 1), ("D5", 1), ("A4", 1), ("B4", 1.5), ("D5", .5), ("G5", 2),
         ("E5", 1), ("G5", 1), ("C6", 1.5), ("B5", .5), ("A5", 1), ("E5", 1), ("G5", 2),
         ("A5", 1), ("G5", .5), ("F5", .5), ("E5", 1), ("C5", 1), ("D5", 3), ("r", 1)]
    b = [("G5", 1.5), ("E5", .5), ("C5", 1), ("E5", 1), ("C6", 1.5), ("B5", .5), ("A5", 2),
         ("A5", 1), ("C6", 1), ("D6", 1), ("C6", 1), ("B5", 1.5), ("A5", .5), ("G5", 2),
         ("E5", 1), ("G5", 1), ("B5", 1), ("C6", 1), ("A5", 1.5), ("G5", .5), ("E5", 2),
         ("A5", 1), ("C6", 1), ("B5", 1), ("A5", 1), ("G5", 2), ("F5", 1), ("D5", 1)]
    play_melody(mix, lambda f, d: epiano(f, d, 0.5), a + b, pan=0.2, send=0.4)
    return mix.render(2.0, 0.6)


def music_puzzle():
    mix = Mix(104, 16)
    prog = ["F", "Dm7", "Gm7", "C7"] * 3 + ["Bb", "C7", "F", "C7"]
    walk = {"F": [41, 45, 48, 50], "Dm7": [38, 41, 45, 48], "Gm7": [43, 46, 50, 48], "C7": [36, 40, 43, 46], "Bb": [46, 45, 41, 43]}
    for bar, ch in enumerate(prog):
        b = bar * 4
        for i, k in enumerate(walk[ch]):
            mix.add(pluck_bass(midi(k), 0.9 * mix.beat, 0.6), b + i, pan=0, send=0.05)
        for off in (1, 3):
            mix.add(snap(0.35), b + off, pan=0.3, send=0.2)
        mix.add(kick(0.5), b, send=0.0)
        mix.add(kick(0.35), b + 2.5, send=0.0)
        for e in range(8):
            mix.add(hat(0.08 if e % 2 else 0.12, 0.02), b + e * 0.5, pan=-0.35, send=0.05, swing=0.08)
        for i, k in enumerate(CHORDS[ch][1:]):
            mix.add(marimba(midi(k + 12), 0.4 * mix.beat, 0.13), b + 1.5 + 0.02 * i, pan=-0.3, send=0.2)
            mix.add(marimba(midi(k + 12), 0.4 * mix.beat, 0.11), b + 3.5 + 0.02 * i, pan=-0.3, send=0.2)
    A = [("C5", .5), ("F5", .5), ("A5", .5), ("F5", .5), ("G5", .5), ("A5", .5), ("C6", 1)]
    B = [("D6", .5), ("C6", .5), ("A5", .5), ("F5", .5), ("A5", 1), ("r", 1)]
    C = [("Bb5", .5), ("A5", .5), ("G5", .5), ("F5", .5), ("G5", .5), ("A5", .5), ("Bb5", 1)]
    D = [("C6", 1.5), ("Bb5", .5), ("A5", .5), ("G5", .5), ("E5", 1)]
    D2 = [("C6", .5), ("E5", .5), ("G5", .5), ("Bb5", .5), ("C6", 2)]
    A2 = [("A5", .5), ("C6", .5), ("F6", .5), ("C6", .5), ("A5", .5), ("G5", .5), ("F5", 1)]
    E = [("D6", .5), ("Bb5", .5), ("F5", .5), ("D5", .5), ("F5", 1), ("Bb5", 1)]
    F = [("C6", 1), ("Bb5", .5), ("A5", .5), ("G5", 1), ("E5", 1)]
    G = [("F5", .5), ("A5", .5), ("C6", .5), ("A5", .5), ("F5", 2)]
    H = [("G5", .5), ("A5", .5), ("Bb5", .5), ("B5", .5), ("C6", 1), ("r", 1)]
    melody = A + B + C + D + A2 + B + C + D2 + A + B + C + D + E + F + G + H
    play_melody(mix, lambda f, d: marimba(f, d, 0.55), melody, pan=0.15, send=0.25, swing=0.08)
    return mix.render(1.4, 0.4)


def music_outside():
    mix = Mix(92, 16)
    prog = ["Dm9", "G13", "Cmaj9", "A7sus"] * 3 + ["Dm9", "G13", "Cmaj9", "A7"]
    for bar, ch in enumerate(prog):
        b = bar * 4
        mix.add(pad([midi(k + 12) for k in CHORDS[ch][1:]], 4.2 * mix.beat, 0.12), b, pan=-0.2, send=0.5)
        root = CHORDS[ch][0]
        while root > 45:
            root -= 12
        mix.add(soft_bass(midi(root), 1.4 * mix.beat, 0.6), b, send=0.02)
        mix.add(soft_bass(midi(root), 0.5 * mix.beat, 0.45), b + 1.5, send=0.02)
        mix.add(soft_bass(midi(root + 7), 1.0 * mix.beat, 0.5), b + 2.5, send=0.02)
        mix.add(kick(0.45), b, send=0.0)
        mix.add(rim(0.25), b + 2, pan=0.2, send=0.25)
        for e in range(8):
            mix.add(hat(0.05 if e % 2 else 0.09, 0.03), b + e * 0.5, pan=0.45, send=0.1, swing=0.06)
    m = [("A5", 1.5), ("F5", .5), ("E5", 1), ("D5", 1), ("B5", 2), ("A5", 1), ("G5", 1),
         ("E5", 1.5), ("G5", .5), ("B5", 1), ("D6", 1), ("C#6", 3), ("r", 1),
         ("D6", 1), ("C6", .5), ("A5", .5), ("F5", 2), ("G5", 1), ("A5", .5), ("B5", .5), ("D6", 2),
         ("E6", 1.5), ("D6", .5), ("B5", 1), ("G5", 1), ("A5", 3), ("r", 1)]
    m2 = [("F5", 1), ("A5", 1), ("C6", 1), ("E6", 1), ("D6", 1.5), ("B5", .5), ("G5", 2),
          ("E5", 1), ("G5", 1), ("B5", 1), ("C6", 1), ("A5", 2), ("E5", 1), ("G5", 1),
          ("F5", 1.5), ("E5", .5), ("D5", 1), ("F5", 1), ("G5", 1.5), ("A5", .5), ("B5", 2),
          ("C6", 1), ("B5", 1), ("G5", 1), ("E5", 1), ("C#6", 2), ("A5", 2)]
    play_melody(mix, lambda f, d: vibes(f, d, 0.45), m + m2, pan=0.25, send=0.45)
    return mix.render(2.2, 0.7)


# ---------------------------------------------------------------- 音效

def sweep(f0, f1, dur, shape="sin", curve=3.0):
    n = int(dur * SR)
    k = np.linspace(0, 1, n)
    f = f1 + (f0 - f1) * np.exp(-curve * k)
    ph = 2 * np.pi * np.cumsum(f) / SR
    return np.sin(ph) if shape == "sin" else 2 / np.pi * np.arcsin(np.sin(ph))


def finish(x, tail=0.15, send=0.18, decay=0.25):
    pad_n = int(tail * SR)
    x = np.concatenate([x, np.zeros(pad_n)])
    wet = convolve(x, reverb_ir(0.6, decay, 7))[:len(x)]
    y = x + send * wet
    fade = int(0.01 * SR)
    y[-fade:] *= np.linspace(1, 0, fade)
    return normalize(y, 0.85)


def sfx_tap():
    # 軟軟的「啵」：音高往下掉一點，像按下一顆有彈性的按鈕。
    n = int(0.09 * SR)
    body = sweep(1100, 620, 0.09, curve=6) * env_adsr(n, a=0.002, d=0.03, s=0.4, r=0.05)
    click = fft_filter(rng.standard_normal(n), lo=2500) * env_exp(n, 0.004) * 0.3
    return finish(body + click, 0.08, 0.1)


def sfx_wrong():
    # 兩聲往下的「嗚嗚」，悶悶的，不刺耳。
    def honk(f, dur):
        n = int(dur * SR)
        t = np.arange(n) / SR
        x = sum(np.sin(2 * np.pi * f * h * t) / h for h in (1, 2, 3, 4))
        x *= 1 + 0.1 * np.sin(2 * np.pi * 7 * t)
        return fft_filter(x, hi=1600) * env_adsr(n, a=0.01, d=0.05, s=0.8, r=0.06)
    a = honk(midi(52), 0.13)
    b = honk(midi(47), 0.22)
    gap = np.zeros(int(0.03 * SR))
    return finish(np.concatenate([a, gap, b]) * 0.8, 0.12, 0.12)


def sfx_success():
    # 往上的琶音，鐘聲音色，尾巴有一點閃光。
    out = np.zeros(int(0.9 * SR))
    for i, n_ in enumerate([72, 76, 79, 84]):
        f = midi(n_)
        n = int(0.6 * SR)
        t = np.arange(n) / SR
        bell = np.sin(2 * np.pi * f * t + 1.2 * np.exp(-t / 0.2) * np.sin(2 * np.pi * f * 3.5 * t)) * np.exp(-t / 0.35)
        s = int(i * 0.075 * SR)
        out[s:s + n] += bell * (0.6 + 0.1 * i)
    spark = fft_filter(rng.standard_normal(len(out)), lo=6000) * env_exp(len(out), 0.15) * 0.06
    return finish(out + spark, 0.3, 0.3, 0.35)


def sfx_pop():
    n = int(0.07 * SR)
    x = sweep(380, 1300, 0.07, curve=-2.5) * env_adsr(n, a=0.002, d=0.02, s=0.5, r=0.04)
    return finish(x, 0.06, 0.08)


def sfx_swipe():
    n = int(0.22 * SR)
    noise = rng.standard_normal(n)
    out = np.zeros(n)
    seg = n // 8
    for k in range(8):
        lo = 600 + k * 500
        part = fft_filter(noise[k * seg:(k + 1) * seg], lo=lo, hi=lo * 2.2)
        out[k * seg:(k + 1) * seg] = part
    e = np.sin(np.linspace(0, np.pi, n)) ** 1.5
    return finish(fft_filter(out, hi=8000) * e, 0.08, 0.15)


def sfx_toggle():
    def block(f):
        n = int(0.04 * SR)
        t = np.arange(n) / SR
        return (np.sin(2 * np.pi * f * t) + 0.3 * np.sin(2 * np.pi * f * 2.7 * t)) * np.exp(-t / 0.012)
    gap = np.zeros(int(0.035 * SR))
    return finish(np.concatenate([block(1400), gap, block(1900) * 0.8]), 0.05, 0.08)


def sfx_dialog():
    out = np.zeros(int(0.45 * SR))
    for i, n_ in enumerate([74, 81]):
        f = midi(n_)
        n = int(0.35 * SR)
        t = np.arange(n) / SR
        x = (np.sin(2 * np.pi * f * t) + 0.2 * np.sin(2 * np.pi * f * 2 * t)) * np.exp(-t / 0.18)
        s = int(i * 0.09 * SR)
        out[s:s + n] += x * 0.6
    return finish(out, 0.15, 0.25)


def main():
    for name, fn in [("sfx_tap", sfx_tap), ("sfx_wrong", sfx_wrong), ("sfx_success", sfx_success), ("sfx_pop", sfx_pop),
                     ("sfx_swipe", sfx_swipe), ("sfx_toggle", sfx_toggle), ("sfx_dialog", sfx_dialog)]:
        to_ogg(name, fn(), 5)
    for name, fn in [("music_title", music_title), ("music_puzzle", music_puzzle), ("music_outside", music_outside)]:
        to_ogg(name, fn(), 3)


if __name__ == "__main__":
    main()
