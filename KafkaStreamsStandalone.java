import org.apache.kafka.clients.producer.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class KafkaStreamsStandalone {
    
    private static String BOOTSTRAP_SERVERS = "";
    private static String USERNAME = "";
    private static String PASSWORD = "";
    private static String INPUT_TOPIC = "kstreams-topic1";
    private static String OUTPUT_TOPIC = "kstreams-topic2";
    private static String APPLICATION_ID = "kafka-streams-app";
    private static String CLIENT_ID = "streams-client-1";
    private static boolean USE_STREAMS_PROTOCOL = false;
    private static final String STATE_DIR = System.getProperty("java.io.tmpdir") + File.separator + "kafka-streams-1";
    private static final String STATE_DIR_2 = System.getProperty("java.io.tmpdir") + File.separator + "kafka-streams-2";
    private static final CountDownLatch latch = new CountDownLatch(1);
    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static final AtomicBoolean starting = new AtomicBoolean(false);
    private static final Object startupLock = new Object();
    private static KafkaStreams streams;
    private static Thread producerThread;
    private static Thread consumerThread;
    private static JTextArea logArea;
    private static JButton startButton;
    private static JButton stopButton;
    
    // Second instance variables
    private static final AtomicBoolean running2 = new AtomicBoolean(false);
    private static final AtomicBoolean starting2 = new AtomicBoolean(false);
    private static final Object startupLock2 = new Object();
    private static KafkaStreams streams2;
    private static Thread consumerThread2;
    private static JButton startButton2;
    private static JButton stopButton2;
    private static String CLIENT_ID_2 = "streams-client-2";

    public static void main(String[] args) {
        // Check if running with CLI arguments
        if (args.length > 0 && args[0].equals("--cli")) {
            runCLIMode(args);
        } else {
            runGUIMode();
        }
    }

    private static void runGUIMode() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        JFrame frame = new JFrame("Kafka Streams Application");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());

        // Configuration Panel
        JPanel configPanel = new JPanel(new GridBagLayout());
        configPanel.setBorder(BorderFactory.createTitledBorder("Configuration"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Bootstrap Servers
        gbc.gridx = 0; gbc.gridy = 0;
        configPanel.add(new JLabel("Bootstrap Servers:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        JTextField bootstrapField = new JTextField("localhost:9092", 30);
        configPanel.add(bootstrapField, gbc);

        // Username
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        configPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        JTextField usernameField = new JTextField(30);
        configPanel.add(usernameField, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        configPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        JPasswordField passwordField = new JPasswordField(30);
        configPanel.add(passwordField, gbc);

        // Input Topic
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1;
        configPanel.add(new JLabel("Input Topic:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        JTextField inputTopicField = new JTextField("kstreams-topic1", 30);
        configPanel.add(inputTopicField, gbc);

        // Output Topic
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1;
        configPanel.add(new JLabel("Output Topic:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        JTextField outputTopicField = new JTextField("kstreams-topic2", 30);
        configPanel.add(outputTopicField, gbc);

        // Application ID
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 1;
        configPanel.add(new JLabel("Application ID:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        JTextField appIdField = new JTextField("kafka-streams-app", 30);
        configPanel.add(appIdField, gbc);

        // Client ID
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 1;
        configPanel.add(new JLabel("Client ID:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        JTextField clientIdField = new JTextField("streams-client-1", 30);
        configPanel.add(clientIdField, gbc);

        // Group Protocol
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 1;
        configPanel.add(new JLabel("Use Streams Protocol:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        JCheckBox streamsProtocolCheckbox = new JCheckBox("Set group.protocol to 'streams'");
        streamsProtocolCheckbox.setSelected(false);
        configPanel.add(streamsProtocolCheckbox, gbc);

        // Control buttons
        JPanel buttonPanel = new JPanel();
        startButton = new JButton("Start");
        stopButton = new JButton("Stop");
        stopButton.setEnabled(false);
        
        JButton saveConfigButton = new JButton("Save Config");
        JButton loadConfigButton = new JButton("Load Config");
        JButton downloadLogsButton = new JButton("Download Logs");

        startButton.addActionListener(e -> {
            BOOTSTRAP_SERVERS = bootstrapField.getText();
            USERNAME = usernameField.getText();
            PASSWORD = new String(passwordField.getPassword());
            INPUT_TOPIC = inputTopicField.getText();
            OUTPUT_TOPIC = outputTopicField.getText();
            APPLICATION_ID = appIdField.getText();
            CLIENT_ID = clientIdField.getText();
            USE_STREAMS_PROTOCOL = streamsProtocolCheckbox.isSelected();
            
            if (BOOTSTRAP_SERVERS.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Bootstrap Servers is required!");
                return;
            }
            
            startApplication();
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            
            // Disable fields while running
            bootstrapField.setEnabled(false);
            usernameField.setEnabled(false);
            passwordField.setEnabled(false);
            inputTopicField.setEnabled(false);
            outputTopicField.setEnabled(false);
            appIdField.setEnabled(false);
            clientIdField.setEnabled(false);
            streamsProtocolCheckbox.setEnabled(false);
        });

        stopButton.addActionListener(e -> {
            stopApplication();
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            
            // Re-enable fields
            bootstrapField.setEnabled(true);
            usernameField.setEnabled(true);
            passwordField.setEnabled(true);
            inputTopicField.setEnabled(true);
            outputTopicField.setEnabled(true);
            appIdField.setEnabled(true);
            clientIdField.setEnabled(true);
            streamsProtocolCheckbox.setEnabled(true);
        });

        saveConfigButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File("kafka-config.properties"));
            if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                try {
                    Properties props = new Properties();
                    props.setProperty("bootstrap.servers", bootstrapField.getText());
                    props.setProperty("username", usernameField.getText());
                    props.setProperty("password", new String(passwordField.getPassword()));
                    props.setProperty("input.topic", inputTopicField.getText());
                    props.setProperty("output.topic", outputTopicField.getText());
                    props.setProperty("application.id", appIdField.getText());
                    props.setProperty("client.id", clientIdField.getText());
                    props.setProperty("use.streams.protocol", String.valueOf(streamsProtocolCheckbox.isSelected()));
                    
                    try (FileOutputStream out = new FileOutputStream(fileChooser.getSelectedFile())) {
                        props.store(out, "Kafka Streams Configuration");
                    }
                    log("Configuration saved to " + fileChooser.getSelectedFile());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Error saving configuration: " + ex.getMessage());
                }
            }
        });

        loadConfigButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                try {
                    Properties props = new Properties();
                    try (FileInputStream in = new FileInputStream(fileChooser.getSelectedFile())) {
                        props.load(in);
                    }
                    bootstrapField.setText(props.getProperty("bootstrap.servers", ""));
                    usernameField.setText(props.getProperty("username", ""));
                    passwordField.setText(props.getProperty("password", ""));
                    inputTopicField.setText(props.getProperty("input.topic", "kstreams-topic1"));
                    outputTopicField.setText(props.getProperty("output.topic", "kstreams-topic2"));
                    appIdField.setText(props.getProperty("application.id", "kafka-streams-app"));
                    clientIdField.setText(props.getProperty("client.id", "streams-client-1"));
                    streamsProtocolCheckbox.setSelected(Boolean.parseBoolean(props.getProperty("use.streams.protocol", "false")));
                    log("Configuration loaded from " + fileChooser.getSelectedFile());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Error loading configuration: " + ex.getMessage());
                }
            }
        });

        downloadLogsButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File("kafka-streams-logs-" + 
                new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".txt"));
            if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                try (FileWriter writer = new FileWriter(fileChooser.getSelectedFile())) {
                    writer.write(logArea.getText());
                    JOptionPane.showMessageDialog(frame, "Logs exported successfully to " + fileChooser.getSelectedFile().getName());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Error exporting logs: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(saveConfigButton);
        buttonPanel.add(loadConfigButton);
        buttonPanel.add(downloadLogsButton);

        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 3;
        configPanel.add(buttonPanel, gbc);

        // Second instance panel
        JPanel instance2Panel = new JPanel();
        instance2Panel.setBorder(BorderFactory.createTitledBorder("Second Instance"));
        startButton2 = new JButton("Start Instance 2");
        stopButton2 = new JButton("Stop Instance 2");
        stopButton2.setEnabled(false);

        startButton2.addActionListener(e -> {
            if (BOOTSTRAP_SERVERS.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Please configure and start the first instance!");
                return;
            }
            startApplication2();
            startButton2.setEnabled(false);
            stopButton2.setEnabled(true);
        });

        stopButton2.addActionListener(e -> {
            stopApplication2();
            startButton2.setEnabled(true);
            stopButton2.setEnabled(false);
        });

        instance2Panel.add(startButton2);
        instance2Panel.add(stopButton2);

        gbc.gridx = 0; gbc.gridy = 9; gbc.gridwidth = 3;
        configPanel.add(instance2Panel, gbc);

        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Logs"));

        mainPanel.add(configPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        frame.add(mainPanel);
        frame.setVisible(true);

        // Redirect console output to text area
        redirectConsoleOutput();
        
        log("Kafka Streams Application Ready");
        log("Enter your configuration and click 'Start' to begin");
    }

    private static void runCLIMode(String[] args) {
        // Parse command line arguments
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--bootstrap-servers=")) {
                BOOTSTRAP_SERVERS = arg.substring(20);
            } else if (arg.startsWith("--username=")) {
                USERNAME = arg.substring(11);
            } else if (arg.startsWith("--password=")) {
                PASSWORD = arg.substring(11);
            } else if (arg.startsWith("--input-topic=")) {
                INPUT_TOPIC = arg.substring(14);
            } else if (arg.startsWith("--output-topic=")) {
                OUTPUT_TOPIC = arg.substring(15);
            } else if (arg.startsWith("--application-id=")) {
                APPLICATION_ID = arg.substring(17);
            } else if (arg.startsWith("--client-id=")) {
                CLIENT_ID = arg.substring(12);
            } else if (arg.equals("--use-streams-protocol")) {
                USE_STREAMS_PROTOCOL = true;
            } else if (arg.equals("--help")) {
                printHelp();
                System.exit(0);
            }
        }

        if (BOOTSTRAP_SERVERS.isEmpty()) {
            System.err.println("Error: --bootstrap-servers is required");
            printHelp();
            System.exit(1);
        }

        System.out.println("Starting Kafka Streams Application in CLI mode");
        System.out.println("Bootstrap Servers: " + BOOTSTRAP_SERVERS);
        System.out.println("Input Topic: " + INPUT_TOPIC);
        System.out.println("Output Topic: " + OUTPUT_TOPIC);
        
        startApplication();
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            stopApplication();
        }));
        
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void printHelp() {
        System.out.println("Kafka Streams Standalone Application");
        System.out.println("\nUsage:");
        System.out.println("  GUI Mode (default): java -jar kafka-streams-standalone.jar");
        System.out.println("  CLI Mode: java -jar kafka-streams-standalone.jar --cli [options]");
        System.out.println("\nCLI Options:");
        System.out.println("  --bootstrap-servers=<servers>  Kafka bootstrap servers (required)");
        System.out.println("  --username=<username>          SASL username (optional)");
        System.out.println("  --password=<password>          SASL password (optional)");
        System.out.println("  --input-topic=<topic>          Input topic (default: kstreams-topic1)");
        System.out.println("  --output-topic=<topic>         Output topic (default: kstreams-topic2)");
        System.out.println("  --application-id=<id>          Application ID (default: kafka-streams-app)");
        System.out.println("  --client-id=<id>               Client ID (default: streams-client-1)");
        System.out.println("  --use-streams-protocol         Set group.protocol to 'streams'");
        System.out.println("  --help                         Show this help message");
    }

    private static void redirectConsoleOutput() {
        PrintStream printStream = new PrintStream(new OutputStream() {
            private StringBuilder buffer = new StringBuilder();
            
            @Override
            public void write(int b) {
                if (b == '\n') {
                    String line = buffer.toString();
                    SwingUtilities.invokeLater(() -> {
                        logArea.append(line + "\n");
                        logArea.setCaretPosition(logArea.getDocument().getLength());
                    });
                    buffer.setLength(0);
                } else {
                    buffer.append((char) b);
                }
            }
        });
        System.setOut(printStream);
        System.setErr(printStream);
    }

    private static void log(String message) {
        String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new Date());
        String logMessage = "[" + timestamp + "] " + message + "\n";
        
        if (logArea != null) {
            SwingUtilities.invokeLater(() -> {
                logArea.append(logMessage);
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });
        } else {
            System.out.print(logMessage);
        }
    }

    private static void cleanupStateDirectory(String stateDirPath) {
        int maxRetries = 3;
        int retryDelayMs = 500;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Path path = Paths.get(stateDirPath);
                if (!Files.exists(path)) {
                    return; // Nothing to clean
                }
                
                boolean allDeleted = true;
                java.util.List<Path> paths = new java.util.ArrayList<>();
                Files.walk(path).sorted(java.util.Comparator.reverseOrder()).forEach(paths::add);
                
                for (Path p : paths) {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        allDeleted = false;
                        if (attempt == maxRetries) {
                            log("Warning: Could not delete " + p + ": " + e.getMessage());
                        }
                    }
                }
                
                if (allDeleted) {
                    log("Cleaned up state directory: " + stateDirPath);
                    return;
                } else if (attempt < maxRetries) {
                    log("Retry cleanup attempt " + (attempt + 1) + " of " + maxRetries + "...");
                    Thread.sleep(retryDelayMs);
                }
            } catch (IOException e) {
                if (attempt == maxRetries) {
                    log("Warning: Could not cleanup state directory " + stateDirPath + ": " + e.getMessage());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log("Cleanup interrupted for: " + stateDirPath);
                return;
            }
        }
    }

    private static void startApplication() {
        if (!starting.compareAndSet(false, true)) {
            log("Application is already starting...");
            return;
        }
        
        running.set(true);
        
        new Thread(() -> {
            synchronized (startupLock) {
                try {
                    Properties streamProps = createStreamProperties();
                    
                    if (!running.get()) {
                        log("Startup cancelled");
                        starting.set(false);
                        return;
                    }
                    
                    StreamsBuilder builder = new StreamsBuilder();
                    KStream<String, String> source = builder.stream(INPUT_TOPIC);
                    source.mapValues(value -> value.toUpperCase()).to(OUTPUT_TOPIC);

                    streams = new KafkaStreams(builder.build(), streamProps);

                    streams.setUncaughtExceptionHandler(e -> {
                        log("ERROR: " + e.getMessage());
                        return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD;
                    });

                    streams.setStateListener((newState, oldState) -> {
                        log("Instance 1 State: " + oldState + " -> " + newState);
                        if (newState == org.apache.kafka.streams.KafkaStreams.State.RUNNING) {
                            log("Instance 1 is now processing partitions");
                        }
                    });

                    if (!running.get()) {
                        log("Startup cancelled, cleaning up...");
                        streams.close(Duration.ofSeconds(5));
                        streams = null;
                        starting.set(false);
                        return;
                    }

                    // Start producer and consumer
                    producerThread = new Thread(() -> produceMessages());
                    consumerThread = new Thread(() -> consumeMessages());
                    
                    producerThread.start();
                    consumerThread.start();
                    
                    log("=== Instance 1 Started ===");
                    log("Role: Producer + Processor");
                    log("Waiting for partition assignment...");
                    
                    streams.start();
                    starting.set(false);
                    log("Kafka Streams Instance 1 started successfully! (Producing test data and processing partitions)");
                    
                } catch (Exception e) {
                    log("ERROR starting application: " + e.getMessage());
                    e.printStackTrace();
                    starting.set(false);
                    stopApplication();
                }
            }
        }).start();
    }

    private static void stopApplication() {
        log("Stopping Instance 1...");
        running.set(false);
        
        // Wait for startup to complete if still in progress
        if (starting.get()) {
            log("Waiting for startup to complete...");
            synchronized (startupLock) {
                // Just wait for the lock to be released
            }
        }
        
        // Step 1: Close Kafka Streams first to gracefully leave the consumer group
        if (streams != null) {
            try {
                log("Closing Kafka Streams Instance 1...");
                streams.close(Duration.ofSeconds(30));
                log("Kafka Streams Instance 1 closed successfully");
            } catch (Exception e) {
                log("Warning: Error closing Kafka Streams Instance 1: " + e.getMessage());
            }
            streams = null;
        }
        
        // Step 2: Wait for producer thread to stop gracefully
        if (producerThread != null && producerThread.isAlive()) {
            try {
                log("Waiting for producer thread to stop...");
                producerThread.join(5000); // Wait up to 5 seconds
                if (producerThread.isAlive()) {
                    producerThread.interrupt();
                    producerThread.join(2000); // Wait another 2 seconds after interrupt
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            producerThread = null;
        }
        
        // Step 3: Wait for consumer thread to stop gracefully
        if (consumerThread != null && consumerThread.isAlive()) {
            try {
                log("Waiting for consumer thread to stop...");
                consumerThread.join(5000);
                if (consumerThread.isAlive()) {
                    consumerThread.interrupt();
                    consumerThread.join(2000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            consumerThread = null;
        }
        
        // Step 4: Clean up state directory after everything is closed
        try {
            Thread.sleep(1000); // Small delay to ensure all file handles are released
            cleanupStateDirectory(STATE_DIR);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        log("=== Instance 1 Stopped ===");
    }

    private static Properties createStreamProperties() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, APPLICATION_ID);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(StreamsConfig.STATE_DIR_CONFIG, STATE_DIR);
        
        if (!USERNAME.isEmpty() && !PASSWORD.isEmpty()) {
            props.put("security.protocol", "SASL_SSL");
            props.put("sasl.mechanism", "PLAIN");
            props.put("sasl.jaas.config",
                    "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                    "username=\"" + USERNAME + "\" password=\"" + PASSWORD + "\";");
        }

        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.CLIENT_ID_CONFIG, CLIENT_ID);
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.AT_LEAST_ONCE);
        
        if (USE_STREAMS_PROTOCOL) {
            props.put("group.protocol", "streams");
        }
        
        // Timeout configurations
        props.put(StreamsConfig.REQUEST_TIMEOUT_MS_CONFIG, "60000");
        props.put(StreamsConfig.RETRY_BACKOFF_MS_CONFIG, "2000");
        
        return props;
    }

    private static void produceMessages() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        
        if (!USERNAME.isEmpty() && !PASSWORD.isEmpty()) {
            props.put("security.protocol", "SASL_SSL");
            props.put("sasl.mechanism", "PLAIN");
            props.put("sasl.jaas.config",
                    "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                    "username=\"" + USERNAME + "\" password=\"" + PASSWORD + "\";");
        }
        
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, 
                  "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, 
                  "org.apache.kafka.common.serialization.StringSerializer");

        Producer<String, String> producer = null;
        try {
            producer = new KafkaProducer<>(props);
            int count = 0;
            while (running.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    String key = "key" + count;
                    String value = "value" + count;
                    ProducerRecord<String, String> record = new ProducerRecord<>(INPUT_TOPIC, key, value);
                    
                    producer.send(record, (metadata, ex) -> {
                        if (ex == null) {
                            log("[>>] Produced: " + key + " -> partition " + metadata.partition());
                        } else {
                            log("[!!] Producer error: " + ex.getMessage());
                        }
                    });
                    
                    count++;
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
            if (running.get()) {
                log("Producer error: " + e.getMessage());
            }
        } finally {
            if (producer != null) {
                try {
                    producer.close();
                    log("Producer closed");
                } catch (Exception e) {
                    log("Warning: Error closing producer: " + e.getMessage());
                }
            }
        }
    }

    private static void startApplication2() {
        if (!starting2.compareAndSet(false, true)) {
            log("Instance 2 is already starting...");
            return;
        }
        
        running2.set(true);
        
        new Thread(() -> {
            synchronized (startupLock2) {
                try {
                    Properties streamProps = createStreamProperties2();
                    
                    if (!running2.get()) {
                        log("Instance 2 startup cancelled");
                        starting2.set(false);
                        return;
                    }
                    
                    StreamsBuilder builder = new StreamsBuilder();
                    KStream<String, String> source = builder.stream(INPUT_TOPIC);
                    source.mapValues(value -> value.toUpperCase()).to(OUTPUT_TOPIC);

                    streams2 = new KafkaStreams(builder.build(), streamProps);

                    streams2.setUncaughtExceptionHandler(e -> {
                        log("ERROR (Instance 2): " + e.getMessage());
                        return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD;
                    });

                    streams2.setStateListener((newState, oldState) -> {
                        log("Instance 2 State: " + oldState + " -> " + newState);
                        if (newState == org.apache.kafka.streams.KafkaStreams.State.RUNNING) {
                            log("Instance 2 is now processing partitions");
                        } else if (newState == org.apache.kafka.streams.KafkaStreams.State.REBALANCING) {
                            log("Instance 2 rebalancing - redistributing partitions...");
                        }
                    });

                    if (!running2.get()) {
                        log("Instance 2 startup cancelled, cleaning up...");
                        streams2.close(Duration.ofSeconds(5));
                        streams2 = null;
                        starting2.set(false);
                        return;
                    }

                    // Start consumer
                    log("=== Instance 2 Started ===");
                    log("Role: Processor only (cooperative with Instance 1)");
                    log("Waiting for partition assignment...");
                    consumerThread2 = new Thread(() -> consumeMessages2());
                    consumerThread2.start();
                    
                    streams2.start();
                    starting2.set(false);
                    log("Kafka Streams Instance 2 started successfully! (Processing partitions cooperatively with Instance 1)");
                    
                } catch (Exception e) {
                    log("ERROR starting Instance 2: " + e.getMessage());
                    e.printStackTrace();
                    starting2.set(false);
                    stopApplication2();
                }
            }
        }).start();
    }

    private static void stopApplication2() {
        log("Stopping Instance 2...");
        running2.set(false);
        
        // Wait for startup to complete if still in progress
        if (starting2.get()) {
            log("Waiting for Instance 2 startup to complete...");
            synchronized (startupLock2) {
                // Just wait for the lock to be released
            }
        }
        
        // Step 1: Close Kafka Streams first to gracefully leave the consumer group
        if (streams2 != null) {
            try {
                log("Closing Kafka Streams Instance 2...");
                streams2.close(Duration.ofSeconds(30));
                log("Kafka Streams Instance 2 closed successfully");
            } catch (Exception e) {
                log("Warning: Error closing Kafka Streams Instance 2: " + e.getMessage());
            }
            streams2 = null;
        }
        
        // Step 2: Wait for consumer thread to stop gracefully
        if (consumerThread2 != null && consumerThread2.isAlive()) {
            try {
                log("Waiting for Instance 2 consumer thread to stop...");
                consumerThread2.join(5000);
                if (consumerThread2.isAlive()) {
                    consumerThread2.interrupt();
                    consumerThread2.join(2000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            consumerThread2 = null;
        }
        
        // Step 3: Clean up state directory after everything is closed
        try {
            Thread.sleep(1000); // Small delay to ensure all file handles are released
            cleanupStateDirectory(STATE_DIR_2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        log("=== Instance 2 Stopped ===");
    }

    private static Properties createStreamProperties2() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, APPLICATION_ID);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(StreamsConfig.STATE_DIR_CONFIG, STATE_DIR_2);
        
        if (!USERNAME.isEmpty() && !PASSWORD.isEmpty()) {
            props.put("security.protocol", "SASL_SSL");
            props.put("sasl.mechanism", "PLAIN");
            props.put("sasl.jaas.config",
                    "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                    "username=\"" + USERNAME + "\" password=\"" + PASSWORD + "\";");
        }

        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.CLIENT_ID_CONFIG, CLIENT_ID_2);
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.AT_LEAST_ONCE);
        
        if (USE_STREAMS_PROTOCOL) {
            props.put("group.protocol", "streams");
        }
        
        // Timeout configurations
        props.put(StreamsConfig.REQUEST_TIMEOUT_MS_CONFIG, "60000");
        props.put(StreamsConfig.RETRY_BACKOFF_MS_CONFIG, "2000");
        
        return props;
    }

    private static void consumeMessages2() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "consumer-2-" + UUID.randomUUID().toString().substring(0, 8));
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        
        if (!USERNAME.isEmpty() && !PASSWORD.isEmpty()) {
            props.put("security.protocol", "SASL_SSL");
            props.put("sasl.mechanism", "PLAIN");
            props.put("sasl.jaas.config",
                    "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                    "username=\"" + USERNAME + "\" password=\"" + PASSWORD + "\";");
        }
        
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, 
                  "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, 
                  "org.apache.kafka.common.serialization.StringDeserializer");

        Consumer<String, String> consumer = null;
        try {
            consumer = new KafkaConsumer<>(props);
            consumer.subscribe(Collections.singletonList(OUTPUT_TOPIC));
            
            while (running2.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                    for (ConsumerRecord<String, String> record : records) {
                        log("  [<<] Instance 2 output: " + record.key() + " = " + record.value() + " (partition " + record.partition() + ")");
                    }
                } catch (org.apache.kafka.common.errors.InterruptException e) {
                    break;
                }
            }
        } catch (Exception e) {
            if (running2.get()) {
                log("Instance 2 Consumer error: " + e.getMessage());
            }
        } finally {
            if (consumer != null) {
                try {
                    consumer.close();
                    log("Instance 2 Consumer closed");
                } catch (Exception e) {
                    log("Warning: Error closing Instance 2 consumer: " + e.getMessage());
                }
            }
        }
    }

    private static void consumeMessages() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "consumer-" + UUID.randomUUID().toString().substring(0, 8));
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        
        if (!USERNAME.isEmpty() && !PASSWORD.isEmpty()) {
            props.put("security.protocol", "SASL_SSL");
            props.put("sasl.mechanism", "PLAIN");
            props.put("sasl.jaas.config",
                    "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                    "username=\"" + USERNAME + "\" password=\"" + PASSWORD + "\";");
        }
        
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, 
                  "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, 
                  "org.apache.kafka.common.serialization.StringDeserializer");

        Consumer<String, String> consumer = null;
        try {
            consumer = new KafkaConsumer<>(props);
            consumer.subscribe(Collections.singletonList(OUTPUT_TOPIC));
            
            while (running.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                    for (ConsumerRecord<String, String> record : records) {
                        log("  [<<] Instance 1 output: " + record.key() + " = " + record.value() + " (partition " + record.partition() + ")");
                    }
                } catch (org.apache.kafka.common.errors.InterruptException e) {
                    break;
                }
            }
        } catch (Exception e) {
            if (running.get()) {
                log("Consumer error: " + e.getMessage());
            }
        } finally {
            if (consumer != null) {
                try {
                    consumer.close();
                    log("Consumer closed");
                } catch (Exception e) {
                    log("Warning: Error closing consumer: " + e.getMessage());
                }
            }
        }
    }
}
