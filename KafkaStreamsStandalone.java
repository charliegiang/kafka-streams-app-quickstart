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
import java.awt.event.*;
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
    private static String INPUT_TOPIC = "kstreams-topic3";
    private static String OUTPUT_TOPIC = "kstreams-topic4";
    private static final CountDownLatch latch = new CountDownLatch(1);
    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static KafkaStreams streams;
    private static Thread producerThread;
    private static Thread consumerThread;
    private static JTextArea logArea;
    private static JButton startButton;
    private static JButton stopButton;

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
        configPanel.add(new JLabel("Username (optional):"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        JTextField usernameField = new JTextField(30);
        configPanel.add(usernameField, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1;
        configPanel.add(new JLabel("Password (optional):"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        JPasswordField passwordField = new JPasswordField(30);
        configPanel.add(passwordField, gbc);

        // Input Topic
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1;
        configPanel.add(new JLabel("Input Topic:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        JTextField inputTopicField = new JTextField("kstreams-topic3", 30);
        configPanel.add(inputTopicField, gbc);

        // Output Topic
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1;
        configPanel.add(new JLabel("Output Topic:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        JTextField outputTopicField = new JTextField("kstreams-topic4", 30);
        configPanel.add(outputTopicField, gbc);

        // Control buttons
        JPanel buttonPanel = new JPanel();
        startButton = new JButton("Start");
        stopButton = new JButton("Stop");
        stopButton.setEnabled(false);
        
        JButton saveConfigButton = new JButton("Save Config");
        JButton loadConfigButton = new JButton("Load Config");

        startButton.addActionListener(e -> {
            BOOTSTRAP_SERVERS = bootstrapField.getText();
            USERNAME = usernameField.getText();
            PASSWORD = new String(passwordField.getPassword());
            INPUT_TOPIC = inputTopicField.getText();
            OUTPUT_TOPIC = outputTopicField.getText();
            
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
                    inputTopicField.setText(props.getProperty("input.topic", "kstreams-topic3"));
                    outputTopicField.setText(props.getProperty("output.topic", "kstreams-topic4"));
                    log("Configuration loaded from " + fileChooser.getSelectedFile());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, "Error loading configuration: " + ex.getMessage());
                }
            }
        });

        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(saveConfigButton);
        buttonPanel.add(loadConfigButton);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 3;
        configPanel.add(buttonPanel, gbc);

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
        System.out.println("  --input-topic=<topic>          Input topic (default: kstreams-topic3)");
        System.out.println("  --output-topic=<topic>         Output topic (default: kstreams-topic4)");
        System.out.println("  --help                         Show this help message");
    }

    private static void redirectConsoleOutput() {
        PrintStream printStream = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                SwingUtilities.invokeLater(() -> {
                    logArea.append(String.valueOf((char) b));
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                });
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

    private static void startApplication() {
        running.set(true);
        
        new Thread(() -> {
            try {
                Properties streamProps = createStreamProperties();
                
                StreamsBuilder builder = new StreamsBuilder();
                KStream<String, String> source = builder.stream(INPUT_TOPIC);
                source.mapValues(value -> value.toUpperCase()).to(OUTPUT_TOPIC);

                streams = new KafkaStreams(builder.build(), streamProps);

                streams.setUncaughtExceptionHandler(e -> {
                    log("ERROR: " + e.getMessage());
                    return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD;
                });

                streams.setStateListener((newState, oldState) -> {
                    log("State changed: " + oldState + " -> " + newState);
                });

                // Start producer and consumer
                producerThread = new Thread(() -> produceMessages());
                consumerThread = new Thread(() -> consumeMessages());
                
                producerThread.start();
                consumerThread.start();
                
                streams.start();
                log("Kafka Streams application started successfully!");
                
            } catch (Exception e) {
                log("ERROR starting application: " + e.getMessage());
                e.printStackTrace();
                stopApplication();
            }
        }).start();
    }

    private static void stopApplication() {
        running.set(false);
        
        if (producerThread != null) {
            producerThread.interrupt();
        }
        if (consumerThread != null) {
            consumerThread.interrupt();
        }
        if (streams != null) {
            streams.close(Duration.ofSeconds(10));
        }
        
        log("Application stopped");
    }

    private static Properties createStreamProperties() {
        Properties props = new Properties();
        String appId = "kafka-streams-app-" + UUID.randomUUID().toString().substring(0, 8);
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, appId);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        
        if (!USERNAME.isEmpty() && !PASSWORD.isEmpty()) {
            props.put("security.protocol", "SASL_SSL");
            props.put("sasl.mechanism", "PLAIN");
            props.put("sasl.jaas.config",
                    "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                    "username=\"" + USERNAME + "\" password=\"" + PASSWORD + "\";");
        }

        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.CLIENT_ID_CONFIG, "streams-client-1");
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.AT_LEAST_ONCE);
        
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

        try (Producer<String, String> producer = new KafkaProducer<>(props)) {
            int count = 0;
            while (running.get()) {
                String key = "key" + count;
                String value = "value" + count;
                ProducerRecord<String, String> record = new ProducerRecord<>(INPUT_TOPIC, key, value);
                
                producer.send(record, (metadata, ex) -> {
                    if (ex == null) {
                        log("Produced: " + key + " to partition " + metadata.partition());
                    } else {
                        log("Producer error: " + ex.getMessage());
                    }
                });
                
                count++;
                Thread.sleep(2000);
            }
        } catch (Exception e) {
            if (running.get()) {
                log("Producer error: " + e.getMessage());
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

        try (Consumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(OUTPUT_TOPIC));
            
            while (running.get()) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, String> record : records) {
                    log("Consumed: " + record.key() + " = " + record.value());
                }
            }
        } catch (Exception e) {
            if (running.get()) {
                log("Consumer error: " + e.getMessage());
            }
        }
    }
}
