package retry;


import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

/**
 * @Date 2022/3/17 8:27 PM
 * @author: leobhao
 */
@Configuration
@EnableRetry
public class SpringRetryConfiguration {


    @Bean
    public RetryService retryService() {
        return new RetryService();
    }

    public static void main(String[] args) throws Exception {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext("retry");
        RetryService retry = applicationContext.getBean("retryService", RetryService.class);
        retry.service();
    }
}
