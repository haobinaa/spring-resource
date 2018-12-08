### aop源码分析

Spring AOP 的原理很简单，就是动态代理，它和 AspectJ 不一样，AspectJ 是直接修改掉你的字节码。

代理模式很简单，接口 + 真实实现类 + 代理类，其中 真实实现类 和 代理类 都要实现接口，实例化的时候要使用代理类。所以，Spring AOP 需要做的是生成这么一个代理类，然后替换掉真实实现类来对外提供服务。

替换的过程怎么理解呢？在 Spring IOC 容器中非常容易实现，就是在 getBean(…) 的时候返回的实际上是代理类的实例，而这个代理类我们自己没写代码，它是 Spring 采用 JDK Proxy 或 CGLIB 动态生成的。

### 以`DefaultAdvisorAutoProxyCreator`为例

配置advice和advisor如下:
``` 
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd">
  <bean id="userServiceImpl" class="aop.service.impl.UserServiceImpl"/>
  <bean id="orderServiceImpl" class="aop.service.impl.OrderServiceImpl"/>
  <!--定义两个 advice-->
  <bean id="logArgsAdvice" class="aop.spring_1_2.LogArgsAdvice"/>
  <bean id="logResultAdvice" class="aop.spring_1_2.LogResultAdvice"/>
  <!--定义两个 advisor-->
  <!--记录 create* 方法的传参-->
  <bean id="logArgsAdvisor" class="org.springframework.aop.support.RegexpMethodPointcutAdvisor">
    <property name="advice" ref="logArgsAdvice" />
    <property name="pattern" value="aop.service.*.create.*" />
  </bean>
  <!--记录 query* 的返回值-->
  <bean id="logResultAdvisor" class="org.springframework.aop.support.RegexpMethodPointcutAdvisor">
    <property name="advice" ref="logResultAdvice" />
    <property name="pattern" value="aop.service.*.query.*" />
  </bean>
  <!--定义DefaultAdvisorAutoProxyCreator-->
  <bean class="org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator" />
</beans>
```

看下`DefaultAdvisorAutoProxyCreator`继承结构:
![]()

可以看到最顶层的接口是`BeanPostProcessor`， 他的两个方法分别在 init-method 的前后得到执行。
``` 
public interface BeanPostProcessor {
    Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException;
    Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException;
}
```


在ioc初始化bean的步骤中`doCreateBean`:
``` 
//AbstractAutowireCapableBeanFactory
protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final Object[] args)
            throws BeanCreationException {

    // Instantiate the bean.
    BeanWrapper instanceWrapper = null;
    if (mbd.isSingleton()) {
        instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
    }
    if (instanceWrapper == null) {
        // 1. 创建实例
        instanceWrapper = createBeanInstance(beanName, mbd, args);
    }
    ...

    // Initialize the bean instance.
    Object exposedObject = bean;
    try {
        // 2. 装载属性
        populateBean(beanName, mbd, instanceWrapper);
        if (exposedObject != null) {
            // 3. 初始化
            exposedObject = initializeBean(beanName, exposedObject, mbd);
        }
    }
    ...
}

```
在上面第 3 步 initializeBean(...) 方法中会调用 BeanPostProcessor 中的方法，如下：
``` 
protected Object initializeBean(final String beanName, final Object bean, RootBeanDefinition mbd) {
   ...
   Object wrappedBean = bean;
   if (mbd == null || !mbd.isSynthetic()) {
      // 1. 执行每一个 BeanPostProcessor 的 postProcessBeforeInitialization 方法
      wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
   }

   try {
      // 调用 bean 配置中的 init-method="xxx"
      invokeInitMethods(beanName, wrappedBean, mbd);
   }
   ...
   if (mbd == null || !mbd.isSynthetic()) {
      // 我们关注的重点是这里！！！
      // 2. 执行每一个 BeanPostProcessor 的 postProcessAfterInitialization 方法
      wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
   }
   return wrappedBean;
}
```

efaultAdvisorAutoProxyCreator 的继承结构中，postProcessAfterInitialization() 方法在其父类 AbstractAutoProxyCreator 这一层被覆写了：
``` 
public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
   if (bean != null) {
      Object cacheKey = getCacheKey(bean.getClass(), beanName);
      if (!this.earlyProxyReferences.contains(cacheKey)) {
         return wrapIfNecessary(bean, beanName, cacheKey);
      }
   }
   return bean;
}
```

