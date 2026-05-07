package com.greengo.service;

import com.greengo.domain.User;

import java.util.List;

// User: find, register, login
public interface UserService {

    User findByUserName(String username);

    default void register(String username, String password, String email) {
        register(username, password, email, null);
    }

    void register(String username, String password, String email, String customerType);

    User login(String username, String password);

    List<User> listAll();
}

