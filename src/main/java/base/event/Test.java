/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package base.event;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
 * spring event 是一个基于注册-发布的观察者模式
 * 注册：refresh -> registerListener 注册所有Listener(继承了ApplicationListener的)
 * 发布: finishRefresh -> pushlishEvent -> multicastEvent -> multicastEvent -> invokeListener
 * （这里就会调用每个listener的onApplication）
 *
 * @author HaoBin
 * @version $Id: Test.java, v0.1 2019/3/19 11:47 HaoBin 
 */
public class Test {


    public static void main(String[] args) {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:event.xml");
        EmailEvent emailEvent = new EmailEvent("163.com");
        applicationContext.publishEvent(emailEvent);
    }
}