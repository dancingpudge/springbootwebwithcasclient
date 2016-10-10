package com.joyowo.cas.filter;

import org.jasig.cas.client.session.*;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * Created by Liuh on 2016/10/9.
 */
@WebListener
public final class SingleSignOutHttpSessionListener implements HttpSessionListener {
    private SessionMappingStorage sessionMappingStorage;

    public SingleSignOutHttpSessionListener() {
    }

    public void sessionCreated(HttpSessionEvent event) {
    }

    public void sessionDestroyed(HttpSessionEvent event) {
        if(this.sessionMappingStorage == null) {
            this.sessionMappingStorage = getSessionMappingStorage();
        }

        HttpSession session = event.getSession();
        this.sessionMappingStorage.removeBySessionById(session.getId());
    }

    protected static SessionMappingStorage getSessionMappingStorage() {
        return SingleSignOutFilter.getSingleSignOutHandler().getSessionMappingStorage();
    }
}
