package dev.barboud.taglens.service;

import dev.barboud.taglens.dto.LoginDto;
import dev.barboud.taglens.dto.UserRegistrationDto;
import dev.barboud.taglens.dto.UserResponseDto;
import dev.barboud.taglens.model.Session;
import dev.barboud.taglens.model.User;
import dev.barboud.taglens.repository.SessionRepository;
import dev.barboud.taglens.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final long SESSION_DURATION_MINUTES = 16;


    public void registerUser(UserRegistrationDto registrationDto) {
        User newUser = new User();
        newUser.setId(UUID.randomUUID());
        newUser.setEmail(registrationDto.getEmail());

        String encodedPassword = passwordEncoder.encode(registrationDto.getPassword());
        newUser.setPasswordHash(encodedPassword);

        userRepository.registerUser(newUser);
    }


    public UUID login(LoginDto loginDto) {
        User user = userRepository.findUserByEmail(loginDto.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        Session session = new Session();
        session.setId(UUID.randomUUID());
        session.setUserId(user.getId());
        session.setExpiresAt(Instant.now().plus(SESSION_DURATION_MINUTES, ChronoUnit.MINUTES));

        sessionRepository.createSession(session);

        return session.getId();
    }


    public void logout(UUID sessionId) {
        sessionRepository.deleteSession(sessionId);
    }

    public UserResponseDto getCurrentUser(UUID userId) {
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        return new UserResponseDto(user.getId(), user.getEmail(), user.getCreatedAt());
    }


}
