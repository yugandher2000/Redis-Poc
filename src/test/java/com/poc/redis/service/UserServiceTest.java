package com.poc.redis.service;

import com.poc.redis.dao.UserRepository;
import com.poc.redis.model.User;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    public UserServiceTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllUsers() {
        User user1 = new User(1L, "Alice", "alice@example.com", "Developer");
        User user2 = new User(2L, "Bob", "bob@example.com", "Manager");
        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));
        List<User> users = userService.getAllUsers();
        assertEquals(2, users.size());
    }

    @Test
    void testGetUserById() {
        User user = new User(1L, "Alice", "alice@example.com", "Developer");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        Optional<User> result = userService.getUserById(1L);
        assertTrue(result.isPresent());
        assertEquals("Alice", result.get().getName());
    }

    @Test
    void testCreateUser() {
        User user = new User(null, "Alice", "alice@example.com", "Developer");
        when(userRepository.save(user)).thenReturn(new User(1L, "Alice", "alice@example.com", "Developer"));
        User created = userService.createUser(user);
        assertNotNull(created.getId());
    }
}

