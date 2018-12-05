/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package aop.spring_2_schema;

import java.util.Arrays;
import org.aspectj.lang.JoinPoint;

/**
 *
 *
 * @author HaoBin
 * @version $Id: LogArgsAspect.java, v0.1 2018/12/5 23:08 HaoBin 
 */
public class LogArgsAspect {

    // 这里可以设置一些自己想要的属性，到时候在配置的时候注入进来
    public void logArgs(JoinPoint joinPoint) {
        System.out.println("[schema-based]方法执行前，打印入参：" + Arrays.toString(joinPoint.getArgs()));
    }
}