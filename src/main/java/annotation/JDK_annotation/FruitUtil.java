/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package annotation.JDK_annotation;

import java.lang.reflect.Field;

/**
 *
 *
 * @author HaoBin
 * @version $Id: FruitUtil.java, v0.1 2018/12/14 17:06 HaoBin 
 */
public class FruitUtil {
    public static void getFruitInfo(Class<?> claszz) {

        String strFruitName = " 水果名称: ";
        Field[] fields = claszz.getDeclaredFields();
        for(Field field : fields) {
            if(field.isAnnotationPresent(FruitName.class)) {
                FruitName fruitName =  field.getAnnotation(FruitName.class);
                strFruitName = strFruitName+fruitName.value();
                System.out.println(strFruitName);
            }
        }

    }
}