package design_pattern.pipeline;

import java.time.LocalDateTime;

/**
 * 传递到管道的上下文
 *
 * @Date 2021/2/20 5:52 下午
 * @author: leobhao
 */
public class PipelineContext {

    private LocalDateTime startTime;
    private LocalDateTime endTime;


    public String getName() {
        return this.getClass().getSimpleName();
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
