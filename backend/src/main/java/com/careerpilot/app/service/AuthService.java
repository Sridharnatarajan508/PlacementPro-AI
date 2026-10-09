package com.careerpilot.app.service;

import com.careerpilot.app.dto.request.LoginRequest;
import com.careerpilot.app.dto.request.RegisterRequest;
import com.careerpilot.app.dto.response.AuthResponse;
import com.careerpilot.app.entity.Profile;
import com.careerpilot.app.entity.User;
import com.careerpilot.app.exception.EmailAlreadyExistsException;
import com.careerpilot.app.repository.ProfileRepository;
import com.careerpilot.app.repository.UserRepository;
import com.careerpilot.app.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.STUDENT)
                .build();

        user = userRepository.save(user);

        // Create an empty profile for the new user
        Profile profile = Profile.builder()
                .user(user)
                .onboardingComplete(false)
                .build();
        profileRepository.save(profile);

        log.info("New user registered: {}", user.getEmail());

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return AuthResponse.of(token, user.getId(), user.getName(), user.getEmail(),
                user.getRole().name(), false);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Profile profile = profileRepository.findByUser(user).orElse(null);
        boolean onboardingComplete = profile != null && Boolean.TRUE.equals(profile.getOnboardingComplete());

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        log.info("User logged in: {}", user.getEmail());

        return AuthResponse.of(token, user.getId(), user.getName(), user.getEmail(),
                user.getRole().name(), onboardingComplete);
    }
}
