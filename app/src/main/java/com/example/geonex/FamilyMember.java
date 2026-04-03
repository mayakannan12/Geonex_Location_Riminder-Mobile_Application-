package com.example.geonex;

public class FamilyMember {

    public static final String ROLE_ADMIN = "Admin";
    public static final String ROLE_MEMBER = "Member";
    public static final String ROLE_CHILD = "Child";

    private int id;
    private String name;
    private String relation;
    private String email;
    private String phone;
    private String role;
    private int reminderCount;
    private String avatarUrl;
    private boolean isOnline;

    public FamilyMember(int id, String name, String relation, String email,
                        String phone, String role, int reminderCount, String avatarUrl) {
        this.id = id;
        this.name = name;
        this.relation = relation;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.reminderCount = reminderCount;
        this.avatarUrl = avatarUrl;
        this.isOnline = false;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRelation() { return relation; }
    public void setRelation(String relation) { this.relation = relation; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public int getReminderCount() { return reminderCount; }
    public void setReminderCount(int reminderCount) { this.reminderCount = reminderCount; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }

    public String getInitials() {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.split(" ");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }
}