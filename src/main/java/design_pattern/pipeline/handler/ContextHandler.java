package design_pattern.pipeline.handler;

import design_pattern.pipeline.PipelineContext;

/**
 * @Date 2021/2/20 5:56 下午
 * @author: leobhao
 */
public interface ContextHandler <T extends PipelineContext>{

    /**
     * 处理上下文数据
     *
     * @param context 上下文数据
     * @return 返回 true 表示下一个 Context 继续处理
     */
    boolean handle(T context);
}
