#!/usr/bin/env python3
"""
Note: I'm lazy, this was copy-pasted from another project called `tama`. Deal with it 🤷

Production Installation & Update Script for tama Game Server
Target: Ubuntu 24.04 VPS

PREREQUISITES - Run these commands BEFORE first installation:
1. Update system packages:
   sudo apt update && sudo apt upgrade -y

2. Install required system dependencies:
   sudo apt install -y curl build-essential pkg-config libssl-dev python3 python3-pip

3. Install Rust (if not already installed):
   curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y
   source "$HOME/.cargo/env"

4. Note: Server runs on port 80 (requires root/CAP_NET_BIND_SERVICE capability)

WHAT THIS SCRIPT DOES:
- Pulls latest code from git
- Builds the game server in release mode
- Installs/updates the binary and static files
- Creates systemd service (if first install)
- Sets up log rotation (if first install)
- Configures production environment (preserves existing)
- Preserves existing database
- Runs database migrations automatically
- Configures firewall (if first install)
- Installs Certbot and obtains Let's Encrypt SSL certificate (if first install)
- Sets up automatic certificate renewal with systemd timer
- Restarts the service

USAGE:
  # First installation:
  cd /path/to/tama
  sudo python3 server/scripts/install.py

  # Updates (also works with first install):
  cd /path/to/tama
  sudo python3 server/scripts/install.py
"""

import os
import sys
import subprocess
import shutil
from pathlib import Path
import time

# Configuration
SERVICE_NAME = "tama-server"
SERVICE_USER = os.environ.get("SUDO_USER", os.environ.get("USER", "ubuntu"))
PROJECT_ROOT = Path("/root/tama")
SERVER_BINARY_SOURCE = PROJECT_ROOT / "target" / "release" / "server"
SERVER_BINARY_DEST = Path("/usr/local/bin/tama-server")
SYSTEMD_SERVICE_PATH = Path(f"/etc/systemd/system/{SERVICE_NAME}.service")
LOG_DIR = Path(f"/var/log/{SERVICE_NAME}")
DATA_DIR = Path(f"/var/lib/{SERVICE_NAME}")
ENV_FILE = Path(f"/etc/{SERVICE_NAME}/env")
STATIC_SOURCE = PROJECT_ROOT / "static"
STATIC_DEST = DATA_DIR / "static"
DOMAIN = "tama.curzel.it"
CERT_DIR = Path("/etc/letsencrypt")

def run_command(cmd, check=True, shell=False, cwd=None):
    """Execute a shell command and return the result."""
    print(f"� Running: {cmd if isinstance(cmd, str) else ' '.join(cmd)}")
    try:
        result = subprocess.run(
            cmd,
            check=check,
            shell=shell,
            cwd=cwd,
            capture_output=True,
            text=True
        )
        if result.stdout:
            print(result.stdout)
        return result
    except subprocess.CalledProcessError as e:
        print(f" Command failed with exit code {e.returncode}")
        if e.stderr:
            print(f"Error: {e.stderr}")
        if check:
            sys.exit(1)
        return e

def check_root():
    """Ensure script is run with sudo privileges."""
    if os.geteuid() != 0:
        print(" This script must be run with sudo privileges")
        print("  Usage: sudo python3 server/scripts/install.py")
        sys.exit(1)
    print(" Running with sudo privileges")

def check_rust():
    """Verify Rust is installed."""
    result = run_command(["which", "cargo"], check=False)
    if result.returncode != 0:
        print(" Rust/Cargo not found. Please install Rust first:")
        print("  curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh")
        sys.exit(1)

    # Get cargo path for the actual user (not root)
    cargo_version = run_command(["sudo", "-u", SERVICE_USER, "bash", "-c", "source $HOME/.cargo/env && cargo --version"])
    print(f" Rust installed: {cargo_version.stdout.strip()}")

def build_server():
    """Build the server in release mode."""
    print("\n=== Building Server ===")
    print("This may take several minutes...")

    # Build as the actual user, not root
    run_command(
        ["sudo", "-u", SERVICE_USER, "bash", "-c",
         f"source $HOME/.cargo/env && cd {PROJECT_ROOT} && cargo build --release --bin server"],
        shell=False
    )

    if not SERVER_BINARY_SOURCE.exists():
        print(f" Build failed: {SERVER_BINARY_SOURCE} not found")
        sys.exit(1)

    print(f" Server binary built: {SERVER_BINARY_SOURCE}")

