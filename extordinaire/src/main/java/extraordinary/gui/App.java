package extraordinary.gui;
import extraordinary.helpers.JsonUtil;
import extraordinary.models.DynamicTimelineConfig;
import extraordinary.models.Niche;
import extraordinary.models.Style;
import extraordinary.models.ContentRequest;
import extraordinary.models.VideoPlan;

import extraordinary.logic.ConceptVision;
import extraordinary.logic.AlphaBlueprint;
import extraordinary.logic.ScriptGenerator;
import extraordinary.logic.AutoStyleSelector;
import extraordinary.content.DynamicNicheChannel;
import extraordinary.ideas.CreativeNotes;

import extraordinary.gui.preview.MasterPreviewWindow;
import extraordinary.gui.preview.ClipPreviewWindow;

import javafx.application.Application;
import javafx.beans.property.*;
import javafx.collections.*;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.*;

/**
 * Extordinaire GUI (no embedded preview — uses separate preview windows)
 */
public class App extends Application {

    // --- UI State ---
    private final StringProperty orientation = new SimpleStringProperty("LANDSCAPE"); // or PORTRAIT
    private final StringProperty scriptText = new SimpleStringProperty("Welcome to The Extordinaire! This is a test narration for our MVP.");
    private final StringProperty status = new SimpleStringProperty("Ready.");
    private final StringProperty topic = new SimpleStringProperty("Engaging videos");
    private final ObjectProperty<Niche> niche = new SimpleObjectProperty<>(Niche.GENERAL);
    private final StringProperty style = new SimpleStringProperty("AUTO");

    // Scenes/Tree model
    private final TreeItem<SceneNode> scenesRoot = new TreeItem<>(SceneNode.root());
    private final ObjectProperty<TreeItem<SceneNode>> selectedNode = new SimpleObjectProperty<>();
    private final ListProperty<ClipItem> currentClips = new SimpleListProperty<>(FXCollections.observableArrayList());

    // External preview windows
    private MasterPreviewWindow masterPreview;
    private ClipPreviewWindow clipPreview;

    // --- Scene types & tags ---
    private enum SceneKind { ROOT, START, BODY, END, SUBSCENE }
    private enum SubTag { HOOK, INTERLUDE, FACT1, FACT2, CUSTOM }

    public static class SceneNode {
        private final SceneKind kind;
        private final SubTag tag;               // only for SUBSCENE
        private final StringProperty title = new SimpleStringProperty();
        private final ListProperty<ClipItem> clips = new SimpleListProperty<>(FXCollections.observableArrayList());

        private SceneNode(SceneKind kind, String title, SubTag tag) {
            this.kind = kind; this.tag = tag; this.title.set(title);
        }
        public static SceneNode root()    { return new SceneNode(SceneKind.ROOT, "ROOT", null); }
        public static SceneNode start()   { return new SceneNode(SceneKind.START, "Scene 1 — Start", null); }
        public static SceneNode end()     { return new SceneNode(SceneKind.END,   "Scene 2 — End", null); }
        public static SceneNode body(int idx) { return new SceneNode(SceneKind.BODY, "Scene " + idx, null); }
        public static SceneNode sub(SubTag tag, String label) { return new SceneNode(SceneKind.SUBSCENE, label, tag); }

        public SceneKind getKind() { return kind; }
        public SubTag getTag()     { return tag; }
        public String getTitle()   { return title.get(); }
        public StringProperty titleProperty() { return title; }
        public ObservableList<ClipItem> getClips() { return clips.get(); }
        public ListProperty<ClipItem> clipsProperty() { return clips; }
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("The Extordinaire — MVP Preview/Export");

