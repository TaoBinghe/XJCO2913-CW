package com.binghetao.service;

import com.binghetao.domain.User;

import java.util.List;

// User: find, register, login
public interface UserService {

    User findByUserName(String username);

    void register(String username, String password);

    User login(String username, String password);

    List<User> listAll();
}
