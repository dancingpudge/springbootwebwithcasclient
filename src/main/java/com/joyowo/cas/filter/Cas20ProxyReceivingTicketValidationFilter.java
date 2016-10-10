package com.joyowo.cas.filter;

import org.jasig.cas.client.Protocol;
import org.jasig.cas.client.configuration.ConfigurationKeys;
import org.jasig.cas.client.proxy.*;
import org.jasig.cas.client.ssl.HttpsURLConnectionFactory;
import org.jasig.cas.client.util.CommonUtils;
import org.jasig.cas.client.util.ReflectUtils;
import org.jasig.cas.client.validation.*;
import org.springframework.beans.factory.annotation.Value;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

/**
 * Created by Liuh on 2016/10/9.
 * Define the urls on which you can validate a service ticket
 */
@WebFilter(filterName="CAS Validation Filter",urlPatterns="*")
public class Cas20ProxyReceivingTicketValidationFilter extends AbstractTicketValidationFilter {
    @Value("${cas.server.url_prefix}")
    private String serverUrlPrefix;

    @Value("${cas.service.server_name}")
    private String serverName;

    private static final String[] RESERVED_INIT_PARAMS;
    private String proxyReceptorUrl;
    private Timer timer;
    private TimerTask timerTask;
    private int millisBetweenCleanUps;
    protected Class<? extends Cas20ServiceTicketValidator> defaultServiceTicketValidatorClass;
    protected Class<? extends Cas20ProxyTicketValidator> defaultProxyTicketValidatorClass;
    private ProxyGrantingTicketStorage proxyGrantingTicketStorage;

    public Cas20ProxyReceivingTicketValidationFilter() {
        this(Protocol.CAS2);
        this.defaultServiceTicketValidatorClass = Cas20ServiceTicketValidator.class;
        this.defaultProxyTicketValidatorClass = Cas20ProxyTicketValidator.class;
    }

    protected Cas20ProxyReceivingTicketValidationFilter(Protocol protocol) {
        super(protocol);
        this.proxyGrantingTicketStorage = new ProxyGrantingTicketStorageImpl();
    }

    protected void initInternal(FilterConfig filterConfig) throws ServletException {
        this.setProxyReceptorUrl(this.getString(ConfigurationKeys.PROXY_RECEPTOR_URL));
        Class proxyGrantingTicketStorageClass = this.getClass(ConfigurationKeys.PROXY_GRANTING_TICKET_STORAGE_CLASS);
        if (proxyGrantingTicketStorageClass != null) {
            this.proxyGrantingTicketStorage = (ProxyGrantingTicketStorage) ReflectUtils.newInstance(proxyGrantingTicketStorageClass, new Object[0]);
            if (this.proxyGrantingTicketStorage instanceof AbstractEncryptedProxyGrantingTicketStorageImpl) {
                AbstractEncryptedProxyGrantingTicketStorageImpl p = (AbstractEncryptedProxyGrantingTicketStorageImpl) this.proxyGrantingTicketStorage;
                String cipherAlgorithm = this.getString(ConfigurationKeys.CIPHER_ALGORITHM);
                String secretKey = this.getString(ConfigurationKeys.SECRET_KEY);
                p.setCipherAlgorithm(cipherAlgorithm);

                try {
                    if (secretKey != null) {
                        p.setSecretKey(secretKey);
                    }
                } catch (Exception var7) {
                    throw new RuntimeException(var7);
                }
            }
        }

        this.millisBetweenCleanUps = this.getInt(ConfigurationKeys.MILLIS_BETWEEN_CLEAN_UPS);
        super.initInternal(filterConfig);
    }

    public void init() {
        super.setServerName(serverName);
        super.init();
        CommonUtils.assertNotNull(this.proxyGrantingTicketStorage, "proxyGrantingTicketStorage cannot be null.");
        if (this.timer == null) {
            this.timer = new Timer(true);
        }

        if (this.timerTask == null) {
            this.timerTask = new CleanUpTimerTask(this.proxyGrantingTicketStorage);
        }

        this.timer.schedule(this.timerTask, (long) this.millisBetweenCleanUps, (long) this.millisBetweenCleanUps);
    }

    private <T> T createNewTicketValidator(Class<? extends Cas20ServiceTicketValidator> ticketValidatorClass, String casServerUrlPrefix, Class<T> clazz) {
        return ticketValidatorClass == null ? ReflectUtils.newInstance(clazz, new Object[]{casServerUrlPrefix}) : ReflectUtils.newInstance((Class<T>) ticketValidatorClass, new Object[]{casServerUrlPrefix});
    }

