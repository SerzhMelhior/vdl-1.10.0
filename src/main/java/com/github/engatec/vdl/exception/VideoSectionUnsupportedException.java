package com.github.engatec.vdl.exception;

/**
 * Thrown when a download cannot be limited to a video section, for example when a yt-dlp
 * configuration file contains options that conflict with section downloading.
 */
public class VideoSectionUnsupportedException extends RuntimeException {

    public VideoSectionUnsupportedException(String message) {
        super(message);
    }
}
