package com.hammroschool.model.entity;

public class Teacher {
    private final Long id;
    private final String name;
    private final String username;
    private final String subject;
    private final boolean active;

    public Teacher(Long id, String name, String username, String subject, boolean active) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.subject = subject;
        this.active = active;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getUsername() { return username; }
    public String getSubject() { return subject; }
    public boolean isActive() { return active; }
}
