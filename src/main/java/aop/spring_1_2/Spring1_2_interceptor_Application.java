/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package aop.spring_1_2;

import aop.service.UserService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
 *
 * @author HaoBin
 * @version $Id: Spring1_2_interceptor_Application.java, v0.1 2018/12/4 21:26 HaoBin 
 */
public class Spring1_2_interceptor_Application {
    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring_1_2_interceptor.xml");
        // 获取AOP代理 UserServiceProxy
        UserService userService = (UserService) context.getBean("userServiceProxy");
        userService.createUser("hao", "bin", 23);
        userService.queryUser();
    }
}