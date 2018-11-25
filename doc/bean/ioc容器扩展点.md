## IOC容器的扩展点

Spring让Bean对象有一定的扩展性，可以让用户加入一些自定义的操作。

在构建BeanFactory的时候，有BeanFactoryPostProcessor

在构建Bean的时候，有BeanPostProcessor

在创建和销毁Bean的时候有InitializingBean和DisposableBean

还有一个就是FactoryBean，这种特殊的Bean可以被用户更多的控制


### BPP(BeanPostProcessor)