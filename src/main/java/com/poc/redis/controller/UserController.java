package com.poc.redis.controller;

import com.poc.redis.model.User;
import com.poc.redis.service.RedisService;
import com.poc.redis.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.boot.actuate.health.Health;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private RedisService redisService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<User>> createUserInBulk(@RequestBody List<User> users) {
        List<User> createdUsers = userService.createUsersInBulk(users);
        return ResponseEntity.ok(createdUsers);
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User createdUser = userService.createUser(user);
        return ResponseEntity.ok(createdUser);
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User user) {
        return userService.updateUser(id, user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping(value = "/redis", produces = {MediaType.APPLICATION_JSON_VALUE})
    public Health redisCheck() throws Exception {
        return redisService.checkRedisHealth();
    }
    @GetMapping("/cache/{key}")
    public Object getCacheValue(@PathVariable String key) {
        return redisTemplate.opsForValue().get(key);
    }
    @DeleteMapping("/deleteCache")
    public void deleteAllCache() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }
    @GetMapping(value = "/getKeys")
    public Object getAllKeys() {
        return redisTemplate.keys("*");
    }

    // Additional Redis operations using the simplified service
    @PostMapping("/redis/set/{key}")
    public ResponseEntity<String> setRedisValue(@PathVariable String key, @RequestBody Object value) {
        try {
            redisService.setValue(key, value);
            return ResponseEntity.ok("Value set successfully for key: " + key);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/redis/get/{key}")
    public ResponseEntity<Object> getRedisValue(@PathVariable String key) {
        try {
            Object value = redisService.getValue(key);
            return value != null ? ResponseEntity.ok(value) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/redis/delete/{key}")
    public ResponseEntity<String> deleteRedisKey(@PathVariable String key) {
        try {
            Boolean deleted = redisService.deleteKey(key);
            return deleted ? ResponseEntity.ok("Key deleted: " + key) : ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/redis/info")
    public ResponseEntity<String> getRedisInfo() {
        try {
            String info = redisService.getConnectionInfo();
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}
