package dev.barboud.taglens.controller;


import dev.barboud.taglens.dto.LoginDto;
import dev.barboud.taglens.dto.UserRegistrationDto;
import dev.barboud.taglens.dto.UserResponseDto;
import dev.barboud.taglens.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


    @PostMapping("/register")
    public ResponseEntity<Void> createUser(@Valid @RequestBody UserRegistrationDto registrationDto) {
        authService.registerUser(registrationDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginDto loginDto) {
        UUID sessionId = authService.login(loginDto);

        ResponseCookie cookie = ResponseCookie.from("session_id", sessionId.toString())
                .httpOnly(true)
                // .secure(true)          // only over HTTPS
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofDays(7))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }



    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "session_id", required = false) UUID sessionId,
                                       HttpServletResponse response) {

        if (sessionId != null) {
            authService.logout(sessionId);
        }

        ResponseCookie deleteCookie = ResponseCookie.from("session_id", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(0)   // delete from browser also
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .build();
    }


    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getCurrentUser(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(authService.getCurrentUser(userId));
    }


}
