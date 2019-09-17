/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package base.beanfactorypostprocessor;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * @author HaoBin
 * @version $Id: MyBeanFactoryPostProcessor.java, v0.1 2018/11/25 21:32 HaoBin
 */
public class MyBeanFactoryPostProcessor implements BeanFactoryPostProcessor {


    /**
     * 可以通过 BeanFactoryPostProcessor 对 BeanDefinition 作一些修改
     * @param beanFactory
     * @throws BeansException
     */
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        System.out.println("调用MyBeanFactoryPostProcessor的postProcessBeanFactory");
        BeanDefinition bd = beanFactory.getBeanDefinition("myJavaBean");
        System.out.println("属性值============" + bd.getPropertyValues().toString());
        MutablePropertyValues pv = bd.getPropertyValues();
        if (pv.contains("remark")) {
            System.out.println(pv.get("remark"));
            pv.addPropertyValue("remark", "把备注信息修改一下");
        }
        System.out.println(pv.get("remark"));
        bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    }
}