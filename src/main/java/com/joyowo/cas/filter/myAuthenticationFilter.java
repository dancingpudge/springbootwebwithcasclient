package com.joyowo.cas.filter;

import org.jasig.cas.client.authentication.*;
import org.springframework.beans.factory.annotation.Value;

import javax.servlet.annotation.WebFilter;

/**
 * Created by Liuh on 2016/10/9.
 * Define the protected urls of your application
 */
@WebFilter(filterName="CAS Authentication Filter",urlPatterns="/*")
public class myAuthenticationFilter extends AuthenticationFilter {

    @Value("${cas.service.server_name}")
    private String serverName;

    @Value("${cas.server.login_url}")
    private String casServerLoginUrl;

    public void init() {
        super.setServerName(serverName);
        super.setCasServerLoginUrl(casServerLoginUrl);
        super.init();
    }
}
