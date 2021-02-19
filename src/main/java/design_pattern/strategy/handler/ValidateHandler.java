package design_pattern.strategy.handler;

import design_pattern.strategy.ValidateRequest;

/**
 * @Date 2021/2/19 9:10 下午
 * @author: leobhao
 */
public interface ValidateHandler {


    /**
     * 获取需要校验的类型
     */
    String getType();


    /**
     * 处理校验逻辑
     */
    void handleValidate(ValidateRequest validateRequest);
}
