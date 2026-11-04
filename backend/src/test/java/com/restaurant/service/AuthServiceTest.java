package com.restaurant.service;

import com.restaurant.dto.AuthDtos;
import com.restaurant.entity.UserAccount;
import com.restaurant.exception.ConflictException;
import com.restaurant.exception.UnauthorizedException;
import com.restaurant.repository.UserAccountRepository;
import com.restaurant.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAccountRepository users;
    @Mock
    private JwtService jwtService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(users, passwordEncoder, jwtService);
    }

    @Test
    void registrationStoresABcryptHash() {
        when(users.existsByEmailIgnoreCase("asha@hearthline.test")).thenReturn(false);
        when(users.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generate(any(UserAccount.class))).thenReturn("token");

        authService.register(new AuthDtos.RegisterRequest("Asha Raman", "asha@hearthline.test", "Customer@123", "9845001122"));

        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(users).save(captor.capture());
        assertNotEquals("Customer@123", captor.getValue().getPasswordHash());
        assertTrue(passwordEncoder.matches("Customer@123", captor.getValue().getPasswordHash()));
    }

    @Test
    void duplicateEmailIsRejected() {
        when(users.existsByEmailIgnoreCase("asha@hearthline.test")).thenReturn(true);
        assertThrows(ConflictException.class, () -> authService.register(
                new AuthDtos.RegisterRequest("Asha Raman", "asha@hearthline.test", "Customer@123", "9845001122")));
    }

    @Test
    void invalidLoginIsRejected() {
        UserAccount user = new UserAccount();
        user.setEmail("asha@hearthline.test");
        user.setPasswordHash(passwordEncoder.encode("Customer@123"));
        user.setEnabled(true);
        when(users.findByEmailWithBranch("asha@hearthline.test")).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedException.class, () -> authService.login(
                new AuthDtos.LoginRequest("asha@hearthline.test", "wrong-pass1")));
    }
}
