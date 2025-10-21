# Redis POC with Sentinel High Availability

## Overview

This project demonstrates a Redis implementation with **Redis Sentinel** for high availability, automatic failover, and read/write separation using master and replica nodes.

## Key Features

- ✅ **Redis Sentinel** for automatic failover
- ✅ **Master-Replica** architecture with read/write separation
- ✅ **Automatic failover** when master fails
- ✅ **Read from replica** to distribute load
- ✅ **Fallback mechanism** from replica to master
- ✅ **Spring Boot Cache** integration with Redis
- ✅ **Health monitoring** for both master and replica

## Architecture

### Redis Sentinel High Availability Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           Spring Boot Application                               │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌─────────────────┐              ┌─────────────────┐                          │
│  │  UserController │              │   RedisService  │                          │
│  │                 │              │                 │                          │
│  │ - getAllUsers() │────────────▶ │ - setValue()    │ (WRITES)                │
│  │ - createUser()  │              │ - getValue()    │ (READS)                 │
│  │ - updateUser()  │              │ - deleteKey()   │ (DELETES)               │
│  │ - deleteUser()  │              │ - hasKey()      │ (READS)                 │
│  └─────────────────┘              └─────────────────┘                          │
│           │                                │                                   │
│           ▼                                ▼                                   │
│  ┌─────────────────┐              ┌─────────────────┐                          │
│  │   UserService   │              │   RedisConfig   │                          │
│  │                 │              │                 │                          │
│  │ @Cacheable      │              │ ┌─────────────┐ │                          │
│  │ @CachePut       │              │ │masterTemplate│ │──┐                       │
│  │ @CacheEvict     │              │ └─────────────┘ │  │                       │
│  └─────────────────┘              │ ┌─────────────┐ │  │                       │
│                                   │ │replicaTemplate│ │──┤                     │
│                                   │ └─────────────┘ │  │                       │
│                                   └─────────────────┘  │                       │
│                                                        │                       │
└────────────────────────────────────────────────────────┼───────────────────────┘
                                                         │
                   ┌─────────────────────────────────────┼───────────────────────┐
                   │            Redis Sentinel Cluster   │                       │
                   │                                      ▼                       │
                   │  ┌─────────────────┐    ┌─────────────────┐                 │
                   │  │ Master Template │    │Replica Template │                 │
                   │  │                 │    │                 │                 │
                   │  │ ReadFrom.MASTER │    │ReadFrom.REPLICA │                 │
                   │  │ (Writes Only)   │    │ (Reads Preferred)│                 │
                   │  └─────────────────┘    └─────────────────┘                 │
                   │           │                       │                         │
                   │           ▼                       ▼                         │
                   │  ┌─────────────────┐    ┌─────────────────┐                 │
                   │  │Sentinel Config  │    │Sentinel Config  │                 │
                   │  │                 │    │                 │                 │
                   │  │Master: mymaster │    │Master: mymaster │                 │
                   │  │Nodes: 26379,    │    │Nodes: 26379,    │                 │
                   │  │       26380,    │    │       26380,    │                 │
                   │  │       26381     │    │       26381     │                 │
                   │  └─────────────────┘    └─────────────────┘                 │
                   └─────────────────────────────────────────────────────────────┘
                                           │
            ┌──────────────────────────────┼──────────────────────────────┐
            │                              │                              │
            ▼                              ▼                              ▼
   ┌─────────────────┐          ┌─────────────────┐          ┌─────────────────┐
   │   Sentinel-1    │          │   Sentinel-2    │          │   Sentinel-3    │
   │                 │          │                 │          │                 │
   │   Port: 26379   │          │   Port: 26380   │          │   Port: 26381   │
   │                 │          │                 │          │                 │
   │ Monitor: master │          │ Monitor: master │          │ Monitor: master │
   │ Quorum: 2       │          │ Quorum: 2       │          │ Quorum: 2       │
   └─────────────────┘          └─────────────────┘          └─────────────────┘
            │                              │                              │
            └──────────────┬───────────────┴──────────────┬───────────────┘
                           │                              │
                           ▼                              ▼
                  ┌─────────────────┐          ┌─────────────────┐
                  │  Redis Master   │          │ Redis Replica   │
                  │                 │ ────────▶│                 │
                  │   Port: 6379    │ REPLICATE │   Port: 6380    │
                  │                 │          │                 │
                  │ ✅ Read/Write   │          │ ✅ Read Only    │
                  │ ✅ Auto-Failover│          │ ✅ Sync Data    │
                  └─────────────────┘          └─────────────────┘
