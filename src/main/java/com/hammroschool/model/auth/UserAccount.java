package com.hammroschool.model.auth;

public class UserAccount {
    private final String username;
    private final String password;
    private final UserRole role;

    public UserAccount(String username, String password, UserRole role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public UserRole getRole() {
        return role;
    }
}