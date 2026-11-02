package com.github.engatec.vdl.util;

import com.github.engatec.vdl.exception.VideoSectionValidationException;
import com.github.engatec.vdl.exception.VideoSectionValidationException.Reason;
import com.github.engatec.vdl.model.VideoSection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VideoSectionParserTest {

    @ParameterizedTest
    @CsvSource({
            "5, 5",
            "05:00, 300",
            "01:02:03, 3723",
            "00:00:01.5, 1.5",
            "'00:00:01,500', 1.5",
            "1:02.25, 62.25"
    })
    void parseTime_shouldParseSupportedFormats(String input, double expectedSeconds) {
        assertThat(VideoSectionParser.parseTime(input)).isEqualTo(expectedSeconds);
    }

    @Test
    void parse_shouldUseStartOfVideoWhenStartIsEmpty() {
        VideoSection section = VideoSectionParser.parse("", "05:00", 600);

        assertThat(section.startSeconds()).isNull();
        assertThat(section.endSeconds()).isEqualTo(300D);
        assertThat(section.toYtDlpRange()).isEqualTo("*00:00:00.000-00:05:00.000");
    }

    @Test
    void parse_shouldUseEndOfVideoWhenEndIsEmpty() {
        VideoSection section = VideoSectionParser.parse("05:00", "", 600);

        assertThat(section.startSeconds()).isEqualTo(300D);
        assertThat(section.endSeconds()).isNull();
        assertThat(section.toYtDlpRange()).isEqualTo("*00:05:00.000-inf");
    }

    @Test
    void parse_shouldAllowUnknownDuration() {
        VideoSection section = VideoSectionParser.parse("10", "9999", null);

        assertThat(section.startSeconds()).isEqualTo(10D);
        assertThat(section.endSeconds()).isEqualTo(9999D);
    }

    @Test
    void parse_shouldRejectEmptyRange() {
        assertValidationError(() -> VideoSectionParser.parse("", "  ", 100), Reason.EMPTY);
    }

    @Test
    void parse_shouldRejectEndNotAfterStart() {
        assertValidationError(() -> VideoSectionParser.parse("10", "10", 100), Reason.END_NOT_AFTER_START);
        assertValidationError(() -> VideoSectionParser.parse("10", "5", 100), Reason.END_NOT_AFTER_START);
        assertValidationError(() -> VideoSectionParser.parse("", "0", 100), Reason.END_NOT_AFTER_START);
    }

    @Test
    void parse_shouldRejectEndAfterKnownDuration() {
        assertValidationError(() -> VideoSectionParser.parse("1", "101", 100), Reason.END_AFTER_DURATION);
    }

    @Test
    void parse_shouldAcceptEndWithinDurationTolerance() {
        VideoSection section = VideoSectionParser.parse("1", "100.5", 100);

        assertThat(section.endSeconds()).isEqualTo(100.5D);
    }

    @Test
    void parse_shouldRejectStartAtOrAfterKnownDuration() {
        assertValidationError(() -> VideoSectionParser.parse("100", "", 100), Reason.START_NOT_BEFORE_DURATION);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1:60", "1:2:60", "1:2:3:4", ":", "1:2,3,4"})
    void parseTime_shouldRejectInvalidFormats(String input) {
        assertValidationError(() -> VideoSectionParser.parseTime(input), Reason.INVALID_FORMAT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "-1:00", "1:-2"})
    void parseTime_shouldRejectNegativeValues(String input) {
        assertValidationError(() -> VideoSectionParser.parseTime(input), Reason.NEGATIVE);
    }

    private void assertValidationError(Runnable action, Reason expectedReason) {
        assertThatThrownBy(action::run)
                .isInstanceOf(VideoSectionValidationException.class)
                .satisfies(it -> assertThat(((VideoSectionValidationException) it).getReason()).isEqualTo(expectedReason));
    }
}
