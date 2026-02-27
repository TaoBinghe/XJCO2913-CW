package com.binghetao.controller;

import com.binghetao.domain.Booking;
import com.binghetao.domain.Result;
import com.binghetao.domain.User;
import com.binghetao.service.BookingService;
import com.binghetao.service.UserService;
import com.binghetao.utils.JwtUtil;
import com.binghetao.utils.ThreadLocalUtil;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private BookingService bookingService;

    @PostMapping("/register")
    public Result<?> register(@Pattern(regexp = "^\\S{5,16}$") String username, @Pattern(regexp = "^\\S{5,16}$") String password) {
        // Check if username already exists.
        User u = userService.findByUserName(username);
        if (u == null) {
            userService.register(username, password);
            return Result.success();
        } else {
            return Result.error("Username already exists");
        }
    }

    @PostMapping("/login")
    public Result<?> login(@RequestParam("username") String username, @RequestParam("password") String password) {
        // Authenticate user in service layer with MyBatis-Plus query.
        User u = userService.login(username, password);
        if (u != null) {
            Map<String, Object> claims = new HashMap<>();
            claims.put("username", u.getUsername());
            claims.put("id", u.getId());
            String token = JwtUtil.genToken(claims);
            return Result.success(token);
        }
        return Result.error("Invalid username or password");
    }

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
}
