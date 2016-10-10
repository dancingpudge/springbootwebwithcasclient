package com.joyowo.cas.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by Liuh on 2016/10/10.
 */
@Component
public class CasFilterProperties {
    @Value("${cas.service.server_name}")
    private String serverName;
    @Value("${cas.server.login_url}")
    private String casServerLoginUrl;
    @Value("${cas.server.url_prefix}")
    private String casServerUrlPrefix;

    public String getServerName() {
        return serverName;
    }

    public String getCasServerLoginUrl() {
        return casServerLoginUrl;
    }

    public String getCasServerUrlPrefix() {
        return casServerUrlPrefix;
    }
}
