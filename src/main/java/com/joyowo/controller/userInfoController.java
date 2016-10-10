package com.joyowo.controller;

import org.jasig.cas.client.authentication.AttributePrincipal;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Created by Liuh on 2016/10/10.
 */
@RequestMapping("/springboot/cas/")
@RestController
public class userInfoController {

    //用户信息获取测试接口
    @RequestMapping(value = "useinfo", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map showUserinfo(HttpServletRequest request){
        AttributePrincipal principal = (AttributePrincipal)request.getUserPrincipal();
        Map attributes = principal.getAttributes();
        return attributes;
    }
}
