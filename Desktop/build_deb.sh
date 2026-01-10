#!/bin/bash
set -e

# Configuration
# Debian package names should be lowercase
APP_NAME="icpx"
DISPLAY_NAME="ICP X"
APP_VERSION="1.0.0"
MAIN_JAR="icpX-${APP_VERSION}.jar"
MAIN_CLASS="com.icpx.Launcher"
INSTALLER_OUTPUT="installer_output"

# Check for required tools
echo "Checking environment..."

if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed or not in PATH."
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed or not in PATH."
    exit 1
fi

if ! command -v jpackage &> /dev/null; then
    echo "Error: jpackage is not found. Please install JDK 17+ package that includes jpackage (e.g., openjdk-17-jdk or similar, depending on your distro)."
    exit 1
fi

echo "Using Java: $(java -version 2>&1 | head -n 1)"
echo "Using Maven: $(mvn -version 2>&1 | head -n 1)"

# 1. Build Project
echo "Building project with Maven..."
# dependency:copy-dependencies copies runtime dependencies to target/dependency
mvn clean package dependency:copy-dependencies -DincludeScope=runtime -DskipTests

# 2. Prepare for JPackage
echo "Preparing files for bundling..."
TARGET_APP="target/app"

# Clean previous build area
rm -rf "$TARGET_APP"
mkdir -p "$TARGET_APP"

# Copy main jar
if [ -f "target/$MAIN_JAR" ]; then
    cp "target/$MAIN_JAR" "$TARGET_APP/"
else
    echo "Error: Main jar not found at target/$MAIN_JAR"
    exit 1
fi

# Copy dependencies
if [ -d "target/dependency" ]; then
    cp target/dependency/*.jar "$TARGET_APP/"
fi

# 3. Create Installer with JPackage
echo "Creating .deb installer with jpackage..."

# Clean output directory
rm -rf "$INSTALLER_OUTPUT"
mkdir -p "$INSTALLER_OUTPUT"

# Run jpackage
# Note: --name must normally be lowercase for Debian packages
jpackage \
  --type deb \
  --name "$APP_NAME" \
  --app-version "$APP_VERSION" \
  --input "$TARGET_APP" \
  --main-jar "$MAIN_JAR" \
  --main-class "$MAIN_CLASS" \
  --dest "$INSTALLER_OUTPUT" \
  --linux-shortcut \
  --linux-menu-group "Office" \
  --description "ICP X Application" \
  --vendor "ICPX Team" \
  --verbose

echo "---------------------------------------------------"
echo "Installer Build Complete!"
if ls "$INSTALLER_OUTPUT"/*.deb 1> /dev/null 2>&1; then
    echo "Your package is available at:"
    ls -l "$INSTALLER_OUTPUT"/*.deb
else
    echo "Warning: No .deb file found in $INSTALLER_OUTPUT. Check the logs above for errors."
fi
echo "---------------------------------------------------"
