package importbean;

import importbean.beans.YellowBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author HaoBin
 * @Create 2019/12/26 10:13
 * @Description:
 **/
@Configuration
public class YellowRegisterConfiguration {

    @Bean
    public YellowBean yellowBean() {
        return new YellowBean();
    }
}
