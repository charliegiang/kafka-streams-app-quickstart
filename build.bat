@echo off
echo Building Kafka Streams Standalone Application...

REM Create directories
if not exist lib mkdir lib
if not exist build mkdir build

REM Set versions
set KAFKA_VERSION=4.0.0
set SLF4J_VERSION=2.0.9

REM Download function
echo Downloading dependencies...

REM Download Kafka dependencies
powershell -Command "& { if (!(Test-Path 'lib\kafka-clients-%KAFKA_VERSION%.jar')) { Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/apache/kafka/kafka-clients/%KAFKA_VERSION%/kafka-clients-%KAFKA_VERSION%.jar' -OutFile 'lib\kafka-clients-%KAFKA_VERSION%.jar' } }"
powershell -Command "& { if (!(Test-Path 'lib\kafka-streams-%KAFKA_VERSION%.jar')) { Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/apache/kafka/kafka-streams/%KAFKA_VERSION%/kafka-streams-%KAFKA_VERSION%.jar' -OutFile 'lib\kafka-streams-%KAFKA_VERSION%.jar' } }"
powershell -Command "& { if (!(Test-Path 'lib\slf4j-api-%SLF4J_VERSION%.jar')) { Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/slf4j/slf4j-api/%SLF4J_VERSION%/slf4j-api-%SLF4J_VERSION%.jar' -OutFile 'lib\slf4j-api-%SLF4J_VERSION%.jar' } }"
powershell -Command "& { if (!(Test-Path 'lib\slf4j-simple-%SLF4J_VERSION%.jar')) { Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/%SLF4J_VERSION%/slf4j-simple-%SLF4J_VERSION%.jar' -OutFile 'lib\slf4j-simple-%SLF4J_VERSION%.jar' } }"
powershell -Command "& { if (!(Test-Path 'lib\zstd-jni-1.5.5-1.jar')) { Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/github/luben/zstd-jni/1.5.5-1/zstd-jni-1.5.5-1.jar' -OutFile 'lib\zstd-jni-1.5.5-1.jar' } }"
powershell -Command "& { if (!(Test-Path 'lib\lz4-java-1.8.0.jar')) { Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/lz4/lz4-java/1.8.0/lz4-java-1.8.0.jar' -OutFile 'lib\lz4-java-1.8.0.jar' } }"
powershell -Command "& { if (!(Test-Path 'lib\snappy-java-1.1.10.5.jar')) { Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/xerial/snappy/snappy-java/1.1.10.5/snappy-java-1.1.10.5.jar' -OutFile 'lib\snappy-java-1.1.10.5.jar' } }"
powershell -Command "& { if (!(Test-Path 'lib\rocksdbjni-7.9.2.jar')) { Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/rocksdb/rocksdbjni/7.9.2/rocksdbjni-7.9.2.jar' -OutFile 'lib\rocksdbjni-7.9.2.jar' } }"

REM Compile
echo Compiling...
javac -cp "lib\*" -d build KafkaStreamsStandalone.java

if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed!
    exit /b 1
)

REM Create uber JAR directory
echo Creating uber JAR...
mkdir uber 2>nul

REM Extract all dependencies
for %%j in (lib\*.jar) do (
    echo Extracting %%~nxj...
    cd uber
    jar xf "..\%%j" 2>nul
    cd ..
)

REM Copy compiled classes
xcopy /E /Y build\*.class uber\ >nul

REM Create manifest
echo Manifest-Version: 1.0 > uber\MANIFEST.MF
echo Main-Class: KafkaStreamsStandalone >> uber\MANIFEST.MF

REM Create final JAR
cd uber
jar cfm ..\kafka-streams-app.jar MANIFEST.MF . 2>nul
cd ..

REM Cleanup
rmdir /s /q uber 2>nul

echo.
echo Build complete!
echo.
echo Generated: kafka-streams-app.jar
echo.
echo To run with GUI:
echo   java -jar kafka-streams-app.jar
echo.
echo To run with CLI:
echo   java -jar kafka-streams-app.jar --cli --bootstrap-servers=localhost:9092
echo.
pause
