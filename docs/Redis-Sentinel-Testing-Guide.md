# Redis Sentinel Testing Guide

## Overview
This guide explains how to test your Redis Sentinel setup with automatic failover and replica reading capabilities.

## Quick Start

### 1. Start Redis Sentinel Cluster
```bash
# Start the Redis Sentinel cluster
docker-compose up -d

# Check if all services are running
docker-compose ps
```

### 2. Verify Sentinel Configuration
```bash
# Connect to Sentinel 1
docker exec -it redis-sentinel-1 redis-cli -p 26379

# Check master info
SENTINEL masters

# Check replicas
SENTINEL replicas mymaster

# Check sentinel nodes
SENTINEL sentinels mymaster
```

### 3. Test Your Application
```bash
# Compile and run your Spring Boot application
mvn clean compile
mvn spring-boot:run
```

## Testing Scenarios

### Scenario 1: Normal Operation
1. **Test Write Operations**:
   ```bash
   # Make a POST request to create data
   curl -X POST http://localhost:9090/users \
     -H "Content-Type: application/json" \
     -d '{"name":"John Doe","email":"john@example.com"}'
   ```

2. **Test Read Operations**:
   ```bash
   # Make a GET request to read data
   curl http://localhost:9090/users/1
   ```

3. **Check Logs**: Your application should show:
   - `Writing to master: ...`
   - `Reading from replica: ...`

### Scenario 2: Master Failover Test
1. **Stop the Master**:
   ```bash
   docker stop redis-master
   ```

2. **Monitor Failover**:
   ```bash
   # Watch sentinel logs
   docker logs -f redis-sentinel-1
   
   # Check new master
   docker exec -it redis-sentinel-1 redis-cli -p 26379 SENTINEL masters
   ```

3. **Test Application**:
   - Your application should continue working
   - New writes will go to the promoted replica
   - **No code changes needed!**

4. **Restore Master**:
   ```bash
   docker start redis-master
   # It will become a replica of the new master
   ```

### Scenario 3: Replica Failure Test
1. **Stop the Replica**:
   ```bash
   docker stop redis-replica
   ```

2. **Test Reads**:
   - Read operations should automatically fallback to master
   - Check logs for: `Failed to read from replica, falling back to master`

3. **Restore Replica**:
   ```bash
   docker start redis-replica
   ```

## Application Endpoints for Testing

### User Management
- `POST /users` - Create user (writes to master)
- `GET /users/{id}` - Get user (reads from replica)
- `PUT /users/{id}` - Update user (writes to master)
- `DELETE /users/{id}` - Delete user (writes to master)

### Redis Operations
- `POST /redis/set/{key}` - Set value (writes to master)
- `GET /redis/get/{key}` - Get value (reads from replica)
- `DELETE /redis/delete/{key}` - Delete key (writes to master)

### Health Check
- `GET /actuator/health` - Check both master and replica health

## Configuration Options

### Enable/Disable Sentinel
In `application.yml`:
```yaml
spring:
  data:
    redis:
      sentinel:
        enabled: true  # Set to false for standalone mode
```

### Switch Read Strategy
In `RedisConfig.java`, you can change read strategies:
- `ReadFrom.MASTER` - Always read from master
- `ReadFrom.REPLICA` - Always read from replica (fails if replica down)
- `ReadFrom.REPLICA_PREFERRED` - Prefer replica, fallback to master
- `ReadFrom.LOWEST_LATENCY` - Choose node with lowest latency

## Monitoring Commands

### Check Redis Replication
```bash
# Connect to current master
docker exec -it redis-master redis-cli INFO replication

# Connect to replica
docker exec -it redis-replica redis-cli INFO replication
```

### Monitor Sentinel
```bash
# Real-time monitoring
docker exec -it redis-sentinel-1 redis-cli -p 26379 MONITOR
```

## Troubleshooting

### Common Issues

1. **Connection Refused**:
   - Ensure all containers are running
   - Check port mappings
   - Verify network connectivity

2. **Sentinel Not Detecting Failure**:
   - Check `down-after-milliseconds` configuration
   - Verify quorum settings (should be 2 for 3 sentinels)

3. **Application Not Reconnecting**:
   - Check Lettuce client configuration
   - Verify Sentinel nodes in application.yml
   - Look for connection pool issues

### Useful Docker Commands
```bash
# View all logs
docker-compose logs

# Restart specific service
docker-compose restart redis-master

# Clean up
docker-compose down -v
```

## Benefits Demonstrated

✅ **High Availability**: Automatic failover when master fails  
✅ **Read Scaling**: Distribute read load to replicas  
✅ **Zero Downtime**: Seamless failover without application restart  
✅ **Monitoring**: Built-in health checks and status monitoring  
✅ **Graceful Degradation**: Fallback mechanisms when components fail  

Your Redis Sentinel setup is now production-ready with automatic failover capabilities!