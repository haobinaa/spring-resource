/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package aop.spring2_aspectJ;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 *
 *
 * @author HaoBin
 * @version $Id: SystemArchitecture.java, v0.1 2018/12/5 21:32 HaoBin 
 */
@Aspect
public class SystemArchitecture {


    @Pointcut("within(aop.service..*)")
    public void inServiceLayer() {}

//    @Pointcut("within(aop.dao..*)")
//    public void inDataAccessLayer() {}

    @Pointcut("execution(* aop.service.*.*(..))")
    public void businessService() {}

//    @Pointcut("execution(* aop.dao.*.*(..))")
//    public void dataAccessOperation() {}

}