        // Startup chime
        try {
            // 1) Try to load from classpath (src/main/resources/Extordinaire.wav)
            java.net.URL url = App.class.getResource("/Extordinaire.wav");

            // 2) Fallback: direct file path (your dev machine)
            if (url == null) {
                System.out.println("Classpath miss; trying direct file path…");
                java.io.File f = new java.io.File(
                    "C:/Users/Matth/eclipse-workspace/extordinaire/src/main/resources/Extordinaire.wav"
                );
                if (f.exists()) {
                    url = f.toURI().toURL();
                }
            }

            // 3) If we got a URL, try to play it
            if (url != null) {
                javafx.scene.media.Media sound =
                        new javafx.scene.media.Media(url.toExternalForm());
                javafx.scene.media.MediaPlayer player =
                        new javafx.scene.media.MediaPlayer(sound);
                player.play();
            } else {
                System.out.println("Chime not found via classpath or file path.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("Could not play chime.");
        }


        // Top controls: Topic, Niche, Style
        TextField topicField = new TextField();
        topicField.setPromptText("Enter topic (e.g. Engaging videos)");
        topicField.textProperty().bindBidirectional(topic);

        ComboBox<Niche> nicheBox = new ComboBox<>();
        nicheBox.getItems().addAll(Niche.values());
        nicheBox.setValue(Niche.GENERAL);
        nicheBox.valueProperty().bindBidirectional(niche);

        ComboBox<String> styleBox = new ComboBox<>();
        styleBox.getItems().addAll("AUTO", "HYPE", "TEACHER", "ANALYST");
        styleBox.setValue("AUTO");
        styleBox.valueProperty().bindBidirectional(style);

        // Force closed-state ComboBox text to white + popup cells white-on-dark
        nicheBox.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Niche it, boolean empty) {
                super.updateItem(it, empty);
                setText(empty || it == null ? null : it.name());
                setStyle("-fx-text-fill: white; -fx-background-color: #202020;");
            }
        });
        styleBox.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String it, boolean empty) {
                super.updateItem(it, empty);
                setText(empty || it == null ? null : it);
                setStyle("-fx-text-fill: white; -fx-background-color: #202020;");
            }
        });
        nicheBox.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Niche it, boolean empty) {
                super.updateItem(it, empty);
                setText(empty || it == null ? null : it.name());
                setStyle("-fx-text-fill: white; -fx-background-color: #202020;");
            }
        });
        styleBox.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String it, boolean empty) {
                super.updateItem(it, empty);
                setText(empty || it == null ? null : it);
                setStyle("-fx-text-fill: white; -fx-background-color: #202020;");
            }
        });

        Button generateScriptBtn = new Button("Generate Script");
        generateScriptBtn.setOnAction(e -> {
            try {
                AutoStyleSelector auto = new AutoStyleSelector();
                Style chosen;
                String styleStr = style.get() == null ? "AUTO" : style.get();
                if ("AUTO".equalsIgnoreCase(styleStr)) {
                    chosen = auto.pick(niche.get());
                } else {
                    try { chosen = Style.valueOf(styleStr); } catch (Exception ex) { chosen = auto.pick(niche.get()); }
                }

                ContentRequest req = new ContentRequest(
                        topic.get().isBlank() ? "AI video editing" : topic.get(),
                        "Universal",
                        niche.get(),
                        chosen
                );

                ConceptVision vision = new ConceptVision();
                AlphaBlueprint blueprint = new AlphaBlueprint();
                ScriptGenerator scripter = new ScriptGenerator();
                DynamicNicheChannel channel = new DynamicNicheChannel();
                CreativeNotes notes = new CreativeNotes();

                String hook = vision.ideateHook(req);
                String script = scripter.generate(req, hook);
                VideoPlan plan = channel.assembleVideo(hook, script);

                scriptText.set(plan.toString());
                String saved = notes.saveDraftToFile(plan.toString());
                status.set((saved != null) ? "Script generated. Saved to: " + saved : "Script generated.");
            } catch (Exception ex) {
                status.set("Script generation failed: " + ex.getMessage());
            }
        });

        Button masterPreviewBtn = new Button("Master Preview");
        Button clipPreviewBtn   = new Button("Clip Preview");

        HBox header = new HBox(8, new Label("Topic:"), topicField,
                new Label("Niche:"), nicheBox,
                new Label("Style:"), styleBox,
                generateScriptBtn, masterPreviewBtn, clipPreviewBtn);
        header.setPadding(new Insets(10));

        // Script Area + TTS buttons
        TextArea scriptArea = new TextArea();
        scriptArea.setPromptText("Script will appear here...");
        scriptArea.setWrapText(true);
        scriptArea.textProperty().bindBidirectional(scriptText);

        Button ttsBtn = new Button("Generate TTS (dummy WAV)");
        Button playTtsBtn = new Button("Play TTS");
        ttsBtn.setOnAction(e -> {
            try {
                Path wav = TTSService.generateDummyWav(scriptText.get());
                status.set("TTS audio created: " + wav);
            } catch (Exception ex) {
                status.set("TTS failed: " + ex.getMessage());
            }
        });
        playTtsBtn.setOnAction(e -> {
            try {
                Path wav = TTSService.getLatestDummyWav();
                if (wav != null && Files.exists(wav)) {
                    var p = new javafx.scene.media.MediaPlayer(new javafx.scene.media.Media(wav.toUri().toString()));
                    p.play();
                    p.setOnEndOfMedia(p::dispose);
                } else {
                    status.set("No TTS file found. Generate TTS first.");
                }
            } catch (Exception ex) {
                status.set("Play TTS failed: " + ex.getMessage());
            }
        });
        HBox ttsRow = new HBox(8, ttsBtn, playTtsBtn);
        ttsRow.setPadding(new Insets(6, 0, 6, 0));

        VBox scriptBox = new VBox(6, new Label("Script:"), scriptArea, ttsRow);
        scriptBox.setPadding(new Insets(10));
        scriptBox.setPrefWidth(380);
        scriptBox.setStyle("-fx-background-color: #161616; -fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: #2a2a2a;");

        // --- SCENES PANE: TreeView with macro scenes + subscenes ---
        TreeView<SceneNode> sceneTree = new TreeView<>(scenesRoot);
        sceneTree.setShowRoot(false);
        sceneTree.setPrefWidth(260);

        // initial locked scenes: Scene 1 (Start), Scene 2 (End)
        if (scenesRoot.getChildren().isEmpty()) {
            scenesRoot.getChildren().addAll(
                new TreeItem<>(SceneNode.start()),
                new TreeItem<>(SceneNode.end())
            );
        }
        sceneTree.getSelectionModel().select(0);
        selectedNode.bind(sceneTree.getSelectionModel().selectedItemProperty());

        // dark cell styling + color badges
        sceneTree.setCellFactory(tv -> new TreeCell<>() {
            @Override protected void updateItem(SceneNode node, boolean empty) {
                super.updateItem(node, empty);
                if (empty || node == null) { setText(null); setGraphic(null); setStyle(""); return; }

                String color;
                if (node.getKind() == SceneKind.START) color = "#33cc66";          // green
                else if (node.getKind() == SceneKind.END) color = "#ff5c5c";       // red
                else if (node.getKind() == SceneKind.SUBSCENE) color = switch (node.getTag()) {
                    case HOOK -> "#7c5cff";
                    case INTERLUDE -> "#ffaa33";
                    case FACT1 -> "#2fb3ff";
                    case FACT2 -> "#ff8ad6";
                    default -> "#8a8a8a";
                };
                else color = "#8a8a8a";

                Region dot = new Region();
                dot.setMinSize(10,10); dot.setMaxSize(10,10);
                dot.setStyle("-fx-background-radius:8; -fx-background-color:"+color+";");

                Label lbl = new Label(node.getTitle());
                HBox row = new HBox(8, dot, lbl);
                setGraphic(row);
                setText(null);

                setStyle("-fx-text-fill: white; -fx-background-color: #161616;");
            }
        });

        // scene controls
        Button addBodyScene = new Button("+ Scene");
        Button addSubscene  = new Button("+ Subscene");
        Button rmNode       = new Button("–");
        Button upNode       = new Button("↑");
        Button dnNode       = new Button("↓");

        addBodyScene.setOnAction(e -> {
            int nextIdx = nextBodySceneIndex();
            TreeItem<SceneNode> ti = new TreeItem<>(SceneNode.body(nextIdx));
            scenesRoot.getChildren().add(ti);
            sceneTree.getSelectionModel().select(ti);
            renumberBodyScenes();
        });

        addSubscene.setOnAction(e -> {
            TreeItem<SceneNode> sel = selectedNode.get();
            if (sel == null) return;

            // only allow subscenes under BODY scenes (not START/END)
            TreeItem<SceneNode> parent = sel;
            if (sel.getValue().getKind() == SceneKind.SUBSCENE) parent = sel.getParent();
            if (parent == null) return;
            if (parent.getValue().getKind() != SceneKind.BODY) { status.set("Add subscenes under BODY scenes only."); return; }

            // simple cycle of placeholder tags
            SubTag[] tags = {SubTag.HOOK, SubTag.INTERLUDE, SubTag.FACT1, SubTag.FACT2};
            int count = (int) parent.getChildren().stream().filter(t -> t.getValue().getKind() == SceneKind.SUBSCENE).count();
            SubTag tag = (count < tags.length) ? tags[count] : SubTag.CUSTOM;

            String macroNum = macroNumber(parent);
            String label = macroNum + "." + (count + 1) + " — " + tag;
            TreeItem<SceneNode> sub = new TreeItem<>(SceneNode.sub(tag, label));
            parent.getChildren().add(sub);
            parent.setExpanded(true);
            sceneTree.getSelectionModel().select(sub);
        });

        rmNode.setOnAction(e -> {
            TreeItem<SceneNode> sel = selectedNode.get();
            if (sel == null || sel.getParent() == null) return; // ignore root
            SceneKind k = sel.getValue().getKind();
            if (k == SceneKind.START || k == SceneKind.END) { status.set("Start/End are locked."); return; }
            sel.getParent().getChildren().remove(sel);
            renumberBodyScenes();
        });

        upNode.setOnAction(e -> {
            TreeItem<SceneNode> sel = selectedNode.get();
            if (sel == null || sel.getParent() == null) return;
            TreeItem<SceneNode> parent = sel.getParent();
            int idx = parent.getChildren().indexOf(sel);
            if (idx <= 0) return;
            if (parent == scenesRoot && (idx == 2)) return; // don't move a body above END
            Collections.swap(parent.getChildren(), idx, idx - 1);
            sceneTree.getSelectionModel().select(parent.getChildren().get(idx - 1));
            renumberBodyScenes();
        });

        dnNode.setOnAction(e -> {
            TreeItem<SceneNode> sel = selectedNode.get();
            if (sel == null || sel.getParent() == null) return;
            TreeItem<SceneNode> parent = sel.getParent();
            int idx = parent.getChildren().indexOf(sel);
            if (idx < 0 || idx >= parent.getChildren().size() - 1) return;
            if (parent == scenesRoot && (idx == 0 || idx == 1)) return; // can't move START or END
            Collections.swap(parent.getChildren(), idx, idx + 1);
            sceneTree.getSelectionModel().select(parent.getChildren().get(idx + 1));
            renumberBodyScenes();
        });

        HBox sceneBtns = new HBox(6, addBodyScene, addSubscene, rmNode, upNode, dnNode);

        VBox scenePane = new VBox(8, new Label("Scenes:"), sceneTree, sceneBtns);
        scenePane.setPadding(new Insets(10));
        scenePane.setStyle("-fx-background-color: #161616; -fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: #2a2a2a;");

        // --- Clips table for currently selected node ---
        TableView<ClipItem> table = new TableView<>();
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<ClipItem, String> nameCol = new TableColumn<>("Purpose");
        nameCol.setCellValueFactory(c -> c.getValue().labelProperty());
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());

        // NEW: base filename column (helps visualize duplicates)
        TableColumn<ClipItem, String> fileCol = new TableColumn<>("File");
        fileCol.setCellValueFactory(c -> new SimpleStringProperty(baseName(c.getValue().getPath())));
        fileCol.setEditable(false);

        TableColumn<ClipItem, String> pathCol = new TableColumn<>("Clip Path");
        pathCol.setCellValueFactory(c -> c.getValue().pathProperty());
        pathCol.setCellFactory(TextFieldTableCell.forTableColumn());

        TableColumn<ClipItem, String> inCol = colForDouble("In (sec)", ClipItem::inSecProperty);
        TableColumn<ClipItem, String> outCol = colForDouble("Out (sec)", ClipItem::outSecProperty);

        TableColumn<ClipItem, String> overlayCol = new TableColumn<>("Overlay Text");
        overlayCol.setCellValueFactory(c -> c.getValue().overlayTextProperty());
        overlayCol.setCellFactory(TextFieldTableCell.forTableColumn());

        table.getColumns().setAll(nameCol, fileCol, pathCol, inCol, outCol, overlayCol);

        selectedNode.addListener((obs, old, ti) -> {
            if (ti == null) { table.setItems(FXCollections.observableArrayList()); return; }
            currentClips.set(ti.getValue().clipsProperty().get());
            table.setItems(currentClips.get());
        });
        // bind initial selection
        currentClips.set(scenesRoot.getChildren().get(0).getValue().clipsProperty().get());
        table.setItems(currentClips.get());

        // Buttons for clips
        Button addClipBtn = new Button("Add Clip");
        addClipBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choose a video/image clip");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Media", "*.mp4", "*.mov", "*.mkv", "*.png", "*.jpg", "*.jpeg"));
            File f = fc.showOpenDialog(stage);
            if (f != null) {
                ClipItem ci = new ClipItem(f.getAbsolutePath());
                ci.setLabel("Clip");
                currentClips.get().add(ci);
                status.set("Added: " + f.getName());
            }
        });

        Button removeClipBtn = new Button("Remove");
        removeClipBtn.setOnAction(e -> {
            ClipItem sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) currentClips.get().remove(sel);
        });

        // NEW: Duplicate button
        Button duplicateClipBtn = new Button("Duplicate");
        duplicateClipBtn.setOnAction(e -> {
            ClipItem sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            ClipItem copy = new ClipItem(sel.getPath());
            copy.setLabel(sel.getLabel() + " (dup)");
            copy.inSecProperty().set(sel.getInSec());
            copy.outSecProperty().set(sel.getOutSec());
            copy.overlayTextProperty().set(sel.getOverlayText());
            currentClips.get().add(copy);
            status.set("Duplicated: " + baseName(sel.getPath()));
        });

        Button moveUpBtn = new Button("↑");
        moveUpBtn.setOnAction(e -> {
            int idx = table.getSelectionModel().getSelectedIndex();
            if (idx > 0) { Collections.swap(currentClips.get(), idx, idx-1); table.getSelectionModel().select(idx-1); }
        });
        Button moveDownBtn = new Button("↓");
        moveDownBtn.setOnAction(e -> {
            int idx = table.getSelectionModel().getSelectedIndex();
            if (idx >=0 && idx < currentClips.get().size()-1) { Collections.swap(currentClips.get(), idx, idx+1); table.getSelectionModel().select(idx+1); }
        });

        // NEW: reuse count feedback on selection
        table.getSelectionModel().selectedItemProperty().addListener((o, oldSel, sel) -> {
            if (sel != null) {
                int n = reuseCountForPath(sel.getPath());
                status.set(baseName(sel.getPath()) + " used x" + n);
            }
        });

        ToggleGroup orientGroup = new ToggleGroup();
        RadioButton rbLand = new RadioButton("Landscape"); rbLand.setToggleGroup(orientGroup); rbLand.setSelected(true);
        RadioButton rbPort = new RadioButton("Portrait");  rbPort.setToggleGroup(orientGroup);
        rbLand.setOnAction(e -> orientation.set("LANDSCAPE"));
        rbPort.setOnAction(e -> orientation.set("PORTRAIT"));

        Button exportBtn = new Button("Save Video (Render)");
        exportBtn.setOnAction(e -> renderExport(stage));

        VBox timelineBox = new VBox(8,
                new Label("Clips in Selected Node:"),
                table,
                new HBox(8, addClipBtn, removeClipBtn, duplicateClipBtn, moveUpBtn, moveDownBtn),
                new HBox(12, new Label("Orientation:"), rbLand, rbPort),
                exportBtn
        );
        timelineBox.setPadding(new Insets(10));
        timelineBox.setStyle("-fx-background-color: #161616; -fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: #2a2a2a;");

        // Wire preview buttons (external windows)
        masterPreviewBtn.setOnAction(e -> {
            if (masterPreview == null) masterPreview = new MasterPreviewWindow();
            boolean portrait = "PORTRAIT".equals(orientation.get());
            masterPreview.open(collectAllClipsInOrder(), portrait);
        });

        clipPreviewBtn.setOnAction(e -> {
            ClipItem sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                if (clipPreview == null) clipPreview = new ClipPreviewWindow();
                boolean portrait = "PORTRAIT".equals(orientation.get());
                clipPreview.open(sel, portrait);
            } else {
                status.set("Select a clip to preview.");
            }
        });

        // Layout: left = script/tts, middle = scenes, right = clips
        HBox main = new HBox(12, scriptBox, scenePane, timelineBox);
        main.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(main);

        // Status bar
        Label statusLbl = new Label();
        statusLbl.textProperty().bind(status);
        HBox statusBar = new HBox(statusLbl);
        statusBar.setPadding(new Insets(8));
        statusBar.setStyle("-fx-background-color: transparent; -fx-text-fill: #ddd;");
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 1280, 720);

        // Dark theme + card styling (scenes pane included!)
        installDarkTheme(scene);
        header.setStyle("-fx-background-color: #161616; -fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: #2a2a2a; -fx-padding: 12;");

        stage.setScene(scene);
        stage.show();
    }

    // ----------------- CSS & Helpers -----------------

    /** CSS for clean dark theme, including TreeView (scenes) and white table text. */
    private void installDarkTheme(Scene scene) {
        String css = String.join("\n",
            /* base */
            ".root {",
            "  -fx-background-color: #0f0f0f;",
            "  -fx-text-fill: #EDEDED;",
            "  -fx-accent: #7c5cff;",
            "  -fx-focus-color: #7c5cff;",
            "  -fx-faint-focus-color: rgba(124,92,255,0.25);",
            "}",
            "Label { -fx-text-fill: #EDEDED; }",
            /* inputs */
            ".text-field, .text-area, .combo-box-base {",
            "  -fx-background-color: #202020;",
            "  -fx-text-fill: #EFEFEF;",
            "  -fx-prompt-text-fill: #9A9AA;",
            "  -fx-background-insets: 0;",
            "  -fx-background-radius: 10;",
            "  -fx-border-color: #333;",
            "  -fx-border-radius: 10;",
            "}",
            // Force closed-state ComboBox text + arrow to white
            ".combo-box-base .text { -fx-fill: white; }",
            ".combo-box-base { -fx-mark-color: white; }",

            // Combo popup styling
            ".combo-box-popup > .list-view,",
            ".combo-box-popup > .list-view .virtual-flow .clipped-container .sheet { -fx-background-color: #202020; }",
            ".combo-box-popup > .list-view .list-cell { -fx-background-color: #202020; -fx-text-fill: white; }",
            ".combo-box-popup > .list-view .list-cell:filled:hover { -fx-background-color: #2e2e2e; }",
            ".combo-box-popup > .list-view .list-cell:filled:selected { -fx-background-color: #3a3a3a; -fx-text-fill: white; }",

            ".text-area .content { -fx-background-color: #202020; }",

            /* buttons */
            ".button {",
            "  -fx-background-color: #2A2A2A;",
            "  -fx-text-fill: #F2F2F2;",
            "  -fx-background-radius: 10;",
            "  -fx-border-radius: 10;",
            "  -fx-border-color: #333;",
            "}",
            ".button:hover { -fx-background-color: #333333; }",

            /* table */
            ".table-view {",
            "  -fx-background-color: #141414;",
            "  -fx-control-inner-background: #141414;",
            "  -fx-background-radius: 12;",
            "  -fx-border-radius: 12;",
            "  -fx-border-color: #2a2a2a;",
            "  -fx-padding: 6;",
            "}",
            ".table-row-cell { -fx-background-color: #141414; }",
            ".table-view .table-cell { -fx-text-fill: white; }",
            ".table-view .table-cell .text { -fx-fill: white; }",
            ".table-row-cell:filled:selected .text { -fx-fill: white; }",
            ".table-row-cell:filled:selected { -fx-background-color: #2a2aa; }",
            ".table-row-cell:filled:hover { -fx-background-color: #1c1c1c; }",

            /* tree (scenes) */
            ".tree-view {",
            "  -fx-background-color: #161616;",
            "  -fx-control-inner-background: #161616;",
            "  -fx-background-radius: 12;",
            "  -fx-border-radius: 12;",
            "  -fx-border-color: #2a2a2a;",
            "  -fx-padding: 6;",
            "}",
            ".tree-cell { -fx-text-fill: white; -fx-background-color: #161616; }",
            ".tree-cell:filled:selected { -fx-background-color: #2a2a2a; }",
            ".tree-cell:filled:hover { -fx-background-color: #212121; }",

            /* table headers */
            ".table-view .column-header-background { -fx-background-color: #E8E8E8; }",
            ".table-view .column-header, .table-view .column-header .label, .table-view .filler { -fx-size: 36px; -fx-text-fill: black; }",
            /* radios */
            ".radio-button { -fx-text-fill: #EDEDED; }",
            ""
        );

        try {
            Path tmp = Files.createTempFile("extord_theme", ".css");
            Files.writeString(tmp, css);
            scene.getStylesheets().add(tmp.toUri().toString());
        } catch (IOException ignored) {}
    }

    private int nextBodySceneIndex() {
        // Scenes 1 and 2 reserved; next body starts at 3
        int countBody = 0;
        for (TreeItem<SceneNode> ti : scenesRoot.getChildren()) {
            if (ti.getValue().getKind() == SceneKind.BODY) countBody++;
        }
        return 3 + countBody;
    }

    private void renumberBodyScenes() {
        // maintain Scene 1 = Start, Scene 2 = End in positions 0 and 1
        List<TreeItem<SceneNode>> kids = scenesRoot.getChildren();
        if (kids.size() >= 2) {
            kids.get(0).getValue().titleProperty().set("Scene 1 — Start");
            kids.get(1).getValue().titleProperty().set("Scene 2 — End");
        }
        int idx = 3;
        for (int i = 2; i < kids.size(); i++) {
            TreeItem<SceneNode> ti = kids.get(i);
            if (ti.getValue().getKind() == SceneKind.BODY) {
                ti.getValue().titleProperty().set("Scene " + idx);
                // renumber its subscenes labels as X.1, X.2...
                int sub = 1;
                for (TreeItem<SceneNode> subTi : ti.getChildren()) {
                    if (subTi.getValue().getKind() == SceneKind.SUBSCENE) {
                        SubTag tag = subTi.getValue().getTag();
                        subTi.getValue().titleProperty().set(idx + "." + (sub++) + " — " + tag);
                    }
                }
                idx++;
            }
        }
    }

    private String macroNumber(TreeItem<SceneNode> macro) {
        String t = macro.getValue().getTitle();
        int p = t.indexOf("Scene ");
        if (p >= 0) {
            String rest = t.substring(p + 6).trim();
            int space = rest.indexOf(' ');
            return (space > 0) ? rest.substring(0, space) : rest;
        }
        return "0";
    }

    private TableColumn<ClipItem, String> colForDouble(
            String name, java.util.function.Function<ClipItem, DoubleProperty> propGetter) {

        TableColumn<ClipItem, String> col = new TableColumn<>(name);

        // Bind to the actual DoubleProperty, so the table updates when the value changes.
        col.setCellValueFactory(c -> {
            DoubleProperty p = propGetter.apply(c.getValue());
            return javafx.beans.binding.Bindings.createStringBinding(
                    () -> String.format(java.util.Locale.US, "%.2f", p.get()),
                    p
            );
        });

        // keep your editable behavior
        col.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<String>() {
            @Override public String toString(String s) { return s; }
            @Override public String fromString(String s) { return s; }
        }));
        col.setOnEditCommit(ev -> {
            try {
                double v = Double.parseDouble(ev.getNewValue());
                propGetter.apply(ev.getRowValue()).set(v);
            } catch (NumberFormatException ignored) {}
        });

        return col;
    }

    private void renderExport(Stage stage) {
        try {
            List<ClipItem> clips = collectAllClipsInOrder();
            if (clips.isEmpty()) {
                status.set("No clips in scenes. Add clips first.");
                return;
            }

            Path workDir = Files.createTempDirectory("extord_export");

            Path narration = TTSService.getLatestDummyWav();
            if (narration == null || !Files.exists(narration)) {
                narration = TTSService.generateDummyWav(scriptText.get());
            }

            boolean portrait = "PORTRAIT".equals(orientation.get());
            int targetW = portrait ? 1080 : 1920;
            int targetH = portrait ? 1920 : 1080;

            List<String> filterParts = new ArrayList<>();
            StringBuilder inputArgs = new StringBuilder();

            int idx = 0;
            for (ClipItem c : clips) {
                inputArgs.append(" -ss ").append(c.getInSec())
                        .append(" -to ").append(c.getOutSec() > 0 ? c.getOutSec() : 1e9)
                        .append(" -i ").append(quote(c.getPath()));

                String vLabel = "v" + idx;
                String aLabel = "a" + idx;

                String scalePad = String.format(Locale.US,
                        "scale=w=%d:h=%d:force_original_aspect_ratio=decrease," +
                                "pad=%d:%d:(ow-iw)/2:(oh-ih)/2",
                        targetW, targetH, targetW, targetH);

                String overlay = "";
                if (c.getOverlayText() != null && !c.getOverlayText().isBlank()) {
                    overlay = ",drawtext=text='" + c.getOverlayText().replace(":", "\\:").replace("'", "\\'")
                            + "':x=(w-text_w)/2:y=h-200:fontsize=48:fontcolor=white:box=1:boxcolor=black@0.5:boxborderw=10";
                }

                filterParts.add(String.format(Locale.US,
                        "[%d:v]fps=30,%s%s[%s];[%d:a]anull[%s]",
                        idx, scalePad, overlay, vLabel, idx, aLabel));
                idx++;
            }

            StringBuilder videoStreams = new StringBuilder();
            StringBuilder audioStreams = new StringBuilder();
            for (int i = 0; i < clips.size(); i++) {
                videoStreams.append("[").append("v").append(i).append("]");
                audioStreams.append("[").append("a").append(i).append("]");
            }

            // Concatenate processed clips
            String concat = String.format(Locale.US, "%s%sconcat=n=%d:v=1:a=1[vcat][acat]",
                    videoStreams, audioStreams, clips.size());

            // narration input is last (index = number of clip inputs)
            String narrationIn = String.format("[%d:a]", clips.size());

            // Build final filter graph with sidechain ducking
            String filterComplex = String.join(";",
                    String.join(";", filterParts),
                    concat,
                    narrationIn + "anull[nar]",
                    "[acat][nar]sidechaincompress=threshold=0.05:ratio=8:attack=5:release=200:makeup=3[mix]"
            );

            Path outFile = chooseSavePath(stage, portrait ? "output_portrait.mp4" : "output_landscape.mp4");
            if (outFile == null) { status.set("Export canceled."); return; }

            List<String> cmd = new ArrayList<>();
            cmd.add("ffmpeg");
            cmd.add("-y");
            // clip inputs
            if (!inputArgs.toString().isBlank()) {
                for (String s : inputArgs.toString().trim().split(" ")) if (!s.isBlank()) cmd.add(s);
            }
            // narration input
            cmd.add("-i"); cmd.add(narration.toAbsolutePath().toString());
            // filters & maps
            cmd.add("-filter_complex"); cmd.add(filterComplex);
            cmd.add("-map"); cmd.add("[vcat]");
            cmd.add("-map"); cmd.add("[mix]");
            // enc params
            cmd.add("-c:v"); cmd.add("libx264");
            cmd.add("-pix_fmt"); cmd.add("yuv420p");
            cmd.add("-crf"); cmd.add("18");
            cmd.add("-preset"); cmd.add("veryfast");
            cmd.add("-r"); cmd.add("30");
            cmd.add("-c:a"); cmd.add("aac");
            cmd.add("-b:a"); cmd.add("192k");
            cmd.add(outFile.toAbsolutePath().toString());

            status.set("Rendering… Ensure ffmpeg is installed and on PATH.");
            int exit = runProcess(cmd);
            status.set(exit == 0 ? "Export complete: " + outFile : "Export failed. Check ffmpeg output/paths.");
        } catch (Exception ex) {
            status.set("Export error: " + ex.getMessage());
        }
    }

    private List<ClipItem> collectAllClipsInOrder() {
        List<ClipItem> all = new ArrayList<>();
        for (TreeItem<SceneNode> ti : scenesRoot.getChildren()) {
            SceneKind k = ti.getValue().getKind();
            if (k == SceneKind.START || k == SceneKind.END || k == SceneKind.BODY) {
                all.addAll(ti.getValue().getClips());
                for (TreeItem<SceneNode> sub : ti.getChildren()) {
                    if (sub.getValue().getKind() == SceneKind.SUBSCENE) {
                        all.addAll(sub.getValue().getClips());
                    }
                }
            }
        }
        return all;
    }

    private static Path chooseSavePath(Stage stage, String suggestedName) throws IOException {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save Rendered Video");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("MP4", "*.mp4"));
        fc.setInitialFileName(suggestedName);
        File f = fc.showSaveDialog(stage);
        return (f != null) ? f.toPath() : null;
    }

    private static int runProcess(List<String> cmd) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) System.out.println(line);
        }
        return p.waitFor();
    }

    private static String quote(String s) { return "\"" + s.replace("\"", "\\\"") + "\""; }

    // --- Reuse helpers ---
    private static String baseName(String path) {
        if (path == null) return "";
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return (slash >= 0 ? path.substring(slash + 1) : path);
    }
    private int reuseCountForPath(String path) {
        int n = 0;
        for (ClipItem c : collectAllClipsInOrder()) {
            if (Objects.equals(c.getPath(), path)) n++;
        }
        return n;
    }

    // --- Data classes/services ---
    public static class ClipItem {
        private final StringProperty label = new SimpleStringProperty("Clip"); // quick purpose/name
        private final StringProperty path = new SimpleStringProperty();
        private final DoubleProperty inSec = new SimpleDoubleProperty(0.0);
        private final DoubleProperty outSec = new SimpleDoubleProperty(0.0); // 0 = till end
        private final StringProperty overlayText = new SimpleStringProperty("");

        public ClipItem(String path) { setPath(path); }

        public String getLabel() { return label.get(); }
        public void setLabel(String v) { label.set(v); }
        public StringProperty labelProperty() { return label; }

        public String getPath() { return path.get(); }
        public void setPath(String v) { path.set(v); }
        public StringProperty pathProperty() { return path; }

        public double getInSec() { return inSec.get(); }
        public DoubleProperty inSecProperty() { return inSec; }

        public double getOutSec() { return outSec.get(); }
        public DoubleProperty outSecProperty() { return outSec; }

        public String getOverlayText() { return overlayText.get(); }
        public StringProperty overlayTextProperty() { return overlayText; }
    }

    /** Replace with real TTS later. For now, writes a tiny WAV so the pipeline works. */
    public static class TTSService {
        private static Path last;

        public static Path generateDummyWav(String text) throws IOException {
            Path dir = Files.createTempDirectory("extord_tts");
            Path wav = dir.resolve("narration.wav");
            Path txt = dir.resolve("narration.txt");
            Files.writeString(txt, text == null ? "" : text);
            writeSilentWav(wav, 1.0); // 1s silence placeholder — swap with real TTS later
            last = wav;
            return wav;
        }

        public static Path getLatestDummyWav() { return last; }

        /** Write a 16-bit mono PCM WAV of silence for the given seconds. */
        private static void writeSilentWav(Path out, double seconds) throws IOException {
            int sampleRate = 16000;
            int bitsPerSample = 16;
            int channels = 1;
            int numSamples = (int) Math.round(sampleRate * seconds);
            int byteRate = sampleRate * channels * bitsPerSample / 8;
            int blockAlign = channels * bitsPerSample / 8;
            int dataSize = numSamples * blockAlign;
            int chunkSize = 36 + dataSize;

            try (OutputStream os = Files.newOutputStream(out);
                 DataOutputStream dos = new DataOutputStream(os)) {

                // RIFF header
                dos.writeBytes("RIFF");
                dos.write(intLE(chunkSize));
                dos.writeBytes("WAVE");

                // fmt subchunk
                dos.writeBytes("fmt ");
                dos.write(intLE(16));                      // PCM
                dos.write(shortLE((short) 1));             // AudioFormat = PCM
                dos.write(shortLE((short) channels));      // NumChannels
                dos.write(intLE(sampleRate));              // SampleRate
                dos.write(intLE(byteRate));                // ByteRate
                dos.write(shortLE((short) blockAlign));    // BlockAlign
                dos.write(shortLE((short) bitsPerSample)); // BitsPerSample

                // data subchunk
                dos.writeBytes("data");
                dos.write(intLE(dataSize));

                // PCM silence
                byte[] frame = new byte[blockAlign];
                for (int i = 0; i < numSamples; i++) dos.write(frame);
            }
        }

        private static byte[] intLE(int v) {
            return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array();
        }

        private static byte[] shortLE(short v) {
            return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(v).array();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
