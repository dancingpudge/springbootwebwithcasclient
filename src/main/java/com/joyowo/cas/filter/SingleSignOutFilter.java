package com.joyowo.cas.filter;

import com.joyowo.cas.properties.CasFilterProperties;
import org.jasig.cas.client.configuration.ConfigurationKeys;
import org.jasig.cas.client.session.SessionMappingStorage;
import org.jasig.cas.client.session.SingleSignOutHandler;
import org.jasig.cas.client.util.AbstractConfigurationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Liuh on 2016/10/9.
 * Filter to handle logout requests sent directly by the CAS server
 */
@WebFilter(filterName="CAS Single Sign Out Filter",urlPatterns="/*")
public final class SingleSignOutFilter extends AbstractConfigurationFilter {
    @Autowired
    CasFilterProperties casFilterProperties;

    private static final SingleSignOutHandler HANDLER = new SingleSignOutHandler();
    private AtomicBoolean handlerInitialized = new AtomicBoolean(false);

    public SingleSignOutFilter() {
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        if(!this.isIgnoreInitConfiguration()) {
            this.setArtifactParameterName(this.getString(ConfigurationKeys.ARTIFACT_PARAMETER_NAME));
            this.setLogoutParameterName(this.getString(ConfigurationKeys.LOGOUT_PARAMETER_NAME));
            this.setFrontLogoutParameterName(this.getString(ConfigurationKeys.FRONT_LOGOUT_PARAMETER_NAME));
            this.setRelayStateParameterName(this.getString(ConfigurationKeys.RELAY_STATE_PARAMETER_NAME));
            this.setCasServerUrlPrefix(casFilterProperties.getCasServerUrlPrefix());
            HANDLER.setArtifactParameterOverPost(this.getBoolean(ConfigurationKeys.ARTIFACT_PARAMETER_OVER_POST));
            HANDLER.setEagerlyCreateSessions(this.getBoolean(ConfigurationKeys.EAGERLY_CREATE_SESSIONS));
        }

        HANDLER.init();
        this.handlerInitialized.set(true);
    }

    public void setArtifactParameterName(String name) {
        HANDLER.setArtifactParameterName(name);
    }

    public void setLogoutParameterName(String name) {
        HANDLER.setLogoutParameterName(name);
    }

    public void setFrontLogoutParameterName(String name) {
        HANDLER.setFrontLogoutParameterName(name);
    }

    public void setRelayStateParameterName(String name) {
        HANDLER.setRelayStateParameterName(name);
    }

    public void setCasServerUrlPrefix(String casServerUrlPrefix) {
        HANDLER.setCasServerUrlPrefix(casServerUrlPrefix);
    }

    public void setSessionMappingStorage(SessionMappingStorage storage) {
        HANDLER.setSessionMappingStorage(storage);
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest)servletRequest;
        HttpServletResponse response = (HttpServletResponse)servletResponse;
        if(!this.handlerInitialized.getAndSet(true)) {
            HANDLER.init();
        }

        if(HANDLER.process(request, response)) {
            filterChain.doFilter(servletRequest, servletResponse);
        }

    }

    public void destroy() {
    }

    protected static SingleSignOutHandler getSingleSignOutHandler() {
        return HANDLER;
    }
}
