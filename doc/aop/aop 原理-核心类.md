### aop 原理简介

aop 的本质是通过代理模式为目标对象生产代理对象，并将横切逻辑插入到目标方法执行的前后。

### 面向切面编程相关术语

- JoinPoint(运行点): 执行增强逻辑的地方. JoinPoint->Invocation->MethodInvocation
- Pointcut(切入点): 用来选择 jointPoint， 即在什么地方调用(Class match 或 Method match)
- Advice(增强)： 想要增强的功能, spring 对 advice 进行了加强封装了 before、after 等

这些概念连起来，意思就是我要在哪里（Pointcut）什么时候（@Before）对谁（JoinPoint）做什么（Advice）

spring 将 Advice和Pointcut两个封装起来了，就叫Advisor。

![](https://raw.githubusercontent.com/haobinaa/spring-resource/master/images/aop/advisor.png)

#### JoinPoint - 连接点

连接点是指程序执行过程中的一些点，比如方法调用，异常处理等。
在 Spring AOP 中，仅支持方法级别的连接点。


``` 
public interface Joinpoint {

    /** 用于执行拦截器链中的下一个拦截器逻辑 */
    Object proceed() throws Throwable;

    Object getThis();

    AccessibleObject getStaticPart();

}
```

`Joinpoint` 接口中，`proceed` 方法是核心，该方法用于执行拦截器逻辑。关于拦截器这里简单说一下吧，以前置通知拦截器为例。在执行目标方法前，该拦截器首先会执行前置通知逻辑，如果拦截器链中还有其他的拦截器，则继续调用下一个拦截器逻辑。直到拦截器链中没有其他的拦截器后，再去调用目标方法。

方法调用是一个连接点， 方法调用 `Invocation` 的定义如下:
``` 
public interface Invocation extends Joinpoint {
    Object[] getArguments();
}

public interface MethodInvocation extends Invocation {
    Method getMethod();
}
```


继承关系链: JoinPoint -> Invocation -> MethodInvocation


#### Pointcut 切点

连接点是靠切点来选择的， Pointcut 定义如下:
``` 
public interface Pointcut {

    /** 返回一个类型过滤器 */
    ClassFilter getClassFilter();

    /** 返回一个方法匹配器 */
    MethodMatcher getMethodMatcher();

    Pointcut TRUE = TruePointcut.INSTANCE;
}
```

Pointcut 接口中定义了两个接口，分别用于返回类型过滤器和方法匹配器,两个接口均定义了 matches 方法，用户只要实现了 matches 方法，即可对连接点进行选择。类型过滤器和方法匹配器接口的定义如下：
``` 
public interface ClassFilter {
    boolean matches(Class<?> clazz);
    ClassFilter TRUE = TrueClassFilter.INSTANCE;

}

public interface MethodMatcher {
    boolean matches(Method method, Class<?> targetClass);
    boolean matches(Method method, Class<?> targetClass, Object... args);
    boolean isRuntime();
    MethodMatcher TRUE = TrueMethodMatcher.INSTANCE;
}
```

#### Advice 通知

Advice 即定义的横切逻辑，Pointcut 定义了在什么地方调用， advice 则定义了在什么时候调用, Spring 有这几种类型:
- 前置通知（Before advice）- 在目标方便调用前执行通知
- 后置通知（After advice）- 在目标方法完成后执行通知
- 返回通知（After returning advice）- 在目标方法执行成功后，调用通知
- 异常通知（After throwing advice）- 在目标方法抛出异常后，执行通知
- 环绕通知（Around advice）- 在目标方法调用前后均可执行自定义逻辑

以 `Before Advice` 和 `After Returning Advice` 为例:
``` 
// Advice 只是一个空接口
public interface Advice {

}
// BeforeAdvice  本身也什么都没做
public interface BeforeAdvice extends Advice {

}

// Method Before 定义了 method 之前执行
public interface MethodBeforeAdvice extends BeforeAdvice {

    void before(Method method, Object[] args, Object target) throws Throwable;
}

/** AfterAdvice */
public interface AfterAdvice extends Advice {

}

// 方法返回之后执行
public interface AfterReturningAdvice extends AfterAdvice {

    void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable;
}
```

#### Aspect 切面

切面 Aspect 整合了切点和通知两个模块，切点解决了 where 问题，通知解决了 when 和 how 问题。切面把两者整合起来，就可以解决 对什么方法（where）在何时（when - 前置还是后置，或者环绕）执行什么样的横切逻辑.


在 Spring 中 `PointcutAdvisor` 就是对切面的定义:
``` 
public interface Advisor {

    Advice getAdvice();
    boolean isPerInstance();
}

public interface PointcutAdvisor extends Advisor {

    Pointcut getPointcut();
}
```

### AOP 入口分析

在 [AOP 创建过程源码分析](./aop_sourcecode.md)中分析过 Spring 是如何向 Bean 中织入 Advice， 即通过 BeanPostProcessor 接口：
``` 
public abstract class AbstractAutoProxyCreator extends ProxyProcessorSupport
        implements SmartInstantiationAwareBeanPostProcessor, BeanFactoryAware {
    
    @Override
    /** bean 初始化后置处理方法 */
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean != null) {
            Object cacheKey = getCacheKey(bean.getClass(), beanName);
            if (!this.earlyProxyReferences.contains(cacheKey)) {
                // 如果需要，为 bean 生成代理对象
                return wrapIfNecessary(bean, beanName, cacheKey);
            }
        }
        return bean;
    }
    
    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
        if (beanName != null && this.targetSourcedBeans.contains(beanName)) {
            return bean;
        }
        if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {
            return bean;
        }

        /*
         * 如果是基础设施类（Pointcut、Advice、Advisor 等接口的实现类），或是应该跳过的类，
         * 则不应该生成代理，此时直接返回 bean
         */ 
        if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {
            // 将 <cacheKey, FALSE> 键值对放入缓存中，供上面的 if 分支使用
            this.advisedBeans.put(cacheKey, Boolean.FALSE);
            return bean;
        }

        // 为目标 bean 查找合适的通知器
        Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);
        /*
         * 若 specificInterceptors != null，即 specificInterceptors != DO_NOT_PROXY，
         * 则为 bean 生成代理对象，否则直接返回 bean
         */ 
        if (specificInterceptors != DO_NOT_PROXY) {
            this.advisedBeans.put(cacheKey, Boolean.TRUE);
            // 创建代理
            Object proxy = createProxy(
                    bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));
            this.proxyTypes.put(cacheKey, proxy.getClass());
            /*
             * 返回代理对象，此时 IOC 容器输入 bean，得到 proxy。此时，
             * beanName 对应的 bean 是代理对象，而非原始的 bean
             */ 
            return proxy;
        }

        this.advisedBeans.put(cacheKey, Boolean.FALSE);
        // specificInterceptors = null，直接返回 bean
        return bean;
    }
}
```

大概总结下来就是如下流程:

1. 若 bean 是 AOP 基础设施类型，则直接返回
2. 为 bean 查找合适的通知器
3. 如果通知器数组不为空，则为 bean 生成代理对象，并返回该对象
4. 若数组为空，则返回原始 bean