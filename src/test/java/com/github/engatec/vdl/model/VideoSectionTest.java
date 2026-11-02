package com.github.engatec.vdl.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VideoSectionTest {

    @Test
    void toYtDlpRange_shouldFormatBoundaries() {
        VideoSection section = new VideoSection(300D, 600D);

        assertThat(section.toYtDlpRange()).isEqualTo("*00:05:00.000-00:10:00.000");
        assertThat(section.toFilenameSuffix()).isEqualTo("[00-05-00--00-10-00]");
    }

    @Test
    void toYtDlpRange_shouldUseInfinityForOpenEnd() {
        VideoSection section = VideoSection.of(300D, null);

        assertThat(section.toYtDlpRange()).isEqualTo("*00:05:00.000-inf");
        assertThat(section.toFilenameSuffix()).isEqualTo("[00-05-00--end]");
    }

    @Test
    void toFilenameSuffix_shouldIncludeMilliseconds() {
        VideoSection section = new VideoSection(1.5D, 2.25D);

        assertThat(section.toFilenameSuffix()).isEqualTo("[00-00-01.500--00-00-02.250]");
    }
}
