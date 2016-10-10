# 基于springboot,整合了casclient的java web项目demo
---
## 目的

当需要建立javaweb项目时可以直接拿来用


## 主要类及作用

cas 分为服务端，与客户端，那么客户端如何与服务端进行交互呢，或者说服务端发送的response报文客户端如何接收呢？
这就要用到配置。cas client通过filter拦截与cas服务器进行交互。它的主要配置主要有filter:com.joyowo.cas.filter包下的五个类：

Cas20ProxyReceivingTicketValidationFilter 对于client接收到的ticket进行验证

HttpServletRequestWrapperFilter

myAuthenticationFilter 判断用户是否登录，如果登录则进入第二步，否则重定向到cas服务器

SingleSignOutFilter

SingleSignOutHttpSessionListener


###配置文件说明

	#cas client config
    cas:
        server:
          login_url: http://your.cas.cerver:host/login
          url_prefix: http://1your.cas.cerver:host/

        service:
          server_name: http://localhost:8080

注意casServerLoginUrl指服务器的地址；而serverName指的是应用的地址


##未修改之前的web.xml格式

	```xml
	<!--  1 -->
	<filter>
		<filter-name>CAS Authentication Filter</filter-name>
		<filter-class>org.jasig.cas.client.authentication.AuthenticationFilter</filter-class>
		<init-param>
			<param-name>casServerLoginUrl</param-name>
			<param-value>http://server.cas.com:8080/cas/login</param-value>
		</init-param>
		<init-param>
			<param-name>serverName</param-name>
			<param-value>http://client.app.com:8070</param-value>
		</init-param>
	</filter>
	
	<!--  2-- >
	<filter>
		<filter-name>CAS Validation Filter</filter-name>
		<filter-class>org.jasig.cas.client.validation.Cas20ProxyReceivingTicketValidationFilter</filter-class>
	<init-param>
		<param-name>casServerUrlPrefix</param-name>
		<param-value>http://server.cas.com:8080/cas/login</param-value>
	</init-param>
	<init-param>
		<param-name>serverName</param-name>
		<param-value>http://client.app.com:8070</param-value>
	</init-param>
	</filter>
	
	<!--  3 -->
	<filter>
		<filter-name>CAS HttpServletRequest Wrapper Filter</filter-name>
		<filter-class>org.jasig.cas.client.util.HttpServletRequestWrapperFilter</filter-class>
	</filter>
	
	<!--   4 -->
	<filter>
		<filter-name>CAS Assertion Thread Local Filter</filter-name>
		<filter-class>org.jasig.cas.client.util.AssertionThreadLocalFilter</filter-class>
	</filter>
	
	<!--   filter mapping的顺序不能乱-->
	<filter-mapping>
		<filter-name>CAS Authentication Filter</filter-name>
		<url-pattern>/*</url-pattern>
	</filter-mapping>
	<filter-mapping>
		<filter-name>CAS Validation Filter</filter-name>
		<url-pattern>/*</url-pattern>
	</filter-mapping>
	<filter-mapping>
		<filter-name>CAS HttpServletRequest WrapperFilter</filter-name>
		<url-pattern>/*</url-pattern>
	</filter-mapping>
	<filter-mapping>
		<filter-name>CAS Assertion Thread Local Filter</filter-name>
		<url-pattern>/*</url-pattern>
	</filter-mapping>
	
	```

###参考文档

http://blog.csdn.net/zhurhyme/article/details/29349543

https://github.com/apereo/java-cas-client

