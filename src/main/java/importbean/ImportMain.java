package importbean;

import java.util.stream.Stream;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @Author HaoBin
 * @Create 2019/12/26 9:56
 * @Description: spring  手动注册 bean 的方式
 **/
public class ImportMain {

    public static void main(String[] args) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Config.class);
        String[] beanDefinitionName = ctx.getBeanDefinitionNames();
        Stream.of(beanDefinitionName).forEach(System.out::println);
    }
}


