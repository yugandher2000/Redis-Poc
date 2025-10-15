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
    RedisService redisService;
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
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        User updatedUser = userService.updateUser(id, user);
        if (updatedUser != null) {
            return ResponseEntity.ok(updatedUser);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping(value = "/redis", produces = {MediaType.APPLICATION_JSON_VALUE})
    public Health redisCheck() throws Exception {
        return redisService.health();
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
}
