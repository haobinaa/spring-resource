package design_pattern.strategy.handler;

import design_pattern.strategy.ValidateRequest;
import org.springframework.stereotype.Component;

/**
 * @Date 2021/2/19 9:16 下午
 * @author: leobhao
 */
@Component
public class BeforeRequestValidateHandler implements ValidateHandler {

    @Override
    public String getType() {
        return "before";
    }

    @Override
    public void handleValidate(ValidateRequest validateRequest) {
        System.out.println("请求前校验");
        // 校验逻辑处理
    }
}
