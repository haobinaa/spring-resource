/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package base.event;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
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