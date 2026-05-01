package com.classpet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "teachers")
public class Teacher {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(length = 100)
    private String systemName = "班级宠物园";

    @Column(length = 100)
    private String className = "默认班级";

    @Column(length = 30)
    private String theme = "pink";

    private boolean activated = false;

    private String activateCode;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Teacher() {}
    public Teacher(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getSystemName() { return systemName; }
    public void setSystemName(String systemName) { this.systemName = systemName; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
    public boolean isActivated() { return activated; }
    public void setActivated(boolean activated) { this.activated = activated; }
    public String getActivateCode() { return activateCode; }
    public void setActivateCode(String activateCode) { this.activateCode = activateCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}