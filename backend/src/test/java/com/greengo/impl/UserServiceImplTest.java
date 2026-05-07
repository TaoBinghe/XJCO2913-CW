package com.greengo.impl;

import com.greengo.domain.User;
import com.greengo.mapper.UserMapper;
import com.greengo.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        assertEquals("CUSTOMER", userCaptor.getValue().getRole());
        assertEquals(1, userCaptor.getValue().getStatus());
    }

    @Test
    void registerRejectsInvalidEmail() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> userService.register("alice01", "secret", "not-an-email"));

        assertEquals("Valid email is required", error.getMessage());
        verify(userMapper, never()).insert(any());
    }
}
