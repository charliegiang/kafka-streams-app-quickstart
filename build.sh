#!/bin/bash

echo "Building Kafka Streams Standalone Application..."

# Create lib directory
mkdir -p lib
mkdir -p build

# Download Kafka dependencies if not present
KAFKA_VERSION="4.0.0"
SLF4J_VERSION="2.0.9"

download_jar() {
    local GROUP_PATH=$(echo $1 | tr '.' '/')
    local ARTIFACT=$2
    local VERSION=$3
    local FILE="lib/${ARTIFACT}-${VERSION}.jar"
    
    if [ ! -f "$FILE" ]; then
        echo "Downloading $ARTIFACT..."
        curl -L -o "$FILE" \
            "https://repo1.maven.org/maven2/${GROUP_PATH}/${ARTIFACT}/${VERSION}/${ARTIFACT}-${VERSION}.jar"
    fi
}

# Download all required JARs
download_jar "org.apache.kafka" "kafka-clients" "$KAFKA_VERSION"
download_jar "org.apache.kafka" "kafka-streams" "$KAFKA_VERSION"
download_jar "org.slf4j" "slf4j-api" "$SLF4J_VERSION"
download_jar "org.slf4j" "slf4j-simple" "$SLF4J_VERSION"
download_jar "com.github.luben" "zstd-jni" "1.5.5-1"
download_jar "org.lz4" "lz4-java" "1.8.0"
download_jar "org.xerial.snappy" "snappy-java" "1.1.10.5"
download_jar "org.rocksdb" "rocksdbjni" "7.9.2"

# Compile the Java file
echo "Compiling..."
javac -cp "lib/*" -d build KafkaStreamsStandalone.java

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

# Create manifest file
cat > build/MANIFEST.MF << EOF
Manifest-Version: 1.0
Main-Class: KafkaStreamsStandalone
Class-Path: .
EOF

# Create the executable JAR
echo "Creating executable JAR..."
cd build
jar cfm ../kafka-streams-app.jar MANIFEST.MF *.class

# Extract all dependencies into the JAR
cd ..
for jar in lib/*.jar; do
    echo "Adding $(basename $jar) to executable..."
    jar xf "$jar" 2>/dev/null
    jar uf kafka-streams-app.jar META-INF org com ch rocksd 2>/dev/null
    rm -rf META-INF org com ch rocksd
done

echo ""
echo "✅ Build complete!"
echo ""
echo "📦 Generated: kafka-streams-app.jar"
echo ""
echo "To run with GUI:"
echo "  java -jar kafka-streams-app.jar"
echo ""
echo "To run with CLI:"
echo "  java -jar kafka-streams-app.jar --cli --bootstrap-servers=localhost:9092"
echo ""
echo "The JAR file can be distributed and run on any system with Java 11+ installed."
