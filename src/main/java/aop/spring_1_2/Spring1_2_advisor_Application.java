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
 * @version $Id: Spring1_2_Application.java, v0.1 2018/11/29 21:20 HaoBin 
 */
public class Spring1_2_advisor_Application {

    /**
     * 只拦截特定的方法
     * @param args
     */
    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring_1_2_advice.xml");
        // 获取AOP代理 UserServiceProxy
        UserService userService = (UserService) context.getBean("userServiceProxy");
        userService.createUser("hao", "bin", 23);
        userService.queryUser();
    }

}