wrapIfNecessary方法将返回代理类:
``` 
protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
   if (beanName != null && this.targetSourcedBeans.contains(beanName)) {
      return bean;
   }
   if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
      return bean;
   }
   if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
      this.advisedBeans.put(cacheKey, Boolean.FALSE);
      return bean;
   }

   // 返回匹配当前 bean 的所有的 advisor、advice、interceptor
   // 对于本文的例子，"userServiceImpl" 和 "OrderServiceImpl" 这两个 bean 创建过程中，
   //   到这边的时候都会返回两个 advisor
   Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
   if (specificInterceptors != DO_NOT_PROXY) {
      this.advisedBeans.put(cacheKey, Boolean.TRUE);
      // 创建代理...创建代理...创建代理...
      // 这里的SingletonTargetSource封装了真实实现类的信息
      Object proxy = createProxy(
            bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
      this.proxyTypes.put(cacheKey, proxy.getClass());
      return proxy;
   }

   this.advisedBeans.put(cacheKey, Boolean.FALSE);
   return bean;
}
```
getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null)，这个方法将得到所有的可用于拦截当前 bean 的 advisor、advice、interceptor

createProxy创建代理, :
``` 
// 注意看这个方法的几个参数，
//   第三个参数携带了所有的 advisors
//   第四个参数 targetSource 携带了真实实现的信息
protected Object createProxy(
      Class<?> beanClass, String beanName, Object[] specificInterceptors, TargetSource targetSource) {

   if (this.beanFactory instanceof ConfigurableListableBeanFactory) {
      AutoProxyUtils.exposeTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName, beanClass);
   }

   // 创建 ProxyFactory 实例
   ProxyFactory proxyFactory = new ProxyFactory();
   proxyFactory.copyFrom(this);

   // 在 schema-based 的配置方式中，我们介绍过，如果希望使用 CGLIB 来代理接口，可以配置
   // proxy-target-class="true",这样不管有没有接口，都使用 CGLIB 来生成代理：
   //   <aop:config proxy-target-class="true">......</aop:config>
   if (!proxyFactory.isProxyTargetClass()) {
      if (shouldProxyTargetClass(beanClass, beanName)) {
         proxyFactory.setProxyTargetClass(true);
      }
      else {
         // 点进去稍微看一下代码就知道了，主要就两句：
         // 1. 有接口的，调用一次或多次：proxyFactory.addInterface(ifc);
         // 2. 没有接口的，调用：proxyFactory.setProxyTargetClass(true);
         evaluateProxyInterfaces(beanClass, proxyFactory);
      }
   }

   // 这个方法会返回匹配了当前 bean 的 advisors 数组
   // 对于本文的例子，"userServiceImpl" 和 "OrderServiceImpl" 到这边的时候都会返回两个 advisor
   // 注意：如果 specificInterceptors 中有 advice 和 interceptor，它们也会被包装成 advisor，进去看下源码就清楚了
   Advisor[] advisors = buildAdvisors(beanName, specificInterceptors);
   for (Advisor advisor : advisors) {
      proxyFactory.addAdvisor(advisor);
   }

   proxyFactory.setTargetSource(targetSource);
   customizeProxyFactory(proxyFactory);

   proxyFactory.setFrozen(this.freezeProxy);
   if (advisorsPreFiltered()) {
      proxyFactory.setPreFiltered(true);
   }

   return proxyFactory.getProxy(getProxyClassLoader());
}
```

这个方法主要是在内部创建了一个 ProxyFactory 的实例，然后 set 了一大堆内容，剩下的工作就都是这个 ProxyFactory 实例的了，通过这个实例来创建代理: `getProxy(classLoader)`:

### ProxyFactory

 ProxyFactory#getProxy(classLoader) :
 ``` 
 public Object getProxy(ClassLoader classLoader) {
    return createAopProxy().getProxy(classLoader);
 }
 ```
 该方法首先通过 createAopProxy() 创建一个 AopProxy 的实例：
 ``` 
 protected final synchronized AopProxy createAopProxy() {
    if (!this.active) {
       activate();
    }
    return getAopProxyFactory().createAopProxy(this);
 }
 ```
### 参考资料

- [spring aop 源码解析](https://javadoop.com/post/spring-aop-source)
- [深入分析java web技术内幕]