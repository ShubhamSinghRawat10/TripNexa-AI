package com.tripnexa.smarttrip.service;

import com.tripnexa.smarttrip.dto.auth.AuthResponse;
import com.tripnexa.smarttrip.dto.auth.LoginRequest;
import com.tripnexa.smarttrip.dto.auth.SignUpRequest;
import com.tripnexa.smarttrip.entity.User;
import com.tripnexa.smarttrip.exception.ConflictException;
import com.tripnexa.smarttrip.repository.UserRepository;
import com.tripnexa.smarttrip.security.JwtService;
import com.tripnexa.smarttrip.security.UserPrincipal;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse signUp(SignUpRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        try {
            User user = userRepository.saveAndFlush(
                new User(request.name().trim(), email, passwordEncoder.encode(request.password())));
            return response(UserPrincipal.from(user));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("An account with this email already exists");
        }
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        var authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(email, request.password()));
        return response((UserPrincipal) authentication.getPrincipal());
    }

    private AuthResponse response(UserPrincipal principal) {
        return new AuthResponse(
            jwtService.generate(principal),
            "Bearer",
            jwtService.expirationSeconds(),
            new AuthResponse.UserSummary(principal.id(), principal.name(), principal.email())
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
