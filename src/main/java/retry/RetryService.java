package retry;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * @Date 2022/3/17 8:28 PM
 * @author: leobhao
 */
@Service
public class RetryService {

    /**
     * @EnableRetry 开启重试
     * @RetryAble 注解在需要重试的方法上
     * @Backoff 重试中的退避策略（怎么做下一次的重试）
     * @Recover 兜底方法
     * @throws IllegalAccessException
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
