package dev.barboud.taglens.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class UserResponseDto {
    UUID id;
    String email;
    LocalDateTime createdAt;
}
