/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package annotation.JDK_annotation;

/**
 *
 *
 * @author HaoBin
 * @version $Id: Apple.java, v0.1 2018/12/14 17:21 HaoBin 
 */
public class Apple {

    @FruitName("apple")
    private String appleName;

    public String getAppleName() {
        return appleName;
    }

    public void setAppleName(String appleName) {
        this.appleName = appleName;
    }

    public static void main(String[] args) {
        FruitUtil.getFruitInfo(Apple.class);
    }
}