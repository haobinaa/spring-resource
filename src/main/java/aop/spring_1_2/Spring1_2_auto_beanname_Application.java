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
public class Spring1_2_auto_beanname_Application {

    /**
     * 只拦截特定的方法
     * 根据配置， 只会拦截createUser
     * Advisor 决定该拦截哪些方法，拦截后需要完成的工作还是内部的 Advice 来做
     * @param args
     */
    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring_1_2_BeanNameAutoProxy.xml");
        // 获取AOP代理 UserServiceProxy
        UserService userService = (UserService) context.getBean(UserService.class);
        OrderService orderService = (OrderService) context.getBean(OrderService.class);
        userService.createUser("hao", "bin", 23);
        userService.queryUser();
        orderService.createOrder("leo", "something");
        orderService.queryOrder("leo");
    }

}