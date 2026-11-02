# VDL - Video Downloader
A cross-platform GUI for [yt-dlp](https://github.com/yt-dlp/yt-dlp).

> This is a fork of [engatec/vdl](https://github.com/engatec/vdl) with support for downloading video fragments without re-encoding.

Download videos from youtube.com and many more sites.
![pic1](https://user-images.githubusercontent.com/6572208/149672268-6558d127-264a-4a14-a0ea-3b080ce78eba.png)

Choose any available format up to 4K.
![pic2](https://user-images.githubusercontent.com/6572208/149672269-5b2a8935-1d91-4d1d-ae62-82849ad8d146.png)

Download multiple files simultaneously.
![pic3](https://user-images.githubusercontent.com/6572208/149672270-c622b56f-efb4-4e93-aa48-e4d3cc575844.png)

Create subscriptions and don't miss anything from your favorite channels.
![pic4](https://user-images.githubusercontent.com/6572208/149672271-1fc12a0c-7aff-452b-b0f2-85ea5dd84d0e.png)

## Installation
[Download](https://github.com/SerzhMelhior/vdl-1.10.0/releases) installer or portable version for your operating system and run it.  

## Building
FFmpeg is not stored in this repository. For a local Windows build, place a Windows build of `ffmpeg.exe` into `packaging/bin/windows/`. The GitHub Actions workflow downloads and verifies it automatically.

## FAQ
### How to Fix “App Is Damaged and Can’t Be Opened” on Mac?
Run `sudo xattr -cr "/Applications/vdl.app"` command which should do the trick.
