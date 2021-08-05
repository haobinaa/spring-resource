package base.retry;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * @Date 2021/7/29 5:12 下午
 * @author: leobhao
 */
@Service
public class SpringRetry {

    /**
     * @Retryable 开启重试
     * @BackOff 重试策略
     * @Recover 兜底方法， 重试结束后还是失败则执行
     */
    @Retryable(value = IllegalAccessException.class, maxAttempts = 5,
            backoff= @Backoff(value = 1500, maxDelay = 100000, multiplier = 1.2))
    public void service() throws IllegalAccessException {
        System.out.println("service method...");
        throw new IllegalAccessException("manual exception");
    }


    @Recover
    public void recover(IllegalAccessException e){
        System.out.println("service retry after Recover => " + e.getMessage());
    }
}
