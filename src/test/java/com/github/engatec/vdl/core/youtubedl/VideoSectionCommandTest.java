package com.github.engatec.vdl.core.youtubedl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.github.engatec.vdl.TestHelper;
import com.github.engatec.vdl.core.ApplicationContext;
import com.github.engatec.vdl.core.youtubedl.processbuilder.DownloadProcessBuilder;
import com.github.engatec.vdl.core.youtubedl.processbuilder.DownloadWithConfigFileProcessBuilder;
import com.github.engatec.vdl.exception.VideoSectionUnsupportedException;
import com.github.engatec.vdl.model.downloadable.BaseDownloadable;
import com.github.engatec.vdl.model.downloadable.Downloadable;
import com.github.engatec.vdl.preference.property.ConfigProperty;
import com.github.engatec.vdl.preference.property.engine.AuthPasswordConfigProperty;
import com.github.engatec.vdl.preference.property.engine.AuthUsernameConfigProperty;
import com.github.engatec.vdl.preference.property.engine.ConfigFilePathConfigProperty;
import com.github.engatec.vdl.preference.property.engine.CookiesFileLocationConfigProperty;
import com.github.engatec.vdl.preference.property.engine.ForceIpV4ConfigProperty;
import com.github.engatec.vdl.preference.property.engine.ForceIpV6ConfigProperty;
import com.github.engatec.vdl.preference.property.engine.MarkWatchedConfigProperty;
import com.github.engatec.vdl.preference.property.engine.NetrcConfigProperty;
import com.github.engatec.vdl.preference.property.engine.NoContinueConfigProperty;
import com.github.engatec.vdl.preference.property.engine.NoMTimeConfigProperty;
import com.github.engatec.vdl.preference.property.engine.NoPartConfigProperty;
import com.github.engatec.vdl.preference.property.engine.OutputTemplateConfigProperty;
import com.github.engatec.vdl.preference.property.engine.ProxyEnabledConfigProperty;
import com.github.engatec.vdl.preference.property.engine.ProxyUrlConfigProperty;
import com.github.engatec.vdl.preference.property.engine.RateLimitConfigProperty;
import com.github.engatec.vdl.preference.property.engine.ReadCookiesConfigProperty;
import com.github.engatec.vdl.preference.property.engine.SocketTimeoutConfigProperty;
import com.github.engatec.vdl.preference.property.engine.SourceAddressConfigProperty;
import com.github.engatec.vdl.preference.property.engine.TwoFactorCodeConfigProperty;
import com.github.engatec.vdl.preference.property.engine.VideoPasswordConfigProperty;
import com.github.engatec.vdl.preference.property.engine.WriteSubtitlesConfigProperty;
import com.github.engatec.vdl.preference.property.engine.PreferredSubtitlesConfigProperty;
import com.github.engatec.vdl.preference.property.engine.EmbedSubtitlesConfigProperty;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VideoSectionCommandTest {

    private static final String FORMAT_ID = "bestvideo[height<=1080]+bestaudio";
    private static final String URL = "https://www.youtube.com/watch?v=test";
    private static final String OUTPUT_TEMPLATE = "%(title).150s %(id).40s %(resolution).10s.%(ext)s";

    @TempDir
    Path tempDir;

    @BeforeAll
    static void setUp() {
        TestHelper.initTestApplicationContext();
    }

    @BeforeEach
    void setUpPreferences() {
        mockPreference(OutputTemplateConfigProperty.class, OUTPUT_TEMPLATE);
        mockPreference(MarkWatchedConfigProperty.class, false);
        mockPreference(NoContinueConfigProperty.class, false);
        mockPreference(NoPartConfigProperty.class, false);
        mockPreference(NoMTimeConfigProperty.class, false);
        mockPreference(ReadCookiesConfigProperty.class, false);
        mockPreference(CookiesFileLocationConfigProperty.class, StringUtils.EMPTY);
        mockPreference(WriteSubtitlesConfigProperty.class, false);
        mockPreference(PreferredSubtitlesConfigProperty.class, StringUtils.EMPTY);
        mockPreference(EmbedSubtitlesConfigProperty.class, false);
        mockPreference(RateLimitConfigProperty.class, "0");
        mockPreference(ProxyEnabledConfigProperty.class, false);
        mockPreference(ProxyUrlConfigProperty.class, StringUtils.EMPTY);
        mockPreference(SocketTimeoutConfigProperty.class, StringUtils.EMPTY);
        mockPreference(SourceAddressConfigProperty.class, StringUtils.EMPTY);
        mockPreference(ForceIpV4ConfigProperty.class, false);
        mockPreference(ForceIpV6ConfigProperty.class, false);
        mockPreference(AuthUsernameConfigProperty.class, StringUtils.EMPTY);
        mockPreference(AuthPasswordConfigProperty.class, StringUtils.EMPTY);
        mockPreference(TwoFactorCodeConfigProperty.class, StringUtils.EMPTY);
        mockPreference(NetrcConfigProperty.class, false);
        mockPreference(VideoPasswordConfigProperty.class, StringUtils.EMPTY);
    }

    @Test
    void downloadProcessBuilder_shouldBuildSectionCommand() {
        Downloadable downloadable = createDownloadable();
        downloadable.setSectionStartSeconds(300D);
        downloadable.setSectionEndSeconds(600D);

        List<String> command = new DownloadProcessBuilder(downloadable).buildCommand();

        assertThat(command).containsSubsequence("--download-sections", "*00:05:00.000-00:10:00.000");
        assertThat(command).contains("--no-force-keyframes-at-cuts");
        assertThat(command).doesNotContain("--force-keyframes-at-cuts", "--recode-video", "--merge-output-format");
        assertThat(command).containsSubsequence("-f", FORMAT_ID);
        assertThat(command.subList(command.size() - 2, command.size())).containsExactly("--", URL);

        String outputPath = command.get(command.indexOf("-o") + 1);
        assertThat(outputPath)
                .endsWith(" %(resolution).10s [00-05-00--00-10-00].%(ext)s");
    }

    @Test
    void downloadProcessBuilder_shouldKeepOutputTemplateForFullDownload() {
        List<String> command = new DownloadProcessBuilder(createDownloadable()).buildCommand();

        assertThat(command).doesNotContain("--download-sections", "--no-force-keyframes-at-cuts");
        String outputPath = command.get(command.indexOf("-o") + 1);
        assertThat(outputPath)
                .doesNotContain("[00-05-00--00-10-00]")
                .endsWith(".%(ext)s");
    }

    @Test
    void configFileProcessBuilder_shouldAddSectionOptionsAfterConfigLocation() throws Exception {
        Path config = writeConfig("--no-mtime\n-f other\n-o other-path\n");
        mockPreference(ConfigFilePathConfigProperty.class, config.toString());

        Downloadable downloadable = createDownloadable();
        downloadable.setSectionStartSeconds(300D);
        downloadable.setSectionEndSeconds(600D);

        List<String> command = new DownloadWithConfigFileProcessBuilder(downloadable).buildCommand();

        assertThat(command).containsSubsequence("--config-location", config.toString());
        assertThat(command.indexOf("--download-sections")).isGreaterThan(command.indexOf("--config-location"));
        assertThat(command).containsSubsequence("--download-sections", "*00:05:00.000-00:10:00.000");
        assertThat(command).contains("--no-force-keyframes-at-cuts");
        assertThat(command).containsSubsequence("-f", FORMAT_ID);
        assertThat(command).doesNotContain("other-path");
        assertThat(command.subList(command.size() - 2, command.size())).containsExactly("--", URL);

        String outputPath = command.get(command.indexOf("-o") + 1);
        assertThat(outputPath).endsWith(" %(resolution).10s [00-05-00--00-10-00].%(ext)s");
    }

    @Test
    void configFileProcessBuilder_shouldKeepConfigFormatAndOutputForFullDownload() throws Exception {
        Path config = writeConfig("--no-mtime\n-f other\n-o other-path\n");
        mockPreference(ConfigFilePathConfigProperty.class, config.toString());

        List<String> command = new DownloadWithConfigFileProcessBuilder(createDownloadable()).buildCommand();

        assertThat(command).doesNotContain("-f", "-o", "--download-sections", "--no-force-keyframes-at-cuts");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "--download-sections \"*0-inf\"",
            "--force-keyframes-at-cuts",
            "--recode-video mp4",
            "--merge-output-format mp4"
    })
    void configFileProcessBuilder_shouldRejectSectionConflicts(String configLine) throws Exception {
        Path config = writeConfig(configLine + "\n");
        mockPreference(ConfigFilePathConfigProperty.class, config.toString());

        Downloadable downloadable = createDownloadable();
        downloadable.setSectionStartSeconds(10D);
        downloadable.setSectionEndSeconds(20D);

        assertThatThrownBy(() -> new DownloadWithConfigFileProcessBuilder(downloadable).buildCommand())
                .isInstanceOf(VideoSectionUnsupportedException.class);
    }

    private Downloadable createDownloadable() {
        BaseDownloadable downloadable = new BaseDownloadable();
        downloadable.setFormatId(FORMAT_ID);
        downloadable.setTitle("Title");
        downloadable.setBaseUrl(URL);
        downloadable.setDownloadPath(Path.of("downloads"));
        return downloadable;
    }

    private Path writeConfig(String content) throws Exception {
        Path config = tempDir.resolve("yt-dlp.conf");
        Files.writeString(config, content);
        return config;
    }

    private static <V, T extends ConfigProperty<?, V>> void mockPreference(Class<T> configPropertyClass, V value) {
        T mock = Mockito.mock(configPropertyClass);
        Mockito.when(mock.getValue()).thenReturn(value);
        Mockito.when(ApplicationContext.getInstance().getConfigRegistry().get(configPropertyClass)).thenReturn(mock);
    }
}
