# Redis Sentinel Architecture - Text Diagram

## ASCII Architecture Diagram

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

## Component Flow Explanation:

### 1. Configuration Layer
- **application.yml** → Contains Sentinel nodes and master name
- **ApplicationProperties** → Binds YAML config to Java objects  
- **RedisConfig** → Creates connection factories and templates

### 2. Connection Strategy
- **Master Connection Factory** → `ReadFrom.MASTER` for writes
- **Replica Connection Factory** → `ReadFrom.REPLICA_PREFERRED` for reads

### 3. Service Layer
- **UserController** → REST endpoints
- **UserService** → Business logic with `@Cacheable` annotations
- **RedisService** → Direct Redis operations with master/replica logic

### 4. Failover Process
```
Master Fails → Sentinels Vote → Promote Replica → App Reconnects → Continue
     ↓              ↓               ↓              ↓              ↓
   5 sec         2/3 agree      Replica→Master   Automatic    Zero Downtime
```

## Key Benefits:

🔄 **Automatic Failover**: Replica becomes master when needed  
📊 **Read Scaling**: Reads from replica, writes to master  
🛡️ **High Availability**: 3 sentinels ensure consensus  
⚡ **Zero Downtime**: Lettuce client handles reconnection  
📈 **Performance**: Load distribution across nodes