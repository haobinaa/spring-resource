/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package annotation.JDK_annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 *
 * @author HaoBin
 * @version $Id: FruitName.java, v0.1 2018/12/14 17:01 HaoBin 
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FruitName {
    String value() default "";
}
