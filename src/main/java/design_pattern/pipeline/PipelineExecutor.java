package design_pattern.pipeline;

import design_pattern.pipeline.handler.ContextHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 管道执行器
 *
 * @Date 2021/2/22 2:52 下午
 * @author: leobhao
 */
@Component
public class PipelineExecutor {

    /**
     * 管道路由表
     */
    @Resource
    private Map<Class<? extends PipelineContext>,
                List<? extends ContextHandler<? super PipelineContext>>> pipelineRouteMap;


    /**
     * 数据上下文处理， 直到最后一个处理器返回 true 则返回
     * @param context 数据上下文
     * @return 有异常就停止
     */
    public boolean acceptSync(PipelineContext context) {
        Objects.requireNonNull(context, "上下文数据不能为 null");
        // 拿到数据类型
        Class<? extends PipelineContext> dataType = context.getClass();
        // 获取数据处理管道
        List<? extends ContextHandler<? super PipelineContext>> pipeline = pipelineRouteMap.get(dataType);

        if (CollectionUtils.isEmpty(pipeline)) {
            System.out.println(dataType.getSimpleName() + "管道为空");
            return false;
        }

        // 管道是否畅通
        boolean lastSuccess = true;

        for (ContextHandler<? super PipelineContext> handler : pipeline) {
            try {
                // 当前处理器处理数据，并返回是否继续向下处理
                lastSuccess = handler.handle(context);
            } catch (Throwable ex) {
                lastSuccess = false;
                System.out.println(context.getName() + "执行异常");
            }

            // 不再向下处理
            if (!lastSuccess) { break; }
        }

        return lastSuccess;
    }
}
