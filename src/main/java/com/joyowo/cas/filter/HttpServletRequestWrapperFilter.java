package com.joyowo.cas.filter;

import org.jasig.cas.client.authentication.AttributePrincipal;
import org.jasig.cas.client.configuration.ConfigurationKeys;
import org.jasig.cas.client.util.AbstractConfigurationFilter;
import org.jasig.cas.client.util.CommonUtils;
import org.jasig.cas.client.validation.Assertion;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.Iterator;

/**
 * Created by Liuh on 2016/10/9.
 * Put the CAS principal in the HTTP request
 */
@WebFilter(filterName="CAS HttpServletRequest Wrapper Filter",urlPatterns="*")
public final class HttpServletRequestWrapperFilter extends AbstractConfigurationFilter {
    private String roleAttribute;
    private boolean ignoreCase;

    public HttpServletRequestWrapperFilter() {
    }

    public void destroy() {
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        AttributePrincipal principal = this.retrievePrincipalFromSessionOrRequest(servletRequest);
        filterChain.doFilter(new HttpServletRequestWrapperFilter.CasHttpServletRequestWrapper((HttpServletRequest)servletRequest, principal), servletResponse);
    }

    protected AttributePrincipal retrievePrincipalFromSessionOrRequest(ServletRequest servletRequest) {
        HttpServletRequest request = (HttpServletRequest)servletRequest;
        HttpSession session = request.getSession(false);
        Assertion assertion = (Assertion)((Assertion)(session == null?request.getAttribute("_const_cas_assertion_"):session.getAttribute("_const_cas_assertion_")));
        return assertion == null?null:assertion.getPrincipal();
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        this.roleAttribute = this.getString(ConfigurationKeys.ROLE_ATTRIBUTE);
        this.ignoreCase = this.getBoolean(ConfigurationKeys.IGNORE_CASE);
    }

    final class CasHttpServletRequestWrapper extends HttpServletRequestWrapper {
        private final AttributePrincipal principal;

        CasHttpServletRequestWrapper(HttpServletRequest request, AttributePrincipal principal) {
            super(request);
            this.principal = principal;
        }

        public Principal getUserPrincipal() {
            return this.principal;
        }

        public String getRemoteUser() {
            return this.principal != null?this.principal.getName():null;
        }

        public boolean isUserInRole(String role) {
            if(CommonUtils.isBlank(role)) {
                HttpServletRequestWrapperFilter.this.logger.debug("No valid role provided.  Returning false.");
                return false;
            } else if(this.principal == null) {
                HttpServletRequestWrapperFilter.this.logger.debug("No Principal in Request.  Returning false.");
                return false;
            } else if(CommonUtils.isBlank(HttpServletRequestWrapperFilter.this.roleAttribute)) {
                HttpServletRequestWrapperFilter.this.logger.debug("No Role Attribute Configured. Returning false.");
                return false;
            } else {
                Object value = this.principal.getAttributes().get(HttpServletRequestWrapperFilter.this.roleAttribute);
                if(value instanceof Collection) {
                    Iterator isMember = ((Collection)value).iterator();

                    while(isMember.hasNext()) {
                        Object o = isMember.next();
                        if(this.rolesEqual(role, o)) {
                            HttpServletRequestWrapperFilter.this.logger.debug("User [{}] is in role [{}]: true", this.getRemoteUser(), role);
                            return true;
                        }
                    }
                }

                boolean isMember1 = this.rolesEqual(role, value);
                HttpServletRequestWrapperFilter.this.logger.debug("User [{}] is in role [{}]: {}", new Object[]{this.getRemoteUser(), role, Boolean.valueOf(isMember1)});
                return isMember1;
            }
        }

        private boolean rolesEqual(String given, Object candidate) {
            return HttpServletRequestWrapperFilter.this.ignoreCase?given.equalsIgnoreCase(candidate.toString()):given.equals(candidate);
        }
    }
}
