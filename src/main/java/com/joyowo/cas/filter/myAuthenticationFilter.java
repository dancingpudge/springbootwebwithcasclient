package com.joyowo.cas.filter;

import com.joyowo.cas.properties.CasFilterProperties;
import org.jasig.cas.client.authentication.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.servlet.annotation.WebFilter;

/**
 * Created by Liuh on 2016/10/9.
 * Define the protected urls of your application
 */
@WebFilter(filterName="CAS Authentication Filter",urlPatterns="/*")
public class MyAuthenticationFilter extends AuthenticationFilter {

    @Autowired
    CasFilterProperties casFilterProperties;

    public void init() {
        super.setServerName(casFilterProperties.getServerName());
        super.setCasServerLoginUrl(casFilterProperties.getCasServerLoginUrl());
        super.init();
    }
}
