package com.restaurant.service;

import com.restaurant.dto.AuthDtos;
import com.restaurant.entity.UserAccount;
import com.restaurant.enums.UserRole;
import com.restaurant.exception.ConflictException;
import com.restaurant.exception.ResourceNotFoundException;
import com.restaurant.exception.UnauthorizedException;
import com.restaurant.mapper.ViewMapper;
import com.restaurant.repository.UserAccountRepository;
import com.restaurant.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserAccountRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthDtos.AuthResult register(AuthDtos.RegisterRequest request) {
        PasswordRules.check(request.password());
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists.");
        }
        UserAccount user = new UserAccount();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setRole(UserRole.CUSTOMER);
        user.setEnabled(true);
        user.setTokenVersion(0);
        users.save(user);
        return new AuthDtos.AuthResult(ViewMapper.toUser(user), jwtService.generate(user));
    }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResult login(AuthDtos.LoginRequest request) {
        UserAccount user = users.findByEmailWithBranch(request.email().trim())
                .orElseThrow(() -> new UnauthorizedException("The email or password is incorrect."));
        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("The email or password is incorrect.");
        }
        return new AuthDtos.AuthResult(ViewMapper.toUser(user), jwtService.generate(user));
    }

    @Transactional(readOnly = true)
    public AuthDtos.UserResponse me(Long userId) {
        return ViewMapper.toUser(load(userId));
    }

    @Transactional
    public void logout(Long userId) {
        UserAccount user = load(userId);
        user.setTokenVersion(user.getTokenVersion() + 1);
    }

    private UserAccount load(Long userId) {
        return users.findWithBranchById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
    }
}
