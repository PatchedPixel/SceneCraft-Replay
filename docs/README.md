# SceneCraft Documentation

A short guide to what SceneCraft is, how its workflow fits together, and where to find things. For installation, supported versions and the roadmap, see the [main README](../README.md).

## Contents

- [What is SceneCraft?](#what-is-scenecraft)
- [Requirements](#requirements)
- [How it works](#how-it-works)
- [Using SceneCraft](#using-scenecraft)
- [Default controls](#default-controls)
- [Rendering](#rendering)
- [Project layout](#project-layout)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

## What is SceneCraft?

SceneCraft records your Minecraft sessions, lets you replay them from any angle, and renders them as video. It started as a port of [ReplayMod](https://github.com/ReplayMod/ReplayMod) to modern Minecraft and keeps its familiar record → replay → edit → render workflow.

## Requirements

- Minecraft and a mod loader that matches the jar you downloaded.
- The Java version your Minecraft version requires.
- **FFmpeg** for video output. SceneCraft uses the default `ffmpeg` executable unless you point to your own in the render settings. PNG and OpenEXR sequences do not need FFmpeg.

## How it works

1. **Record.** While you play, SceneCraft saves the packets the game receives into a `.mcpr` replay file. This happens automatically in singleplayer and on servers.
2. **Replay.** A replay is played back inside the game. You fly a free camera through the recorded world or spectate a recorded player, and you control the time (play, pause, speed, jump).
3. **Edit.** On the timeline you place keyframes: position keyframes for the camera path, time keyframes to speed up, slow down or reverse playback, and markers to bookmark moments.
4. **Render.** The camera path is rendered frame by frame, independent of your real frame rate, and encoded to a video or an image sequence.

## Using SceneCraft

| Screen | Purpose |
|---|---|
| Replay Viewer | Pick a recording from the SceneCraft button on the title screen. |
| Editor | Move the camera, add keyframes, edit the timeline. |
| Recording Pause Screen | Shown when you pause while recording. |
| Settings | Recording, replay and render options. |

Screenshots of these screens are in [`docs/assets`](assets).

**Timeline basics**

- Left-click empty space on the lower timeline to move the cursor.
- Click a keyframe to select it, double-click to edit it, and drag to move it.
- Right-click a keyframe to snap the camera to it.

**Where files go.** Recordings are `.mcpr` files in the replay folder. Replay backups (`raw`) and replay caches (`cache`) are kept in separate subfolders, so do not confuse the cache with your recordings.

## Default controls

All of these can be rebound under **Options → Controls** (SceneCraft category). Keys marked "unbound" have no default key.

| Action | Default key |
|---|---|
| Play / Pause replay | `P` |
| Add event marker | `M` |
| Position / Spectator keyframe | `I` |
| Time keyframe | `O` |
| Position keyframe only | unbound |
| Position/Spectator + Time keyframe | unbound |
| Toggle path preview | `H` |
| Open keyframe presets | `X` |
| Clear keyframes | `C` |
| Roll clockwise / counterclockwise | `L` / `J` |
| Reset camera tilt | `K` |
| Capture thumbnail | `N` |
| Toggle lighting (full brightness) | `Z` |
| Quick Mode | `Q` |
| Player overview | `B` |
| SceneCraft settings | unbound |

## Rendering

Open the render settings from the editor, pick a method, resolution, frame rate and encoding preset, then start the render. The video is saved when it finishes.

**Rendering methods**

| Method | Description |
|---|---|
| Default | Renders at the resolution you choose. The fastest option. |
| Stereoscopic | Side-by-side 3D video. Each eye is half the video width. |
| Cubic | 360° panorama using a cube projection. |
| Equirectangular | 360° panorama in the format used by YouTube 360 and most 360 players. |
| ODS | Stereoscopic 360° (VR video). Needs an Iris shader pack and the OpenGL backend. |
| Blender export | Experimental. Exports visible geometry as a `.blend` file. Uses a lot of RAM and has known issues. |

**Encoding presets:** MP4 (default, potato, custom bitrate), WebM (custom bitrate), MKV (lossless), PNG sequence and OpenEXR sequence.

Rendering works on both the OpenGL and Vulkan backends of Minecraft. When a preset uses YUV 4:2:0, the width and height must be even numbers. For cubic rendering the height must also be divisible by 6.

**Tips**

- Close other heavy programs. Rendering ignores your normal frame rate, so a slow render is not a sign that something is broken.

## Project layout

For people reading or contributing to the code. All code is under `src/main/java/net/patchedpixel/scenecraft`.

| Package | Responsibility                                                                                   |
|---|--------------------------------------------------------------------------------------------------|
| `core` | The core system of mod (will be remove later).                                                   |
| `record` | Recording the session into a replay file.                                                        |
| `replay` | Replay playback, the free camera, and the replay senders (full and quick).                       |
| `editor`, `pathing`, `simplepathing` | The timeline, keyframes and camera path (editor GUI, interpolation, path preview).               |
| `render` | Video, PNG and EXR output, the capturers that read frames from the GPU, and the render pipeline. |
| `scenestudio` | Replay data library: packet data, filters, IO.                                                   |
| `gui` | The GUI toolkit used by the editor screens.                                                      |
| `extras` | Optional extras such as Quick Mode, player overview, full brightness and advanced screenshots.   |
| `compat` | Compatibility with other mods.                                                                   |
| `mixin` | Mixins (configured in `scenecraft.mixins.json`).                                                 |
| `platform` | Platform (mod loader) entry and high level mod loader abstraction.                               |

## Troubleshooting

| Problem | What to check |
|---|---|
| Rendering fails to start | Make sure FFmpeg is installed, or set its path in the render settings. |
| Video size error | With YUV 4:2:0 presets use even width and height (cubic: height divisible by 6). |
| ODS render option unavailable | ODS needs Iris and the OpenGL backend. |
| Render or replay behaves oddly with Sodium | Use a SceneCraft-compatible Sodium build or remove Sodium before rendering. |
| Something else broke | Open an issue and attach your `latest.log`, plus the crash report if there was one. |

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). In short: one goal per pull request, keep client performance in mind, and describe what you changed and how you tested it.

## Credits and license

Built on [ReplayMod](https://github.com/ReplayMod/ReplayMod) by johni0702, CrushedPixel and contributors. SceneCraft is licensed under the GNU General Public License v3.0, see [LICENSE.md](../LICENSE.md).
