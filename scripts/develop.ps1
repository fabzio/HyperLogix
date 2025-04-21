# Check for Bun.js
if (-not (Get-Command "bun" -ErrorAction SilentlyContinue)) {
  Write-Host "Bun.js is not installed. Would you like to install it? (y/n)" -ForegroundColor Yellow
  $response = Read-Host
  if ($response -eq "y") {
      powershell -c "irm bun.sh/install.ps1 | iex"
      if ($?) {
          Write-Host "Bun.js installed successfully." -ForegroundColor Green
      } else {
          Write-Host "Failed to install Bun.js." -ForegroundColor Red
          exit 1
      }
  } else {
      Write-Host "Bun.js installation skipped. Exiting..." -ForegroundColor Red
      exit 1
  }
} else {
  Write-Host "Bun.js is installed." -ForegroundColor Green
}

# Check for JDK 21
if (-not (Get-Command "java" -ErrorAction SilentlyContinue)) {
  Write-Host "Java is not installed. Would you like to install Liberica JDK 21? (y/n)" -ForegroundColor Yellow
  $response = Read-Host
  if ($response -eq "y") {
      Write-Host "Downloading and installing Liberica JDK 21..." -ForegroundColor Cyan
      $jdkUrl = "https://download.bell-sw.com/java/21.0.7+9/bellsoft-jdk21.0.7+9-windows-amd64.msi"
      $installerPath = "$env:TEMP\liberica-jdk21-installer.msi"
      Invoke-WebRequest -Uri $jdkUrl -OutFile $installerPath
      Start-Process -FilePath msiexec.exe -ArgumentList "/i", $installerPath, "/quiet", "/norestart" -Wait
      Remove-Item $installerPath
      if (Get-Command "java" -ErrorAction SilentlyContinue) {
          Write-Host "Liberica JDK 21 installed successfully." -ForegroundColor Green
      } else {
          Write-Host "Failed to install Liberica JDK 21." -ForegroundColor Red
          exit 1
      }
  } else {
      Write-Host "Liberica JDK 21 installation skipped. Exiting..." -ForegroundColor Red
      exit 1
  }
} else {
  $javaVersion = java -version 2>&1 | Select-String -Pattern 'version "(\d+)' | ForEach-Object { $_.Matches.Groups[1].Value }
  if ($javaVersion -ne "21") {
      Write-Host "Java version $javaVersion is installed, but JDK 21 is required. Please update." -ForegroundColor Yellow
      exit 1
  } else {
      Write-Host "JDK 21 is installed." -ForegroundColor Green
  }
}

# Check for Docker or Podman
$containerCommand = ""
if (Get-Command "docker" -ErrorAction SilentlyContinue) {
  $containerCommand = "docker"
} elseif (Get-Command "podman" -ErrorAction SilentlyContinue) {
  $containerCommand = "podman"
} else {
  Write-Host "Neither Docker nor Podman is installed. Would you like to install Docker? (y/n)" -ForegroundColor Yellow
  $response = Read-Host
  if ($response -eq "y") {
      Write-Host "Downloading and installing Docker..." -ForegroundColor Cyan
      $dockerUrl = "https://desktop.docker.com/win/stable/Docker%20Desktop%20Installer.exe"
      $installerPath = "$env:TEMP\docker-installer.exe"
      Invoke-WebRequest -Uri $dockerUrl -OutFile $installerPath
      Start-Process -FilePath $installerPath -ArgumentList "/quiet" -Wait
      Remove-Item $installerPath
      if (Get-Command "docker" -ErrorAction SilentlyContinue) {
          Write-Host "Docker installed successfully." -ForegroundColor Green
          $containerCommand = "docker"
      } else {
          Write-Host "Failed to install Docker." -ForegroundColor Red
          exit 1
      }
  } else {
      Write-Host "Docker installation skipped. Exiting..." -ForegroundColor Red
      exit 1
  }
}

# Check if container command is available (either docker or podman)
if (-not $containerCommand) {
  Write-Host "Neither Docker nor Podman is installed or detected." -ForegroundColor Red
  exit 1
}

# Check if PostgreSQL container is already running or exists
$containerName = "hyperlogix-db"

# Query the container to check if it exists
$containerExists = & $containerCommand ps -a -q -f "name=$containerName"

if ($containerExists) {
    # Check if the container is running
    $containerRunning = & $containerCommand ps -q -f "name=$containerName"
    if ($containerRunning) {
        Write-Host "PostgreSQL container '$containerName' is already running." -ForegroundColor Green
    } else {
        Write-Host "PostgreSQL container '$containerName' exists but is not running. Starting it..." -ForegroundColor Cyan
        & $containerCommand start $containerName
        if ($?) {
            Write-Host "PostgreSQL container started successfully." -ForegroundColor Green
        } else {
            Write-Host "Failed to start PostgreSQL container." -ForegroundColor Red
            exit 1
        }
    }
} else {
    Write-Host "Starting a new PostgreSQL container with $containerCommand..." -ForegroundColor Cyan
    & $containerCommand run --name $containerName -e POSTGRES_PASSWORD=admin -d -p 5432:5432 postgres:latest
    if ($?) {
        Write-Host "PostgreSQL container started successfully." -ForegroundColor Green
    } else {
        Write-Host "Failed to start PostgreSQL container." -ForegroundColor Red
        exit 1
    }
}
#Install turborepo
if (-not (Get-Command "turbo" -ErrorAction SilentlyContinue)) {
  Write-Host "Turborepo is not installed. Would you like to install it? (y/n)" -ForegroundColor Yellow
  $response = Read-Host
  if ($response -eq "y") {
      Write-Host "Installing Turborepo..." -ForegroundColor Cyan
      bun install
      if ($?) {
          Write-Host "Turborepo installed successfully." -ForegroundColor Green
      } else {
          Write-Host "Failed to install Turborepo." -ForegroundColor Red
          exit 1
      }
  } else {
      Write-Host "Turborepo installation skipped. Exiting..." -ForegroundColor Red
      exit 1
  }
} else {
  Write-Host "Turborepo is already installed." -ForegroundColor Green
}

# Install server dependencies
Write-Host "Installing server dependencies..." -ForegroundColor Cyan
Push-Location ./server
./mvnw clean install -DskipTests
if ($?) {
  Write-Host "Server dependencies installed successfully." -ForegroundColor Green
} else {
  Write-Host "Failed to install server dependencies." -ForegroundColor Red
  exit 1
}
Pop-Location

# Install client dependencies
Write-Host "Installing client dependencies..." -ForegroundColor Cyan
Push-Location ./client
bun install
if ($?) {
  Write-Host "Client dependencies installed successfully." -ForegroundColor Green
} else {
  Write-Host "Failed to install client dependencies." -ForegroundColor Red
  exit 1
}
Pop-Location

# Run server tests
Write-Host "Running server tests..." -ForegroundColor Cyan
Push-Location ./server
./mvnw test
if ($?) {
  Write-Host "Server tests passed successfully." -ForegroundColor Green
} else {
  Write-Host "Server tests failed." -ForegroundColor Red
  exit 1
}
Pop-Location

# Run client tests
Write-Host "Running client tests..." -ForegroundColor Cyan
Push-Location ./client
bun run test
if ($?) {
  Write-Host "Client tests passed successfully." -ForegroundColor Green
} else {
  Write-Host "Client tests failed." -ForegroundColor Red
  exit 1
}
Pop-Location

Write-Host "All setup steps completed successfully." -ForegroundColor Green
Write-Host "Run 'bun dev' to start developing. Happy hacking!! ;)" -ForegroundColor Green
