/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package aop.spring2_aspectJ;

import java.util.Arrays;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

/**
 *
 *
 * @author HaoBin
 * @version $Id: LogArgsAspect.java, v0.1 2018/12/5 22:36 HaoBin 
 */
@Aspect
public class LogArgsAspect {

    // 这里可以设置一些自己想要的属性，到时候在配置的时候注入进来
    private boolean trace = true;

    @Before("aop.spring2_aspectJ.SystemArchitecture.businessService()")
    public void logArgs(JoinPoint joinPoint) {
        if (trace) {
            System.out.println("[@AspectJ]方法执行前，打印入参：" + Arrays.toString(joinPoint.getArgs()));
        }
    }

    public void setTrace(boolean trace) {
        this.trace = trace;
    }

}