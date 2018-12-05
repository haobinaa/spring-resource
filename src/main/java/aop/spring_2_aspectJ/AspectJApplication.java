/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package aop.spring_2_aspectJ;

import aop.service.UserService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
 *
 * @author HaoBin
 * @version $Id: AspectJApplication.java, v0.1 2018/12/5 22:39 HaoBin 
 */
public class AspectJApplication {

    public static void main(String[] args) {

        // 启动 Spring 的 IOC 容器
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring_2_0_aspectj.xml");

        UserService userService = context.getBean(UserService.class);

        userService.createUser("Tom", "Cruise", 55);
        userService.queryUser();
    }
}