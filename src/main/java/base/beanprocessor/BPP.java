/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package base.beanprocessor;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

/**
 *
 *
 * @author HaoBin
 * @version $Id: BPP.java, v0.1 2018/11/18 17:48 HaoBin 
 */
public class BPP {

    public BPP() {
        System.out.println("bbp construct");
    }

    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:base.xml");
        context.getBean(BPP.class);
    }
}