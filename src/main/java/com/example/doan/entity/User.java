package com.example.doan.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name="users")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String username;

    private String password;

    private String email;

    private String name;

    private String phone;

    private String address;

    private String dob;

    private String gender;

    private String avatarUrl;

    private String joinedDate;

    private String role;

    @Column(name = "custom_permissions", length = 1000)
    private String customPermissions;

    private Boolean enabled;

    private Boolean accountNonLocked;

    private Boolean deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String createdBy;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
        if (role != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            authorities.add(new SimpleGrantedAuthority(role));
            getActivePermissions().forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
        }
        return authorities;
    }

    public List<String> getActivePermissions() {
        if (customPermissions != null && !customPermissions.trim().isEmpty()) {
            return java.util.Arrays.stream(customPermissions.split(","))
                    .map(String::trim)
                    .filter(p -> !p.isEmpty())
                    .collect(java.util.stream.Collectors.toList());
        }
        return getPermissionsByRole(role);
    }

    public static List<String> getPermissionsByRole(String role) {
        if (role == null) return List.of();
        if ("ADMIN".equalsIgnoreCase(role)) {
            return List.of(
                    "users.read", "users.create", "users.update", "users.delete",
                    "product.view", "product.create", "product.update", "product.delete", "product.hard-delete", "product.import", "product.trash", "product.restore",
                    "inventory.view", "inventory.manage",
                    "stats.view",
                    "order.checkout", "order.view", "order.manage",
                    "installment.submit", "installment.view",
                    "settings.manage"
            );
        } else if ("STAFF".equalsIgnoreCase(role)) {
            return List.of(
                    "product.view", "product.create", "product.update", "product.delete", "product.import",
                    "inventory.view", "inventory.manage",
                    "stats.view",
                    "order.view", "order.manage",
                    "installment.view"
            );
        } else if ("CUSTOMER".equalsIgnoreCase(role)) {
            return List.of(
                    "product.view",
                    "order.checkout", "order.view",
                    "installment.submit", "installment.view"
            );
        }
        return List.of();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
