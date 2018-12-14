/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package annotation.Spring_annotation;

import java.lang.reflect.Method;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 *
 *
 * @author HaoBin
 * @version $Id: LogAspect.java, v0.1 2018/12/14 18:15 HaoBin 
 */
@Aspect
@Component
public class LogAspect {
    @Pointcut(value = "@@annotation(annotation.Spring_annotation.Log)")
    private void pointcut() {}

    /**
     * 方法执行前后
     * @param point
     * @param log
     * @return
     */
    @Around(value = "pointcut() && @@annotation(log)")
    public Object around(ProceedingJoinPoint point, Log log) {
        System.out.println("=========== 执行了around ============");
        String requestUrl = log.requestUrl();
        // 拦截class
        Class clazz = point.getTarget().getClass();
        // 拦截Method
        Method method = ((MethodSignature) point.getSignature()).getMethod();
        System.out.println(" 执行了 Class：" + clazz + " Method：" + method + " request url: " + requestUrl);
        try {
            return point.proceed(); //继续执行
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            return throwable.getMessage();
        }
    }

    /**
     * 方法执行后
     * @param joinPoint
     * @param log
     * @param result
     * @return
     */
    @AfterReturning(value = "pointcut() && @annotation(log)", returning = "result")
    public Object afterReturning(JoinPoint joinPoint, Log log, Object result) {
        System.out.println("++++执行了afterReturning方法++++");
        System.out.println("执行结果：" + result);
        return result;
    }

    /**
     * 方法执行后 并抛出异常
     *
     * @param joinPoint
     * @param log
     * @param ex
     */
    @AfterThrowing(value = "pointcut() && @annotation(log)", throwing = "ex")
    public void afterThrowing(JoinPoint joinPoint, Log log, Exception ex) {
        System.out.println("++++执行了afterThrowing方法++++");
        System.out.println("请求：" + log.requestUrl() + " 出现异常");
    }
}