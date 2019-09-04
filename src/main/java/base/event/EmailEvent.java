/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package base.event;

import org.springframework.context.ApplicationEvent;

/**
 *
 *
 * @author HaoBin
 * @version $Id: EmailEvent.java, v0.1 2019/3/19 11:43 HaoBin 
 */
public class EmailEvent extends ApplicationEvent {

    public String email;

    public EmailEvent(Object source) {
        super(source);
    }

    public EmailEvent(Object source, String email) {
        super(source);
        this.email = email;
    }

    public void print() {
        System.out.println("print email");
    }
}