# Redis POC - Setup and Run Guide

## 🚀 Quick Start

This guide will walk you through setting up and running the Redis Sentinel POC from scratch, including all dependencies and configurations.

---

## 📋 Prerequisites

### **Required Software:**
- **Java 17+** (JDK 17 or higher)
- **Maven 3.6+** 
- **Docker & Docker Compose**
- **Git** (to clone the repository)

### **Verify Prerequisites:**
```bash
# Check Java version
java -version
# Should show: openjdk version "17" or higher

# Check Maven
mvn -version
# Should show: Apache Maven 3.6+ 

# Check Docker
docker --version
docker-compose --version
```

---

## 🛠️ Step 1: Project Setup

### **1.1 Clone the Repository**
```bash
git clone <your-repo-url>
cd redisPoc
```

### **1.2 Project Structure Overview**
```
redisPoc/
├── src/main/java/com/poc/redis/
│   ├── config/
│   │   ├── ApplicationProperties.java    # Redis configuration
│   │   └── RedisConfig.java             # Connection factories & templates
│   ├── controller/
│   │   └── UserController.java          # REST API endpoints
│   ├── service/
│   │   ├── UserService.java             # Business logic with cache annotations
│   │   └── RedisService.java            # Direct Redis operations
│   ├── model/
│   │   └── User.java                    # Entity model
│   └── redisCacheManagement/
│       ├── FallbackRedisCacheManager.java  # Master-replica cache manager
│       └── FallbackRedisCache.java         # Cache implementation
├── src/main/resources/
│   └── application.yml                  # Application configuration
├── docker-compose.yml                  # Redis Sentinel cluster setup
├── sentinel1.conf                      # Sentinel 1 configuration
├── sentinel2.conf                      # Sentinel 2 configuration
├── sentinel3.conf                      # Sentinel 3 configuration
└── pom.xml                             # Maven dependencies
```

---

## 🐳 Step 2: Redis Setup

### **2.1 Start Redis Master & Replica (Standalone Mode)**
```bash
# Start only Redis master and replica (for initial testing)
docker-compose up -d redis-master redis-replica

# Verify containers are running
docker-compose ps
```

**Expected Output:**
```
NAME            STATUS                    PORTS
redis-master    Up (healthy)             0.0.0.0:6379->6379/tcp
redis-replica   Up (healthy)             0.0.0.0:6380->6379/tcp
```

### **2.2 Test Redis Connectivity**
```bash
# Test master connection
docker exec -it redis-master redis-cli ping
# Should return: PONG

# Test replica connection  
docker exec -it redis-replica redis-cli ping
# Should return: PONG

# Verify replication
docker exec -it redis-master redis-cli SET test "hello"
docker exec -it redis-replica redis-cli GET test
# Should return: "hello"
```

---

## 🏃‍♂️ Step 3: Application Setup

### **3.1 Configure Application**
Edit `src/main/resources/application.yml`:

```yaml
spring:
  data:
    redis:
      # Standalone configuration (for initial testing)
      host: localhost
      port: 6379
      
      # Sentinel configuration (disable for now)
      sentinel:
        enabled: false  # Start with standalone mode
        master: mymaster
        nodes:
          - localhost:26379
          - localhost:26380
          - localhost:26381
```

### **3.2 Build the Application**
```bash
# Clean and compile
mvn clean compile

# Run tests (optional)
mvn test

# Package the application (optional)
mvn package
```

### **3.3 Start the Application**
```bash
# Start Spring Boot application
mvn spring-boot:run
```

**Expected Output:**
```
INFO  - Master RedisTemplate configured successfully
INFO  - Replica RedisTemplate configured successfully
INFO  - Creating STANDALONE Master Connection Factory
INFO  - Creating STANDALONE Replica Connection Factory
INFO  - Started RedisPocApplication in X.XXX seconds
```

---

## ✅ Step 4: Verify Setup

### **4.1 Health Check**
```bash
# Check application health
curl http://localhost:9090/actuator/health
```

**Expected Response:**
```json
{
  "status": "UP",
  "components": {
    "redis": {
      "status": "UP",
      "details": {
        "master": "UP",
        "replica": "UP",
        "message": "Redis Sentinel cluster is up and running"
      }
    }
  }
}
```

### **4.2 Test API Endpoints**

#### **Create a User:**
```bash
curl -X POST http://localhost:9090/users \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com","designation":"Developer"}'
```

#### **Get All Users:**
```bash
curl http://localhost:9090/users
```

#### **Get User by ID:**
```bash
curl http://localhost:9090/users/1
```

### **4.3 Verify Cache Operations**
```bash
# Check cache keys in Redis master
docker exec -it redis-master redis-cli KEYS "*"

# Expected output:
# 1) "masterNode-> users::1"
# 2) "masterNode-> users::all-users"

# Check cache keys in Redis replica
docker exec -it redis-replica redis-cli KEYS "*"

# Expected output:
# 1) "replicaNode-> users::1" 
# 2) "replicaNode-> users::all-users"
```

---

## 🎯 Step 5: Redis Sentinel Setup (High Availability)

### **5.1 Start Full Sentinel Cluster**
```bash
# Stop current containers
docker-compose down

# Start complete setup with Sentinels
docker-compose up -d

# Verify all services are running
docker-compose ps
```

