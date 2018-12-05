/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package aop.spring_2_schema;

import aop.service.UserService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
 *
 * @author HaoBin
 * @version $Id: SchemaBaseApplication.java, v0.1 2018/12/5 23:10 HaoBin 
 */
public class SchemaBaseApplication {

    public static void main(String[] args) {

        // 启动 Spring 的 IOC 容器
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring_2_0_schema_based.xml");

        UserService userService = context.getBean(UserService.class);

        userService.createUser("Tom", "Cruise", 55);
    }

}