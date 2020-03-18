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
![](https://raw.githubusercontent.com/haobinaa/spring-resource/master/images/DefaultAdvisorAutoProxyCreator.png)

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
        // 3. 初始化
        exposedObject = initializeBean(beanName, exposedObject, mbd);
    }
    ...
}

```
在上面第 3 步 `initializeBean`会调用 BeanPostProcessor 中的方法，如下：
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

`DefaultAdvisorAutoProxyCreator` 的继承结构中，postProcessAfterInitialization
() 方法在其父类 `AbstractAutoProxyCreator` 这一层被覆写了：
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

wrapIfNecessary 将返回代理类:
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
   Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
   if (specificInterceptors != DO_NOT_PROXY) {
      this.advisedBeans.put(cacheKey, Boolean.TRUE);
      // 创建代理
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
`getAdvicesAndAdvisorsForBean`将得到所有的可用于拦截当前 bean 的 advisor、advice、interceptor。
然后调用 `createProxy` 创建代理, :
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
 // ProxyCreateSupport
 protected final synchronized AopProxy createAopProxy() {
    if (!this.active) {
       activate();
    }
    return getAopProxyFactory().createAopProxy(this);
 }
 ```
 AopProxy是通过AopProxyFactory创建的， 所以需要AopProxyFactory的实例， ProxyCreateSupport的构造方法中:
 ``` 
 public ProxyCreatorSupport() {
    this.aopProxyFactory = new DefaultAopProxyFactory();
 }
 ```
 可以看到是用l`DefaultAopProxyFactory `, 他的createAopProxy方法:
 ``` 
 // DefaultAopProxyFactory 
 public class DefaultAopProxyFactory implements AopProxyFactory, Serializable {
 
    @Override
    public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
       // (我也没用过这个optimize，默认false) || (proxy-target-class=true) || (没有接口)
       if (config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config)) {
          Class<?> targetClass = config.getTargetClass();
          if (targetClass == null) {
             throw new AopConfigException("TargetSource cannot determine target class: " +
                   "Either an interface or a target is required for proxy creation.");
          }
          // 如果要代理的类本身就是接口，也会用 JDK 动态代理
          // 我也没用过这个。。。
          if (targetClass.isInterface() || Proxy.isProxyClass(targetClass)) {
             return new JdkDynamicAopProxy(config);
          }
          return new ObjenesisCglibAopProxy(config);
       }
       else {
          // 如果有接口，会跑到这个分支
          return new JdkDynamicAopProxy(config);
       }
    }
    // 判断是否有实现自定义的接口
    private boolean hasNoUserSuppliedProxyInterfaces(AdvisedSupport config) {
       Class<?>[] ifcs = config.getProxiedInterfaces();
       return (ifcs.length == 0 || (ifcs.length == 1 && SpringProxy.class.isAssignableFrom(ifcs[0])));
    }
 
 }
 ```
 到这里，我们知道 createAopProxy 方法有可能返回 JdkDynamicAopProxy 实例，也有可能返回 ObjenesisCglibAopProxy 实例，这里总结一下：
1. 如果被代理的目标类实现了一个或多个自定义的接口，那么就会使用 JDK 动态代理，如果没有实现任何接口，会使用 CGLIB 实现代理，如果设置了 proxy-target-class="true"，那么都会使用 CGLIB。
2. JDK 动态代理基于接口，所以只有接口中的方法会被增强，而 CGLIB 基于类继承，需要注意就是如果方法使用了 final 修饰，或者是 private 方法，是不能被增强的。

