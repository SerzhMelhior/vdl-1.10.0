package com.github.engatec.vdl.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.engatec.vdl.core.AppExecutors;
import com.github.engatec.vdl.core.ApplicationContext;
import com.github.engatec.vdl.core.QueueManager;
import com.github.engatec.vdl.core.youtubedl.processbuilder.DownloadProcessBuilder;
import com.github.engatec.vdl.core.youtubedl.processbuilder.DownloadWithConfigFileProcessBuilder;
import com.github.engatec.vdl.core.youtubedl.processbuilder.YoutubeDlProcessBuilder;
import com.github.engatec.vdl.exception.ProcessException;
import com.github.engatec.vdl.exception.VideoSectionUnsupportedException;
import com.github.engatec.vdl.model.DownloadStatus;
import com.github.engatec.vdl.model.QueueItem;
import com.github.engatec.vdl.preference.property.engine.UseConfigFileConfigProperty;
import com.github.engatec.vdl.service.data.DownloadProgressData;
import com.github.engatec.vdl.ui.helper.Dialogs;
import com.github.engatec.vdl.util.FfmpegProgressParser;
import com.github.engatec.vdl.util.LabelUtils;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QueueItemDownloadService extends Service<DownloadProgressData> {

    private static final Logger LOGGER = LogManager.getLogger(QueueItemDownloadService.class);

    private final ApplicationContext ctx = ApplicationContext.getInstance();
    private final QueueManager queueManager = ctx.getManager(QueueManager.class);

    private static final String SIZE_SEPARATOR = " / ";

    private static final String ERROR_PREFIX = "ERROR:";

    private static final String GROUP_PROGRESS = "progress";
    private static final String GROUP_SIZE = "size";
    private static final String GROUP_THROUGHPUT = "throughput";
    private static final String GROUP_DESTINATION = "destination";
    private static final String GROUP_FORMATS = "formats";
    private static final int MAX_ERROR_MESSAGE_LENGTH = 20_000;

    private static final Pattern DOWNLOAD_PROGRESS_PATTERN = Pattern.compile(
            "\\s*\\[download].*?" +
                    "(?<progress>\\d+\\.?\\d*)%.*?" +
                    "of.*?(?<size>\\d+.+?)\\s+" +
                    "(in.*)?" +
                    "at\\s+(?<throughput>.+?)\\s+" +
                    ".*"
    );

    private static final Pattern DOWNLOAD_DESTINATION_PATTERN = Pattern.compile("\\s*\\[download] Destination:(?<destination>.*)");
    private static final Pattern DOWNLOAD_FORMATS_PATTERN = Pattern.compile("\\s*\\[info].*Downloading\\s+\\d+\\s+format\\(s\\):\\s*(?<" + GROUP_FORMATS + ">\\S+)");
    private static final Pattern MERGE_PATTERN = Pattern.compile("\\s*\\[(Merger|ffmpeg)] Merging formats into \"(?<destination>.*)\"");
    private static final Pattern EXTRACT_AUDIO_PATTERN = Pattern.compile("\\s*\\[(ExtractAudio|ffmpeg)] Destination:(?<destination>.*)");

    private final List<Process> processes = Collections.synchronizedList(new ArrayList<>());
    private final QueueItem queueItem;
    private static final int MAX_PROGRESS = 100;

    private static int countSectionStreams(QueueItem queueItem) {
        if (!queueItem.hasVideoSection()) {
            return 1;
        }

        // A concrete format id usually looks like "625+140". Fallback expressions may contain
        // alternatives separated by '/', so only the first (preferred) alternative is relevant.
        String formatId = StringUtils.substringBefore(StringUtils.defaultString(queueItem.getFormatId()), "/");
        return (int) formatId.chars().filter(it -> it == '+').count() + 1;
    }

    public QueueItemDownloadService(QueueItem queueItem) {
        super();
        this.queueItem = queueItem;
        setExecutor(ctx.appExecutors().get(AppExecutors.Type.QUEUE_EXECUTOR));

        queueItem.progressProperty().bind(progressProperty());
        valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                updateQueueItem(null, newValue.getSize(), newValue.getThroughput());
            }
        });
    }

    @Override
    public void start() {
        DownloadStatus status = queueItem.getStatus();
        if (status != DownloadStatus.READY) {
            String msg = "Queue item must be in state " + DownloadStatus.READY + ". Was in state " + status;
            LOGGER.error(msg);
            throw new IllegalStateException(msg);
        }
        queueItem.setStatus(DownloadStatus.SCHEDULED);

        super.start();
    }

    @Override
    public void reset() {
        queueItem.setStatus(DownloadStatus.READY);

        DoubleProperty progressProperty = queueItem.progressProperty();
        if (!progressProperty.isBound()) {
            progressProperty.bind(progressProperty());
        }

        super.reset();
    }

    @Override
    public void restart() {
        Set<DownloadStatus> restartableStates = Set.of(DownloadStatus.STOPPED, DownloadStatus.FAILED);
        DownloadStatus status = queueItem.getStatus();
        if (!restartableStates.contains(status)) {
            String msg = String.format("Queue item must be in one of the following states: %s. Was in state %s.", restartableStates, status);
            LOGGER.error(msg);
            throw new IllegalStateException(msg);
        }

        super.restart();
    }

    @Override
    protected void running() {
        queueItem.setStatus(DownloadStatus.IN_PROGRESS);
    }

    @Override
    protected void succeeded() {
        updateQueueItem(DownloadStatus.FINISHED, null, StringUtils.EMPTY);
        updateProgress(1);
        queueManager.removeItem(queueItem);
    }

    @Override
    protected void cancelled() {
        updateQueueItem(DownloadStatus.STOPPED, StringUtils.EMPTY, StringUtils.EMPTY);
        updateProgress(0);
        terminateProcessses();
    }

    @Override
    protected void failed() {
        updateQueueItem(DownloadStatus.FAILED, null, StringUtils.EMPTY);
        updateProgress(0);
        Throwable exception = Objects.requireNonNullElseGet(getException(), () -> new ProcessException("Unknown download error"));
        LOGGER.error(exception.getMessage(), exception);
        if (queueItem.hasVideoSection()) {
            showSectionFailureDialog(exception);
        }
        terminateProcessses();
    }

    private void showSectionFailureDialog(Throwable exception) {
        String message = StringUtils.defaultIfBlank(exception.getMessage(), ctx.getLocalizedString("download.section.error"));
        Platform.runLater(() -> {
            if (isSectionUnsupportedFailure(exception)) {
                Dialogs.error("download.section.unsupported");
            } else {
                Dialogs.exception("download.section.error", message);
            }
        });
    }

    private boolean isSectionUnsupportedFailure(Throwable exception) {
        if (exception instanceof VideoSectionUnsupportedException) {
            return true;
        }

        String message = StringUtils.defaultString(exception.getMessage()).toLowerCase(Locale.ROOT);
        return message.contains("--download-sections")
                || message.contains("--force-keyframes-at-cuts")
                || message.contains("--recode-video")
                || message.contains("--merge-output-format")
                || message.contains("not supported")
                || message.contains("unsupported");
    }

    private void addProcess(Process process) {
        processes.add(process);
    }

    private void terminateProcessses() {
        for (Process p : processes) {
            p.descendants().forEach(ProcessHandle::destroy);
            p.destroy();
        }
    }

    /**
     * @return all processes created by this service
     */
    public List<Process> getProcesses() {
        synchronized (processes) {
            return List.copyOf(processes);
        }
    }

    private void updateQueueItem(DownloadStatus status, String size, String throughput) {
        if (status != null) {
            queueItem.setStatus(status);
        }

        if (size != null) {
            queueItem.setSize(size);
        }

        queueItem.setThroughput(throughput);
    }

    private void updateProgress(double value) {
        queueItem.progressProperty().unbind();
        queueItem.setProgress(value);
    }

    @Override
    protected Task<DownloadProgressData> createTask() {
        return new Task<>() {
            /* Small performance tweak. JavaFX's updateValue() will actually perform update only if the new value and the old one aren't referencing the same object.
               Instead of creating a new object each time making a swarm of them per download I will just rollover two objects. */
            private DownloadProgressData progressDataCurrent = new DownloadProgressData();
            private DownloadProgressData progressDataPending = new DownloadProgressData();
            private double sectionProgress;
            private final List<Long> sectionCompletedSizes = new ArrayList<>();
            private long sectionCurrentBytes;
            private int sectionStreamIndex;
            private int sectionTotalStreams = countSectionStreams(queueItem);

            private void updateProgressData(double progress, String throughput, String size) {
                progressDataCurrent.setProgress(progress);
                progressDataCurrent.setThroughput(throughput);
                progressDataCurrent.setSize(size);
            }

            private DownloadProgressData getProgressData() {
                progressDataPending.setProgress(progressDataCurrent.getProgress());
                progressDataPending.setThroughput(progressDataCurrent.getThroughput());
                progressDataPending.setSize(progressDataCurrent.getSize());

                DownloadProgressData temp = progressDataCurrent;
                progressDataCurrent = progressDataPending;
                progressDataPending = temp;

                return progressDataCurrent;
            }

            @Override
            protected DownloadProgressData call() throws Exception {
                Process process = createDownloadProcess();
                addProcess(process);

                List<String> errorLines = Collections.synchronizedList(new ArrayList<>());
                AtomicReference<IOException> errorReadFailure = new AtomicReference<>();
                Thread errorReaderThread = new Thread(
                        () -> readErrorStream(process, errorLines, errorReadFailure),
                        "vdl-yt-dlp-stderr"
                );
                errorReaderThread.setDaemon(true);
                errorReaderThread.start();

                try (var reader = process.inputReader()) {
                    reader.lines().filter(StringUtils::isNotBlank).forEach(it -> {
                        if (Thread.interrupted()) {
                            cancel();
                            return;
                        }

                        Matcher formatsMatcher = DOWNLOAD_FORMATS_PATTERN.matcher(it);
                        if (formatsMatcher.matches()) {
                            updateSectionTotalStreams(formatsMatcher.group(GROUP_FORMATS));
                            return;
                        }

                        Matcher progressMatcher = DOWNLOAD_PROGRESS_PATTERN.matcher(it);
                        if (progressMatcher.matches()) {
                            if (queueItem.hasVideoSection()) {
                                // yt-dlp reports section downloads only once per completed stream.
                                // Its percentage cannot be used for the overall progress: stderr from
                                // ffmpeg carries the real position of every stream and is handled in
                                // updateSectionProgress().
                                return;
                            }

                            double currentProgress = Double.parseDouble(progressMatcher.group(GROUP_PROGRESS));
                            updateProgressData(currentProgress, progressMatcher.group(GROUP_THROUGHPUT), calculateSizeString(progressDataCurrent.getSize(), progressMatcher.group(GROUP_SIZE)));
                            updateProgress(currentProgress, MAX_PROGRESS);
                            updateValue(getProgressData());
                        } else {
                            Matcher downloadDestinationMatcher = DOWNLOAD_DESTINATION_PATTERN.matcher(it);
                            Matcher mergeMatcher = MERGE_PATTERN.matcher(it);
                            Matcher extractAudioMatcher = EXTRACT_AUDIO_PATTERN.matcher(it);
                            if (downloadDestinationMatcher.matches()) {
                                queueManager.addDestination(queueItem, downloadDestinationMatcher.group(GROUP_DESTINATION));
                                if (StringUtils.isNotBlank(progressDataCurrent.getSize())) {
                                    updateProgressData(0, progressDataCurrent.getThroughput(), progressDataCurrent.getSize() + SIZE_SEPARATOR);
                                }
                            } else if (mergeMatcher.matches()) {
                                queueManager.addDestination(queueItem, mergeMatcher.group(GROUP_DESTINATION));
                            } else if (extractAudioMatcher.matches()) {
                                queueManager.addDestination(queueItem, extractAudioMatcher.group(GROUP_DESTINATION));
                            }
                        }
                    });
                }

                errorReaderThread.join();
                IOException readFailure = errorReadFailure.get();
                if (readFailure != null) {
                    process.destroy();
                    throw readFailure;
                }

                // ErrorStream aggregates not only errors, but warnings and ffmpeg progress as well.
                // Progress lines were already consumed while reading, so only real diagnostics remain.
                boolean hasErrors = errorLines.stream()
                        .filter(StringUtils::isNotBlank)
                        .map(StringUtils::strip)
                        .anyMatch(it -> it.startsWith(ERROR_PREFIX));

                int exitCode = process.waitFor();
                if (hasErrors || (exitCode != 0 && queueItem.hasVideoSection())) {
                    throw new ProcessException(buildErrorMessage(errorLines, exitCode));
                }

                List<String> warnings = errorLines.stream()
                        .filter(it -> !FfmpegProgressParser.isProgressLine(it))
                        .toList();
                if (CollectionUtils.isNotEmpty(warnings)) {
                    LOGGER.warn(String.join(System.lineSeparator(), warnings));
                }

                return null;
            }

            private void readErrorStream(Process process, List<String> errorLines, AtomicReference<IOException> failure) {
                try (var reader = new BufferedReader(new InputStreamReader(process.getErrorStream(), ctx.getSystemCharset()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (StringUtils.isBlank(line)) {
                            continue;
                        }

                        if (FfmpegProgressParser.isProgressLine(line)) {
                            // Progress lines may be emitted dozens of times per second. They are used to
                            // update the UI, but must never end up in the error dialog.
                            updateSectionStats(line);
                            updateSectionProgress(line);
                            continue;
                        }

                        errorLines.add(line);
                    }
                } catch (IOException e) {
                    failure.set(e);
                }
            }

            private String buildErrorMessage(List<String> errorLines, int exitCode) {
                List<String> errors = errorLines.stream()
                        .filter(StringUtils::isNotBlank)
                        .map(StringUtils::strip)
                        .filter(it -> it.startsWith(ERROR_PREFIX))
                        .toList();

                String message;
                if (CollectionUtils.isNotEmpty(errors)) {
                    message = String.join(System.lineSeparator(), errors);
                } else {
                    message = String.join(System.lineSeparator(), errorLines);
                    if (StringUtils.isBlank(message)) {
                        message = "yt-dlp exited with code " + exitCode;
                    }
                }

                // Keep the dialog usable even if yt-dlp printed a very long traceback.
                if (message.length() > MAX_ERROR_MESSAGE_LENGTH) {
                    int half = MAX_ERROR_MESSAGE_LENGTH / 2;
                    message = message.substring(0, half)
                            + System.lineSeparator()
                            + "... (truncated) ..."
                            + System.lineSeparator()
                            + message.substring(message.length() - half);
                }
                return message;
            }

            private synchronized void updateSectionTotalStreams(String formats) {
                if (!queueItem.hasVideoSection()) {
                    return;
                }

                int streams = (int) StringUtils.substringBefore(formats, "/").chars().filter(it -> it == '+').count() + 1;
                sectionTotalStreams = Math.max(1, streams);
            }

            /**
             * Fragment downloads are performed by ffmpeg, which reports progress to stderr. yt-dlp itself does not
             * emit useful percentage updates in this case, so the progress bar would stay at 0% for a long time.
             */
            private synchronized void updateSectionProgress(String line) {
                if (!queueItem.hasVideoSection()) {
                    return;
                }

                Double endSeconds = queueItem.getSectionEndSeconds();
                if (endSeconds == null) {
                    return;
                }

                double startSeconds = Objects.requireNonNullElse(queueItem.getSectionStartSeconds(), 0D);
                double sectionDuration = endSeconds - startSeconds;
                if (sectionDuration <= 0) {
                    return;
                }

                Double currentSeconds = FfmpegProgressParser.parseSeconds(line);
                if (currentSeconds == null || currentSeconds < 0) {
                    return;
                }

                double streamProgress = Math.max(0D, Math.min(1D, currentSeconds / sectionDuration));
                int totalStreams = Math.max(sectionTotalStreams, sectionStreamIndex + 1);

                // Section downloads consist of one or more sequential streams (video, audio). The first
                // stream must not occupy the whole progress bar: otherwise it stays at 99% while the
                // remaining stream is being downloaded. Split the bar evenly between the streams and
                // keep 1% for the final merge step.
                double progress = Math.min(0.99D, ((sectionStreamIndex + streamProgress) / totalStreams) * 0.99D);
                if (progress > sectionProgress) {
                    sectionProgress = progress;
                    updateProgress(progress * MAX_PROGRESS, MAX_PROGRESS);
                }
            }

            /**
             * Fragment downloads are performed sequentially for video and audio streams. ffmpeg reports the number of
             * bytes written and the elapsed time in its progress line. Use them to show the current size and speed,
             * accumulating sizes of already completed streams.
             */
            private synchronized void updateSectionStats(String line) {
                if (!queueItem.hasVideoSection()) {
                    return;
                }

                Long sizeBytes = FfmpegProgressParser.parseSizeBytes(line);
                if (sizeBytes == null || sizeBytes <= 0) {
                    return;
                }

                if (sizeBytes < sectionCurrentBytes) {
                    // A new ffmpeg stream has started (video is followed by audio) and its output size
                    // is counted from zero again.
                    if (sectionCurrentBytes > 0) {
                        sectionCompletedSizes.add(sectionCurrentBytes);
                        sectionStreamIndex++;
                        sectionTotalStreams = Math.max(sectionTotalStreams, sectionStreamIndex + 1);
                    }
                    sectionCurrentBytes = sizeBytes;
                } else if (sizeBytes > sectionCurrentBytes) {
                    sectionCurrentBytes = sizeBytes;
                }

                Double elapsedSeconds = FfmpegProgressParser.parseElapsedSeconds(line);
                if (elapsedSeconds == null || elapsedSeconds <= 0) {
                    return;
                }

                long bytesPerSecond = Math.round(sectionCurrentBytes / elapsedSeconds);
                if (bytesPerSecond <= 0) {
                    return;
                }

                StringJoiner sizeJoiner = new StringJoiner(SIZE_SEPARATOR);
                sectionCompletedSizes.forEach(it -> sizeJoiner.add(LabelUtils.formatIecSize(it)));
                sizeJoiner.add(LabelUtils.formatIecSize(sectionCurrentBytes));

                String sizeText = sizeJoiner.toString();
                String throughputText = LabelUtils.formatIecSize(bytesPerSecond) + "/s";
                Platform.runLater(() -> updateQueueItem(null, sizeText, throughputText));
            }

            private Process createDownloadProcess() throws IOException {
                Boolean useConfigFile = ctx.getConfigRegistry().get(UseConfigFileConfigProperty.class).getValue();
                YoutubeDlProcessBuilder pb = useConfigFile ? new DownloadWithConfigFileProcessBuilder(queueItem) : new DownloadProcessBuilder(queueItem);
                List<String> command = pb.buildCommand();
                return pb.buildProcess(command);
            }

            /**
             * Method updates latest not finished item size
             */
            private String calculateSizeString(String currentlyDisplayed, String youtubeDlParsed) {
                String finishedItemsSize = StringUtils.substringBeforeLast(currentlyDisplayed, SIZE_SEPARATOR);
                if (Objects.equals(currentlyDisplayed, finishedItemsSize)) { // There's no yet finished item
                    return youtubeDlParsed;
                } else {
                    return finishedItemsSize + SIZE_SEPARATOR + youtubeDlParsed;
                }
            }
        };
    }
}
