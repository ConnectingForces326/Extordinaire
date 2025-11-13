package extraordinary.prototype;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import extraordinary.core.LayoutEngine;
import extraordinary.core.LayoutResult;
import extraordinary.core.LayoutRule;
import extraordinary.core.SectionSpec;
import extraordinary.core.SectionTiming;
import extraordinary.helpers.JsonUtil;
import extraordinary.models.DynamicTimelineConfig;
import extraordinary.models.DynamicTimelineConfig.Marker;
import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.StringConverter;

public class DynamicTimeMarkerPrototype extends Application {

    // ===== Row model for the table =====
    public static class Row {
        private final StringProperty name = new SimpleStringProperty("");
        private final DoubleProperty weight = new SimpleDoubleProperty(1.0);
        private final IntegerProperty start = new SimpleIntegerProperty(0);
        private final IntegerProperty end = new SimpleIntegerProperty(0);
        private final boolean locked; // HOOK/END

        public Row(String name, double weight, boolean locked) {
            this.name.set(name);
            this.weight.set(weight);
            this.locked = locked;
        }

        public String getName() { return name.get(); }
        public void setName(String s) { name.set(s); }
        public StringProperty nameProperty() { return name; }

        public double getWeight() { return weight.get(); }
        public void setWeight(double w) { weight.set(w); }
        public DoubleProperty weightProperty() { return weight; }

        public int getStart() { return start.get(); }
        public void setStart(int v) { start.set(v); }
        public IntegerProperty startProperty() { return start; }

        public int getEnd() { return end.get(); }
        public void setEnd(int v) { end.set(v); }
        public IntegerProperty endProperty() { return end; }

        public boolean isLocked() { return locked; }
    }

    // ===== UI state =====
    private final ObservableList<Row> rows = FXCollections.observableArrayList();
    private boolean suppressListListener = false; // reentrancy guard
    private Label totalLabel;
    private Pane timelinePane;
    private TableView<Row> table;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Dynamic Time Marker Prototype — V5 (core)");

        // default rows
        rows.addAll(
                new Row("HOOK", 2.0, true),
                new Row("SECTION1", 1.2, false),
                new Row("SECTION2", 1.1, false),
                new Row("END", 0.7, true)
        );

        // top bar
        Button addBtn = new Button("+ Add");
        addBtn.setOnAction(e -> {
            int idx = Math.max(1, rows.size() - 1);
            rows.add(idx, new Row(nextSectionName(rows), 1.0, false));
            autoFit();
        });

        Button removeBtn = new Button("– Remove");
        Button resetBtn = new Button("Reset");
        Button copyBtn = new Button("Copy + Save JSON");

        resetBtn.setOnAction(e -> {
            rows.setAll(
                    new Row("HOOK", 2.0, true),
                    new Row("SECTION1", 1.2, false),
                    new Row("SECTION2", 1.1, false),
                    new Row("END", 0.7, true)
            );
            autoFit();
        });

        // Copy + Save JSON of the current layout (using Gson + shared model)
        copyBtn.setOnAction(e -> {
            try {
                // 1) build specs from current rows
                List<SectionSpec> specs = new ArrayList<>();
                for (Row r : rows) {
                    specs.add(new SectionSpec(r.getName(), r.getWeight(), null, null));
                }

                // 2) compute layout with the same shorts preset rule
                LayoutRule rule = LayoutRule.shortsPreset(); // matches 30–59 target
                LayoutResult result = LayoutEngine.layout(specs, rule);

                // 3) turn layout result into shared DynamicTimelineConfig model
                DynamicTimelineConfig config = buildConfigFromLayout(result);

                // 4) save JSON to disk
                exportConfigToFile(config);

                // 5) also copy JSON to clipboard
                String json = JsonUtil.toJson(config);
                ClipboardContent cc = new ClipboardContent();
                cc.putString(json);
                Clipboard.getSystemClipboard().setContent(cc);

                totalLabel.setText("Total: " + result.totalSec() + "s (JSON saved & copied)");

            } catch (Exception ex) {
                ex.printStackTrace();
                totalLabel.setText("Error exporting JSON (see console)");
            }
        });

        totalLabel = new Label("Total: --s (target 30–59)");

        HBox top = new HBox(10, addBtn, removeBtn, resetBtn, copyBtn, totalLabel);
        top.setPadding(new Insets(10));
        top.setAlignment(Pos.CENTER_LEFT);

