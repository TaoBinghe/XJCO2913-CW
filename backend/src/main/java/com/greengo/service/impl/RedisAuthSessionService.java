package com.greengo.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greengo.domain.AuthSession;
import com.greengo.domain.User;
import com.greengo.service.AuthSessionService;
import com.greengo.utils.RedisKeys;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
public class RedisAuthSessionService implements AuthSessionService {

    private static final Duration SESSION_TTL = Duration.ofHours(12);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisAuthSessionService(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String createSession(User user) {
        String sid = UUID.randomUUID().toString().replace("-", "");
        AuthSession session = AuthSession.builder()
                .sid(sid)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .status(user.getStatus())
                .loginAt(LocalDateTime.now())
                .build();

        try {
            String sessionJson = objectMapper.writeValueAsString(session);
            stringRedisTemplate.opsForValue().set(RedisKeys.authSession(sid), sessionJson, SESSION_TTL);
            stringRedisTemplate.opsForSet().add(RedisKeys.authUserSessions(user.getId()), sid);
            stringRedisTemplate.expire(RedisKeys.authUserSessions(user.getId()), SESSION_TTL);
            return sid;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to create login session");
        }
    }

    @Override
    public AuthSession getSession(String sid) {
        if (sid == null || sid.isBlank()) {
            return null;
        }

        String sessionJson = stringRedisTemplate.opsForValue().get(RedisKeys.authSession(sid));
        if (sessionJson == null || sessionJson.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(sessionJson, AuthSession.class);
        } catch (Exception e) {
            stringRedisTemplate.delete(RedisKeys.authSession(sid));
            return null;
        }
    }

    @Override
    public void invalidateSession(String sid) {
        AuthSession session = getSession(sid);
        if (session != null && session.getUserId() != null) {
            stringRedisTemplate.opsForSet().remove(RedisKeys.authUserSessions(session.getUserId()), sid);
        }
        stringRedisTemplate.delete(RedisKeys.authSession(sid));
    }
}