备注： [jdk代理和cglib代理的简单使用](https://github.com/haobinaa/spring-resource/blob/master/doc/aop/jdk%E5%8A%A8%E6%80%81%E4%BB%A3%E7%90%86%E5%92%8Ccglib%E5%8A%A8%E6%80%81%E4%BB%A3%E7%90%86.md)

分别看一下两个AopProxy实现类的`getProxy`方法：

#### JdkDynamicAopProxy 
``` 
@Override
public Object getProxy(ClassLoader classLoader) {
   if (logger.isDebugEnabled()) {
      logger.debug("Creating JDK dynamic proxy: target source is " + this.advised.getTargetSource());
   }
   Class<?>[] proxiedInterfaces = AopProxyUtils.completeProxiedInterfaces(this.advised, true);
   findDefinedEqualsAndHashCodeMethods(proxiedInterfaces);
   // 第三个参数是需要实现InvocationHandler
   return Proxy.newProxyInstance(classLoader, proxiedInterfaces, this);
}
```
java.lang.reflect.Proxy.newProxyInstance(…) 方法需要三个参数，第一个是 ClassLoader，第二个参数代表需要实现哪些接口，第三个参数最重要，是 InvocationHandler 实例，我们看到这里传了 this，因为 JdkDynamicAopProxy 本身实现了 InvocationHandler 接口。


``` 
// JdkDynamicAopProxy实现InvocationHandler
@Override
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
   MethodInvocation invocation;
   Object oldProxy = null;
   boolean setProxyContext = false;

   TargetSource targetSource = this.advised.targetSource;
   Class<?> targetClass = null;
   Object target = null;

   try {
      if (!this.equalsDefined && AopUtils.isEqualsMethod(method)) {
         // The target does not implement the equals(Object) method itself.
         // 代理的 equals 方法
         return equals(args[0]);
      }
      else if (!this.hashCodeDefined && AopUtils.isHashCodeMethod(method)) {
         // The target does not implement the hashCode() method itself.
         // 代理的 hashCode 方法
         return hashCode();
      }
      else if (method.getDeclaringClass() == DecoratingProxy.class) {
         // There is only getDecoratedClass() declared -> dispatch to proxy config.
         // 
         return AopProxyUtils.ultimateTargetClass(this.advised);
      }
      else if (!this.advised.opaque && method.getDeclaringClass().isInterface() &&
            method.getDeclaringClass().isAssignableFrom(Advised.class)) {
         // Service invocations on ProxyConfig with the proxy config...
         return AopUtils.invokeJoinpointUsingReflection(this.advised, method, args);
      }

      Object retVal;

      // 如果设置了 exposeProxy，那么将 proxy 放到 ThreadLocal 中
      if (this.advised.exposeProxy) {
         // Make invocation available if necessary.
         oldProxy = AopContext.setCurrentProxy(proxy);
         setProxyContext = true;
      }

      // May be null. Get as late as possible to minimize the time we "own" the target,
      // in case it comes from a pool.
      target = targetSource.getTarget();
      if (target != null) {
         targetClass = target.getClass();
      }

      // Get the interception chain for this method.
      // 创建一个 chain，包含所有要执行的 advice
      List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);

      // Check whether we have any advice. If we don't, we can fallback on direct
      // reflective invocation of the target, and avoid creating a MethodInvocation.
      if (chain.isEmpty()) {
         // We can skip creating a MethodInvocation: just invoke the target directly
         // Note that the final invoker must be an InvokerInterceptor so we know it does
         // nothing but a reflective operation on the target, and no hot swapping or fancy proxying.
         // chain 是空的，说明不需要被增强，这种情况很简单
         Object[] argsToUse = AopProxyUtils.adaptArgumentsIfNecessary(method, args);
         retVal = AopUtils.invokeJoinpointUsingReflection(target, method, argsToUse);
      }
      else {
         // We need to create a method invocation...
         // 执行方法，得到返回值
         invocation = new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain);
         // Proceed to the joinpoint through the interceptor chain.
         retVal = invocation.proceed();
      }

      // Massage return value if necessary.
      Class<?> returnType = method.getReturnType();
      if (retVal != null && retVal == target &&
            returnType != Object.class && returnType.isInstance(proxy) &&
            !RawTargetAccess.class.isAssignableFrom(method.getDeclaringClass())) {
         // Special case: it returned "this" and the return type of the method
         // is type-compatible. Note that we can't help if the target sets
         // a reference to itself in another returned object.
         retVal = proxy;
      }
      else if (retVal == null && returnType != Void.TYPE && returnType.isPrimitive()) {
         throw new AopInvocationException(
               "Null return value from advice does not match primitive return type for: " + method);
      }
      return retVal;
   }
   finally {
      if (target != null && !targetSource.isStatic()) {
         // Must have come from TargetSource.
         targetSource.releaseTarget(target);
      }
      if (setProxyContext) {
         // Restore old proxy.
         AopContext.setCurrentProxy(oldProxy);
      }
   }
}
```
大概步骤是：在执行每个方法的时候，判断下该方法是否需要被一次或多次增强（执行一个或多个 advice）




#### ObjenesisCglibAopProxy

ObjenesisCglibAopProxy 继承了 CglibAopProxy，而 CglibAopProxy 继承了 AopProxy。

ObjenesisCglibAopProxy的 getProxy(classLoader) 方法在父类 CglibAopProxy 类中， 代码量较大， 主要是CGLIB的核心 `Enhancer`






### 参考资料

- [spring aop 源码解析](https://javadoop.com/post/spring-aop-source)
- [深入分析java web技术内幕]