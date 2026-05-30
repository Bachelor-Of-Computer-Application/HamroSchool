package com.hammroschool.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.hammroschool.model.auth.UserRole;

class InMemoryAuthServiceTest {
    @Test
    void storesAndReadsAccountsFromSqlDatabase() {
        InMemoryAuthService authService = new InMemoryAuthService(
                "jdbc:h2:mem:hammro-auth-test;DB_CLOSE_DELAY=-1",
                "sa",
                "",
                "org.h2.Driver");

        assertTrue(authService.createAccount("alice", "secret123", UserRole.TEACHER));
        assertFalse(authService.createAccount("alice", "another", UserRole.STUDENT));

        assertTrue(authService.authenticate("alice", "secret123", UserRole.TEACHER).isPresent());
        assertEquals(2, authService.getAccounts().size());
    }
}