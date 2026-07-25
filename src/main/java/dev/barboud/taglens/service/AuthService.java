package dev.barboud.taglens.service;

import dev.barboud.taglens.dto.UserRegistrationDto;
import dev.barboud.taglens.model.User;
import dev.barboud.taglens.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;


    public void registerUser(UserRegistrationDto registrationDto) {
        User newUser = new User();
        newUser.setId(UUID.randomUUID());
        newUser.setEmail(registrationDto.getEmail());

        String encodedPassword = passwordEncoder.encode(registrationDto.getPassword());
        newUser.setPasswordHash(encodedPassword);

        userRepository.registerUser(newUser);
    }

}
