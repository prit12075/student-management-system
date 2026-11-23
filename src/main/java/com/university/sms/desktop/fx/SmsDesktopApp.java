package com.university.sms.desktop.fx;

import com.university.sms.core.jdbc.ArchivedStudent;
import com.university.sms.core.jdbc.SqliteArchiveDao;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * UNIT 5 - JavaFX (Arindam): offline <b>Archive Viewer</b> desktop client.
 *
 * <p>Reads the SQLite archive written by the web console through {@link SqliteArchiveDao}
 * (Unit 4 JDBC) and shows it with JavaFX controls:</p>
 * <ul>
 *   <li>Application life cycle: {@link #init()}, {@link #start(Stage)}, {@link #stop()}</li>
 *   <li>Stage → Scene → layout panes ({@link BorderPane}, {@link VBox}, {@link HBox}, {@link SplitPane})</li>
 *   <li>{@link MenuBar} with {@link Menu}, {@link MenuItem}, {@link CheckMenuItem}, accelerators</li>
 *   <li>{@link TableView} with typed {@link TableColumn}s, a custom row factory and sorting</li>
 *   <li>{@link FilteredList} + property listeners for live search, {@link ComboBox} filter</li>
 *   <li>{@link BarChart} of department average SGPA</li>
 *   <li>{@link Task} - JDBC loading runs off the JavaFX Application Thread</li>
 * </ul>
 * Run: {@code ./mvnw -q compile exec:java -Dexec.mainClass=com.university.sms.desktop.fx.DesktopLauncher}
 */
public class SmsDesktopApp extends Application {

    private final ObservableList<ArchivedStudent> master = FXCollections.observableArrayList();
    private final FilteredList<ArchivedStudent> filtered = new FilteredList<>(master, s -> true);

    private final TextField searchField = new TextField();
    private final ComboBox<String> deptBox = new ComboBox<>();
    private final CheckMenuItem shortageOnly = new CheckMenuItem("Show only attendance shortage");
    private final Label statusLabel = new Label("Ready");
    private final Label countLabel = new Label();
    private final ProgressIndicator progress = new ProgressIndicator();
    private final BarChart<String, Number> chart = new BarChart<>(new CategoryAxis(), new NumberAxis(0, 10, 1));

    private SqliteArchiveDao dao;
    private Stage stage;

    @Override
    public void init() {
        // runs on the launcher thread before start(): pick the archive file
        List<String> args = getParameters().getRaw();
        Path db = args.isEmpty() ? Path.of("archive", "sms_archive.db") : Path.of(args.get(0));
        dao = new SqliteArchiveDao(db);
    }

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;

        BorderPane root = new BorderPane();
        root.setTop(new VBox(buildMenuBar(), buildToolbar()));
        root.setCenter(buildCenter());
        root.setBottom(buildStatusBar());

        Scene scene = new Scene(root, 1080, 640);
        primaryStage.setTitle("Apex University - Semester Archive Viewer (JavaFX)");
        primaryStage.setScene(scene);
        primaryStage.show();

        loadArchive();
    }

    @Override
    public void stop() {
        System.out.println("Archive viewer closed.");
    }

    // ------------------------------------------------------------------ UI builders

    private MenuBar buildMenuBar() {
        MenuItem open = new MenuItem("Open archive...");
        open.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));
        open.setOnAction(e -> chooseArchive());

        MenuItem reload = new MenuItem("Reload");
        reload.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN));
        reload.setOnAction(e -> loadArchive());

        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(e -> Platform.exit());

        Menu file = new Menu("File");
        file.getItems().addAll(open, reload, new SeparatorMenuItem(), exit);

        shortageOnly.setOnAction(e -> applyFilters());
        MenuItem clear = new MenuItem("Clear filters");
        clear.setOnAction(e -> {
            searchField.clear();
            deptBox.getSelectionModel().select("All");
            shortageOnly.setSelected(false);
            applyFilters();
        });
        Menu view = new Menu("View");
        view.getItems().addAll(shortageOnly, clear);

        MenuItem about = new MenuItem("About this archive");
        about.setOnAction(e -> showAbout());
        Menu help = new Menu("Help");
        help.getItems().add(about);

        return new MenuBar(file, view, help);
    }

    private HBox buildToolbar() {
        searchField.setPromptText("Search name or roll number...");
        searchField.textProperty().addListener((obs, oldText, newText) -> applyFilters());   // live search
        deptBox.getItems().add("All");
        deptBox.getSelectionModel().selectFirst();
        deptBox.setOnAction(e -> applyFilters());

        HBox bar = new HBox(10, new Label("Search:"), searchField, new Label("Department:"), deptBox, countLabel);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10));
        bar.setStyle("-fx-background-color: #e2e8f0;");
        return bar;
    }

    private SplitPane buildCenter() {
        TableView<ArchivedStudent> table = new TableView<>();
        table.getColumns().add(textColumn("Roll No", ArchivedStudent::rollNumber, 110));
        table.getColumns().add(textColumn("Name", ArchivedStudent::fullName, 170));
        table.getColumns().add(textColumn("Dept", ArchivedStudent::department, 60));
        table.getColumns().add(numberColumn("Sem", s -> s.semester(), 50));
        table.getColumns().add(numberColumn("Attendance %", ArchivedStudent::attendance, 100));
        table.getColumns().add(numberColumn("Average %", ArchivedStudent::average, 90));
        table.getColumns().add(numberColumn("SGPA", ArchivedStudent::sgpa, 60));
        table.getColumns().add(numberColumn("Backlogs", s -> s.backlogs(), 70));
        table.getColumns().add(textColumn("Status", ArchivedStudent::status, 90));

        SortedList<ArchivedStudent> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());   // header click sorts
        table.setItems(sorted);

        // custom row factory: shortage rows are tinted red
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(ArchivedStudent item, boolean empty) {
                super.updateItem(item, empty);
                setStyle(!empty && item != null && "SHORTAGE".equals(item.status())
                        ? "-fx-background-color: #fee2e2;" : "");
            }
        });
        table.setPlaceholder(new Label("Archive is empty. Take a snapshot from Admin → Java Lab Console → JDBC Archive."));

        chart.setTitle("Avg SGPA per department (SQL GROUP BY)");
        chart.setLegendVisible(false);
        chart.setAnimated(false);

        SplitPane split = new SplitPane(table, chart);
        split.setOrientation(Orientation.HORIZONTAL);
        split.setDividerPositions(0.60);
        return split;
    }

    private HBox buildStatusBar() {
        progress.setPrefSize(18, 18);
        progress.setVisible(false);
        HBox status = new HBox(8, progress, statusLabel);
        status.setPadding(new Insets(6, 10, 6, 10));
        status.setAlignment(Pos.CENTER_LEFT);
        return status;
    }

    /** Generic helper: builds a String column from any getter (method reference). */
    private static TableColumn<ArchivedStudent, String> textColumn(
            String title, java.util.function.Function<ArchivedStudent, String> getter, double width) {
        TableColumn<ArchivedStudent, String> col = new TableColumn<>(title);
        col.setCellValueFactory(cell -> new ReadOnlyStringWrapper(getter.apply(cell.getValue())));
        col.setPrefWidth(width);
        return col;
    }

    private static TableColumn<ArchivedStudent, Number> numberColumn(
            String title, java.util.function.Function<ArchivedStudent, Number> getter, double width) {
        TableColumn<ArchivedStudent, Number> col = new TableColumn<>(title);
        col.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(getter.apply(cell.getValue())));
        col.setPrefWidth(width);
        return col;
    }

    // ------------------------------------------------------------------ behaviour

    /** Loads rows + chart data on a background thread with a JavaFX Task. */
    private void loadArchive() {
        if (!Files.exists(dao.getDbFile())) {
            statusLabel.setText("No archive at " + dao.getDbFile().toAbsolutePath()
                    + " - take a snapshot in the web console first (File > Open to pick another).");
            return;
        }
        record Loaded(List<ArchivedStudent> rows, Map<String, Double> averages) {
        }
        Task<Loaded> task = new Task<>() {
            @Override
            protected Loaded call() throws Exception {          // runs on a worker thread
                updateMessage("Reading " + dao.getDbFile().getFileName() + " via JDBC...");
                return new Loaded(dao.findAllStudents(), dao.departmentAverageSgpa());
            }
        };
        progress.setVisible(true);
        statusLabel.textProperty().bind(task.messageProperty());
        task.setOnSucceeded(e -> {                               // back on the FX thread
            statusLabel.textProperty().unbind();
            progress.setVisible(false);
            Loaded loaded = task.getValue();
            master.setAll(loaded.rows());
            refreshDepartments();
            refreshChart(loaded.averages());
            applyFilters();
            statusLabel.setText("Loaded " + loaded.rows().size() + " students from " + dao.getUrl());
        });
        task.setOnFailed(e -> {
            statusLabel.textProperty().unbind();
            progress.setVisible(false);
            statusLabel.setText("Load failed: " + task.getException().getMessage());
        });
        Thread worker = new Thread(task, "archive-loader");
        worker.setDaemon(true);
        worker.start();
    }

    private void applyFilters() {
        String text = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String dept = deptBox.getValue();
        boolean onlyShort = shortageOnly.isSelected();
        filtered.setPredicate(s ->
                (text.isEmpty() || s.fullName().toLowerCase().contains(text) || s.rollNumber().toLowerCase().contains(text))
                        && (dept == null || "All".equals(dept) || dept.equals(s.department()))
                        && (!onlyShort || "SHORTAGE".equals(s.status())));
        countLabel.setText(filtered.size() + " of " + master.size() + " students");
    }

    private void refreshDepartments() {
        TreeSet<String> departments = new TreeSet<>();
        for (ArchivedStudent s : master) {
            departments.add(s.department());
        }
        String selected = deptBox.getValue();
        deptBox.getItems().setAll("All");
        deptBox.getItems().addAll(departments);
        deptBox.getSelectionModel().select(departments.contains(selected) ? selected : "All");
    }

    private void refreshChart(Map<String, Double> averages) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        averages.forEach((dept, avg) -> series.getData().add(new XYChart.Data<>(dept, avg)));
        chart.getData().setAll(List.of(series));
    }

    private void chooseArchive() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open SQLite archive");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite archive", "*.db"));
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            dao = new SqliteArchiveDao(file.toPath());
            loadArchive();
        }
    }

    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About this archive");
        alert.setHeaderText("SQLite archive - read with JDBC");
        try {
            var summary = dao.summary();
            alert.setContentText("Database: " + summary.databaseProduct() + " " + summary.databaseVersion()
                    + "\nDriver: " + summary.driverName()
                    + "\nTables: " + summary.rowCounts()
                    + "\nLast snapshot: " + summary.lastSnapshotAt()
                    + "\n\narchived_students columns:\n" + String.join("\n", dao.columnsOf("archived_students")));
        } catch (Exception ex) {
            alert.setContentText("Could not read archive: " + ex.getMessage());
        }
        alert.showAndWait();
    }
}
