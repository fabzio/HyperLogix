#!/bin/bash

# Check for Bun.js
if ! command -v bun &> /dev/null; then
  echo -e "\e[33mBun.js is not installed. Would you like to install it? (y/n)\e[0m"
  read -r response
  if [[ "$response" == "y" ]]; then
    curl -fsSL https://bun.sh/install | bash
    if [[ $? -eq 0 ]]; then
      echo -e "\e[32mBun.js installed successfully.\e[0m"
    else
      echo -e "\e[31mFailed to install Bun.js.\e[0m"
      exit 1
    fi
  else
    echo -e "\e[31mBun.js installation skipped. Exiting...\e[0m"
    exit 1
  fi
else
  echo -e "\e[32mBun.js is installed.\e[0m"
fi

# Check for JDK 21
if ! command -v java &> /dev/null; then
  echo -e "\e[33mJava is not installed. Would you like to install Liberica JDK 21? (y/n)\e[0m"
  read -r response
  if [[ "$response" == "y" ]]; then
    echo -e "\e[36mDownloading and installing Liberica JDK 21...\e[0m"
    jdk_url="https://download.bell-sw.com/java/21.0.7+9/bellsoft-jdk21.0.7+9-linux-amd64.tar.gz"
    installer_path="/tmp/liberica-jdk21.tar.gz"
    curl -L "$jdk_url" -o "$installer_path"
    tar -xzf "$installer_path" -C /opt
    rm "$installer_path"
    export PATH="/opt/$(ls /opt | grep jdk21)/bin:$PATH"
    if command -v java &> /dev/null; then
      echo -e "\e[32mLiberica JDK 21 installed successfully.\e[0m"
    else
      echo -e "\e[31mFailed to install Liberica JDK 21.\e[0m"
      exit 1
    fi
  else
    echo -e "\e[31mLiberica JDK 21 installation skipped. Exiting...\e[0m"
    exit 1
  fi
else
  java_version=$(java -version 2>&1 | grep -oP 'version "\K\d+')
  if [[ "$java_version" != "21" ]]; then
    echo -e "\e[33mJava version $java_version is installed, but JDK 21 is required. Please update.\e[0m"
    exit 1
  else
    echo -e "\e[32mJDK 21 is installed.\e[0m"
  fi
fi

# Check for Docker or Podman
container_command=""
if command -v docker &> /dev/null; then
  container_command="docker"
elif command -v podman &> /dev/null; then
  container_command="podman"
else
  echo -e "\e[33mNeither Docker nor Podman is installed. Would you like to install Docker? (y/n)\e[0m"
  read -r response
  if [[ "$response" == "y" ]]; then
    echo -e "\e[36mDownloading and installing Docker...\e[0m"
    curl -fsSL https://get.docker.com | sh
    if command -v docker &> /dev/null; then
      echo -e "\e[32mDocker installed successfully.\e[0m"
      container_command="docker"
    else
      echo -e "\e[31mFailed to install Docker.\e[0m"
      exit 1
    fi
  else
    echo -e "\e[31mDocker installation skipped. Exiting...\e[0m"
    exit 1
  fi
fi

# Check if container command is available (either docker or podman)
if [[ -z "$container_command" ]]; then
  echo -e "\e[31mNeither Docker nor Podman is installed or detected.\e[0m"
  exit 1
fi

# Check if PostgreSQL container is already running or exists
container_name="hyperlogix-db"
container_exists=$($container_command ps -a -q -f "name=$container_name")

if [[ -n "$container_exists" ]]; then
  container_running=$($container_command ps -q -f "name=$container_name")
  if [[ -n "$container_running" ]]; then
    echo -e "\e[32mPostgreSQL container '$container_name' is already running.\e[0m"
  else
    echo -e "\e[36mPostgreSQL container '$container_name' exists but is not running. Starting it...\e[0m"
    $container_command start "$container_name"
    if [[ $? -eq 0 ]]; then
      echo -e "\e[32mPostgreSQL container started successfully.\e[0m"
    else
      echo -e "\e[31mFailed to start PostgreSQL container.\e[0m"
      exit 1
    fi
  fi
else
  echo -e "\e[36mStarting a new PostgreSQL container with $container_command...\e[0m"
  $container_command run --name "$container_name" -e POSTGRES_PASSWORD=admin -d -p 5432:5432 postgres:latest
  if [[ $? -eq 0 ]]; then
    echo -e "\e[32mPostgreSQL container started successfully.\e[0m"
  else
    echo -e "\e[31mFailed to start PostgreSQL container.\e[0m"
    exit 1
  fi
fi
# Install turborepo
if ! command -v turbo &> /dev/null; then
  echo -e "\e[33mTurbo is not installed. Would you like to install it? (y/n)\e[0m"
  read -r response
  if [[ "$response" == "y" ]]; then
    chmod +x ./server/mvnw 
    bun install
    if [[ $? -eq 0 ]]; then
      echo -e "\e[32mTurbo installed successfully.\e[0m"
    else
      echo -e "\e[31mFailed to install Turbo.\e[0m"
      exit 1
    fi
  else
    echo -e "\e[31mTurbo installation skipped. Exiting...\e[0m"
    exit 1
  fi
else
  echo -e "\e[32mTurbo is installed.\e[0m"
fi

# Install server dependencies
echo -e "\e[36mInstalling server dependencies...\e[0m"
pushd ./server > /dev/null
./mvnw clean install -DskipTests
if [[ $? -eq 0 ]]; then
  echo -e "\e[32mServer dependencies installed successfully.\e[0m"
else
  echo -e "\e[31mFailed to install server dependencies.\e[0m"
  exit 1
fi
popd > /dev/null

# Install client dependencies
echo -e "\e[36mInstalling client dependencies...\e[0m"
pushd ./client > /dev/null
bun install
if [[ $? -eq 0 ]]; then
  echo -e "\e[32mClient dependencies installed successfully.\e[0m"
else
  echo -e "\e[31mFailed to install client dependencies.\e[0m"
  exit 1
fi
popd > /dev/null

# Run server tests
echo -e "\e[36mRunning server tests...\e[0m"
pushd ./server > /dev/null
./mvnw test
if [[ $? -eq 0 ]]; then
  echo -e "\e[32mServer tests passed successfully.\e[0m"
else
  echo -e "\e[31mServer tests failed.\e[0m"
  exit 1
fi
popd > /dev/null

# Run client tests
echo -e "\e[36mRunning client tests...\e[0m"
pushd ./client > /dev/null
bun run test
if [[ $? -eq 0 ]]; then
  echo -e "\e[32mClient tests passed successfully.\e[0m"
else
  echo -e "\e[31mClient tests failed.\e[0m"
  exit 1
fi
popd > /dev/null
echo -e "\e[32mAll setup steps completed successfully.\e[0m"
echo -e "\e[36mRun 'bun dev' to start developing. Happy hacking!! ;)\e[0m"
