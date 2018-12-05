/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package aop.spring_2_aspectJ;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;

/**
 *
 *
 * @author HaoBin
 * @version $Id: LogResultAspect.java, v0.1 2018/12/5 22:38 HaoBin 
 */
@Aspect
public class LogResultAspect {

    private boolean trace;

    @AfterReturning(pointcut = "aop.spring_2_aspectJ.SystemArchitecture.businessService()",
            returning = "result")
    public void logResult(Object result) {
        if (trace) {
            System.out.println("[@AspectJ]返回值：" + result);
        }
    }

    public void setTrace(boolean trace) {
        this.trace = trace;
    }
}