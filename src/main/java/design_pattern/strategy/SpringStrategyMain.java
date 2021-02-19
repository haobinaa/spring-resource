package design_pattern.strategy;

import design_pattern.strategy.handler.ValidateHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 *
 * spring 实现策略模式
 *
 * @Date 2021/2/19 9:03 下午
 * @author: leobhao
 */
public class SpringStrategyMain {


    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("design_pattern.xml");
        ValidateHandlerFactory factory = context.getBean(ValidateHandlerFactory.class);
        ValidateRequest beforeRequest = new ValidateRequest("before");
        ValidateHandler beforeHandler = factory.getHandler(beforeRequest.getType());
        beforeHandler.handleValidate(beforeRequest);
    }
}