def install_binary():
    """Copy the binary to a system location."""
    print("\n=== Installing Binary ===")

    # Copy binary to /usr/local/bin
    os.system(f'sudo systemctl stop {SERVICE_NAME}')
    os.system(f'killall {SERVICE_NAME}')
    shutil.copy2(SERVER_BINARY_SOURCE, SERVER_BINARY_DEST)
    os.chmod(SERVER_BINARY_DEST, 0o755)
    print(f"✓ Binary installed: {SERVER_BINARY_DEST}")

def install_static_files():
    """Copy static files to the data directory."""
    print("\n=== Installing Static Files ===")

    if not STATIC_SOURCE.exists():
        print(f"⚠ Static directory not found: {STATIC_SOURCE}")
        print("  Skipping static files installation")
        return

    # Remove old static files if they exist
    if STATIC_DEST.exists():
        shutil.rmtree(STATIC_DEST)
        print(f"✓ Removed old static files")

    # Copy static directory
    shutil.copytree(STATIC_SOURCE, STATIC_DEST)

    # Set proper ownership recursively
    for root, dirs, files in os.walk(STATIC_DEST.parent):
        for d in dirs:
            shutil.chown(os.path.join(root, d), user=SERVICE_USER, group=SERVICE_USER)
        for f in files:
            shutil.chown(os.path.join(root, f), user=SERVICE_USER, group=SERVICE_USER)

    print(f"✓ Static files installed: {STATIC_DEST}")

def create_directories():
    """Create necessary directories for logs and data."""
    print("\n=== Creating Directories ===")

    directories = [
        (LOG_DIR, "logs"),
        (DATA_DIR, "database and application data"),
        (ENV_FILE.parent, "environment configuration"),
    ]

    for dir_path, description in directories:
        dir_path.mkdir(parents=True, exist_ok=True)
        shutil.chown(dir_path, user=SERVICE_USER, group=SERVICE_USER)
        print(f" Created {dir_path} ({description})")

def create_env_file():
    """Create production environment file."""
    print("\n=== Creating Environment File ===")

    if ENV_FILE.exists():
        print(f" Environment file exists (preserved): {ENV_FILE}")
        return

    env_content = f"""# tama Server Production Environment Configuration
# Database location (relative to WorkingDirectory)
DATABASE_PATH=tama.db

# Server configuration
SERVER_PORT=443
JWT_SECRET=change-this-secret-in-production-{os.urandom(16).hex()}
SESSION_DURATION_SECONDS=86400

# Logging
RUST_LOG=info
RUST_BACKTRACE=1

# Add any additional environment variables here
"""

    ENV_FILE.write_text(env_content)
    os.chmod(ENV_FILE, 0o640)
    shutil.chown(ENV_FILE, user="root", group=SERVICE_USER)
    print(f" Environment file created: {ENV_FILE}")

def create_systemd_service():
    """Create systemd service file for the server (updates if exists)."""
    print("\n=== Systemd Service ===")

    service_content = f"""[Unit]
Description=tama Game Server
After=network.target
Documentation=https://github.com/yourusername/tama

[Service]
Type=simple
User={SERVICE_USER}
Group={SERVICE_USER}
WorkingDirectory={DATA_DIR}
EnvironmentFile={ENV_FILE}

# Server binary
ExecStart={SERVER_BINARY_DEST}

# Automatic restart configuration
Restart=always
RestartSec=10
StartLimitInterval=0

# Allow binding to ports 80 and 443
AmbientCapabilities=CAP_NET_BIND_SERVICE

# Security hardening
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths={DATA_DIR} {LOG_DIR}
ReadOnlyPaths=/etc/letsencrypt
ProtectKernelTunables=true
ProtectKernelModules=true
ProtectControlGroups=true

# Logging
StandardOutput=append:{LOG_DIR}/server.log
StandardError=append:{LOG_DIR}/error.log
SyslogIdentifier={SERVICE_NAME}

# Resource limits (adjust as needed)
LimitNOFILE=65536
LimitNPROC=4096

[Install]
WantedBy=multi-user.target
"""

    SYSTEMD_SERVICE_PATH.write_text(service_content)
    os.chmod(SYSTEMD_SERVICE_PATH, 0o644)
    print(f" Systemd service created: {SYSTEMD_SERVICE_PATH}")

