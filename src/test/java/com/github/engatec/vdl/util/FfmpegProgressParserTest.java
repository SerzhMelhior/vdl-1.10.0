package com.github.engatec.vdl.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FfmpegProgressParserTest {

    @Test
    void shouldParseStandardProgressLine() {
        String line = "frame= 1540 fps=205 q=-1.0 size=    4864kB time=00:00:20.30 bitrate=1962.9kbits/s speed= 2.7x";

        assertThat(FfmpegProgressParser.parseSeconds(line)).isEqualTo(20.30D);
    }

    @Test
    void shouldParseHours() {
        String line = "frame=  100 fps=50 q=-1.0 size= 1024kB time=01:02:03.50 bitrate=1000.0kbits/s speed=1x";

        assertThat(FfmpegProgressParser.parseSeconds(line)).isEqualTo(3723.50D);
    }

    @Test
    void shouldParseSecondsWithoutHours() {
        assertThat(FfmpegProgressParser.parseSeconds("time=02:03.50")).isEqualTo(123.50D);
    }

    @Test
    void shouldPreserveNegativeTime() {
        assertThat(FfmpegProgressParser.parseSeconds("time=-00:00:05.33")).isEqualTo(-5.33D);
    }

    @Test
    void shouldReturnNullForNonProgressLines() {
        assertThat(FfmpegProgressParser.parseSeconds("Duration: 00:51:15.33, start: 0.000000, bitrate: 3220 kb/s")).isNull();
        assertThat(FfmpegProgressParser.parseSeconds("time=N/A")).isNull();
        assertThat(FfmpegProgressParser.parseSeconds(null)).isNull();
    }

    @Test
    void shouldRecognizeProgressLines() {
        assertThat(FfmpegProgressParser.isProgressLine("frame=  100 fps=50 time=00:00:10.00")).isTrue();
        assertThat(FfmpegProgressParser.isProgressLine("  frame=  100 fps=50 time=00:00:10.00")).isTrue();
        assertThat(FfmpegProgressParser.isProgressLine("size=     159KiB time=00:00:10.01")).isTrue();
        assertThat(FfmpegProgressParser.isProgressLine("Lsize=   25799KiB time=00:00:10.00")).isTrue();
        assertThat(FfmpegProgressParser.isProgressLine("Input #0, mov,mp4,m4a,3gp,3g2,mj2")).isFalse();
        assertThat(FfmpegProgressParser.isProgressLine(null)).isFalse();
    }

    @Test
    void shouldParseSizeInDifferentUnits() {
        assertThat(FfmpegProgressParser.parseSizeBytes("frame= 1540 fps=205 size= 4864kB time=00:00:20.30")).isEqualTo(4_980_736L);
        assertThat(FfmpegProgressParser.parseSizeBytes("frame=  369 fps= 24 q=-1.0 Lsize= 25799KiB time=00:00:10.00")).isEqualTo(26_418_176L);
        assertThat(FfmpegProgressParser.parseSizeBytes("size=     159KiB time=00:00:10.01")).isEqualTo(162_816L);
        assertThat(FfmpegProgressParser.parseSizeBytes("size=1.5MiB time=00:00:10.00")).isEqualTo(1_572_864L);
        assertThat(FfmpegProgressParser.parseSizeBytes("size=1024 time=00:00:10.00")).isEqualTo(1024L);
    }

    @Test
    void shouldNotParseUnknownSize() {
        assertThat(FfmpegProgressParser.parseSizeBytes("size=N/A time=N/A")).isNull();
        assertThat(FfmpegProgressParser.parseSizeBytes("frame= 1 fps=1 time=00:00:01.00")).isNull();
        assertThat(FfmpegProgressParser.parseSizeBytes(null)).isNull();
    }

    @Test
    void shouldParseElapsedSeconds() {
        assertThat(FfmpegProgressParser.parseElapsedSeconds("frame= 369 fps=24 size=25799KiB time=00:00:10.00 elapsed=0:00:15.34")).isEqualTo(15.34D);
        assertThat(FfmpegProgressParser.parseElapsedSeconds("elapsed=1:02:03.50")).isEqualTo(3723.50D);
    }

    @Test
    void shouldNotParseMissingElapsedSeconds() {
        assertThat(FfmpegProgressParser.parseElapsedSeconds("size=159KiB time=00:00:10.01")).isNull();
        assertThat(FfmpegProgressParser.parseElapsedSeconds(null)).isNull();
    }
}
