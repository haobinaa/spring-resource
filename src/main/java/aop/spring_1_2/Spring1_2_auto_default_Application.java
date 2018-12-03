/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package aop.spring_1_2;

import aop.service.OrderService;
import aop.service.UserService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
 *
 * @author HaoBin
 * @version $Id: Spring1_2_Application.java, v0.1 2018/11/29 21:20 HaoBin 
 */
public class Spring1_2_auto_default_Application {

    /**
     * 自动匹配advisor， 根据正则表达式
     */
    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring_1_2_DefaultAdvisorAutoProxy.xml");
        // 获取AOP代理 UserServiceProxy
        UserService userService = (UserService) context.getBean(UserService.class);
        OrderService orderService = (OrderService) context.getBean(OrderService.class);
        userService.createUser("hao", "bin", 23);
        userService.queryUser();
        orderService.createOrder("leo", "something");
        orderService.queryOrder("leo");
    }

}