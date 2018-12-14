/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package annotation.Spring_annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 *
 * @author HaoBin
 * @version $Id: Log.java, v0.1 2018/12/14 18:14 HaoBin 
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {
    String requestUrl() default "/";
}
