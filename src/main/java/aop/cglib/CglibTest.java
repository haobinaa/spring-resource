/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package aop.cglib;

import net.sf.cglib.proxy.Enhancer;

/**
 *
 *
 * @author HaoBin
 * @version $Id: CglibTest.java, v0.1 2018/12/10 18:44 HaoBin 
 */
public class CglibTest {

    public static void main(String[] args) {
        ProxyObj proxyObj = new ProxyObj();
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(RealObj.class);
        enhancer.setCallback(proxyObj);
        RealObj obj = (RealObj) enhancer.create();
        obj.visit();
    }

}