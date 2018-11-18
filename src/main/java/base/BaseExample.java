/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package base;

import bean.IMessageService;
import bean.impl.MessageServiceImpl;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * application context 简单使用
 *
 * @author HaoBin
 * @version $Id: BaseExample.java, v0.1 2018/11/15 23:18 HaoBin 
 */
public class BaseExample {

    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:base.xml");
        System.out.println("启动 context");
        System.out.println(context.getId());
        System.out.println(context.getApplicationName());
        IMessageService messageService = context.getBean(MessageServiceImpl.class);
        System.out.println(messageService.getMessage());
    }

}