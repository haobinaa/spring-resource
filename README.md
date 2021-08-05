# spring-resource
spring resouce code learn

自己学习Spring过程中的一些记录， 太复杂的功能记录大概流程， 一些重要点注释实现细节和补充一些使用的例子

## spring 源码阅读记录

### IOC 

- [bean的定义-BeanDefinition ](./doc/bean/bean.md)
- [IOC启动过程分析](https://github.com/haobinaa/spring-resource/blob/master/doc/ioc/ioc.md)
- [IOC容器中的常用扩展点](./doc/ioc/ioc容器扩展点.md)
- [spring LifeCycle](doc/ioc/lifecycle.md)
- [手动导入Bean的方式(import方式导入bean，这也是SpringBoot的@EnableAutoConfiguration的基石)](src/main/java/importbean/EnableColor.java)

### AOP

- [aop 与 AspectJ 概述](./doc/aop/aop概述.md)
- [AspectJ 使用](./doc/aop/AspectJ使用.md)
- [aop 代理创建过程源码](./doc/aop/aop_sourcecode.md)
- [jdk动态代理和cglib动态代理](./doc/aop/jdk动态代理和cglib动态代理.md)
- [aop 原理-核心类和入口分析](./doc/aop/aop%20原理-核心类.md)
- [aop 原理-拦截器执行链](./doc/aop/Aop%20拦截器链执行过程.md)

### MVC 

- [SpringMVC源码分析](./doc/mvc/spring_mvc_source.md)


## spring 实战

### 设计模式
- [结合spring的策略模式](./src/main/java/design_pattern/strategy/SpringStrategyMain.java)
- [结合spring的管道模式](./src/main/java/design_pattern/pipeline/SpringPipelineMain.java)

### 特性实践

- [spring事件机制-ApplicationEvent](./doc/ioc/spring事件处理与监听.md)
