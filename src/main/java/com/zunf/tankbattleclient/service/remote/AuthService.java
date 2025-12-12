package com.zunf.tankbattleclient.service.remote;

public class AuthService {

    public String login(String username, String password) {
        // Mock login implementation
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            return "mock-token-for-" + username;
        }
        return null;
    }

    public boolean register(String username, String password) {
        // Mock register implementation
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            // In a real implementation, you would check if the username already exists
            // and save the user details to a database
            System.out.println("User registered: " + username);
            return true;
        }
        return false;
    }
}
