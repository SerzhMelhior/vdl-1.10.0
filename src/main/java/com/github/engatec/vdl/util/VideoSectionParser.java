package com.github.engatec.vdl.util;

import java.math.BigDecimal;
import java.util.regex.Pattern;

import com.github.engatec.vdl.exception.VideoSectionValidationException;
import com.github.engatec.vdl.exception.VideoSectionValidationException.Reason;
import com.github.engatec.vdl.model.VideoSection;
import org.apache.commons.lang3.StringUtils;

public final class VideoSectionParser {

    private static final Pattern UNSIGNED_INTEGER = Pattern.compile("\\d+");
    private static final Pattern SECONDS = Pattern.compile("\\d+(?:[.,]\\d+)?");
    private static final double DURATION_TOLERANCE_SECONDS = 0.5D;

    private VideoSectionParser() {
    }

    /**
     * Parses and validates a video section entered by the user.
     *
     * @param startText       start time or blank for the beginning of the video
     * @param endText         end time or blank for the end of the video
     * @param durationSeconds video duration in seconds, or null/0 if unknown
     */
    public static VideoSection parse(String startText, String endText, Integer durationSeconds) {
        boolean startBlank = StringUtils.isBlank(startText);
        boolean endBlank = StringUtils.isBlank(endText);
        if (startBlank && endBlank) {
            throw new VideoSectionValidationException(Reason.EMPTY);
        }

        Double start = startBlank ? null : parseTime(startText);
        Double end = endBlank ? null : parseTime(endText);

        double effectiveStart = start == null ? 0D : start;
        boolean durationKnown = durationSeconds != null && durationSeconds > 0;

        if (durationKnown && start != null && start >= durationSeconds) {
            throw new VideoSectionValidationException(Reason.START_NOT_BEFORE_DURATION);
        }

        if (end != null && end <= effectiveStart) {
            throw new VideoSectionValidationException(Reason.END_NOT_AFTER_START);
        }

        if (durationKnown && end != null && end > durationSeconds + DURATION_TOLERANCE_SECONDS) {
            throw new VideoSectionValidationException(Reason.END_AFTER_DURATION);
        }

        return VideoSection.of(start, end);
    }

    /**
     * Parses {@code SS}, {@code MM:SS} or {@code HH:MM:SS}. A fractional part of a second is allowed,
     * both a dot and a comma can be used as a decimal separator.
     *
     * @return time in seconds
     */
    public static double parseTime(String text) {
        if (StringUtils.isBlank(text)) {
            throw new VideoSectionValidationException(Reason.INVALID_FORMAT);
        }

        String normalized = text.strip();
        if (normalized.startsWith("-")) {
            throw new VideoSectionValidationException(Reason.NEGATIVE);
        }

        String[] parts = normalized.split(":", -1);
        if (parts.length > 3) {
            throw new VideoSectionValidationException(Reason.INVALID_FORMAT);
        }

        for (int i = 0; i < parts.length - 1; i++) {
            if (!UNSIGNED_INTEGER.matcher(parts[i]).matches()) {
                throw new VideoSectionValidationException(Reason.INVALID_FORMAT);
            }
        }

        String secondsPart = parts[parts.length - 1];
        if (!SECONDS.matcher(secondsPart).matches()) {
            if (secondsPart.startsWith("-")) {
                throw new VideoSectionValidationException(Reason.NEGATIVE);
            }
            throw new VideoSectionValidationException(Reason.INVALID_FORMAT);
        }

        try {
            BigDecimal seconds = new BigDecimal(secondsPart.replace(',', '.'));
            if (seconds.signum() < 0) {
                throw new VideoSectionValidationException(Reason.NEGATIVE);
            }

            BigDecimal totalSeconds;
            if (parts.length == 1) {
                totalSeconds = seconds;
            } else if (parts.length == 2) {
                BigDecimal minutes = new BigDecimal(parts[0]);
                requireSecondsWithinMinute(seconds);
                totalSeconds = minutes.multiply(BigDecimal.valueOf(60)).add(seconds);
            } else {
                BigDecimal hours = new BigDecimal(parts[0]);
                BigDecimal minutes = new BigDecimal(parts[1]);
                requireMinutesWithinHour(minutes);
                requireSecondsWithinMinute(seconds);
                totalSeconds = hours.multiply(BigDecimal.valueOf(3600))
                        .add(minutes.multiply(BigDecimal.valueOf(60)))
                        .add(seconds);
            }

            return totalSeconds.doubleValue();
        } catch (NumberFormatException e) {
            throw new VideoSectionValidationException(Reason.INVALID_FORMAT);
        }
    }

    private static void requireSecondsWithinMinute(BigDecimal seconds) {
        if (seconds.compareTo(BigDecimal.valueOf(60)) >= 0) {
            throw new VideoSectionValidationException(Reason.INVALID_FORMAT);
        }
    }

    private static void requireMinutesWithinHour(BigDecimal minutes) {
        if (minutes.compareTo(BigDecimal.valueOf(60)) >= 0) {
            throw new VideoSectionValidationException(Reason.INVALID_FORMAT);
        }
    }
}
