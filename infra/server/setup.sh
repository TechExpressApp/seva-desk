#!/bin/sh
# One-time setup of a fresh Ubuntu 24.04 server (AWS Lightsail, Hetzner, ...). Run as the default user:
#   ssh ubuntu@<ip> 'sudo sh -s' < infra/server/setup.sh
set -eu

# 2 GB swap: building the Java and Angular images needs more memory than a 2 GB server has free.
if ! swapon --show | grep -q /swapfile; then
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
  echo 'vm.swappiness=10' > /etc/sysctl.d/99-swap.conf
  sysctl -p /etc/sysctl.d/99-swap.conf
fi

# Docker and the Compose plugin from Ubuntu's own packages.
export DEBIAN_FRONTEND=noninteractive
apt-get update -q
apt-get install -y -q docker.io docker-compose-v2 unattended-upgrades
systemctl enable --now docker

# Let the login user run docker without sudo (takes effect on the next SSH login).
LOGIN_USER="${SUDO_USER:-ubuntu}"
usermod -aG docker "$LOGIN_USER"

# App folder
mkdir -p /opt/sevadesk
chown "$LOGIN_USER":"$LOGIN_USER" /opt/sevadesk

echo "Server ready. Docker: $(docker --version)"
