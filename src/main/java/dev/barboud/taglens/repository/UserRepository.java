package dev.barboud.taglens.repository;

import dev.barboud.taglens.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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


    public Optional<User> findUserByEmail(String email) {
        String sql = "SELECT id, email, password_hash FROM users WHERE email = ?";
        List<User> results = jdbcTemplate.query(sql, (rs, rowNum) -> {
            User u = new User();
            u.setId(UUID.fromString(rs.getString("id")));
            u.setEmail(rs.getString("email"));
            u.setPasswordHash(rs.getString("password_hash"));
            return u;
        }, email);

        return results.stream().findFirst();
    }



    public Optional<User> findUserById(UUID id) {
        String sql = "SELECT id, email, password_hash, created_at FROM users WHERE id = ?";
        List<User> results = jdbcTemplate.query(sql, (rs, rowNum) -> {
            User user = new User();
            user.setId(UUID.fromString(rs.getString("id")));
            user.setEmail(rs.getString("email"));
            user.setPasswordHash(rs.getString("password_hash"));
            user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            return user;
        }, id);

        return results.stream().findFirst();
    }
}
