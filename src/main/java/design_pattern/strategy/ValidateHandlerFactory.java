package design_pattern.strategy;

import design_pattern.strategy.handler.ValidateHandler;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 策略工厂
 *
 * @Date 2021/2/19 9:42 下午
 * @author: leobhao
 */
@Component
public class ValidateHandlerFactory implements InitializingBean, ApplicationContextAware {

    private static final Map<String, ValidateHandler> VALIDATE_HANDLER_MAP = new HashMap<>();

    private ApplicationContext appContext;

    public ValidateHandler getHandler(String type) {
        return VALIDATE_HANDLER_MAP.get(type);
    }


    @Override
    public void afterPropertiesSet() {
        // 将 Spring 容器中所有的 FormSubmitHandler 注册到 FORM_SUBMIT_HANDLER_MAP
        appContext.getBeansOfType(ValidateHandler.class)
                .values()
                .forEach(handler -> VALIDATE_HANDLER_MAP.put(handler.getType(), handler));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        appContext = applicationContext;
    }

}
