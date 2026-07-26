package dev.barboud.taglens.repository;

import dev.barboud.taglens.model.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SessionRepository {
    private final JdbcTemplate jdbcTemplate;

    public void createSession(Session session) {
        jdbcTemplate.update(
                "INSERT INTO sessions (id, user_id, expires_at) VALUES (?, ?, ?)",
                session.getId(), session.getUserId(), Timestamp.from(session.getExpiresAt())
        );
    }


    public Optional<Session> findValidSession(UUID sessionId) {
        String sql = "SELECT id, user_id, expires_at FROM sessions WHERE id = ? AND expires_at > NOW()";
        List<Session> results = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Session s = new Session();
            s.setId(UUID.fromString(rs.getString("id")));
            s.setUserId(UUID.fromString(rs.getString("user_id")));
            s.setExpiresAt(rs.getTimestamp("expires_at").toInstant());
            return s;
        }, sessionId);

        return results.stream().findFirst();
    }


    public void deleteSession(UUID sessionId) {
        jdbcTemplate.update("DELETE FROM sessions WHERE id = ?", sessionId);
    }

}