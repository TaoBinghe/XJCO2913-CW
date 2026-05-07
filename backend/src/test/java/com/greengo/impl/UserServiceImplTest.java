package com.greengo.impl;

import com.greengo.domain.User;
import com.greengo.mapper.UserMapper;
import com.greengo.service.impl.UserServiceImpl;
import com.greengo.utils.Md5Util;
import com.greengo.utils.PasswordHashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl();
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

    @Test
    void registerStoresNormalizedEmail() {
        when(userMapper.insert(any(User.class))).thenReturn(1);

        userService.register("alice01", "secret", " Alice@Example.COM ");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        assertEquals("alice01", userCaptor.getValue().getUsername());
        assertEquals("alice@example.com", userCaptor.getValue().getEmail());
        assertNotEquals(Md5Util.getMD5String("secret"), userCaptor.getValue().getPassword());
        assertTrue(PasswordHashUtil.matches("secret", userCaptor.getValue().getPassword()));
        assertEquals("CUSTOMER", userCaptor.getValue().getRole());
        assertEquals(1, userCaptor.getValue().getStatus());
    }

    @Test
    void registerRejectsInvalidEmail() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> userService.register("alice01", "secret", "not-an-email"));

        assertEquals("Valid email is required", error.getMessage());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void loginAcceptsBcryptPasswordWithoutRehashing() {
        User user = User.builder()
                .id(1L)
                .username("alice01")
                .password(PasswordHashUtil.hash("secret"))
                .build();
        when(userMapper.selectOne(any())).thenReturn(user);

        User loggedIn = userService.login("alice01", "secret");

        assertEquals(user, loggedIn);
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void loginUpgradesLegacyMd5PasswordAfterSuccessfulAuthentication() {
        String legacyHash = Md5Util.getMD5String("secret");
        User user = User.builder()
                .id(1L)
                .username("alice01")
                .password(legacyHash)
                .build();
        when(userMapper.selectOne(any())).thenReturn(user);

        User loggedIn = userService.login("alice01", "secret");

        assertEquals(user, loggedIn);
        assertNotEquals(legacyHash, user.getPassword());
        assertTrue(PasswordHashUtil.matches("secret", user.getPassword()));
        verify(userMapper).updateById(user);
    }

    @Test
    void loginRejectsWrongPasswordWithoutUpgradingHash() {
        String legacyHash = Md5Util.getMD5String("secret");
        User user = User.builder()
                .id(1L)
                .username("alice01")
                .password(legacyHash)
                .build();
        when(userMapper.selectOne(any())).thenReturn(user);

        User loggedIn = userService.login("alice01", "wrong");

        assertNull(loggedIn);
        assertEquals(legacyHash, user.getPassword());
        verify(userMapper, never()).updateById(any(User.class));
    }
}
