package com.restaurant.security;

import com.restaurant.entity.UserAccount;
import com.restaurant.repository.UserAccountRepository;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JwtAuthenticator {

    private final JwtService jwtService;
    private final UserAccountRepository users;

    public JwtAuthenticator(JwtService jwtService, UserAccountRepository users) {
        this.jwtService = jwtService;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Authentication authenticate(String token) {
        try {
            Claims claims = jwtService.parse(token);
            Long userId = Long.valueOf(claims.getSubject());
            Integer version = claims.get("ver", Integer.class);
            if (version == null) {
                return null;
            }
            UserAccount user = users.findWithBranchById(userId).orElse(null);
            if (user == null || !user.isEnabled() || user.getTokenVersion() != version) {
                return null;
            }
            RestaurantUserDetails details = new RestaurantUserDetails(user);
            return new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
