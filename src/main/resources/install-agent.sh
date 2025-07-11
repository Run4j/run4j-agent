#!/bin/bash

echo "🔧 Installing Run4j VPS Agent..."

# Update system & install dependencies
apt update
apt install -y podman openjdk-21-jdk curl postgresql postgresql-contrib cockpit cockpit-podman -y

# Enable and start PostgreSQL
systemctl enable postgresql
systemctl start postgresql

# Set password for default 'postgres' user
sudo -u postgres psql -c "ALTER USER postgres PASSWORD 'your_root_pg_password';"

# Optional: adjust pg_hba.conf for local md5 auth
PG_HBA="/etc/postgresql/$(ls /etc/postgresql)/main/pg_hba.conf"
sed -i 's/local\s\+all\s\+postgres\s\+peer/local all postgres md5/' $PG_HBA
systemctl restart postgresql

# Enable and start Cockpit UI (for Podman web interface)
systemctl enable --now cockpit.socket

# ✅ Allow root login in Cockpit (remove from disallowed-users)
DISALLOWED_USERS_FILE="/etc/cockpit/disallowed-users"
if grep -q "^root" "$DISALLOWED_USERS_FILE"; then
    sed -i '/^root$/d' "$DISALLOWED_USERS_FILE"
    echo "👤 Removed 'root' from Cockpit disallowed-users."
fi

# Create agent directory
mkdir -p /opt/run4j-agent
cd /opt/run4j-agent


# Download latest release JAR dynamically from GitHub
LATEST_URL=$(curl -s https://api.github.com/repos/Run4j/run4j-agent/releases/latest \
  | grep "browser_download_url" \
  | grep -E "run4j-agent.*\.jar" \
  | cut -d '"' -f 4)

curl -Lo run4j-vps-agent-latest.jar "$LATEST_URL"

## Download latest agent JAR
#curl -LO https://run4j.io/releases/run4j-vps-agent-latest.jar


# Create systemd service for the Run4j Agent
cat <<EOF > /etc/systemd/system/run4j-agent.service
[Unit]
Description=Run4j VPS Agent
After=network.target postgresql.service

[Service]
ExecStart=/usr/bin/java -jar /opt/run4j-agent/run4j-vps-agent-latest.jar
Restart=always
User=root
WorkingDirectory=/opt/run4j-agent

[Install]
WantedBy=multi-user.target
EOF

# Start and enable Run4j agent
systemctl daemon-reexec
systemctl daemon-reload
systemctl enable run4j-agent
systemctl start run4j-agent

echo "✅ Run4j Agent installed and started!"
echo "🌐 Cockpit UI available at: https://<YOUR-VPS-IP>:9090"
echo "ℹ️  Login with your VPS root or sudo user credentials"
