@echo off
echo Building Kafka Streams Standalone Application...

REM Create directories
if not exist lib mkdir lib
if not exist build mkdir build

REM Set versions
set KAFKA_VERSION=3.6.0
set SLF4J_VERSION=2.0.9

REM Download function
echo Downloading dependencies...

REM Download Kafka dependencies
powershell -Command "& { if (!(Test-Path 'lib\kafka-clients-%KAFKA_VERSION%.jar')) { Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/apache/kafka/kafka-clients/%KAFKA_VERSION%/kafka-clients-%KAFKA_VERSION%.jar' -OutFile 'lib\kafka-clients-%KAFKA_VERSION%.jar' } }"
powershell -Command "& { if (!(Test-Path 'lib\kafka-streams-%KAFKA_VERSION%.jar')) { Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/apache/kafka/kafka-streams/%KAFKA_VERSION%/kafka-streams-%KAFKA_VERSION%.jar' -OutFile 'lib\kafka-streams-%KAFKA_VERSION%.jar' } }"
powershell -Command "& { if (!(Test-Path 'lib\slf4j-api-%SLF4J_VERSION%.jar')) { Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/slf4j/slf4j-api/%SLF4J_VERSION%/slf4j-api-%SLF4J_VERSION%.jar' -OutFile 'lib\slf4j-api-%SLF4J_VERSION%.jar' } }"
powershell -Command "& { if (!(Test-Path 'lib\slf4j-simple-%SLF4J_VERSION%.jar')) { Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/%SLF4J_VERSION%/slf4j-simple-%SLF4J_VERSION%.jar' -OutFile 'lib\slf4j-simple-%SLF4J_VERSION%.jar' } }"

REM Compile
echo Compiling...
javac -cp "lib\*" -d build src\main\java\KafkaStreamsStandalone.java

if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed!
    exit /b 1
)

REM Create manifest
echo Manifest-Version: 1.0 > build\MANIFEST.MF
echo Main-Class: KafkaStreamsStandalone >> build\MANIFEST.MF
echo Class-Path: . >> build\MANIFEST.MF

REM Create JAR
echo Creating executable JAR...
cd build
jar cfm ..\kafka-streams-app.jar MANIFEST.MF *.class
cd ..

REM Add dependencies to JAR
for %%j in (lib\*.jar) do (
    echo Adding %%~nxj to executable...
    jar xf "%%j" 2>nul
    jar uf kafka-streams-app.jar META-INF org com 2>nul
    rmdir /s /q META-INF 2>nul
    rmdir /s /q org 2>nul
    rmdir /s /q com 2>nul
)

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
