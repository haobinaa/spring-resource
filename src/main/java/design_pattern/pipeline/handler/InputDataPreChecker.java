package design_pattern.pipeline.handler;

import design_pattern.pipeline.InstanceBuildContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.Map;

/**
 * 输出数据校验
 *
 * @Date 2021/2/20 6:06 下午
 * @author: leobhao
 */
@Component
public class InputDataPreChecker implements ContextHandler<InstanceBuildContext> {

    @Override
    public boolean handle(InstanceBuildContext context) {
        System.out.println("-- 输出数据校验 --");
        Map<String, Object> formInput = context.getFormInput();
        if (ObjectUtils.isEmpty(formInput)) {
            System.out.println("输出数据为空");
            context.setErrorMsg("输出数据不能为空");
            return false;
        }
        return true;
    }
}
