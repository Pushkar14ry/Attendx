package com.attendx.repository;

import com.attendx.model.Student;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(StudentRepository.class)
class StudentRepositoryTest {

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("attendx_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
        r.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    StudentRepository repo;

    @Test
    void saveAndFind() {
        Student s = repo.save(100, "TestUser");
        assertThat(s.rollNo()).isEqualTo(100);
        assertThat(s.name()).isEqualTo("TestUser");
    }

    @Test
    void findAll_returnsSeedData() {
        List<Student> all = repo.findAll();
        assertThat(all).isNotEmpty();
        assertThat(all.stream().anyMatch(s -> s.name().equals("Nihan"))).isTrue();
    }

    @Test
    void existsByRollNo_works() {
        assertThat(repo.existsByRollNo(1)).isTrue();
        assertThat(repo.existsByRollNo(9999)).isFalse();
    }

    @Test
    void deleteByRollNo_removes() {
        repo.save(200, "DeleteMe");
        assertThat(repo.existsByRollNo(200)).isTrue();
        repo.deleteByRollNo(200);
        assertThat(repo.existsByRollNo(200)).isFalse();
    }

    @Test
    void findByRollNo_returnsEmpty_whenNotFound() {
        Optional<Student> opt = repo.findByRollNo(9999);
        assertThat(opt).isEmpty();
    }
}
