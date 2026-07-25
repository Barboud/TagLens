package dev.barboud.taglens.repository;

import dev.barboud.taglens.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;


    public void registerUser(User user) {
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash) VALUES (?, ?, ?)",
                user.getId(), user.getEmail(), user.getPasswordHash()
        );
    }
}
