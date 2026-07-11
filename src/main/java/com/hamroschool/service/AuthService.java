package com.hamroschool.service;

import java.util.List;
import java.util.Optional;

import com.hamroschool.model.auth.UserAccount;
import com.hamroschool.model.auth.UserRole;

public interface AuthService {
    Optional<UserAccount> authenticate(String username, String password, UserRole role);

    boolean createAccount(String username, String password, UserRole role);

    /**
     * Creates an account with full profile details.
     * All extra fields are stored in MongoDB alongside credentials.
     */
    boolean createFullAccount(UserAccount account);

    List<UserAccount> getAccounts();

    /**
     * Get all users with a specific role
     */
    List<UserAccount> getAllUsersByRole(UserRole role);

    /**
     * Updates the password for an existing account.
     *
     * @return {@code true} if the account was found and the password updated,
     *         {@code false} if no account with that username exists.
     */
    boolean updatePassword(String username, String newPassword);

    /**
     * Updates all editable fields of an existing account.
     * The username is used as the lookup key and cannot be changed here.
     *
     * @return {@code true} if the account was found and updated.
     */
    boolean updateAccount(UserAccount updated);

    /**
     * Deletes an account by username.
     *
     * @return {@code true} if a document was deleted.
     */
    boolean deleteAccount(String username);
}