```

## How Redis Sentinel Works

### Automatic Failover Process:

1. **Normal Operations:**
   - Master handles all write operations
   - Replica syncs data from master
   - Read operations prefer replica (load distribution)

2. **When Master Fails:**
   - Sentinels detect master failure (after 5 seconds)
   - Majority of sentinels (2 out of 3) agree master is down
   - Sentinels elect a new master from available replicas
   - **Replica automatically becomes the new Master**
   - Sentinels update configuration
   - Applications automatically connect to new master

3. **When Old Master Recovers:**
   - Old master rejoins as a replica
   - Data syncs from new master

## Configuration

### application.yml
```yaml
spring:
  data:
    redis:
      # Standalone fallback configuration
      host: localhost
      port: 6379
      password: # optional
      ttl: 60s
      
      # Sentinel configuration (Primary)
      sentinel:
        enabled: true
        master: mymaster
        password: # optional sentinel password
        database: 0
        enable-read-from-replica: true
        nodes:
          - localhost:26379
          - localhost:26380
          - localhost:26381
```

### Key Components

1. **ApplicationProperties.java**
   - Configures Sentinel settings
   - Supports both standalone and sentinel modes

2. **RedisConfig.java**
   - Creates separate connection factories for master and replica
   - Configures ReadFrom strategies (MASTER vs REPLICA_PREFERRED)
   - Provides fallback to standalone mode

3. **RedisService.java**
   - Implements read/write separation
   - Handles fallback from replica to master
   - Provides health monitoring

## Setup Instructions

### 1. Start Redis Sentinel Cluster
```bash
# Start the Redis Sentinel cluster with Docker Compose
docker-compose -f docker-compose-sentinel.yml up -d

# Verify all containers are running
docker ps
```

### 2. Configure Application
Update `application.yml` to enable Sentinel mode:
```yaml
sentinel:
  enabled: true  # Set to true to enable Sentinel
```

### 3. Run Application
```bash
mvn spring-boot:run
```

### 4. Test Failover
```bash
# Stop master to test automatic failover
docker stop redis-master

# Watch logs to see automatic failover
docker logs redis-sentinel-1

# Your application should automatically connect to new master
```

## Testing the Setup

### 1. Health Check
```bash
curl http://localhost:9090/actuator/health
```

### 2. Create User (Write to Master)
```bash
curl -X POST http://localhost:9090/users \
  -H "Content-Type: application/json" \
  -d '{"name": "John Doe", "email": "john@example.com"}'
```

### 3. Get User (Read from Replica)
```bash
curl http://localhost:9090/users/1
```

### 4. Test Failover
1. Stop master: `docker stop redis-master`
2. Make API calls - should still work
3. Check logs for failover messages

## Benefits of This Architecture

1. **High Availability**: No single point of failure
2. **Automatic Failover**: No manual intervention needed
3. **Load Distribution**: Reads from replica reduce master load
4. **Scalability**: Can add more replicas for read scaling
5. **Data Persistence**: Automatic replication ensures data safety

## Monitoring

The application provides health endpoints that monitor both master and replica connections:

- **Master Status**: Monitors write operations
- **Replica Status**: Monitors read operations  
- **Sentinel Status**: Monitors cluster health

## Fallback Strategy

- If replica fails → automatically read from master
- If master fails → Sentinel promotes replica to master
- If Sentinel fails → application falls back to standalone mode

This ensures your application remains resilient under various failure scenarios.