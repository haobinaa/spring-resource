/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package aop.spring_2_schema;

import org.aspectj.lang.annotation.Pointcut;

/**
 *
 *
 * @author HaoBin
 * @version $Id: SystemArchitecture.java, v0.1 2018/12/5 23:09 HaoBin 
 */
public class SystemArchitecture {

    @Pointcut("within(aop.web..*)")
    public void inWebLayer() {
    }

    @Pointcut("within(aop.service..*)")
    public void inServiceLayer() {
    }

    @Pointcut("execution(* aop.service.*.*(..))")
    public void businessService() {
    }

}