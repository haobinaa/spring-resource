package design_pattern.pipeline.handler;

import design_pattern.pipeline.InstanceBuildContext;
import org.springframework.stereotype.Component;

/**
 * @Date 2021/2/20 6:13 下午
 * @author: leobhao
 */
@Component
public class ModelInstanceCreator implements ContextHandler<InstanceBuildContext> {


    @Override
    public boolean handle(InstanceBuildContext context) {
        System.out.println("-- 根据输入模型创建 --");
        // 处理模型实例
        return true;
    }
}
