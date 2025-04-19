#!/bin/bash
# Startup Script for GCP VM - Docker + UFW Configuration
# Full path: /tmp/startup_script.sh

# ==========================================
# INITIAL SETUP & LOGGING CONFIGURATION
# ==========================================
exec > >(tee /var/log/startup-script.log|logger -t startup-script -s 2>/dev/console) 2>&1
set -x

# ==========================================
# NETWORK CONNECTIVITY CHECK
# ==========================================
echo "Checking network connectivity..."
for i in {1..30}; do
  if ping -c 1 google.com &>/dev/null; then
    echo "Network available"
    break
  fi
  echo "Waiting for network... Attempt $i/30"
  sleep 2
  if [ $i -eq 30 ]; then
    echo "ERROR: Network not available after 60 seconds"
    exit 1
  fi
done

# ==========================================
# SYSTEM UPDATE & DEPENDENCY INSTALLATION
# ==========================================
echo "Updating package lists..."
for i in {1..5}; do
  sudo apt-get update && break || sleep 15
  [ $i -eq 5 ] && echo "ERROR: Failed to update packages" && exit 1
done

echo "Installing base dependencies..."
DEBIAN_FRONTEND=noninteractive sudo apt-get install -y \
  apt-transport-https \
  ca-certificates \
  curl \
  software-properties-common \
  gnupg-agent \
  ufw

# ==========================================
# DOCKER INSTALLATION
# ==========================================
echo "Setting up Docker..."
# Add Docker's official GPG key
for i in {1..5}; do
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \
  sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg && break || sleep 15
  [ $i -eq 5 ] && echo "ERROR: Failed to add Docker GPG key" && exit 1
done

# Add Docker repository
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] \
https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | \
sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Install Docker Engine
for i in {1..5}; do
  sudo apt-get update && \
  sudo apt-get install -y docker-ce docker-ce-cli containerd.io && break || sleep 15
  [ $i -eq 5 ] && echo "ERROR: Failed to install Docker" && exit 1
done

# Configure Docker to start on boot
sudo systemctl enable docker
sudo systemctl start docker

# Add current user to docker group
sudo usermod -aG docker ubuntu

# ==========================================
# FIREWALL CONFIGURATION (UFW)
# ==========================================
echo "Configuring firewall..."
sudo ufw --force reset

# Default policies
sudo ufw default deny incoming
sudo ufw default allow outgoing

# Allow essential ports
sudo ufw allow 22/tcp comment 'SSH'
sudo ufw allow 80/tcp comment 'HTTP'
sudo ufw allow 443/tcp comment 'HTTPS'
sudo ufw allow 8500/tcp comment 'Custom Port 8500'

# Enable UFW
sudo ufw --force enable

# Verify UFW status
sudo ufw status verbose

echo "All done! System is ready."

# Create completion flag
echo "COMPLETED $(date)" | sudo tee /tmp/startup-script-complete
