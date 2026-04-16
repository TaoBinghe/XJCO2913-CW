package com.greengo.controller;

import com.greengo.domain.Result;
import com.greengo.domain.User;
import com.greengo.service.AuthSessionService;
import com.greengo.service.UserService;
import com.greengo.utils.AuthUtil;
import com.greengo.utils.JwtUtil;
import com.greengo.utils.ThreadLocalUtil;
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

    @Autowired
    private AuthSessionService authSessionService;

    @GetMapping("/user/list")
    public Result<List<User>> listUsers() {
        if (!AuthUtil.isAdmin()) {
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
        claims.put("sid", authSessionService.createSession(u));
        String token = JwtUtil.genToken(claims);
        return Result.success(token);
    }

    @PostMapping("/logout")
    @SuppressWarnings("unchecked")
    public Result<?> logout() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        if (claims == null || claims.get("sid") == null) {
            return Result.error("Unauthorized");
        }
        authSessionService.invalidateSession(claims.get("sid").toString());
        return Result.success();
    }
}