        // table
        table = new TableView<>(rows);
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPrefHeight(300);

        removeBtn.setOnAction(e -> {
            Row sel = table.getSelectionModel().getSelectedItem();
            if (sel != null && !sel.isLocked()
                    && !sel.getName().equals("HOOK")
                    && !sel.getName().equals("END")) {
                rows.remove(sel);
                autoFit();
            }
        });

        table.setOnKeyPressed(ev -> {
            if (ev.getCode() == KeyCode.DELETE) {
                Row sel = table.getSelectionModel().getSelectedItem();
                if (sel != null && !sel.isLocked()
                        && !sel.getName().equals("HOOK")
                        && !sel.getName().equals("END")) {
                    rows.remove(sel);
                    autoFit();
                }
            }
        });

        TableColumn<Row, String> cName = new TableColumn<>("Section");
        cName.setCellValueFactory(new PropertyValueFactory<>("name"));
        cName.setCellFactory(TextFieldTableCell.forTableColumn());
        cName.setOnEditCommit(ev -> {
            Row r = ev.getRowValue();
            if (!r.isLocked()) {
                r.setName(ev.getNewValue());
                autoFit();
            } else {
                table.refresh();
            }
        });

        TableColumn<Row, Number> cWeight = new TableColumn<>("Weight");
        cWeight.setCellValueFactory(cell -> cell.getValue().weightProperty());
        cWeight.setCellFactory(col -> new TextFieldTableCell<>(new StringConverter<Number>() {
            @Override
            public String toString(Number n) {
                return n == null ? "" : String.format(Locale.US, "%.2f", n.doubleValue());
            }

            @Override
            public Number fromString(String s) {
                try {
                    return Double.parseDouble(s.trim());
                } catch (Exception e) {
                    return 1.0;
                }
            }
        }));
        cWeight.setOnEditCommit(ev -> {
            Row r = ev.getRowValue();
            double v = ev.getNewValue() == null ? 1.0 : ev.getNewValue().doubleValue();
            if (v <= 0 || Double.isNaN(v) || Double.isInfinite(v)) v = 1.0;
            r.setWeight(v);
            autoFit();
        });

        TableColumn<Row, Number> cStart = new TableColumn<>("Start s");
        cStart.setCellValueFactory(cell -> cell.getValue().startProperty());
        TableColumn<Row, Number> cEnd = new TableColumn<>("End s");
        cEnd.setCellValueFactory(cell -> cell.getValue().endProperty());

        table.getColumns().addAll(cName, cWeight, cStart, cEnd);

        // IMPORTANT: list-change listener with guard
        rows.addListener((ListChangeListener<Row>) c -> {
            if (suppressListListener) return;
            autoFit();
        });

        // timeline pane
        timelinePane = new Pane();
        timelinePane.setPrefHeight(120);
        timelinePane.setStyle(
                "-fx-background-color:#111;-fx-border-color:#333;" +
                "-fx-border-radius:8;-fx-background-radius:8;");
        BorderPane timelineWrap = new BorderPane(timelinePane);
        timelineWrap.setPadding(new Insets(10));

        VBox center = new VBox(8, timelineWrap, table);
        center.setPadding(new Insets(8));
        Scene scene = new Scene(new BorderPane(center, top, null, null, null), 1040, 580);
        stage.setScene(scene);
        stage.show();

