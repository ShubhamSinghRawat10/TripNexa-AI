package com.tripnexa.smarttrip.security;

import com.tripnexa.smarttrip.entity.User;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record UserPrincipal(UUID id, String name, String email, String passwordHash) implements UserDetails {
    public static UserPrincipal from(User user) {
        return new UserPrincipal(user.getId(), user.getName(), user.getEmail(), user.getPasswordHash());
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(); }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
}
