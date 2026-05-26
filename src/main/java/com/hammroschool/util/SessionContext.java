package com.hammroschool.util;

import java.util.Optional;

import com.hammroschool.model.auth.UserAccount;

public final class SessionContext {
    private static final SessionContext INSTANCE = new SessionContext();

    private UserAccount currentUser;

    private SessionContext() {
    }

    public static SessionContext getInstance() {
        return INSTANCE;
    }

    public Optional<UserAccount> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    public UserAccount requireCurrentUser() {
        if (currentUser == null) {
            throw new IllegalStateException("No active user session.");
        }

        return currentUser;
    }

    public void setCurrentUser(UserAccount currentUser) {
        this.currentUser = currentUser;
    }

    public void clear() {
        currentUser = null;
    }
}