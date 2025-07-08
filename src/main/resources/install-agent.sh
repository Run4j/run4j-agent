#!/bin/bash

echo "🔧 Installing Run4j VPS Agent..."

# Update system & install dependencies
apt update
apt install -y podman openjdk-21-jdk curl postgresql postgresql-contrib

# Enable and start PostgreSQL
systemctl enable postgresql
systemctl start postgresql

# Set password for default 'postgres' user
sudo -u postgres psql -c "ALTER USER postgres PASSWORD 'your_root_pg_password';"

# Optional: adjust pg_hba.conf for local md5 auth
PG_HBA="/etc/postgresql/$(ls /etc/postgresql)/main/pg_hba.conf"
sed -i 's/local\s\+all\s\+postgres\s\+peer/local all postgres md5/' $PG_HBA
systemctl restart postgresql

# Create agent directory
mkdir -p /opt/run4j-agent
cd /opt/run4j-agent

# Download latest agent JAR
curl -LO https://run4j.io/releases/run4j-vps-agent-latest.jar
# or use private download:
# curl -LO https://master.run4j.com/files/view/2

# Optional: write systemd service
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

# Start agent with systemd
systemctl daemon-reexec
systemctl daemon-reload
systemctl enable run4j-agent
systemctl start run4j-agent

echo "✅ Run4j Agent installed and started!"
