package com.github.engatec.vdl.util;

import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.engatec.vdl.model.downloadable.Audio;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

public class LabelUtils {

    private static final String N_A = "N/A";
    private static final String QUESTION_MARK = "?";
    private static final String AUDIO_LABEL_DELIMITER = " / ";
    private static final String[] IEC_SIZE_UNITS = {"B", "KiB", "MiB", "GiB", "TiB", "PiB"};

    public static String formatResolution(Integer width, Integer height) {
        String w = width == null ? QUESTION_MARK : width.toString();
        String h = height == null ? QUESTION_MARK : height.toString();
        return w.equals(QUESTION_MARK) && h.equals(QUESTION_MARK) ? N_A : w + "x" + h;
    }

    public static String formatSize(Long filesize) {
        return formatSize(filesize, N_A);
    }

    public static String formatSize(Long filesize, String defaultValue) {
        return filesize == null || filesize == 0 ? defaultValue : FileUtils.byteCountToDisplaySize(filesize);
    }

    /**
     * Formats a byte count using binary units, e.g. {@code 25.19MiB}.
     */
    public static String formatIecSize(long bytes) {
        if (bytes < 1024) {
            return bytes + IEC_SIZE_UNITS[0];
        }

        double value = bytes;
        int unitIndex = 0;
        while (value >= 1024 && unitIndex < IEC_SIZE_UNITS.length - 1) {
            value /= 1024;
            unitIndex++;
        }

        return String.format(Locale.ROOT, "%.2f%s", value, IEC_SIZE_UNITS[unitIndex]);
    }

    public static String formatCodec(String codec) {
        codec = StringUtils.defaultIfBlank(codec, N_A);
        return StringUtils.substringBefore(codec, ".");
    }

    public static String formatFps(Double fps) {
        if (fps == null) {
            return N_A;
        }

        return fps % 1 == 0 ? String.valueOf(fps.intValue()) : fps.toString();
    }

    public static String formatAudio(Audio item) {
        return Stream.of(item.getExtension(), formatSize(item.getFilesize(), StringUtils.EMPTY))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining(AUDIO_LABEL_DELIMITER));
    }
}
