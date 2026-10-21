package com.example.auth;

import com.example.auth.dto.LoginRequest;
import com.example.auth.entity.User;
import com.example.auth.repository.UserRepository;
import com.example.auth.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class KN14LoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;

    @BeforeEach
    void setUp() {
        if (userRepository.findByEmail("test1@example.com").isEmpty()) {
            testUser = User.builder()
                    .username("test1")
                    .email("test1@example.com")
                    .password(passwordEncoder.encode("Password123"))
                    .tokenVersion(1L)
                    .failedAttempts(0)
                    .roles(new java.util.HashSet<>(java.util.List.of("ROLE_USER")))
                    .build();
            testUser = userRepository.save(testUser);
        } else {
            testUser = userRepository.findByEmail("test1@example.com").get();
            testUser.setPassword(passwordEncoder.encode("Password123"));
            testUser.setFailedAttempts(0);
            testUser.setLockTime(null);
            testUser = userRepository.save(testUser);
        }
    }

    @Test
    void test1_LoginSuccess() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("test1@example.com");
        request.setPassword("Password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists());
    }

    @Test
    void test2_LoginInvalidEmail() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("notfound@example.com");
        request.setPassword("AnyPassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Tên đăng nhập hoặc mật khẩu không chính xác"));
    }

    @Test
    void test3_LoginWrongPassword() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("test1@example.com");
        request.setPassword("WrongPassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", org.hamcrest.Matchers.containsString("Tên đăng nhập hoặc mật khẩu không chính xác")));

        User user = userRepository.findByEmail("test1@example.com").get();
        assertEquals(1, user.getFailedAttempts());
    }

    @Test
    void test4_LoginLockAccount() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("test1@example.com");
        request.setPassword("WrongPassword");

        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Tài khoản đã bị khóa tạm thời. Vui lòng thử lại sau 15 phút."));

        User user = userRepository.findByEmail("test1@example.com").get();
        assertNotNull(user.getLockTime());
        assertEquals(5, user.getFailedAttempts());
    }

    @Test
    void test5_LoginCorrectPasswordWhileLocked() throws Exception {
        testUser.setFailedAttempts(5);
        testUser.setLockTime(LocalDateTime.now());
        userRepository.save(testUser);

        LoginRequest request = new LoginRequest();
        request.setUsername("test1@example.com");
        request.setPassword("Password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Tài khoản đã bị khóa tạm thời. Vui lòng thử lại sau 15 phút."));
    }

    @Test
    void test6_LoginAfterLockExpired() throws Exception {
        testUser.setFailedAttempts(5);
        testUser.setLockTime(LocalDateTime.now().minusMinutes(16));
        userRepository.save(testUser);

        LoginRequest request = new LoginRequest();
        request.setUsername("test1@example.com");
        request.setPassword("Password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        User user = userRepository.findByEmail("test1@example.com").get();
        assertEquals(0, user.getFailedAttempts());
        assertNull(user.getLockTime());
    }
}
