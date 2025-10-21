# Redis Sentinel Configuration Guide

## Overview
Redis Sentinel configuration files define how each Sentinel instance monitors and manages Redis master/replica failover.

## Configuration Files Explanation

### sentinel1.conf, sentinel2.conf, sentinel3.conf

Each file configures a separate Sentinel instance with these key settings:

## 🔧 Configuration Parameters

### 1. **Port Configuration**
```conf
port 26379  # sentinel1
port 26379  # sentinel2 (mapped to 26380 in Docker)
port 26379  # sentinel3 (mapped to 26381 in Docker)
```
- **Purpose**: Defines the port each Sentinel listens on
- **Docker Mapping**: Docker maps internal port 26379 to external ports 26379, 26380, 26381

### 2. **Master Monitoring**
```conf
sentinel monitor mymaster redis-master 6379 2
```
- **`mymaster`**: Name of the master (can be any name)
- **`redis-master`**: Hostname/IP of the Redis master
- **`6379`**: Port of the Redis master
- **`2`**: **Quorum** - Number of Sentinels that must agree master is down before failover

### 3. **Failure Detection**
```conf
sentinel down-after-milliseconds mymaster 5000
```
- **Purpose**: Time (5 seconds) to wait before considering master as down
- **Impact**: Faster = quicker failover, but more false positives
- **Recommendation**: 5-30 seconds for production

### 4. **Failover Timeout**
```conf
sentinel failover-timeout mymaster 10000
```
- **Purpose**: Maximum time (10 seconds) allowed for failover process
- **Impact**: If failover takes longer, it's considered failed
- **Recommendation**: 3x down-after-milliseconds

### 5. **Parallel Sync Limit**
```conf
sentinel parallel-syncs mymaster 1
```
- **Purpose**: Number of replicas that can sync with new master simultaneously
- **Impact**: Higher = faster sync, but more load on new master
- **Recommendation**: 1 for stability, higher for faster recovery

### 6. **Announce Settings**
```conf
sentinel announce-ip 127.0.0.1
sentinel announce-port 26379/26380/26381
```
- **Purpose**: How this Sentinel announces itself to other Sentinels
- **Impact**: Critical for Sentinel cluster discovery
- **Docker**: Must match the external ports

## 🔄 How Sentinels Work Together

### Consensus Process
```
Sentinel 1: "Master is down!"
Sentinel 2: "I agree, master is down!"
Sentinel 3: "I also agree!"
Result: 3/3 agree → Quorum reached (need 2) → Start failover
```

### Failover Process
1. **Detection**: 2+ Sentinels agree master is down
2. **Election**: Sentinels elect a leader to perform failover
3. **Promotion**: Leader promotes best replica to master
4. **Reconfiguration**: All Sentinels update their configuration
5. **Notification**: Your application gets new master address

## 🏗️ Docker Integration

In your `docker-compose.yml`:

```yaml
redis-sentinel-1:
  ports:
    - "26379:26379"
  volumes:
    - ./sentinel1.conf:/etc/redis/sentinel.conf

redis-sentinel-2:
  ports:
    - "26380:26379"  # External 26380 → Internal 26379
  volumes:
    - ./sentinel2.conf:/etc/redis/sentinel.conf

redis-sentinel-3:
  ports:
    - "26381:26379"  # External 26381 → Internal 26379
  volumes:
    - ./sentinel3.conf:/etc/redis/sentinel.conf
```

## 🔍 Why This Configuration?

### **Quorum = 2** (out of 3 Sentinels)
- **Prevents Split Brain**: Need majority to make decisions
- **Fault Tolerance**: Can lose 1 Sentinel and still work
- **Prevents False Positives**: Single Sentinel can't trigger failover alone

### **down-after-milliseconds = 5000**
- **Balance**: Fast enough for quick failover, slow enough to avoid false alarms
- **Network Tolerance**: Allows for temporary network hiccups

### **parallel-syncs = 1**
- **Conservative**: Only 1 replica syncs at a time
- **Stability**: Prevents overwhelming the new master
- **Gradual Recovery**: Ensures stable state during failover

## 📊 Monitoring Commands

You can connect to any Sentinel to check status:

```bash
# Connect to Sentinel 1
docker exec -it redis-sentinel-1 redis-cli -p 26379

# Check master info
SENTINEL masters

# Check replicas
SENTINEL replicas mymaster

# Check other sentinels
SENTINEL sentinels mymaster

# Check if master is down
SENTINEL ckquorum mymaster
```

## 🎯 Key Benefits

✅ **High Availability**: Automatic failover without manual intervention  
✅ **Consensus**: Multiple Sentinels prevent false failovers  
✅ **Service Discovery**: Your app finds new master automatically  
✅ **Monitoring**: Continuous health checking  
✅ **Notification**: Real-time alerts about topology changes  

The three configuration files ensure your Redis Sentinel cluster works reliably with proper consensus and failover capabilities!