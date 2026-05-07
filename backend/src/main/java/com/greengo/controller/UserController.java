package com.greengo.controller;

import com.greengo.domain.Booking;
import com.greengo.domain.Result;
import com.greengo.domain.User;
import com.greengo.service.AuthSessionService;
import com.greengo.service.BookingService;
import com.greengo.service.UserService;
import com.greengo.utils.JwtUtil;
import com.greengo.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

// User API: register, login, my orders
@RestController
@RequestMapping("/user")
public class UserController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    @Autowired
    private UserService userService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private AuthSessionService authSessionService;

    // Register new user
    @PostMapping("/register")
    public Result<?> register(@RequestParam String username,
                              @RequestParam String password,
                              @RequestParam String email,
                              @RequestParam(required = false) String customerType) {
        if (!isValidEmail(email)) {
            return Result.error("Valid email is required");
        }
        // Check if username already exists
        User u = userService.findByUserName(username);
        if (u == null) {
            try {
                userService.register(username, password, email, customerType);
                return Result.success();
            } catch (IllegalArgumentException e) {
                return Result.error(e.getMessage());
            }
        } else {
            return Result.error("Username already exists");
        }
    }

    // Login, returns JWT token
    @PostMapping("/login")
    public Result<?> login(@RequestParam("username") @Validated String username,
                           @Validated @RequestParam("password") String password) {
        // Authenticate user in service layer
        User u = userService.login(username, password);
        if (u != null) {
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
        return Result.error("Invalid username or password");
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

    // Get current user's bookings
    @GetMapping("/my-orders")
    @SuppressWarnings("unchecked")
    public Result<List<Booking>> myOrders() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        if (claims == null || claims.get("id") == null) {
            return Result.error("Unauthorized");
        }
        Long userId = ((Number) claims.get("id")).longValue();
        List<Booking> list = bookingService.listBookingsByUserId(userId);
        return Result.<List<Booking>>success(list);
    }

    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }
}

