package com.github.engatec.vdl.exception;

public class VideoSectionValidationException extends RuntimeException {

    public enum Reason {
        EMPTY,
        INVALID_FORMAT,
        NEGATIVE,
        END_NOT_AFTER_START,
        END_AFTER_DURATION,
        START_NOT_BEFORE_DURATION
    }

    private final Reason reason;

    public VideoSectionValidationException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
