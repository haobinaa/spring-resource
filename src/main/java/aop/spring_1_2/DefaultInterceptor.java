/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package aop.spring_1_2;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 *
 *
 * @author HaoBin
 * @version $Id: DefaultInterceptor.java, v0.1 2018/12/4 21:27 HaoBin 
 */
public class DefaultInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        System.out.println("Before: invocation=[" + methodInvocation + "]");
        // 执行 真实实现类 的方法
        Object rval = methodInvocation.proceed();
        System.out.println("Invocation returned");
        return rval;
    }
}