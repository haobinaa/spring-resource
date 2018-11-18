### BeanPostProcessor作用

BeanPostProcessor也称为Bean后置处理器，它是Spring中定义的接口，在Spring容器的创建过程中（具体为Bean初始化前后）会回调所有实现BeanPostProcessor接口中定义的两个方法的bean。
``` 
public interface BeanPostProcessor {


    Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException;

    
    Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException;

}
```