def setup_logrotate():
    """Configure log rotation."""
    print("\n=== Setting Up Log Rotation ===")

    logrotate_config = Path(f"/etc/logrotate.d/{SERVICE_NAME}")
    logrotate_content = f"""{LOG_DIR}/*.log {{
    daily
    rotate 14
    compress
    delaycompress
    missingok
    notifempty
    create 0640 {SERVICE_USER} {SERVICE_USER}
    sharedscripts
    postrotate
        systemctl reload {SERVICE_NAME} >/dev/null 2>&1 || true
    endscript
}}
"""

    logrotate_config.write_text(logrotate_content)
    os.chmod(logrotate_config, 0o644)
    print(f" Log rotation configured: {logrotate_config}")

def run_migrations():
    """Run database migrations using sqlx-cli."""
    print("\n=== Running Database Migrations ===")

    # Check if sqlx is installed
    result = run_command(["which", "sqlx"], check=False)
    if result.returncode != 0:
        print("⚠ sqlx-cli not found, installing...")
        run_command(
            ["cargo", "install", "sqlx-cli", "--no-default-features", "--features", "sqlite"],
            shell=False
        )

    # Get database URL from env file or use default
    db_url = f"sqlite://{DATA_DIR}/tama.db"

    print(f"Running migrations for database: {db_url}")

    # Run migrations
    result = run_command(
        ["sqlx", "migrate", "run", "--database-url", db_url, "--source", f"{PROJECT_ROOT}/migrations"],
        check=False,
        cwd=PROJECT_ROOT
    )

    if result.returncode == 0:
        print("✓ Migrations completed successfully")
    else:
        print("⚠ Migration failed, but continuing (migrations will run on server startup)")

def check_database():
    """Check database status."""
    print("\n=== Database ===")
    db_path = DATA_DIR / "tama.db"
    if db_path.exists():
        print(f"✓ Database exists (preserved): {db_path}")
    else:
        print(f"ℹ Database will be created on first startup: {db_path}")

def configure_firewall():
    """Configure firewall to allow HTTP/HTTPS traffic."""
    print("\n=== Configuring Firewall ===")

    # Check if ufw is installed
    result = run_command(["which", "ufw"], check=False)
    if result.returncode != 0:
        print("⚠ ufw not found, skipping firewall configuration")
        print("  Install with: sudo apt install ufw")
        return

    # Allow SSH (don't lock yourself out!)
    run_command(["ufw", "allow", "22/tcp"])
    print("✓ Allowed SSH (port 22)")

    # Allow HTTP
    run_command(["ufw", "allow", "80/tcp"])
    print("✓ Allowed HTTP (port 80)")

    # Allow HTTPS
    run_command(["ufw", "allow", "443/tcp"])
    print("✓ Allowed HTTPS (port 443)")

    # Enable firewall (with --force to avoid interactive prompt)
    result = run_command(["ufw", "--force", "enable"])
    print("✓ Firewall enabled")

    # Show status
    run_command(["ufw", "status"])

def install_certbot():
    """Install Certbot if not already installed."""
    print("\n=== Installing Certbot ===")

    # Check if certbot is already installed
    result = run_command(["which", "certbot"], check=False)
    if result.returncode == 0:
        print("✓ Certbot already installed")
        return

    print("Installing Certbot and required packages...")
    run_command(["apt", "update"])
    run_command(["apt", "install", "-y", "certbot", "python3-certbot"])
    print("✓ Certbot installed")