**Expected Output:**
```
NAME               STATUS                    PORTS
redis-master       Up (healthy)             0.0.0.0:6379->6379/tcp
redis-replica      Up (healthy)             0.0.0.0:6380->6379/tcp
redis-sentinel-1   Up                       0.0.0.0:26379->26379/tcp
redis-sentinel-2   Up                       0.0.0.0:26380->26379/tcp  
redis-sentinel-3   Up                       0.0.0.0:26381->26379/tcp
```

### **5.2 Enable Sentinel Mode in Application**
Update `application.yml`:
```yaml
spring:
  data:
    redis:
      sentinel:
        enabled: true  # Enable Sentinel mode
```

### **5.3 Restart Application**
```bash
# Stop the application (Ctrl+C)
# Restart with Sentinel mode
mvn spring-boot:run
```

**Expected Output:**
```
INFO  - Creating SENTINEL Master Connection Factory
INFO  - Creating SENTINEL Replica Connection Factory
INFO  - Added Sentinel node: localhost:26379
INFO  - Added Sentinel node: localhost:26380  
INFO  - Added Sentinel node: localhost:26381
```

---

## 🧪 Step 6: Test High Availability

### **6.1 Test Normal Operations**
```bash
# Create some test data
curl -X POST http://localhost:9090/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@example.com"}'

# Verify data is cached
docker exec -it redis-master redis-cli KEYS "*users*"
```

### **6.2 Test Automatic Failover**

#### **Monitor Sentinel Activity:**
```bash
# In terminal 1 - Monitor Sentinel logs
docker logs -f redis-sentinel-1

# In terminal 2 - Monitor Redis operations
docker exec -it redis-master redis-cli MONITOR
```

#### **Simulate Master Failure:**
```bash
# Stop Redis master
docker stop redis-master

# Watch sentinel logs for failover messages
# Should see: "Replica promoted to master"
```

#### **Test Application During Failover:**
```bash
# Application should continue working
curl http://localhost:9090/users/1

# Check new master (should be the former replica)
docker exec -it redis-sentinel-1 redis-cli -p 26379 SENTINEL masters
```

#### **Restore Master:**
```bash
# Restart original master (becomes replica)
docker start redis-master

# It will automatically rejoin as replica
```

---

## 🔍 Step 7: Monitoring and Debugging

### **7.1 Redis Monitoring Commands**

#### **Check Sentinel Status:**
```bash
# Connect to any Sentinel
docker exec -it redis-sentinel-1 redis-cli -p 26379

# Check master info
SENTINEL masters

# Check replicas
SENTINEL replicas mymaster

# Check other sentinels
SENTINEL sentinels mymaster
```

#### **Check Replication Status:**
```bash
# On master
docker exec -it redis-master redis-cli INFO replication

# On replica  
docker exec -it redis-replica redis-cli INFO replication
```

### **7.2 Application Monitoring**

#### **Real-time Cache Monitoring:**
```bash
# Monitor Redis operations
docker exec -it redis-master redis-cli MONITOR

# In another terminal, make API calls
curl http://localhost:9090/users/1
```

#### **Cache Statistics:**
```bash
# Check cache hit/miss ratios
docker exec -it redis-master redis-cli INFO stats

# Check memory usage
docker exec -it redis-master redis-cli INFO memory
```

### **7.3 Application Logs**
Monitor application logs for:
- `Writing to master: ...` (cache writes)
- `Reading from replica: ...` (cache reads)
- `Failed to read from replica, falling back to master` (fallback scenarios)

---

## 🚨 Troubleshooting

### **Common Issues:**

#### **1. Application Won't Start**
```bash
# Check Java version
java -version

# Clean and rebuild
mvn clean compile

# Check Redis connectivity
docker exec -it redis-master redis-cli ping
```

#### **2. Sentinels Failing to Start**
```bash
# Check Sentinel logs
docker logs redis-sentinel-1

# Common fix: restart with clean slate
docker-compose down -v
docker-compose up -d
```

#### **3. Cache Not Working**
```bash
# Verify Redis connection
curl http://localhost:9090/actuator/health

# Check Redis keys
docker exec -it redis-master redis-cli KEYS "*"

# Monitor Redis operations
docker exec -it redis-master redis-cli MONITOR
```

#### **4. Failover Not Working**
```bash
# Check Sentinel quorum
docker exec -it redis-sentinel-1 redis-cli -p 26379 SENTINEL ckquorum mymaster

# Verify Sentinel configuration
docker exec -it redis-sentinel-1 cat /etc/redis/sentinel.conf
```

---

## 🎯 Success Criteria

✅ **Redis Containers Running**: Master, replica, and 3 sentinels all UP  
✅ **Application Started**: Spring Boot app running on port 9090  
✅ **API Responding**: All CRUD operations working  
✅ **Cache Working**: Keys visible in both master and replica Redis  
✅ **Health Check Passing**: `/actuator/health` returns UP status  
✅ **Automatic Failover**: Master failure promotes replica automatically  
✅ **Zero Downtime**: Application continues working during failover  

---

## 🎉 What's Next?

- **Production Deployment**: Configure for production environment
- **Monitoring Setup**: Add Prometheus/Grafana monitoring  
- **Security**: Configure Redis AUTH and SSL/TLS
- **Scaling**: Add more replica nodes for read scaling
- **Backup Strategy**: Implement Redis persistence and backup

Your Redis Sentinel High Availability setup is now complete and production-ready! 🚀