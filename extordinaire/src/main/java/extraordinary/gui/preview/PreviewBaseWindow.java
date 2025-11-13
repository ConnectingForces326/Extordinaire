package extraordinary.gui.preview;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;

/** Shared preview window: media view + seek + time + transport (play/pause/stop). */
public abstract class PreviewBaseWindow {
    protected final Stage stage = new Stage();
    protected final MediaView mediaView = new MediaView();
    protected MediaPlayer player;

    protected final Slider seek = new Slider(0, 1, 0);
    protected final Label time = new Label("00:00 / 00:00");
    protected final Button play = new Button("Play");
    protected final Button pause = new Button("Pause");
    protected final Button stop = new Button("Stop");

    protected void initUI(String title) {
        stage.setTitle(title);
        mediaView.setPreserveRatio(true);

        play.setOnAction(e -> { if (player != null) player.play(); });
        pause.setOnAction(e -> { if (player != null) player.pause(); });
        stop.setOnAction(e -> { if (player != null) player.stop(); });

        seek.valueProperty().addListener((o, ov, nv) -> {
            if (player != null && seek.isValueChanging()) {
                Duration total = player.getTotalDuration();
                if (total != null && !total.isUnknown() && total.toMillis() > 0) {
                    player.seek(total.multiply(seek.getValue()));
                }
            }
        });

        VBox root = new VBox(8,
                mediaView,
                new HBox(8, seek, time),
                new HBox(8, play, pause, stop)
        );
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color:#0f0f0f; -fx-text-fill:#EDEDED;");

        stage.setScene(new Scene(root, 960, 540));
        stage.setOnCloseRequest(e -> disposePlayer());
    }

    protected void bindToPlayer(MediaPlayer p) {
        disposePlayer();
        this.player = p;
        mediaView.setMediaPlayer(p);

        p.setOnReady(() -> {
            updateTimeLabel(Duration.ZERO, p.getTotalDuration());
            seek.setValue(0);
        });

        p.currentTimeProperty().addListener((o, ov, nv) -> {
            Duration total = p.getTotalDuration();
            updateTimeLabel(nv, total);
            if (!seek.isValueChanging() && total != null && !total.isUnknown() && total.toMillis() > 0) {
                seek.setValue(nv.toMillis() / total.toMillis());
            }
        });
    }

    protected void updateTimeLabel(Duration cur, Duration total) {
        time.setText(fmt(cur) + " / " + fmt(total));
    }
    protected String fmt(Duration d) {
        if (d == null || d.isUnknown() || d.lessThan(Duration.ZERO)) return "00:00";
        long s = (long)Math.floor(d.toSeconds());
        return String.format("%02d:%02d", (s/60), (s%60));
    }

    public void show() { stage.show(); stage.toFront(); }

    public void disposePlayer() {
        if (player != null) {
            try { player.stop(); } catch (Exception ignore) {}
            try { player.dispose(); } catch (Exception ignore) {}
            player = null;
        }
    }
}
