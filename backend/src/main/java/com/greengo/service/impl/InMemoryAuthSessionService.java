package com.greengo.service.impl;

import com.greengo.domain.AuthSession;
import com.greengo.domain.User;
import com.greengo.service.AuthSessionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryAuthSessionService implements AuthSessionService {

    private static final Duration SESSION_TTL = Duration.ofHours(12);

    private final Map<String, SessionHolder> sessions = new ConcurrentHashMap<>();

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
        sessions.put(sid, new SessionHolder(session, System.currentTimeMillis() + SESSION_TTL.toMillis()));
        return sid;
    }

    @Override
    public AuthSession getSession(String sid) {
        if (sid == null || sid.isBlank()) {
            return null;
        }

        SessionHolder holder = sessions.get(sid);
        if (holder == null) {
            return null;
        }
        if (holder.expiresAtMillis < System.currentTimeMillis()) {
            sessions.remove(sid);
            return null;
        }
        return holder.session;
    }

    @Override
    public void invalidateSession(String sid) {
        if (sid == null || sid.isBlank()) {
            return;
        }
        sessions.remove(sid);
    }

    private record SessionHolder(AuthSession session, long expiresAtMillis) {
    }
}
