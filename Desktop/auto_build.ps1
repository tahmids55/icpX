$ErrorActionPreference = "Stop"
Write-Host "Starting automated build setup..."

$workDir = $PWD.Path
$toolsDir = Join-Path $workDir "tools"
if (-not (Test-Path $toolsDir)) { New-Item -ItemType Directory -Path $toolsDir | Out-Null }

# 1. Download and Setup JDK 17 (Portable)
$jdkZip = Join-Path $toolsDir "jdk17.zip"
# Using a fixed URL for reliability
$jdkUrl = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.10%2B7/OpenJDK17U-jdk_x64_windows_hotspot_17.0.10_7.zip"

if (-not (Test-Path $jdkZip)) {
    Write-Host "Downloading JDK 17 (approx 180MB)..."
    Invoke-WebRequest -Uri $jdkUrl -OutFile $jdkZip
}

# Check for extracted folder
$jdkRoot = Get-ChildItem -Path $toolsDir -Directory | Where-Object { $_.Name -like "jdk-17*" } | Select-Object -First 1
if (-not $jdkRoot) {
    Write-Host "Extracting JDK..."
    Expand-Archive -Path $jdkZip -DestinationPath $toolsDir
    $jdkRoot = Get-ChildItem -Path $toolsDir -Directory | Where-Object { $_.Name -like "jdk-17*" } | Select-Object -First 1
}

$env:JAVA_HOME = $jdkRoot.FullName
$env:PATH = "$($env:JAVA_HOME)\bin;$($env:PATH)"
Write-Host "Java available at: $($env:JAVA_HOME)"

# 2. Download and Setup Maven (Portable)
$mvnZip = Join-Path $toolsDir "maven.zip"
$mvnUrl = "https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip"

if (-not (Test-Path $mvnZip)) {
    Write-Host "Downloading Maven..."
    Invoke-WebRequest -Uri $mvnUrl -OutFile $mvnZip
}

$mvnRoot = Get-ChildItem -Path $toolsDir -Directory | Where-Object { $_.Name -like "apache-maven*" } | Select-Object -First 1
if (-not $mvnRoot) {
    Write-Host "Extracting Maven..."
    Expand-Archive -Path $mvnZip -DestinationPath $toolsDir
    $mvnRoot = Get-ChildItem -Path $toolsDir -Directory | Where-Object { $_.Name -like "apache-maven*" } | Select-Object -First 1
}

$env:PATH = "$($mvnRoot.FullName)\bin;$($env:PATH)"
Write-Host "Maven available at: $($mvnRoot.FullName)"

# 2.5 Download and Setup WiX Toolset (Required for creating installers)
$wixZip = Join-Path $toolsDir "wix311.zip"
$wixUrl = "https://github.com/wixtoolset/wix3/releases/download/wix3112rtm/wix311-binaries.zip"

if (-not (Test-Path $wixZip)) {
    Write-Host "Downloading WiX Toolset..."
    Invoke-WebRequest -Uri $wixUrl -OutFile $wixZip
}

$wixRoot = Join-Path $toolsDir "wix311"
if (-not (Test-Path $wixRoot)) {
    Write-Host "Extracting WiX..."
    New-Item -ItemType Directory -Path $wixRoot | Out-Null
    Expand-Archive -Path $wixZip -DestinationPath $wixRoot
}

$env:PATH = "$($wixRoot);$($env:PATH)"
Write-Host "WiX available at: $wixRoot"

# 3. Build Project
Write-Host "Building project with Maven..."
mvn clean package dependency:copy-dependencies -DincludeScope=runtime -DskipTests

# 4. Prepare for JPackage
Write-Host "Preparing files for bundling..."
$targetApp = Join-Path $workDir "target\app"
if (Test-Path $targetApp) { Remove-Item -Recurse -Force $targetApp -ErrorAction Ignore }
New-Item -ItemType Directory -Path $targetApp | Out-Null
Copy-Item (Join-Path $workDir "target\icpX-1.0.0.jar") $targetApp
Copy-Item (Join-Path $workDir "target\dependency\*") $targetApp

# 5. Run JPackage
Write-Host "Creating app installer with jpackage..."
$distDir = Join-Path $workDir "installer_output"
if (Test-Path $distDir) { Remove-Item -Recurse -Force $distDir }

# Using --type exe to create a Windows Installer
jpackage `
  --type exe `
  --name icpX `
  --input $targetApp `
  --main-jar icpX-1.0.0.jar `
  --main-class com.icpx.Launcher `
  --dest $distDir `
  --win-menu `
  --win-shortcut `
  --win-dir-chooser `
  --description "ICP X Application" `
  --vendor "ICPX Team"

Write-Host "---------------------------------------------------"
Write-Host "Installer Build Complete!"
Write-Host "Your Setup file is here: $distDir\icpX-1.0.0.exe"
Write-Host "---------------------------------------------------"