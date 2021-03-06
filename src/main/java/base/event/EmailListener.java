/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package base.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 *
 *
 * @author HaoBin
 * @version $Id: EmailListener.java, v0.1 2019/3/19 11:45 HaoBin 
 */
public class EmailListener implements ApplicationListener<EmailEvent> {


    @Override
    public void onApplicationEvent(EmailEvent event) {
        event.print();
        System.out.println("email event email is:" + event.getSource());
    }
}