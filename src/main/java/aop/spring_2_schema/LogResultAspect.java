/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package aop.spring_2_schema;

/**
 *
 *
 * @author HaoBin
 * @version $Id: LogResultAspect.java, v0.1 2018/12/5 23:07 HaoBin 
 */
public class LogResultAspect {

    public void logResult(Object result) {
        System.out.println("[schema-based]返回值：" + result);
    }
}