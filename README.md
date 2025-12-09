# Kafka Streams Standalone Application

A self-contained Kafka Streams application that can be run with just Java installed - no IDE, Maven, or other dependencies required!

## Features

- ✨ **GUI Mode**: User-friendly interface for configuration
- 🖥️ **CLI Mode**: Command-line interface for automation/scripting  
- 💾 **Save/Load Config**: Store and reuse your Kafka configurations
- 📦 **Single JAR**: Everything bundled in one executable file
- 🔐 **SASL Support**: Built-in authentication support

## Quick Start

### Option 1: Use Pre-built JAR (Easiest)
Just download `kafka-streams-app.jar` and run:
```bash
java -jar kafka-streams-app.jar
```

### Option 2: Build from Source

**On Linux/Mac:**
```bash
chmod +x build.sh
./build.sh
java -jar kafka-streams-app.jar
```

**On Windows:**
```batch
build.bat
java -jar kafka-streams-app.jar
```

## Requirements

- Java 11 or higher
- That's it! No other dependencies needed

## Usage

### GUI Mode (Default)
Simply double-click the JAR file or run:
```bash
java -jar kafka-streams-app.jar
```

1. Enter your Kafka broker address (e.g., `localhost:9092`)
2. Optional: Add SASL credentials if needed
3. Configure input/output topics
4. Click "Start" to begin processing

### CLI Mode
Perfect for servers or automation:
```bash
java -jar kafka-streams-app.jar --cli \
  --bootstrap-servers=broker1:9092 \
  --username=myuser \
  --password=mypass \
  --input-topic=input \
  --output-topic=output
```

### What It Does

The application:
1. Reads messages from the input topic
2. Transforms them (converts to uppercase)
3. Writes to the output topic
4. Shows real-time logs of all activity

## Configuration Options

| Parameter | Description | Default |
|-----------|-------------|---------|
| Bootstrap Servers | Kafka broker addresses | localhost:9092 |
| Username | SASL username (optional) | - |
| Password | SASL password (optional) | - |
| Input Topic | Topic to read from | kstreams-topic3 |
| Output Topic | Topic to write to | kstreams-topic4 |

## Saving Configurations

In GUI mode, you can:
- **Save Config**: Store your settings to a file
- **Load Config**: Load previously saved settings

This is useful for:
- Different environments (dev/staging/prod)
- Multiple Kafka clusters
- Sharing configurations with team members

## Distribution

To share with others:
1. Send them the `kafka-streams-app.jar` file
2. Tell them to install Java 11+ if they don't have it
3. They can run it immediately - no setup needed!

## Troubleshooting

**"No Java found" error:**
- Install Java 11 or higher from https://adoptium.net/

**Connection issues:**
- Check your Kafka broker is running
- Verify the broker address is correct
- Check firewall/network settings

**Authentication fails:**
- Verify your SASL credentials
- Ensure your Kafka cluster has SASL enabled

## How to Check Java Version
```bash
java -version
```

If you see version 11 or higher, you're good to go!

## Example Use Cases

1. **Development Testing**: Quick way to test Kafka streams without complex setup
2. **Demo/POC**: Show Kafka Streams functionality without installation hassle  
3. **Learning**: Understand Kafka Streams with a working example
4. **Debugging**: Test connectivity and data flow in Kafka clusters

## License

Free to use and modify as needed!
