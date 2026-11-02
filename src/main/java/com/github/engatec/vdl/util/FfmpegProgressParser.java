package com.github.engatec.vdl.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts progress values from an ffmpeg progress line, e.g.
 * {@code frame= 1540 fps=205 q=-1.0 size= 4864kB time=00:00:20.30 bitrate=1962.9kbits/s speed=2.7x}.
 */
public final class FfmpegProgressParser {

    private static final Pattern TIME_PATTERN = Pattern.compile(
            "\\btime=\\s*(-?)(?:(\\d+):)?(\\d+):(\\d+(?:[.,]\\d+)?)"
    );

    private static final Pattern ELAPSED_PATTERN = Pattern.compile(
            "\\belapsed=\\s*(\\d+):(\\d+):(\\d+(?:[.,]\\d+)?)"
    );

    private static final Pattern SIZE_PATTERN = Pattern.compile(
            "\\bL?size=\\s*(\\d+(?:[.,]\\d+)?)\\s*([KMGTP]?i?B)?",
            Pattern.CASE_INSENSITIVE
    );

    private FfmpegProgressParser() {
    }

    /**
     * @return progress timestamp in seconds, or {@code null} if the line does not contain one
     */
    public static Double parseSeconds(String line) {
        if (line == null) {
            return null;
        }

        Matcher matcher = TIME_PATTERN.matcher(line);
        if (!matcher.find()) {
            return null;
        }

        double seconds = 0D;
        if (matcher.group(2) != null) {
            seconds += Integer.parseInt(matcher.group(2)) * 3600D;
        }
        seconds += Integer.parseInt(matcher.group(3)) * 60D;
        seconds += Double.parseDouble(matcher.group(4).replace(',', '.'));

        return "-".equals(matcher.group(1)) ? -seconds : seconds;
    }

    public static boolean isProgressLine(String line) {
        if (line == null) {
            return false;
        }

        String stripped = line.stripLeading();
        return stripped.startsWith("frame=") || stripped.startsWith("size=") || stripped.startsWith("Lsize=");
    }

    /**
     * @return size written by ffmpeg in bytes, or {@code null} if the line does not contain a size
     */
    public static Long parseSizeBytes(String line) {
        if (line == null) {
            return null;
        }

        Matcher matcher = SIZE_PATTERN.matcher(line);
        if (!matcher.find()) {
            return null;
        }

        double value = Double.parseDouble(matcher.group(1).replace(',', '.'));
        long multiplier = 1L;
        String unit = matcher.group(2);
        if (unit != null && !unit.isEmpty()) {
            multiplier = switch (Character.toUpperCase(unit.charAt(0))) {
                case 'K' -> 1024L;
                case 'M' -> 1024L * 1024L;
                case 'G' -> 1024L * 1024L * 1024L;
                case 'T' -> 1024L * 1024L * 1024L * 1024L;
                case 'P' -> 1024L * 1024L * 1024L * 1024L * 1024L;
                default -> 1L;
            };
        }

        return Math.round(value * multiplier);
    }

    /**
     * @return ffmpeg's elapsed wall-clock time in seconds, or {@code null} if the line does not contain it
     */
    public static Double parseElapsedSeconds(String line) {
        if (line == null) {
            return null;
        }

        Matcher matcher = ELAPSED_PATTERN.matcher(line);
        if (!matcher.find()) {
            return null;
        }

        double seconds = Integer.parseInt(matcher.group(1)) * 3600D;
        seconds += Integer.parseInt(matcher.group(2)) * 60D;
        seconds += Double.parseDouble(matcher.group(3).replace(',', '.'));
        return seconds;
    }
}
