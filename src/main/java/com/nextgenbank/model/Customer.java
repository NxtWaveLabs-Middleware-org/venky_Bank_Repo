package com.nextgenbank.model;

import java.io.Serializable;

public class Customer implements Serializable {
    private long customerId;
    private String username;
    private String fullName;
    private String email;

    public Customer() {}

    public Customer(long customerId, String username, String fullName, String email) {
        this.customerId = customerId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
    }

    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
