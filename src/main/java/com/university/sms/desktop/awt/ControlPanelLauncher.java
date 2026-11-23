package com.university.sms.desktop.awt;

import com.university.sms.StudentManagementSystemApplication;
import com.university.sms.core.model.StudentRecord;
import com.university.sms.service.JavaLabService;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URI;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * UNIT 3 - Event handling with AWT + multithreading (Vivekanand).
 *
 * <p>A desktop control panel for the ERP server. It shows the <b>delegation event model</b>:
 * a source component (Button, Choice, Checkbox, TextField, Frame) generates an event object,
 * and a registered <b>listener</b> object handles it.</p>
 * <ul>
 *   <li>{@link ActionListener} - implemented by this class itself (Start button) and as lambdas</li>
 *   <li>{@link java.awt.event.ItemListener} - port {@link Choice} and "open browser" {@link Checkbox}</li>
 *   <li>{@link KeyAdapter} - anonymous inner class: Enter in the quick-lookup field</li>
 *   <li>{@link WindowAdapter} - closing the window stops the server cleanly</li>
 * </ul>
 * Spring Boot is started on a <b>background thread</b> so the AWT Event Dispatch Thread never
 * freezes; results are pushed back to the UI with {@link EventQueue#invokeLater(Runnable)}.
 *
 * <p>Run: {@code ./mvnw -q compile exec:java -Dexec.mainClass=com.university.sms.desktop.awt.ControlPanelLauncher}</p>
 */
public class ControlPanelLauncher extends Frame implements ActionListener {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Label statusLabel = new Label("Server: STOPPED", Label.CENTER);
    private final Choice portChoice = new Choice();
    private final Checkbox openBrowser = new Checkbox("Open browser when ready", true);
    private final Button startButton = new Button("Start Server");
    private final Button stopButton = new Button("Stop Server");
    private final Button openButton = new Button("Open Portal");
    private final Button clearButton = new Button("Clear Log");
    private final TextField lookupField = new TextField(18);
    private final TextArea logArea = new TextArea("", 16, 70, TextArea.SCROLLBARS_VERTICAL_ONLY);

    private volatile ConfigurableApplicationContext context;   // written by the server thread
    private String pendingLookup;                                // optional --lookup=ROLL argument

    public ControlPanelLauncher() {
        super("Apex University ERP - Control Panel (AWT)");
        setLayout(new BorderLayout(8, 8));
        setBackground(new Color(241, 245, 249));

        // ---------- north: title + status ----------
        Panel north = new Panel(new GridLayout(2, 1));
        Label title = new Label("Student Management System - Server Control", Label.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        statusLabel.setForeground(new Color(185, 28, 28));
        north.add(title);
        north.add(statusLabel);
        add(north, BorderLayout.NORTH);

        // ---------- center: log ----------
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(logArea, BorderLayout.CENTER);

        // ---------- south: controls ----------
        Panel controls = new Panel(new FlowLayout(FlowLayout.CENTER, 8, 6));
        portChoice.add("8080");
        portChoice.add("8081");
        portChoice.add("9090");
        controls.add(new Label("Port:"));
        controls.add(portChoice);
        controls.add(openBrowser);
        controls.add(startButton);
        controls.add(stopButton);
        controls.add(openButton);
        controls.add(clearButton);

        Panel lookup = new Panel(new FlowLayout(FlowLayout.CENTER, 8, 6));
        lookup.add(new Label("Quick student lookup (roll no + Enter):"));
        lookup.add(lookupField);

        Panel south = new Panel(new GridLayout(2, 1));
        south.add(controls);
        south.add(lookup);
        add(south, BorderLayout.SOUTH);

        registerListeners();
        stopButton.setEnabled(false);
        openButton.setEnabled(false);
        setSize(820, 560);
        setLocationRelativeTo(null);
        log("Control panel ready. Choose a port and press 'Start Server'.");
    }

    private void registerListeners() {
        // 1) this class implements ActionListener -> actionPerformed(...) below
        startButton.addActionListener(this);

        // 2) lambda expressions (ActionListener is a functional interface)
        stopButton.addActionListener(e -> stopServer());
        openButton.addActionListener(e -> openPortal());
        clearButton.addActionListener(e -> logArea.setText(""));

        // 3) ItemListener for Choice and Checkbox
        portChoice.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                log("Port changed to " + portChoice.getSelectedItem());
            }
        });
        openBrowser.addItemListener(e -> log("Open browser automatically: " + openBrowser.getState()));

        // 4) anonymous inner class extending an adapter class (only keyPressed is needed)
        lookupField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    lookupStudent(lookupField.getText());
                }
            }
        });

        // 5) WindowAdapter: react to the close button of the window
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                log("Window closing - shutting down...");
                stopServer();
                dispose();
                System.exit(0);
            }
        });
    }

    /** Handles the Start button (ActionListener implemented by the frame). */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == startButton) {
            startServer();
        }
    }

    private void startServer() {
        if (context != null) {
            log("Server is already running.");
            return;
        }
        String port = portChoice.getSelectedItem();
        startButton.setEnabled(false);
        setStatus("Server: STARTING on port " + port + " ...", new Color(180, 83, 9));
        log("Starting Spring Boot on a background thread (port " + port + ")");

        Thread serverThread = new Thread(() -> {
            try {
                ConfigurableApplicationContext ctx = new SpringApplicationBuilder(StudentManagementSystemApplication.class)
                        .headless(false)                         // keep AWT usable in the same JVM
                        .run("--server.port=" + port);       // command-line arg overrides application.properties
                context = ctx;
                String serverThreadName = Thread.currentThread().getName();
                EventQueue.invokeLater(() -> {               // back to the Event Dispatch Thread
                    setStatus("Server: RUNNING at http://localhost:" + port, new Color(21, 128, 61));
                    stopButton.setEnabled(true);
                    openButton.setEnabled(true);
                    log("Server started on thread '" + serverThreadName + "', UI updated on '"
                            + Thread.currentThread().getName() + "'");
                    if (openBrowser.getState()) {
                        openPortal();
                    }
                    if (pendingLookup != null) {
                        lookupStudent(pendingLookup);
                    }
                });
            } catch (Exception ex) {
                EventQueue.invokeLater(() -> {
                    setStatus("Server: FAILED", new Color(185, 28, 28));
                    startButton.setEnabled(true);
                    log("Start failed: " + ex.getMessage());
                });
            }
        }, "spring-boot-server");
        serverThread.setDaemon(false);
        serverThread.start();
    }

    private void stopServer() {
        ConfigurableApplicationContext ctx = context;
        if (ctx == null) {
            return;
        }
        context = null;
        ctx.close();
        setStatus("Server: STOPPED", new Color(185, 28, 28));
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        openButton.setEnabled(false);
        log("Server stopped.");
    }

    private void openPortal() {
        String url = "http://localhost:" + portChoice.getSelectedItem() + "/login";
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                log("Opened " + url);
            } else {
                log("Open this address in your browser: " + url);
            }
        } catch (Exception ex) {
            log("Could not open browser: " + ex.getMessage());
        }
    }

    /** Uses the running Spring context to look up a student by roll number. */
    private void lookupStudent(String roll) {
        ConfigurableApplicationContext ctx = context;
        if (ctx == null) {
            log("Start the server first.");
            return;
        }
        String key = roll.trim().toUpperCase();
        List<StudentRecord> records = ctx.getBean(JavaLabService.class).loadStudentRecords();
        records.stream()
                .filter(r -> r.getRoll().equals(key))
                .findFirst()
                .ifPresentOrElse(
                        r -> log("FOUND  " + r.toReportLine()),
                        () -> log("No student with roll number " + key));
        lookupField.setText("");
    }

    private void setStatus(String text, Color color) {
        statusLabel.setText(text);
        statusLabel.setForeground(color);
    }

    private void log(String message) {
        logArea.append("[" + LocalTime.now().format(TIME) + "] " + message + "\n");
    }

    /**
     * Optional command-line arguments: {@code --port=8081} pre-selects the port and
     * {@code --autostart} presses "Start Server" automatically and {@code --lookup=ROLL}
     * runs one quick lookup once the server is up.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            ControlPanelLauncher panel = new ControlPanelLauncher();
            panel.setVisible(true);
            for (String arg : args) {
                if (arg.startsWith("--port=")) {
                    panel.portChoice.select(arg.substring("--port=".length()));
                } else if (arg.startsWith("--lookup=")) {
                    panel.pendingLookup = arg.substring("--lookup=".length());
                }
            }
            for (String arg : args) {
                if (arg.equals("--autostart")) {
                    panel.startServer();
                }
            }
        });
    }
}
