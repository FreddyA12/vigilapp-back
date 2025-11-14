package com.fram.vigilapp.repository;

import com.fram.vigilapp.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .passwordHash("hashedPassword")
                .role("USER")
                .status("ACTIVE")
                .build();
    }

    @Test
    void findByEmail_WithExistingEmail_ShouldReturnUser() {
        // Given
        entityManager.persist(testUser);
        entityManager.flush();

        // When
        User found = userRepository.findByEmail("test@example.com");

        // Then
        assertNotNull(found);
        assertEquals("test@example.com", found.getEmail());
        assertEquals("John", found.getFirstName());
        assertEquals("Doe", found.getLastName());
    }

    @Test
    void findByEmail_WithNonExistentEmail_ShouldReturnNull() {
        // When
        User found = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertNull(found);
    }

    @Test
    void findByEmail_ShouldBeCaseInsensitive() {
        // Given
        entityManager.persist(testUser);
        entityManager.flush();

        // When
        User found = userRepository.findByEmail("TEST@EXAMPLE.COM");

        // Then
        assertNotNull(found);
        assertEquals("test@example.com", found.getEmail());
    }

    @Test
    void findByStatus_ShouldReturnUsersWithStatus() {
        // Given
        User activeUser1 = User.builder()
                .email("active1@example.com")
                .firstName("Active")
                .lastName("One")
                .passwordHash("hash")
                .role("USER")
                .status("ACTIVE")
                .build();

        User activeUser2 = User.builder()
                .email("active2@example.com")
                .firstName("Active")
                .lastName("Two")
                .passwordHash("hash")
                .role("USER")
                .status("ACTIVE")
                .build();

        User blockedUser = User.builder()
                .email("blocked@example.com")
                .firstName("Blocked")
                .lastName("User")
                .passwordHash("hash")
                .role("USER")
                .status("BLOCKED")
                .build();

        entityManager.persist(activeUser1);
        entityManager.persist(activeUser2);
        entityManager.persist(blockedUser);
        entityManager.flush();

        // When
        List<User> activeUsers = userRepository.findByStatus("ACTIVE");

        // Then
        assertEquals(2, activeUsers.size());
        assertTrue(activeUsers.stream().allMatch(u -> u.getStatus().equals("ACTIVE")));
    }

    @Test
    void save_ShouldPersistUser() {
        // When
        User saved = userRepository.save(testUser);

        // Then
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());

        User found = entityManager.find(User.class, saved.getId());
        assertNotNull(found);
        assertEquals("test@example.com", found.getEmail());
    }

    @Test
    void count_ShouldReturnNumberOfUsers() {
        // Given
        entityManager.persist(testUser);
        User user2 = User.builder()
                .email("user2@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .passwordHash("hash")
                .role("USER")
                .status("ACTIVE")
                .build();
        entityManager.persist(user2);
        entityManager.flush();

        // When
        long count = userRepository.count();

        // Then
        assertEquals(2, count);
    }

    @Test
    void deleteById_ShouldRemoveUser() {
        // Given
        entityManager.persist(testUser);
        entityManager.flush();
        UUID userId = testUser.getId();

        // When
        userRepository.deleteById(userId);
        entityManager.flush();

        // Then
        User found = entityManager.find(User.class, userId);
        assertNull(found);
    }
}
