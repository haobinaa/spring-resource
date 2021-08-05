package base.applicationcontext;

import base.retry.SpringRetry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.stereotype.Component;

/**
 * @Date 2021/7/29 5:10 下午
 * @author: leobhao
 */
@EnableRetry
@Component
public class ComponentScanApplication {

    public static void main(String[] args) throws Exception {
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:component_scan.xml");
        SpringRetry retry = context.getBean(SpringRetry.class);
        retry.service();
    }
}
