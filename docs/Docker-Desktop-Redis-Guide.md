# Docker Desktop Redis Monitoring Guide

## 🖥️ Step-by-Step: Using Docker Desktop

### Step 1: Open Docker Desktop
1. **Launch Docker Desktop** from your system tray or applications
2. **Click on "Containers"** tab in the left sidebar
3. You should see your running containers:
   - `redis-master` (port 6379)
   - `redis-replica` (port 6380)

### Step 2: Access Redis CLI through Docker Desktop
1. **Click on the `redis-master` container**
2. **Click on the "Exec" tab** (next to Logs, Inspect, etc.)
3. **In the command field, type:** `redis-cli`
4. **Click "Run"** or press Enter
5. You'll see the Redis prompt: `127.0.0.1:6379>`

### Step 3: Repeat for Replica
1. **Click on the `redis-replica` container**
2. **Click on the "Exec" tab**
3. **Type:** `redis-cli`
4. **Click "Run"**
5. You'll see: `127.0.0.1:6379>` (internal port)

## 🏥 Health Check Commands

### Master Health Check
In the **redis-master** container Exec tab:

```redis
# Basic connectivity
PING
# Expected: PONG

# Server information
INFO server
# Shows Redis version, uptime, etc.

# Memory usage
INFO memory
# Shows memory consumption

# Replication status (as master)
INFO replication
# Should show:
# role:master
# connected_slaves:1
```

### Replica Health Check
In the **redis-replica** container Exec tab:

```redis
# Basic connectivity
PING
# Expected: PONG

# Replication status (as replica)
INFO replication
# Should show:
# role:slave
# master_host:redis-master
# master_port:6379
# master_link_status:up

# Check if replication is working
LASTSAVE
# Shows last save time
```

## 🔄 Data Synchronization Verification

### Method 1: Real-time Sync Test

#### In Master Container (Docker Desktop):
```redis
# Set a test key
SET sync:test "Hello from Master"
SET user:123 "John Doe"
SET cache:timestamp "2025-10-21"

# Check what we just set
GET sync:test
```

#### In Replica Container (Docker Desktop):
```redis
# Check if data synced
GET sync:test
# Should return: "Hello from Master"

GET user:123
# Should return: "John Doe"

GET cache:timestamp
# Should return: "2025-10-21"
```

### Method 2: Application Cache Sync Test

#### Step 1: Generate Cache Data
**First, make API calls to your application:**

**Open PowerShell and run:**
```powershell
# Create a user (this will cache data)
Invoke-WebRequest -Uri "http://localhost:9090/users" -Method POST -Headers @{"Content-Type"="application/json"} -Body '{"name":"Test User","email":"test@example.com"}'

# Get all users (this will cache the list)
Invoke-WebRequest -Uri "http://localhost:9090/users" -Method GET
```

#### Step 2: Check Cache in Both Nodes

**In Master Container (Docker Desktop):**
```redis
# List all cache keys
KEYS "*"

# Check specific cache keys
KEYS "masterNode*"

# Get cached user data
GET "masterNode-> users::all-users"
```

**In Replica Container (Docker Desktop):**
```redis
# List all cache keys (should be similar to master)
KEYS "*"

# Check if application data is syncing
KEYS "masterNode*"

# Get the same cached data
GET "masterNode-> users::all-users"
```

## 📊 Advanced Health Monitoring

### Comprehensive Health Check Script

**Run these commands in both Master and Replica:**

```redis
# 1. Basic Status
INFO server

# 2. Performance Metrics
INFO stats

# 3. Memory Usage
INFO memory

# 4. Replication Health
INFO replication

# 5. Client Connections
INFO clients

# 6. Keyspace Information
INFO keyspace

# 7. Persistence Status
LASTSAVE
```

### Monitor Real-time Activity

**In Master Container:**
```redis
# Watch all commands in real-time
MONITOR
```

**Then make API calls to your application and watch the commands flow!**

## 🚨 Health Status Indicators

### ✅ Healthy Master Signs:
```
role:master
connected_slaves:1
master_repl_offset:>0
```

### ✅ Healthy Replica Signs:
```
role:slave
master_host:redis-master
master_port:6379
master_link_status:up
master_last_io_seconds_ago:<30
```

### ❌ Warning Signs:
```
master_link_status:down
master_last_io_seconds_ago:>60
connected_slaves:0 (on master)
```

## 🔧 Docker Desktop Container Logs

### View Container Logs:
1. **Click on container** (redis-master or redis-replica)
2. **Click "Logs" tab**
3. **Look for:**
   - Connection messages
   - Replication sync messages
   - Error messages

### Useful Log Patterns:
```
# Successful replication
"Replica ... asks for synchronization"
"Full resync requested by replica"
"Synchronization with replica ... succeeded"

# Health indicators
"Ready to accept connections"
"Background saving started"
```

## 🎯 Quick Sync Verification Commands

### PowerShell Commands (Run from your terminal):

```powershell
# Check master directly
docker exec -it redis-master redis-cli PING

# Check replica directly  
docker exec -it redis-replica redis-cli PING

# Quick sync test
docker exec -it redis-master redis-cli SET test:sync "$(Get-Date)"
docker exec -it redis-replica redis-cli GET test:sync

# Check replication status
docker exec -it redis-master redis-cli INFO replication
docker exec -it redis-replica redis-cli INFO replication
```

## 📱 Docker Desktop Mobile/Quick Actions

### Container Actions in Docker Desktop:
1. **Restart Container:** Click ⟲ restart icon
2. **Stop Container:** Click ⏹️ stop icon  
3. **View Stats:** Click on "Stats" tab for real-time metrics
4. **Export Logs:** Click "..." menu → Export logs

### Quick Container Stats:
- **CPU Usage:** Should be low unless under load
- **Memory Usage:** Monitor for memory leaks
- **Network I/O:** Shows replication traffic

## 🛠️ Troubleshooting Guide

### If Replication is Broken:

**In Master:**
```redis
# Check connected slaves
INFO replication
# Should show connected_slaves:1

# Manual sync trigger
BGSAVE
```

**In Replica:**
```redis
# Force resync with master
SLAVEOF redis-master 6379

# Check sync status
INFO replication
```

### Performance Monitoring:
```redis
# Check slow queries
SLOWLOG GET 10

# Monitor memory efficiency
MEMORY STATS

# Check hit/miss ratio
INFO stats
# Look at: keyspace_hits vs keyspace_misses
```

## 🎉 Quick Test Checklist

### ✅ Verification Checklist:

1. **Both containers running:** ✅ Green status in Docker Desktop
2. **Master PING:** ✅ Returns PONG  
3. **Replica PING:** ✅ Returns PONG
4. **Replication status:** ✅ Master shows 1 slave, Replica shows master connection
5. **Data sync:** ✅ Set key in master, get same key from replica
6. **Application cache:** ✅ API calls create cache entries visible in both nodes
7. **No errors in logs:** ✅ Clean logs in Docker Desktop

Use Docker Desktop's visual interface for easy monitoring, and these commands will help you verify everything is working perfectly! 🚀