package design_pattern.strategy.handler;

import design_pattern.strategy.ValidateRequest;
import org.springframework.stereotype.Component;

/**
 * @Date 2021/2/19 9:18 下午
 * @author: leobhao
 */
@Component
public class LunchRequestValidateHandler implements ValidateHandler{

    @Override
    public String getType() {
        return "lunch";
    }

    @Override
    public void handleValidate(ValidateRequest validateRequest) {
        System.out.println("请求时校验");
    }
}
