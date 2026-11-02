package com.github.engatec.vdl.model.downloadable;

import java.nio.file.Path;

import com.github.engatec.vdl.model.VideoSection;
import com.github.engatec.vdl.model.postprocessing.Postprocessable;

public interface Downloadable extends Postprocessable {

    String getFormatId();
    void setFormatId(String formatId);

    String getTitle();
    void setTitle(String title);

    String getBaseUrl();
    void setBaseUrl(String baseUrl);

    Path getDownloadPath();
    void setDownloadPath(Path downloadPath);

    Double getSectionStartSeconds();
    void setSectionStartSeconds(Double sectionStartSeconds);

    Double getSectionEndSeconds();
    void setSectionEndSeconds(Double sectionEndSeconds);

    default VideoSection getVideoSection() {
        return VideoSection.of(getSectionStartSeconds(), getSectionEndSeconds());
    }

    default boolean hasVideoSection() {
        return getSectionStartSeconds() != null || getSectionEndSeconds() != null;
    }
}
