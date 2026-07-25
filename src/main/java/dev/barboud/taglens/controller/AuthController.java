package dev.barboud.taglens.controller;


import dev.barboud.taglens.dto.UserRegistrationDto;
import dev.barboud.taglens.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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


}
