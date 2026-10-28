package dev.barboud.taglens.controller;


import dev.barboud.taglens.dto.user.UserLoginDto;
import dev.barboud.taglens.dto.user.UserRegistrationDto;
import dev.barboud.taglens.dto.user.UserResponseDto;
import dev.barboud.taglens.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Authentication", description = "Register, log in and log out using a session cookie")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


    @Operation(summary = "Register a new user", description = "Creates a new account with the given details.")
    @ApiResponse(responseCode = "201", description = "User created")
    @ApiResponse(responseCode = "400", description = "Validation failed (invalid or missing fields)")
    @PostMapping("/register")
    public ResponseEntity<Void> createUser(@Valid @RequestBody UserRegistrationDto registrationDto) {
        authService.registerUser(registrationDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


    @Operation(summary = "Log in",
            description = "Checks the credentials and sets an HttpOnly `session_id` cookie (valid for 7 days).")
    @ApiResponse(responseCode = "200", description = "Logged in, session cookie set")
    @ApiResponse(responseCode = "400", description = "Validation failed (invalid or missing fields)")
    @ApiResponse(responseCode = "401", description = "Invalid email or password")
    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody UserLoginDto loginDto) {
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



    @Operation(summary = "Log out", description = "Deletes the session (if any) and clears the `session_id` cookie.")
    @ApiResponse(responseCode = "200", description = "Logged out, session cookie cleared")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Parameter(hidden = true)
                                       @CookieValue(name = "session_id", required = false) UUID sessionId,
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


    @Operation(summary = "Get the current user", description = "Returns the profile of the logged-in user.")
    @ApiResponse(responseCode = "200", description = "Current user returned")
    @ApiResponse(responseCode = "401", description = "Not logged in or session expired")
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getCurrentUser(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(authService.getCurrentUser(userId));
    }


}
