# VDL - Video Downloader

VDL is a desktop GUI for [yt-dlp](https://github.com/yt-dlp/yt-dlp). This fork is based on [engatec/vdl](https://github.com/engatec/vdl) and adds downloading video fragments without re-encoding. A portable Windows build is published in [Releases](https://github.com/SerzhMelhior/vdl-1.10.0/releases/latest).

Download videos from YouTube and other sites supported by yt-dlp.

Choose any format reported by yt-dlp.

Download multiple files at once.

Subscribe to channels and playlists.

## What's new in 1.10.1

### Windows 7 compatibility

- The portable build now bundles the [nicolaasjan/yt-dlp](https://github.com/nicolaasjan/yt-dlp) Windows 7-compatible yt-dlp build instead of the official build, which now requires Windows 10 or newer.
- Bundled yt-dlp: `2026.10.29.091737` (Windows 7-compatible build with embedded Python; no separate Python installation is required).
- The yt-dlp update check and self-update use the same fork, so accepting an update cannot replace the Windows 7-compatible binary with an incompatible official build.
- The Windows build workflow downloads the pinned Win7-compatible yt-dlp build and verifies its SHA-256.

## What's new in 1.10.0

### Fragment downloads

- Added a **Download fragment** checkbox to video search results.
- Set **Start** and/or **End** using `SS`, `MM:SS`, or `HH:MM:SS`. Fractional seconds may use `.` or `,`. Leave **Start** empty to begin at the start of the video and **End** empty to download to the end. At least one boundary is required.
- The range is validated before a download starts, including against the video duration when it is known. Invalid values are shown inline with localized messages.
- Fragments are requested with yt-dlp's `--download-sections` and `--no-force-keyframes-at-cuts`. The selected format is kept and the streams are copied without re-encoding. Cut points therefore follow keyframes and may differ slightly from the requested time.
- The range is saved with the queue item, so retries and restarts keep it. Fragment file names get a suffix such as `[00-01-30--00-02-00]`, or `[00-01-30--end]` when **End** is empty.
- Fragment progress is read from FFmpeg output, including the separate video and audio stages. Current size and speed are shown while downloading.
- Fragment failures open a bounded, scrollable error dialog. FFmpeg progress lines are kept out of the error text.

### Other changes

- The Windows build workflow downloads FFmpeg 9.0.2 essentials from gyan.dev and verifies its SHA-256 instead of keeping the Windows `ffmpeg.exe` in Git.
- `yt-dlp.exe` was updated.
- The built-in update check now points to this fork's GitHub releases.
- The application version is now 1.10.0.

## Features

- Download video, audio, and subtitles from sites supported by yt-dlp.
- Select the exact video/audio format, including separate streams that yt-dlp merges.
- Download video fragments without re-encoding.
- Queue multiple downloads and run them concurrently.
- Browse download history and open completed files.
- Subscribe to channels and playlists and update them later.
- Configure proxy, rate limit, cookies, authentication, output template, and audio extraction.

## Installation (Windows)

The 1.10.1 release provides a portable 64-bit Windows build.

1. Download `vdl-video-downloader-1.10.1-windows-portable.zip` from [Releases](https://github.com/SerzhMelhior/vdl-1.10.0/releases/latest).
2. Extract the archive, keeping the folder structure intact.
3. Run `vdl.exe`.

The package includes its own Java runtime, FFmpeg, and yt-dlp, so no separate installation is required. Python is not required: the bundled yt-dlp is the Windows 7-compatible build and embeds its own Python runtime. This release does not include an MSI installer or builds for other operating systems.

`upd_windows.jar` is used by the application's built-in updater. For a manual installation, use the portable ZIP.

## Building

The Windows build uses JDK 17 and Maven.

1. Install the local `fx-controls` dependency:
   ```
   mvn install:install-file -Dfile=./lib/fx-controls-1.0.jar -DgroupId=com.github.engatec -DartifactId=fx-controls -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
   ```
2. Place a Windows `ffmpeg.exe` in `packaging/bin/windows/`. This file is not tracked in Git. The Windows 7-compatible `yt-dlp.exe` is tracked in `packaging/bin/windows/`.
3. Build the Windows assembly:
   ```
   mvn clean package -Dassembly=windows
   ```

The GitHub Actions workflow downloads FFmpeg 9.0.2 essentials from gyan.dev and verifies its SHA-256 before building. It also downloads the pinned Windows 7-compatible yt-dlp build from `nicolaasjan/yt-dlp` and verifies its SHA-256.

## FAQ

### Does VDL require Python?

No. The application is written in Java and ships its own Java runtime. The bundled `yt-dlp.exe` embeds Python; the Windows 7-compatible build is used because the official yt-dlp build now requires Windows 10 or newer.

### The downloaded fragment does not start exactly at the requested time

Fragment downloads copy the original streams without re-encoding. Cut points therefore snap to keyframes and may differ slightly from the time you entered.

### Can I download only the audio of a fragment?

No. Fragment selection applies to video downloads. The separate **Download audio** action downloads the full audio track.

### Where are the downloads saved?

The default folder is configured in **Preferences → General → Save files to**. You can also enable **Always ask you where to save files**.
