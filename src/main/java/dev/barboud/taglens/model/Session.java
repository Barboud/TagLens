package dev.barboud.taglens.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Session {
    private UUID id;
    private UUID userId;
    private Instant expiresAt;
    private Instant createdAt;

}