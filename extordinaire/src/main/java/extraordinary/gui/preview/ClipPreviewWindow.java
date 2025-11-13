package extraordinary.gui.preview;

import java.io.File;
import java.text.DecimalFormat;

import extraordinary.gui.App.ClipItem;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ClipPreviewWindow {

    private Stage stage;
    private MediaView mediaView;
    private MediaPlayer player;

    private final Slider seekSlider = new Slider(0, 1, 0);
    private final Slider inSlider   = new Slider(0, 1, 0);
    private final Slider outSlider  = new Slider(0, 1, 1);

    private final Label titleLbl = new Label("");
    private final Label timeLbl  = new Label("00:00 / 00:00");
    private final Label inLbl    = new Label("In: 0.00s");
    private final Label outLbl   = new Label("Out: 0.00s");

    private final Button playBtn  = new Button("Play");
    private final Button pauseBtn = new Button("Pause");
    private final Button stopBtn  = new Button("Stop");
    private final Button applyBtn = new Button("Apply Trim");
    private final Button goInBtn  = new Button("Go to In");
    private final Button goOutBtn = new Button("Go to Out");

    private boolean wasPlayingBeforeDrag = false;
    private ClipItem boundClip;

    private static final DecimalFormat SEC = new DecimalFormat("0.00");

    // --- snappy scrubbing: schedule seeks at ~60fps instead of spamming
    private Duration pendingSeek = null;
    private long lastSeekNs = 0L;
    private final AnimationTimer seekPump = new AnimationTimer() {
        @Override public void handle(long now) {
            if (player == null || pendingSeek == null) return;
            // ~16ms cadence
            if (now - lastSeekNs >= 16_000_000L) {
                try {
                    player.seek(pendingSeek);
                } catch (Exception ignored) {}
                lastSeekNs = now;
                pendingSeek = null; // if user keeps dragging, handlers will set it again
            }
        }
    };

    public void open(ClipItem clip, boolean portrait) {
        boundClip = clip;
        ensureStage(portrait);
        loadClip(clip.getPath(), 0);
        titleLbl.setText(safeName(clip.getPath()));

        // init trims
        inSlider.setValue(Math.max(0, clip.getInSec()));
        outSlider.setValue(clip.getOutSec() > 0 ? clip.getOutSec() : outSlider.getMax());
        updateTrimLabels();

        if (!stage.isShowing()) stage.show();
        stage.toFront();
    }

    private void ensureStage(boolean portrait) {
        if (stage != null) return;

        stage = new Stage();
        stage.setTitle("Extordinaire — Clip Preview");

        mediaView = new MediaView();
        mediaView.setPreserveRatio(true);

        StackPane videoPane = new StackPane(mediaView);
        videoPane.setStyle("-fx-background-color: #0b0b0b;");
        videoPane.setPadding(new Insets(10));
        videoPane.setMinHeight(480);
        videoPane.setPrefHeight(portrait ? 720 : 540);

        HBox transport = new HBox(8, playBtn, pauseBtn, stopBtn, timeLbl);
        transport.setAlignment(Pos.CENTER_LEFT);

        // --- seek behavior (pause on touch, immediate jump, live scrub, resume on release)
        seekSlider.setSnapToTicks(false);
        seekSlider.setBlockIncrement(0.1);

        // immediate jump on click + pause
        seekSlider.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (player != null) {
                wasPlayingBeforeDrag = player.getStatus() == MediaPlayer.Status.PLAYING;
                player.pause();
                double v = mouseToSliderValue(seekSlider, e.getX());
                seekSlider.setValue(v);
                // immediate jump for click-snappiness
                player.seek(Duration.seconds(v));
                lastSeekNs = System.nanoTime();
                updateTimeLabel();
                e.consume();
            }
        });

        // while dragging, schedule seeks for smoothness
        seekSlider.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (player != null) {
                double v = mouseToSliderValue(seekSlider, e.getX());
                seekSlider.setValue(v);
                pendingSeek = Duration.seconds(v);
                updateTimeLabel();
                e.consume();
            }
        });

        // resume only if it was playing before drag
        seekSlider.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (player != null && wasPlayingBeforeDrag) {
                player.play();
            }
        });

        // --- trim sliders keep IN<=OUT
        inSlider.valueProperty().addListener((o, ov, nv) -> {
            if (inSlider.getValue() > outSlider.getValue()) {
                outSlider.setValue(inSlider.getValue());
            }
            updateTrimLabels();
        });
        outSlider.valueProperty().addListener((o, ov, nv) -> {
            if (outSlider.getValue() < inSlider.getValue()) {
                inSlider.setValue(outSlider.getValue());
            }
            updateTrimLabels();
        });

        // transport
        playBtn.setOnAction(e -> { if (player != null) player.play(); });
        pauseBtn.setOnAction(e -> { if (player != null) player.pause(); });
        stopBtn.setOnAction(e -> {
            if (player != null) {
                player.stop();
                player.seek(Duration.seconds(inSlider.getValue()));
                updateTimeLabel();
            }
        });

        // apply trim -> writes back to ClipItem (App’s table is bound to those properties)
        applyBtn.setOnAction(e -> applyTrimBackToClip());

        // jump buttons
        goInBtn.setOnAction(e -> jumpTo(inSlider.getValue()));
        goOutBtn.setOnAction(e -> jumpTo(outSlider.getValue()));

        // layout
        GridPane trimGrid = new GridPane();
        trimGrid.setHgap(10);
        trimGrid.setVgap(6);
        trimGrid.add(new Label("Trim In:"), 0, 0);
        trimGrid.add(inSlider, 1, 0);
        trimGrid.add(inLbl, 2, 0);
        trimGrid.add(goInBtn, 3, 0);
        trimGrid.add(new Label("Trim Out:"), 0, 1);
        trimGrid.add(outSlider, 1, 1);
        trimGrid.add(outLbl, 2, 1);
        trimGrid.add(goOutBtn, 3, 1);

        HBox applyRow = new HBox(10, applyBtn);
        applyRow.setAlignment(Pos.CENTER_LEFT);

        VBox bottom = new VBox(10, titleLbl, transport, seekSlider, trimGrid, applyRow);
        bottom.setPadding(new Insets(10));
        bottom.setStyle("-fx-background-color: #141414; -fx-text-fill: white;");

        BorderPane root = new BorderPane();
        root.setCenter(videoPane);
        root.setBottom(bottom);

        Scene scene = new Scene(root, portrait ? 900 : 1100, portrait ? 1000 : 700);
        scene.getRoot().setStyle("-fx-base: #141414; -fx-control-inner-background: #141414; -fx-text-fill: white;");
        titleLbl.setStyle("-fx-text-fill: white; -fx-font-size: 14;");
        timeLbl.setStyle("-fx-text-fill: #d0d0d0;");
        inLbl.setStyle("-fx-text-fill: #d0d0d0;");
        outLbl.setStyle("-fx-text-fill: #d0d0d0;");
        applyBtn.setDefaultButton(true);

        stage.setScene(scene);

        // keep video inside the center region
        root.widthProperty().addListener((obs, w0, w1) -> mediaView.setFitWidth(w1.doubleValue() - 20));
        root.heightProperty().addListener((obs, h0, h1) -> mediaView.setFitHeight(h1.doubleValue() - bottom.getHeight() - 30));

        // start the seek pump for snappy scrubbing
        seekPump.start();
    }

    private void loadClip(String absolutePath, int attempt) {
        if (player != null) {
            try { player.dispose(); } catch (Exception ignored) {}
            player = null;
        }

        File f = new File(absolutePath);
        if (!f.exists()) {
            showError("File not found:\n" + absolutePath);
            return;
        }

        Media media;
        try {
            media = new Media(f.toURI().toString());
        } catch (MediaException ex) {
            if (attempt == 0) {
                Platform.runLater(() -> loadClip(absolutePath, 1));
                return;
            }
            showError("Could not load media:\n" + ex.getMessage());
            return;
        }

        player = new MediaPlayer(media);
        mediaView.setMediaPlayer(player);

        player.setOnError(() -> {
            if (attempt == 0) {
                Platform.runLater(() -> loadClip(absolutePath, 1));
            } else {
                showError("Media error:\n" + player.getError());
            }
        });

        player.setOnReady(() -> {
            double total = player.getTotalDuration().toSeconds();

            seekSlider.setMin(0); seekSlider.setMax(total);
            inSlider.setMin(0);   inSlider.setMax(total);
            outSlider.setMin(0);  outSlider.setMax(total);

            if (boundClip != null && boundClip.getOutSec() <= 0) {
                outSlider.setValue(total);
                outLbl.setText("Out: " + SEC.format(total) + "s");
            }

            // force 1st frame reliably
            player.setMute(true);
            player.seek(Duration.seconds(Math.max(0, inSlider.getValue())));
            player.play();
            player.pause();
            player.setMute(false);

            // sync slider while playing (when user isn’t dragging)
            player.currentTimeProperty().addListener((obs, old, cur) -> {
                if (!seekSlider.isPressed() && !seekSlider.isValueChanging()) {
                    seekSlider.setValue(cur.toSeconds());
                    updateTimeLabel();
                }
                // respect OUT bound
                double out = outSlider.getValue();
                if (out > 0 && cur.toSeconds() >= out - 0.01) {
                    player.pause();
                }
            });

            // loop back to IN on end
            player.setOnEndOfMedia(() -> {
                player.seek(Duration.seconds(inSlider.getValue()));
                player.play();
            });

            updateTimeLabel();
            updateTrimLabels();
        });
    }

    private void jumpTo(double seconds) {
        if (player == null) return;
        player.pause();
        // do a tiny mute->play->pause nudge to force frame update on some systems/codecs
        boolean wasMuted = player.isMute();
        player.setMute(true);
        player.seek(Duration.seconds(seconds));
        player.play();
        player.pause();
        player.setMute(wasMuted);

        seekSlider.setValue(seconds);
        updateTimeLabel();
    }

    private void applyTrimBackToClip() {
        if (boundClip == null) return;
        double in = Math.max(0, Math.min(inSlider.getValue(), outSlider.getValue()));
        double out = Math.max(inSlider.getValue(), outSlider.getValue());

        double total = (player != null && player.getTotalDuration() != null) ? player.getTotalDuration().toSeconds() : out;

        boundClip.inSecProperty().set(in);
        boundClip.outSecProperty().set(Math.abs(out - total) < 0.01 ? 0.0 : out); // 0 = “to end”

        applyBtn.setText("Saved ✓");
        Platform.runLater(() -> {
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            applyBtn.setText("Apply Trim");
        });
    }

    private void updateTimeLabel() {
        if (player == null) return;
        double cur = player.getCurrentTime().toSeconds();
        double total = player.getTotalDuration() == null ? 0 : player.getTotalDuration().toSeconds();
        timeLbl.setText(secText(cur) + " / " + secText(total));
    }

    private void updateTrimLabels() {
        inLbl.setText("In: " + SEC.format(inSlider.getValue()) + "s");
        outLbl.setText("Out: " + SEC.format(outSlider.getValue()) + "s");
    }

    private static String secText(double s) {
        int total = (int)Math.floor(s);
        int m = total / 60;
        int sec = total % 60;
        return String.format("%02d:%02d", m, sec);
    }

    private static String safeName(String path) {
        if (path == null) return "(no file)";
        int i = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return i >= 0 ? path.substring(i + 1) : path;
    }

    private static double mouseToSliderValue(Slider s, double mouseX) {
        double min = s.getMin(), max = s.getMax();
        double w = Math.max(1.0, s.getWidth());
        double pct = mouseX / w;
        pct = Math.max(0, Math.min(1, pct));
        return min + pct * (max - min);
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("Clip Preview");
        a.showAndWait();
    }
}
