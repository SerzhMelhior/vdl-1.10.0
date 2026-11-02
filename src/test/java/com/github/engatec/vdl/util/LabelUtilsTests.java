package com.github.engatec.vdl.util;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class LabelUtilsTests {

    @ParameterizedTest
    @MethodSource("fpsProvider")
    void formatFpsTest(Double fps, String expectedString) {
        String actualString = LabelUtils.formatFps(fps);
        assertThat(actualString).isEqualTo(expectedString);
    }

    static Stream<Arguments> fpsProvider() {
        return Stream.of(
                arguments(null, "N/A"),
                arguments(25.0, "25"),
                arguments(25d, "25"),
                arguments(23.98, "23.98")
        );
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0B",
            "512, 512B",
            "1024, 1.00KiB",
            "1536, 1.50KiB",
            "1048576, 1.00MiB",
            "26214400, 25.00MiB",
            "1610612736, 1.50GiB"
    })
    void formatIecSizeTest(long bytes, String expectedString) {
        assertThat(LabelUtils.formatIecSize(bytes)).isEqualTo(expectedString);
    }
}
