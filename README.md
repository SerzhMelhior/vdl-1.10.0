# VDL - Video Downloader

VDL is a desktop GUI for [yt-dlp](https://github.com/yt-dlp/yt-dlp). This fork is based on [engatec/vdl](https://github.com/engatec/vdl) and adds downloading video fragments without re-encoding. A portable Windows build is published in [Releases](https://github.com/SerzhMelhior/vdl-1.10.0/releases/latest).

Download videos from YouTube and other sites supported by yt-dlp.
![Search](https://user-images.githubusercontent.com/6572208/149672268-6558d127-264a-4a14-a0ea-3b080ce78eba.png)

Choose any format reported by yt-dlp.
![Formats](https://user-images.githubusercontent.com/6572208/149672269-5b2a8935-1d91-4d1d-ae62-82849ad8d146.png)

Download multiple files at once.
![Queue](https://user-images.githubusercontent.com/6572208/149672270-c622b56f-efb4-4e93-aa48-e4d3cc575844.png)

Subscribe to channels and playlists.
![Subscriptions](https://user-images.githubusercontent.com/6572208/149672271-1fc12a0c-7aff-452b-b0f2-85ea5dd84d0e.png)

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

The 1.10.0 release provides a portable 64-bit Windows build.

1. Download `vdl-video-downloader-1.10.0-windows-portable.zip` from [Releases](https://github.com/SerzhMelhior/vdl-1.10.0/releases/latest).
2. Extract the archive, keeping the folder structure intact.
3. Run `vdl.exe`.

The package includes its own Java runtime, FFmpeg, and yt-dlp, so no separate installation is required. This release does not include an MSI installer or builds for other operating systems.

`upd_windows.jar` is used by the application's built-in updater. For a manual installation, use the portable ZIP.

## Building

The Windows build uses JDK 17 and Maven.

1. Install the local `fx-controls` dependency:
   ```
   mvn install:install-file -Dfile=./lib/fx-controls-1.0.jar -DgroupId=com.github.engatec -DartifactId=fx-controls -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
   ```
2. Place a Windows `ffmpeg.exe` in `packaging/bin/windows/`. This file is not tracked in Git.
3. Build the Windows assembly:
   ```
   mvn clean package -Dassembly=windows
   ```

The GitHub Actions workflow downloads FFmpeg 9.0.2 essentials from gyan.dev and verifies its SHA-256 before building.

## FAQ

### The downloaded fragment does not start exactly at the requested time

Fragment downloads copy the original streams without re-encoding. Cut points therefore snap to keyframes and may differ slightly from the time you entered.

### Can I download only the audio of a fragment?

No. Fragment selection applies to video downloads. The separate **Download audio** action downloads the full audio track.

### Where are the downloads saved?

The default folder is configured in **Preferences → General → Save files to**. You can also enable **Always ask you where to save files**.
