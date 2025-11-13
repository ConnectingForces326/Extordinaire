package extraordinary.gui.preview;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import extraordinary.gui.App.ClipItem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MasterPreviewWindow {

    private Stage stage;

    private final StackPane mediaPane = new StackPane();
    private final HBox controlsBar = new HBox(8);

    private final MediaView mediaView = new MediaView();
    private final Button playBtn = new Button("Play");
    private final Button pauseBtn = new Button("Pause");
    private final Button stopBtn = new Button("Stop");
    private final Slider scrub = new Slider(0, 1, 0);
    private final Label timeLbl = new Label("00:00 / 00:00");

    private final List<MediaPlayer> playlist = new ArrayList<>();
    private int index = -1;
    private boolean userScrubbing = false;

    public void open(List<ClipItem> clips, boolean portrait) {
        if (stage == null) {
            stage = new Stage();
            stage.setTitle("Extordinaire — Master Preview");

            mediaPane.setStyle("-fx-background-color:#0f0f0f;");
            mediaView.setPreserveRatio(true);

            controlsBar.setAlignment(Pos.CENTER_LEFT);
            controlsBar.setPadding(new Insets(10));
            controlsBar.setStyle("-fx-background-color:#161616; -fx-border-color:#2a2a2a; -fx-border-width:1 0 0 0;");

            scrub.valueChangingProperty().addListener((o, was, is) -> {
                userScrubbing = is;
                MediaPlayer cur = current();
                if (!is && cur != null) cur.seek(Duration.seconds(scrub.getValue()));
            });
            scrub.valueProperty().addListener((o, a, b) -> {
                if (userScrubbing) {
                    MediaPlayer cur = current();
                    if (cur != null) cur.seek(Duration.seconds(b.doubleValue()));
                }
            });

            HBox mainControls = new HBox(8, playBtn, pauseBtn, stopBtn, scrub, timeLbl);
            mainControls.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(scrub, Priority.ALWAYS);

            controlsBar.getChildren().add(mainControls);

            VBox root = new VBox(mediaPane, controlsBar);
            VBox.setVgrow(mediaPane, Priority.ALWAYS);

            Scene scene = new Scene(root, 1100, 680);
            applyDark(scene);
            stage.setScene(scene);

            playBtn.setOnAction(e -> {
                MediaPlayer cur = current();
                if (cur == null) { startPlayback(); return; }
                cur.play();
            });
            pauseBtn.setOnAction(e -> {
                MediaPlayer cur = current();
                if (cur != null) cur.pause();
            });
            stopBtn.setOnAction(e -> stopAll());
        }

        configureSizeForOrientation(portrait);
        buildPlaylist(clips);
        stage.show();
        stage.toFront();
        if (!playlist.isEmpty()) startPlayback();
    }

    private void configureSizeForOrientation(boolean portrait) {
        if (portrait) {
            stage.setWidth(650);
            stage.setHeight(920);
        } else {
            stage.setWidth(1100);
            stage.setHeight(680);
        }
        mediaView.fitWidthProperty().bind(mediaPane.widthProperty());
        mediaView.fitHeightProperty().bind(mediaPane.heightProperty());
    }

    private void buildPlaylist(List<ClipItem> clips) {
        stopAll();
        playlist.clear();
        index = -1;

        for (ClipItem c : clips) {
            String p = c.getPath();
            if (p == null || p.isBlank()) continue;
            if (isImage(p)) continue; // master preview only plays video
            try {
                Media m = new Media(new File(p).toURI().toString());
                MediaPlayer mp = new MediaPlayer(m);
                double in = Math.max(0, c.getInSec());
                double out = (c.getOutSec() > 0) ? c.getOutSec() : Double.POSITIVE_INFINITY;
                mp.setOnReady(() -> {
                    double total = mp.getTotalDuration().toSeconds();
                    mp.setStartTime(Duration.seconds(in));
                    mp.setStopTime(Duration.seconds(Math.min(out, total)));
                });
                mp.setOnEndOfMedia(this::next);
                playlist.add(mp);
            } catch (Exception ignored) {}
        }
    }

    private void startPlayback() {
        next(); // moves to index 0 and plays
    }

    private void next() {
        MediaPlayer prev = current();
        if (prev != null) { prev.stop(); prev.dispose(); }

        index++;
        if (index >= playlist.size()) {
            index = -1;
            scrub.setValue(0);
            timeLbl.setText("00:00 / 00:00");
            mediaPane.getChildren().clear();
            return;
        }

        MediaPlayer cur = playlist.get(index);
        mediaView.setMediaPlayer(cur);
        mediaPane.getChildren().setAll(mediaView);

        cur.setOnReady(() -> {
            double min = cur.getStartTime().toSeconds();
            double max = cur.getStopTime().toSeconds();
            if (max <= 0 || max > cur.getTotalDuration().toSeconds()) {
                max = cur.getTotalDuration().toSeconds();
            }
            scrub.setMin(min);
            scrub.setMax(max);
            scrub.setValue(min);

            cur.currentTimeProperty().addListener((o, a, b) -> {
                if (!userScrubbing) {
                    double t = b.toSeconds();
                    if (t >= scrub.getMin() && t <= scrub.getMax()) scrub.setValue(t);
                }
                updateTimeLabel(cur.getCurrentTime(), cur.getTotalDuration());
            });

            cur.play();
        });
    }

    private void stopAll() {
        MediaPlayer cur = current();
        if (cur != null) { try { cur.stop(); cur.dispose(); } catch (Exception ignored) {} }
        for (MediaPlayer mp : playlist) { try { mp.dispose(); } catch (Exception ignored) {} }
        playlist.clear();
        index = -1;
        mediaPane.getChildren().clear();
        scrub.setValue(0);
        timeLbl.setText("00:00 / 00:00");
    }

    private MediaPlayer current() {
        return (index >= 0 && index < playlist.size()) ? playlist.get(index) : null;
    }

    private boolean isImage(String p) {
        String s = p.toLowerCase();
        return s.endsWith(".png") || s.endsWith(".jpg") || s.endsWith(".jpeg");
    }

    private void updateTimeLabel(Duration cur, Duration tot) {
        timeLbl.setText(format(cur) + " / " + format(tot));
    }

    private String format(Duration d) {
        if (d == null || d.isUnknown()) return "00:00";
        int s = (int) Math.floor(d.toSeconds());
        int m = s / 60;
        s = s % 60;
        return String.format("%02d:%02d", m, s);
    }

    private void applyDark(Scene scene) {
        String css = String.join("\n",
                ".root { -fx-background-color: #0f0f0f; -fx-text-fill: #EDEDED; }",
                "Label { -fx-text-fill: #EDEDED; }",
                ".button { -fx-background-color:#2A2A2A; -fx-text-fill:#F2F2F2; -fx-border-color:#333; -fx-background-radius:8; }",
                ".button:hover { -fx-background-color:#333333; }",
                ".slider .track { -fx-background-color:#555; }",
                ".slider .thumb { -fx-background-color:#ddd; }"
        );
        scene.getStylesheets().add("data:text/css," + css.replace("\n", "%0A"));
    }
}
