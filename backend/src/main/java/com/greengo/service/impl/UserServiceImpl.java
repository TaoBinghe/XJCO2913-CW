package com.greengo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.greengo.domain.User;
import com.greengo.mapper.UserMapper;
import com.greengo.service.UserService;
import com.greengo.utils.Md5Util;
import com.greengo.utils.RentalConstants;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    @Override
    public List<User> listAll() {
        return baseMapper.selectList(null);
    }

    @Override
    public User findByUserName(String username) {
        return lambdaQuery()
                .eq(User::getUsername, username)
                .one();
    }

    @Override
    public void register(String username, String password, String email) {
        register(username, password, email, null);
    }

    @Override
    public void register(String username, String password, String email, String customerType) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(Md5Util.getMD5String(password));
        user.setEmail(normalizeEmail(email));
        user.setCustomerType(normalizeCustomerType(customerType));
        user.setRole("CUSTOMER");
        user.setStatus(1);
        user.setWalletBalance(BigDecimal.ZERO);
        save(user);
    }

    @Override
    public User login(String username, String password) {
        return lambdaQuery()
                .eq(User::getUsername, username)
                .eq(User::getPassword, Md5Util.getMD5String(password))
                .one();
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            throw new IllegalArgumentException("Valid email is required");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Valid email is required");
        }
        return normalized;
    }

    private String normalizeCustomerType(String customerType) {
        if (customerType == null || customerType.isBlank()) {
            return RentalConstants.CUSTOMER_TYPE_REGULAR;
        }
        String normalized = customerType.trim().toUpperCase(Locale.ROOT);
        if (!RentalConstants.CUSTOMER_TYPE_REGULAR.equals(normalized)
                && !RentalConstants.CUSTOMER_TYPE_STUDENT.equals(normalized)
                && !RentalConstants.CUSTOMER_TYPE_SENIOR.equals(normalized)) {
            throw new IllegalArgumentException("Invalid customer type");
        }
        return normalized;
    }
}

