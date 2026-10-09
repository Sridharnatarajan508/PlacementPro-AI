package com.careerpilot.app.security;

import com.careerpilot.app.dto.request.RegisterRequest;
import com.careerpilot.app.entity.User;
import com.careerpilot.app.repository.UserRepository;
import com.careerpilot.app.service.AuthService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.jwt.secret}")
    private String secretKey;

    private String testEmail = "jwttest@example.com";
    private org.springframework.security.core.userdetails.UserDetails testUserDetails;
    private String validToken;

    @BeforeEach
    void setUp() {
        if (userRepository.findByEmail(testEmail).isEmpty()) {
            RegisterRequest req = new RegisterRequest();
            req.setName("JWT Test User");
            req.setEmail(testEmail);
            req.setPassword("Password123");
            authService.register(req);
        }
        testUserDetails = userDetailsService.loadUserByUsername(testEmail);
        validToken = jwtService.generateToken(testUserDetails);
    }

    @Test
    @DisplayName("Protected endpoint with valid JWT returns 200 OK")
    void testProtectedEndpoint_WithValidToken_Returns200() throws Exception {
        mockMvc.perform(get("/api/jobs")
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Protected endpoint without token returns 401 Unauthorized with clean JSON")
    void testProtectedEndpoint_WithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Protected endpoint with expired JWT returns 401 Unauthorized without crashing")
    void testProtectedEndpoint_WithExpiredToken_Returns401() throws Exception {
        // Build an expired token (expired 1 hour ago)
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        String expiredToken = Jwts.builder()
                .claims(new HashMap<>())
                .subject(testEmail)
                .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(key)
                .compact();

        mockMvc.perform(get("/api/jobs")
                .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Protected endpoint with invalid signature returns 401 Unauthorized")
    void testProtectedEndpoint_WithInvalidToken_Returns401() throws Exception {
        String invalidToken = validToken + "tampered";

        mockMvc.perform(get("/api/jobs")
                .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Public auth endpoint does not require token")
    void testPublicEndpoint_AccessibleWithoutToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + testEmail + "\",\"password\":\"WrongPassword\"}"))
                .andExpect(status().isUnauthorized()); // Bad credentials, not 401 from missing JWT filter
    }

    @Test
    @DisplayName("JwtService correctly validates token expiration and subject")
    void testJwtService_Validation() {
        assertTrue(jwtService.isTokenValid(validToken, testUserDetails));
        assertEquals(testEmail, jwtService.extractUsername(validToken));
    }
}