def obtain_ssl_certificate():
    """Obtain SSL certificate from Let's Encrypt using standalone mode."""
    print("\n=== Obtaining SSL Certificate ===")

    # Check if certificate already exists
    cert_path = CERT_DIR / "live" / DOMAIN / "fullchain.pem"
    if cert_path.exists():
        print(f"✓ Certificate already exists for {DOMAIN}")
        print("  To renew, run: sudo certbot renew")
        return

    print(f"Obtaining certificate for {DOMAIN}...")
    print("⚠ Make sure:")
    print(f"  1. DNS for {DOMAIN} points to this server's IP")
    print("  2. Port 80 is accessible from the internet")
    print("  3. The tama service will be temporarily stopped")

    # Stop the service to free up port 80
    print("\nStopping tama service temporarily...")
    run_command(["systemctl", "stop", SERVICE_NAME], check=False)

    # Obtain certificate using standalone mode
    result = run_command([
        "certbot", "certonly",
        "--standalone",
        "--non-interactive",
        "--agree-tos",
        "--email", f"admin@{DOMAIN}",
        "--domains", DOMAIN
    ], check=False)

    if result.returncode == 0:
        print(f"✓ SSL certificate obtained for {DOMAIN}")
        print(f"  Certificate: {cert_path}")
        print(f"  Private key: {CERT_DIR}/live/{DOMAIN}/privkey.pem")
    else:
        print("⚠ Failed to obtain SSL certificate")
        print("  Please check:")
        print(f"    - DNS for {DOMAIN} is correctly configured")
        print("    - Port 80 is accessible from the internet")
        print("    - No other service is using port 80")
        print("\n  You can manually run: sudo certbot certonly --standalone -d " + DOMAIN)

def setup_certbot_renewal():
    """Configure automatic certificate renewal."""
    print("\n=== Setting Up Automatic Certificate Renewal ===")

    # Certbot installs a systemd timer by default, let's verify it
    result = run_command(["systemctl", "list-timers", "certbot.timer"], check=False)

    if result.returncode == 0 and "certbot.timer" in result.stdout:
        print("✓ Certbot renewal timer already active")
    else:
        # Enable the certbot timer
        run_command(["systemctl", "enable", "certbot.timer"], check=False)
        run_command(["systemctl", "start", "certbot.timer"], check=False)
        print("✓ Certbot renewal timer enabled")

    # Create a renewal hook to restart the service after renewal
    hooks_dir = Path("/etc/letsencrypt/renewal-hooks/post")
    hooks_dir.mkdir(parents=True, exist_ok=True)

    hook_script = hooks_dir / "restart-tama.sh"
    hook_content = f"""#!/bin/bash
# Restart tama server after certificate renewal
systemctl restart {SERVICE_NAME}
"""

    hook_script.write_text(hook_content)
    os.chmod(hook_script, 0o755)
    print(f"✓ Renewal hook created: {hook_script}")
    print(f"  Service will automatically restart after certificate renewal")

    # Test renewal (dry run)
    print("\nTesting renewal configuration (dry run)...")
    result = run_command(["certbot", "renew", "--dry-run"], check=False)

    if result.returncode == 0:
        print("✓ Renewal test successful")
        print("  Certificates will auto-renew before expiration")
    else:
        print("⚠ Renewal test had issues, but renewal timer is configured")

def update_env_for_ssl():
    """Update environment file with SSL certificate paths."""
    print("\n=== Updating Environment for SSL ===")

    cert_path = CERT_DIR / "live" / DOMAIN / "fullchain.pem"
    key_path = CERT_DIR / "live" / DOMAIN / "privkey.pem"

    if not cert_path.exists():
        print("⚠ SSL certificate not found, skipping SSL environment configuration")
        return

    # Check if SSL paths are already in the env file
    env_content = ENV_FILE.read_text()

    if "SSL_CERT_PATH" in env_content:
        print("✓ SSL configuration already present in environment file")
        return

    # Add SSL configuration
    ssl_config = f"""
# SSL/TLS Configuration (Let's Encrypt)
SSL_CERT_PATH={cert_path}
SSL_KEY_PATH={key_path}
"""

    with open(ENV_FILE, "a") as f:
        f.write(ssl_config)

    print(f"✓ SSL configuration added to {ENV_FILE}")
    print(f"  Certificate: {cert_path}")
    print(f"  Private key: {key_path}")

