package com.binghetao.controller;

import com.binghetao.domain.Result;
import com.binghetao.domain.User;
import com.binghetao.service.UserService;
import com.binghetao.utils.JwtUtil;
import com.binghetao.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    private boolean isAdmin() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        if (claims == null) return false;
        Object role = claims.get("role");
        return role != null && "MANAGER".equalsIgnoreCase(role.toString());
    }

    @GetMapping("/user/list")
    public Result<List<User>> listUsers() {
        if (!isAdmin()) {
            return Result.error("Forbidden: admin only");
        }
        List<User> users = userService.listAll();
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }

    @PostMapping("/login")
    public Result<?> login(@RequestParam("username") String username, @RequestParam("password") String password) {
        User u = userService.login(username, password);
        if (u == null) {
            return Result.error("Invalid username or password");
        }
        if (!"MANAGER".equalsIgnoreCase(u.getRole())) {
            return Result.error("Not an admin user");
        }
        if (u.getStatus() == null || u.getStatus() != 1) {
            return Result.error("User is disabled");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("username", u.getUsername());
        claims.put("id", u.getId());
        claims.put("role", u.getRole());
        String token = JwtUtil.genToken(claims);
        return Result.success(token);
    }
}

