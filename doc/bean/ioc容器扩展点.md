## IOC容器的扩展点

Spring让Bean对象有一定的扩展性，可以让用户加入一些自定义的操作。

在构建BeanFactory的时候，有BeanFactoryPostProcessor

在构建Bean的时候，有BeanPostProcessor

在创建和销毁Bean的时候有InitializingBean和DisposableBean

还有一个就是[FactoryBean](https://github.com/haobinaa/spring-resource/blob/master/doc/bean/FactoryBean.md)，这种特殊的Bean可以被用户更多的控制


### BeanFactoryPostProcessor

实现该接口，可以在spring的bean创建之前，修改bean的定义属性。也就是说，Spring允许BeanFactoryPostProcessor在容器实例化任何其它bean之前读取配置元数据，并可以根据需要进行修改，例如可以把bean的scope从singleton改为prototype，也可以把property的值给修改掉。可以同时配置多个BeanFactoryPostProcessor，并通过设置'order'属性来控制各个BeanFactoryPostProcessor的执行次序。


```
public interface BeanFactoryPostProcessor {
	void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException;
}
```


BeanFactoryPostProcessor是在spring容器加载了bean的定义文件之后，在bean实例化之前执行的。接口方法的入参是ConfigurrableListableBeanFactory
，使用该参数，可以获取到相关bean的定义信息，如：
####  例子


### BPP(BeanPostProcessor)


### InitializingBean

### DisposableBean


### 参考资料
- [Spring的BeanFactoryPostProcessor和BeanPostProcessor](https://blog.csdn.net/caihaijiang/article/details/35552859)