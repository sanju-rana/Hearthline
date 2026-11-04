package com.restaurant.security;

import com.restaurant.entity.UserAccount;
import com.restaurant.enums.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class RestaurantUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final UserRole role;
    private final Long branchId;
    private final boolean enabled;

    public RestaurantUserDetails(UserAccount user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole();
        this.branchId = user.getBranch() == null ? null : user.getBranch().getId();
        this.enabled = user.isEnabled();
    }

    public Long getId() {
        return id;
    }

    public UserRole getRole() {
        return role;
    }

    public Long getBranchId() {
        return branchId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