        autoFit();
    }

    // ===== Auto-fit using shared core, with reentrancy guard =====
    private void autoFit() {
        if (suppressListListener) return;
        suppressListListener = true;
        try {
            // 1) Build ordered specs
            List<SectionSpec> specs = new ArrayList<>();
            for (Row r : rows) {
                specs.add(new SectionSpec(r.getName(), r.getWeight(), null, null));
            }

            // 2) Call engine (shorts preset 30–59)
            LayoutRule rule = LayoutRule.shortsPreset();
            LayoutResult result = LayoutEngine.layout(specs, rule);

            // 3) Desired rows from engine output
            List<Row> desired = new ArrayList<>();
            for (SectionTiming t : result.timeline()) {
                Row existing = findRowByName(t.name());
                if (existing == null) {
                    boolean locked = "HOOK".equals(t.name()) || "END".equals(t.name());
                    existing = new Row(t.name(), 1.0, locked);
                }
                existing.setStart(t.startSec());
                existing.setEnd(t.endSec());
                desired.add(existing);
            }

            // 4) Only replace the list if order/durations differ (prevents change-loop)
            if (!sameOrderAndDurations(rows, desired)) {
                rows.setAll(desired);
            } else {
                table.refresh();
            }

            // 5) UI refresh
            totalLabel.setText("Total: " + result.totalSec() + "s (target 30–59)");
            drawTimeline(Math.max(result.totalSec(), 59));

        } finally {
            suppressListListener = false;
        }
    }

    private Row findRowByName(String name) {
        for (Row r : rows) if (Objects.equals(r.getName(), name)) return r;
        return null;
    }

    private static boolean sameOrderAndDurations(List<Row> a, List<Row> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            Row r1 = a.get(i), r2 = b.get(i);
            if (!Objects.equals(r1.getName(), r2.getName())) return false;
            int d1 = r1.getEnd() - r1.getStart();
            int d2 = r2.getEnd() - r2.getStart();
            if (d1 != d2) return false;
        }
        return true;
    }

    private static String nextSectionName(List<Row> rows) {
        Set<String> names = rows.stream().map(Row::getName).collect(Collectors.toSet());
        int i = 1;
        while (names.contains("SECTION" + i)) i++;
        return "SECTION" + i;
    }

    private void drawTimeline(int refSeconds) {
        timelinePane.getChildren().clear();
        int totalSeconds = rows.stream().mapToInt(r -> r.getEnd() - r.getStart()).sum();
        if (totalSeconds <= 0) return;

        int visTotal = Math.max(totalSeconds, refSeconds);
        double pxPerSec = Math.min(16, Math.max(8, 1000.0 / Math.max(60, visTotal)));
        double width = Math.max(900, visTotal * pxPerSec);
        timelinePane.setPrefWidth(width + 40);

        double x = 20, y = 28, barH = 56;
        for (Row r : rows) {
            int dur = r.getEnd() - r.getStart();
            double w = (dur / (double) Math.max(1, visTotal)) * width;

            Rectangle rect = new Rectangle(x, y, Math.max(1, w), barH);
            rect.setArcWidth(10);
            rect.setArcHeight(10);
            rect.setFill(pickColor(r.getName()));
            rect.setStroke(Color.BLACK);
            timelinePane.getChildren().add(rect);

            Label lbl = new Label((w < 70) ? (dur + "s") : (r.getName() + " (" + dur + "s)"));
            lbl.setTextFill(Color.WHITE);
            lbl.setLayoutX(x + 6);
            lbl.setLayoutY(y + 18);
            timelinePane.getChildren().add(lbl);

            Label marker = new Label(timecode(r.getEnd()));
            marker.setTextFill(Color.LIGHTGRAY);
            marker.setLayoutX(Math.max(0, x + w - 28));
            marker.setLayoutY(y + barH + 4);
            timelinePane.getChildren().add(marker);

            x += w;
        }
        Label zero = new Label("0:00");
        zero.setTextFill(Color.LIGHTGRAY);
        zero.setLayoutX(2);
        zero.setLayoutY(y + barH + 4);
        timelinePane.getChildren().add(zero);
    }

    private static String timecode(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%d:%02d", m, s);
    }

    private static Color pickColor(String key) {
        int h = Math.abs(key.hashCode());
        double hue = (h % 360);
        return Color.hsb(hue, 0.55, 0.85);
    }

    // ===== Helpers for JSON export =====

    private DynamicTimelineConfig buildConfigFromLayout(LayoutResult result) {
        List<Marker> markers = new ArrayList<>();
        for (SectionTiming t : result.timeline()) {
            int duration = t.endSec() - t.startSec();
            markers.add(new Marker(
                    t.name(),
                    t.startSec(),
                    t.endSec(),
                    duration
            ));
        }
        return new DynamicTimelineConfig("shorts-preset", markers);
    }

    private void exportConfigToFile(DynamicTimelineConfig config) throws Exception {
        Path outDir = Paths.get("data");
        Files.createDirectories(outDir);
        Path outFile = outDir.resolve("dynamic_timeline.json");
        JsonUtil.save(outFile.toString(), config);
        System.out.println("Saved dynamic timeline JSON to " + outFile.toAbsolutePath());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
