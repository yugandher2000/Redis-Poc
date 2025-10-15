package com.poc.redis.service;

import com.poc.redis.model.User;
import com.poc.redis.dao.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UserService {
    private final String CACHE_NAME = "users";
    @Autowired
    private UserRepository userRepository;

    @Cacheable(cacheNames = CACHE_NAME)
    public List<User> getAllUsers() {
        log.info("Fetching all users from database");
        return userRepository.findAll();
    }
    public User getUsersByName(String name) {
        List<User> users = userRepository.findAll();
        Optional<User> user = users.stream().filter(u -> u.getName().equalsIgnoreCase(name)).findFirst();
        return user.orElse(null);
    }

    @Cacheable(cacheNames = CACHE_NAME, key = "#id")
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    @Cacheable(cacheNames = CACHE_NAME,key = "#result.stream().map(u->u.getId()).toList()") //caching each user by their id
    public List<User> createUsersInBulk(List<User> users) {
        return users.stream()
                .map(userRepository::save)
                .toList();
    }
    @CachePut(cacheNames = CACHE_NAME, key = "#result.id") //in spl #result will have the return value of the method
    public User createUser(User user) {
        return userRepository.save(user);
    }

    @CachePut(cacheNames = CACHE_NAME, key = "#result.id")
    public User updateUser(Long id, User userDetails) {
        return userRepository.findById(id).map(user -> {
            user.setName(userDetails.getName());
            user.setEmail(userDetails.getEmail());
            return userRepository.save(user);
        }).orElse(null);
    }

    @CacheEvict(cacheNames = CACHE_NAME, key = "#id")
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}