def enable_and_start_service():
    """Enable and start the systemd service."""
    print("\n=== Enabling and Starting Service ===")

    # Reload systemd
    run_command(["systemctl", "daemon-reload"])
    print(" Systemd daemon reloaded")

    # Enable service (start on boot)
    run_command(["systemctl", "enable", SERVICE_NAME])
    print(f" Service enabled (will start on boot)")

    # Start service
    run_command(["systemctl", "start", SERVICE_NAME])
    print(f" Service started")

    # Wait a moment for service to start
    time.sleep(2)

    # Check status
    result = run_command(["systemctl", "status", SERVICE_NAME], check=False)

    if result.returncode == 0:
        print(" Service is running")
        return True
    else:
        print("� Service may have issues. Check logs with:")
        print(f"  sudo journalctl -u {SERVICE_NAME} -f")
        return False

def print_summary():
    """Print installation/update summary and useful commands."""
    print("\n" + "="*60)
    print("🎮 tama Server Installation/Update Complete!")
    print("="*60)
    print(f"\nService Management:")
    print(f"  Start:   sudo systemctl start {SERVICE_NAME}")
    print(f"  Stop:    sudo systemctl stop {SERVICE_NAME}")
    print(f"  Restart: sudo systemctl restart {SERVICE_NAME}")
    print(f"  Status:  sudo systemctl status {SERVICE_NAME}")

    print(f"\nLog Files:")
    print(f"  Application: {LOG_DIR}/server.log")
    print(f"  Errors:      {LOG_DIR}/error.log")
    print(f"  Live logs:   sudo journalctl -u {SERVICE_NAME} -f")

    print(f"\nConfiguration:")
    print(f"  Environment: {ENV_FILE}")
    print(f"  Service:     {SYSTEMD_SERVICE_PATH}")
    print(f"  Data dir:    {DATA_DIR}")

    print(f"\nServer Details:")
    print(f"  Running as:  {SERVICE_USER}")
    print(f"  Domain:      {DOMAIN}")
    print(f"  Listening:   0.0.0.0:80")
    print(f"  Binary:      {SERVER_BINARY_DEST}")

    # Check if SSL is configured
    cert_path = CERT_DIR / "live" / DOMAIN / "fullchain.pem"
    if cert_path.exists():
        print(f"\nSSL/TLS (Let's Encrypt):")
        print(f"  Certificate: {cert_path}")
        print(f"  Private key: {CERT_DIR}/live/{DOMAIN}/privkey.pem")
        print(f"  Auto-renewal: Enabled (via certbot.timer)")
        print(f"  Check renewal: sudo certbot renew --dry-run")
        print(f"  Renewal timer status: sudo systemctl status certbot.timer")

    print("\nNext Steps:")
    print("  1. Verify server is running:")
    print(f"     curl https://localhost/stats")
    if cert_path.exists():
        print(f"     curl https://{DOMAIN}/stats")
    print("  2. Check logs for any issues:")
    print(f"     sudo tail -f {LOG_DIR}/server.log")
    if not cert_path.exists():
        print("  3. SSL certificate not found. Make sure:")
        print(f"     - DNS for {DOMAIN} points to this server")
        print("     - Then run: sudo certbot certonly --standalone -d " + DOMAIN)
    print("\n")

def git_pull():
    os.system("cd /root/tama && git pull origin $(git branch --show-current)")

def main():
    """Main installation flow."""
    print("="*60)
    print("tama Game Server - Production Installation")
    print("="*60)
    print(f"Project root: {PROJECT_ROOT}")
    print(f"Target user:  {SERVICE_USER}")
    print(f"Domain:       {DOMAIN}")
    print()

    try:
        check_root()
        check_rust()
        git_pull()
        build_server()
        install_binary()
        create_directories()
        install_static_files()
        create_env_file()
        create_systemd_service()
        setup_logrotate()
        configure_firewall()
        check_database()
        run_migrations()
        install_certbot()
        obtain_ssl_certificate()
        setup_certbot_renewal()
        update_env_for_ssl()
        is_running = enable_and_start_service()
        print_summary()

        if not is_running:
            print(f"Service is not running, rebooting in...")
            for i in range(0, 10):
                print(f"... {10 - i}...")
                time.sleep(1)
            os.system("sudo shutdown -r now")

    except KeyboardInterrupt:
        print("\n\n Installation cancelled by user")
        sys.exit(1)
    except Exception as e:
        print(f"\n\n Installation failed: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()
