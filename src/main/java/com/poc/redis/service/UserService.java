package com.poc.redis.service;

import com.poc.redis.dao.UserRepository;
import com.poc.redis.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UserService {
    private final String CACHE_NAME = "users";
    @Autowired
    private UserRepository userRepository;

    @Cacheable(cacheNames = CACHE_NAME, key = "'all-users'")
    public List<User> getAllUsers() {
        log.info("Fetching all users from database");
        return userRepository.findAll();
    }

    @Cacheable(cacheNames = CACHE_NAME, key = "'name:' + #name.toLowerCase()")
    public User getUsersByName(String name) {
        log.info("Fetching user by name: {}", name);
        List<User> users = userRepository.findAll();
        Optional<User> user = users.stream().filter(u -> u.getName().equalsIgnoreCase(name)).findFirst();
        return user.orElse(null);
    }

    @Cacheable(cacheNames = CACHE_NAME, key = "#id")
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @CacheEvict(cacheNames = CACHE_NAME, key = "'all-users'")
    public List<User> createUsersInBulk(List<User> users) {
        log.info("Creating {} users in bulk", users.size());
        return users.stream()
                .map(userRepository::save)
                .toList();
    }

    @Caching(put = @CachePut(cacheNames = CACHE_NAME, key = "#result.id"), evict = @CacheEvict(cacheNames = CACHE_NAME, key = "'all-users'"))
    public User createUser(User user) {
        log.info("Creating user: {}", user.getName());
        return userRepository.save(user);
    }

    @Caching(put = @CachePut(cacheNames = CACHE_NAME, key = "#result.id"), evict = @CacheEvict(cacheNames = CACHE_NAME, key = "'all-users'"))
    public User updateUser(Long id, User userDetails) {
        log.info("Updating user with ID: {}", id);
        return userRepository.findById(id).map(user -> {
            user.setName(userDetails.getName());
            user.setEmail(userDetails.getEmail());
            user.setDesignation(userDetails.getDesignation());
            return userRepository.save(user);
        }).orElse(null);
    }

    @Caching(evict = {
            @CacheEvict(cacheNames = CACHE_NAME, key = "#id"),
            @CacheEvict(cacheNames = CACHE_NAME, key = "'all-users'")
    })
    public void deleteUser(Long id) {
        log.info("Deleting user with ID: {}", id);
        userRepository.deleteById(id);
    }
}