    protected final TicketValidator getTicketValidator(FilterConfig filterConfig) {
        boolean allowAnyProxy = this.getBoolean(ConfigurationKeys.ACCEPT_ANY_PROXY);
        String allowedProxyChains = this.getString(ConfigurationKeys.ALLOWED_PROXY_CHAINS);
        String casServerUrlPrefix = serverUrlPrefix;
        Class ticketValidatorClass = this.getClass(ConfigurationKeys.TICKET_VALIDATOR_CLASS);
        Object validator;
        if (!allowAnyProxy && !CommonUtils.isNotBlank(allowedProxyChains)) {
            validator = (Cas20ServiceTicketValidator) this.createNewTicketValidator(ticketValidatorClass, casServerUrlPrefix, this.defaultServiceTicketValidatorClass);
        } else {
            Cas20ProxyTicketValidator factory = (Cas20ProxyTicketValidator) this.createNewTicketValidator(ticketValidatorClass, casServerUrlPrefix, this.defaultProxyTicketValidatorClass);
            factory.setAcceptAnyProxy(allowAnyProxy);
            factory.setAllowedProxyChains(CommonUtils.createProxyList(allowedProxyChains));
            validator = factory;
        }

        ((Cas20ServiceTicketValidator) validator).setProxyCallbackUrl(this.getString(ConfigurationKeys.PROXY_CALLBACK_URL));
        ((Cas20ServiceTicketValidator) validator).setProxyGrantingTicketStorage(this.proxyGrantingTicketStorage);
        HttpsURLConnectionFactory factory1 = new HttpsURLConnectionFactory(this.getHostnameVerifier(), this.getSSLConfig());
        ((Cas20ServiceTicketValidator) validator).setURLConnectionFactory(factory1);
        ((Cas20ServiceTicketValidator) validator).setProxyRetriever(new Cas20ProxyRetriever(casServerUrlPrefix, this.getString(ConfigurationKeys.ENCODING), factory1));
        ((Cas20ServiceTicketValidator) validator).setRenew(this.getBoolean(ConfigurationKeys.RENEW));
        ((Cas20ServiceTicketValidator) validator).setEncoding(this.getString(ConfigurationKeys.ENCODING));
        HashMap additionalParameters = new HashMap();
        List params = Arrays.asList(RESERVED_INIT_PARAMS);
        Enumeration e = filterConfig.getInitParameterNames();

        while (e.hasMoreElements()) {
            String s = (String) e.nextElement();
            if (!params.contains(s)) {
                additionalParameters.put(s, filterConfig.getInitParameter(s));
            }
        }

        ((Cas20ServiceTicketValidator) validator).setCustomParameters(additionalParameters);
        return (TicketValidator) validator;
    }

    public void destroy() {
        super.destroy();
        this.timer.cancel();
    }

    protected final boolean preFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String requestUri = request.getRequestURI();
        if (!CommonUtils.isEmpty(this.proxyReceptorUrl) && requestUri.endsWith(this.proxyReceptorUrl)) {
            try {
                CommonUtils.readAndRespondToProxyReceptorRequest(request, response, this.proxyGrantingTicketStorage);
                return false;
            } catch (RuntimeException var8) {
                this.logger.error(var8.getMessage(), var8);
                throw var8;
            }
        } else {
            return true;
        }
    }

    public final void setProxyReceptorUrl(String proxyReceptorUrl) {
        this.proxyReceptorUrl = proxyReceptorUrl;
    }

    public void setProxyGrantingTicketStorage(ProxyGrantingTicketStorage storage) {
        this.proxyGrantingTicketStorage = storage;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    public void setTimerTask(TimerTask timerTask) {
        this.timerTask = timerTask;
    }

    public void setMillisBetweenCleanUps(int millisBetweenCleanUps) {
        this.millisBetweenCleanUps = millisBetweenCleanUps;
    }

    static {
        RESERVED_INIT_PARAMS = new String[]{ConfigurationKeys.ARTIFACT_PARAMETER_NAME.getName(), ConfigurationKeys.SERVER_NAME.getName(), ConfigurationKeys.SERVICE.getName(), ConfigurationKeys.RENEW.getName(), ConfigurationKeys.LOGOUT_PARAMETER_NAME.getName(), ConfigurationKeys.ARTIFACT_PARAMETER_OVER_POST.getName(), ConfigurationKeys.EAGERLY_CREATE_SESSIONS.getName(), ConfigurationKeys.ENCODE_SERVICE_URL.getName(), ConfigurationKeys.SSL_CONFIG_FILE.getName(), ConfigurationKeys.ROLE_ATTRIBUTE.getName(), ConfigurationKeys.IGNORE_CASE.getName(), ConfigurationKeys.CAS_SERVER_LOGIN_URL.getName(), ConfigurationKeys.GATEWAY.getName(), ConfigurationKeys.AUTHENTICATION_REDIRECT_STRATEGY_CLASS.getName(), ConfigurationKeys.GATEWAY_STORAGE_CLASS.getName(), ConfigurationKeys.CAS_SERVER_URL_PREFIX.getName(), ConfigurationKeys.ENCODING.getName(), ConfigurationKeys.TOLERANCE.getName(), ConfigurationKeys.IGNORE_PATTERN.getName(), ConfigurationKeys.IGNORE_URL_PATTERN_TYPE.getName(), ConfigurationKeys.HOSTNAME_VERIFIER.getName(), ConfigurationKeys.HOSTNAME_VERIFIER_CONFIG.getName(), ConfigurationKeys.EXCEPTION_ON_VALIDATION_FAILURE.getName(), ConfigurationKeys.REDIRECT_AFTER_VALIDATION.getName(), ConfigurationKeys.USE_SESSION.getName(), ConfigurationKeys.SECRET_KEY.getName(), ConfigurationKeys.CIPHER_ALGORITHM.getName(), ConfigurationKeys.PROXY_RECEPTOR_URL.getName(), ConfigurationKeys.PROXY_GRANTING_TICKET_STORAGE_CLASS.getName(), ConfigurationKeys.MILLIS_BETWEEN_CLEAN_UPS.getName(), ConfigurationKeys.ACCEPT_ANY_PROXY.getName(), ConfigurationKeys.ALLOWED_PROXY_CHAINS.getName(), ConfigurationKeys.TICKET_VALIDATOR_CLASS.getName(), ConfigurationKeys.PROXY_CALLBACK_URL.getName(), ConfigurationKeys.FRONT_LOGOUT_PARAMETER_NAME.getName(), ConfigurationKeys.RELAY_STATE_PARAMETER_NAME.getName()};
    }
}