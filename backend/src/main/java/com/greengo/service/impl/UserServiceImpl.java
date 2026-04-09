package com.greengo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.greengo.domain.User;
import com.greengo.mapper.UserMapper;
import com.greengo.service.UserService;
import com.greengo.utils.Md5Util;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

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
    public void register(String username, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(Md5Util.getMD5String(password));
        user.setRole("CUSTOMER");
        user.setStatus(1);
        save(user);
    }

    @Override
    public User login(String username, String password) {
        return lambdaQuery()
                .eq(User::getUsername, username)
                .eq(User::getPassword, Md5Util.getMD5String(password))
                .one();
    }
}

