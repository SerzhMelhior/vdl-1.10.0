package com.github.engatec.vdl.model;

import java.util.Locale;

/**
 * A time range of a video. Either boundary can be null:
 * <ul>
 *     <li>both null - the whole video</li>
 *     <li>start is null - from the beginning of the video</li>
 *     <li>end is null - until the end of the video</li>
 * </ul>
 */
public record VideoSection(Double startSeconds, Double endSeconds) {

    public static final VideoSection NONE = new VideoSection(null, null);

    public VideoSection {
        validateBoundary(startSeconds);
        validateBoundary(endSeconds);
        if (startSeconds != null && endSeconds != null && endSeconds <= startSeconds) {
            throw new IllegalArgumentException("Section end must be greater than section start");
        }
    }

    private static void validateBoundary(Double seconds) {
        if (seconds != null && (seconds < 0 || seconds.isNaN() || seconds.isInfinite())) {
            throw new IllegalArgumentException("Section boundary must be a finite non-negative number");
        }
    }

    public static VideoSection of(Double startSeconds, Double endSeconds) {
        if (startSeconds == null && endSeconds == null) {
            return NONE;
        }
        return new VideoSection(startSeconds, endSeconds);
    }

    public boolean isDefined() {
        return startSeconds != null || endSeconds != null;
    }

    public double effectiveStartSeconds() {
        return startSeconds == null ? 0D : startSeconds;
    }

    /**
     * @return value for yt-dlp's --download-sections option, e.g. {@code *00:05:00.000-00:10:00.000}
     */
    public String toYtDlpRange() {
        if (!isDefined()) {
            throw new IllegalStateException("Section is not defined");
        }
        return "*" + formatTime(effectiveStartSeconds()) + "-" + (endSeconds == null ? "inf" : formatTime(endSeconds));
    }

    /**
     * @return readable suffix for a file name, e.g. {@code [00-05-00--00-10-00]}
     */
    public String toFilenameSuffix() {
        if (!isDefined()) {
            throw new IllegalStateException("Section is not defined");
        }
        return "[" + formatTimeForFileName(effectiveStartSeconds()) + "--"
                + (endSeconds == null ? "end" : formatTimeForFileName(endSeconds)) + "]";
    }

    private static String formatTime(double seconds) {
        long totalMillis = Math.round(seconds * 1000D);
        long hours = totalMillis / 3_600_000;
        long minutes = totalMillis % 3_600_000 / 60_000;
        long secs = totalMillis % 60_000 / 1_000;
        long millis = totalMillis % 1_000;
        return String.format(Locale.ROOT, "%02d:%02d:%02d.%03d", hours, minutes, secs, millis);
    }

    private static String formatTimeForFileName(double seconds) {
        long totalMillis = Math.round(seconds * 1000D);
        long hours = totalMillis / 3_600_000;
        long minutes = totalMillis % 3_600_000 / 60_000;
        long secs = totalMillis % 60_000 / 1_000;
        long millis = totalMillis % 1_000;
        String formatted = String.format(Locale.ROOT, "%02d-%02d-%02d", hours, minutes, secs);
        return millis == 0 ? formatted : formatted + String.format(Locale.ROOT, ".%03d", millis);
    }
}
