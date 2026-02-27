package com.binghetao.service;

import com.binghetao.domain.User;

// User: find, register, login
public interface UserService {

    // Find user by username
    User findByUserName(String username);

    // Register new user
    void register(String username, String password);

    // Login, returns user if valid
    User login(String username, String password);
}
