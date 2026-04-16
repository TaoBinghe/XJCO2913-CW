package com.greengo.service;

import com.greengo.domain.AuthSession;
import com.greengo.domain.User;

public interface AuthSessionService {

    String createSession(User user);

    AuthSession getSession(String sid);

    void invalidateSession(String sid);
}
