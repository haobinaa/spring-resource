### Spring AOP 简介 与 AspectJ

#### Spring Aop
- 它基于动态代理来实现。默认地，如果使用接口的，用 JDK 提供的动态代理实现，如果没有接口，使用 CGLIB 实现。
- Spring AOP 只能作用于 Spring 容器中的 Bean，它是使用纯粹的 Java 代码实现的，只能作用于 bean 的方法
- Spring 提供了 AspectJ 的支持，一般来说我们用纯的 Spring AOP 就够了
- 很多人会对比 Spring AOP 和 AspectJ 的性能，Spring AOP 是基于代理实现的，在容器启动的时候需要生成代理实例，在方法调用上也会增加栈的深度，使得 Spring AOP 的性能不如 AspectJ 那么好


#### AspectJ
- 属于静态织入，它是通过修改代码来实现的，它的织入时机可以是：
  - Compile-time weaving：编译期织入，如类 A 使用 AspectJ 添加了一个属性，类 B 引用了它，这个场景就需要编译期的时候就进行织入，否则没法编译类 B
  - Post-compile weaving：也就是已经生成了 .class 文件，或已经打成 jar 包了，这种情况我们需要增强处理的话，就要用到编译后织入
  - Load-time weaving：指的是在加载类的时候进行织入，要实现这个时期的织入，有几种常见的方法。1、自定义类加载器来干这个，这个应该是最容易想到的办法，在被织入类加载到 JVM 
  前去对它进行加载，这样就可以在加载的时候定义行为了。2、在 JVM 启动的时候指定 AspectJ 提供的 `agent：-javaagent:xxx/xxx/aspectjweaver.jar`
- 因为 AspectJ 在实际代码运行前完成了织入，所以大家会说它生成的类是没有额外运行时开销的

#### Spring aop的发展

Spring 延用了 AspectJ 中的概念，包括使用了 AspectJ 提供的 jar 包中的注解，但是不依赖于其实现功能。如 @Aspect、@Pointcut、@Before、@After 等注解都是来自于 AspectJ，但是功能的实现是纯 Spring AOP 自己实现的。

目前 Spring AOP 一共有三种配置方式，Spring 做到了很好地向下兼容：
- Spring 1.2 基于接口的配置：最早的 Spring AOP 是完全基于几个接口的，源码实现可以从这里起步
- Spring 2.0 schema-based 配置：Spring 2.0 以后使用 XML 的方式来配置，使用 命名空间 <aop />
- Spring 2.0 @AspectJ 配置：使用注解的方式来配置，这种方式感觉是最方便的，还有，这里虽然叫做 @AspectJ，但是这个和 AspectJ 其实没啥关系


### xml配置aop的使用

#### 普通的advice配置
定义advice:
``` 
public class LogArgsAdvice implements MethodBeforeAdvice {
    public void before(Method method, Object[] objects, Object o) throws Throwable {
        System.out.println("[advice]准备执行方法: " + method.getName() + ", 参数列表：" + Arrays.toString(objects));
    }
}
public class LogResultAdvice implements AfterReturningAdvice {

    public void afterReturning(Object returnValue, Method method, Object[] objects, Object o1) throws Throwable {
        System.out.println("[advice]方法返回：" + returnValue);
    }
}
```
我们可以在方法调用前和调用后进行拦截

[xml配置](https://github.com/haobinaa/spring-resource/blob/master/src/main/resources/spring_1_2_advice.xml)

我们在代理的bean配置`interceptor`，就可以对指定的bean进行拦截， 被代理的bean所有的方法都会执行我们定义好的advice

代理bean的配置如下:
``` 
 <bean id="userServiceProxy" class="org.springframework.aop.framework.ProxyFactoryBean">
    <!--代理的接口-->
    <property name="proxyInterfaces">
      <list>
        <value>aop.service.UserService</value>
      </list>
    </property>
    <!--代理的具体实现-->
    <property name="target" ref="userServiceImpl"/>

    <!--配置拦截器，这里可以配置 advice、advisor、interceptor, 这里先介绍 advice-->
    <property name="interceptorNames">
      <list>
        <value>logArgsAdvice</value>
        <value>logResultAdvice</value>
      </list>
    </property>
  </bean>
```
我们配置好代理接口和实现类，只需要在`interceptorNames`配置上我们的拦截器，可以是advice、advisor、interceptor
#### advisor配置

advice的拦截实现了类级别的拦截，而advisor则是方法级别的拦截，只拦截特定的方法

[xml配置](https://github.com/haobinaa/spring-resource/blob/master/src/main/resources/spring_1_2_advisor.xml)

我们单独看advisor配置:
``` 
  <bean id="logCreateAdvisor" class="org.springframework.aop.support.NameMatchMethodPointcutAdvisor">
    <property name="advice" ref="logArgsAdvice" />
    <property name="mappedNames" value="createUser" />
  </bean>
```
 Advisor部需要指定一个 Advice，Advisor 决定该拦截哪些方法，拦截后需要完成的工作还是内部的 Advice 来做。
 
 advisor有好几个实现类，这里我们使用实现类 NameMatchMethodPointcutAdvisor 来演示，从名字上就可以看出来，它需要我们给它提供方法名字，这样符合该配置的方法才会做拦截。
 
#### interceptor配置

interceptor 与动态代理很像:
``` 
public class DefaultInterceptor implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        System.out.println("Before: invocation=[" + methodInvocation + "]");
        // 执行 真实实现类 的方法
        Object rval = methodInvocation.proceed();
        System.out.println("Invocation returned");
        return rval;
    }
}
```

[xml配置](https://github.com/haobinaa/spring-resource/blob/master/src/main/resources/spring_1_2_interceptor.xml)

#### AutoProxy

在之前的advice配置当中， 我们配置出一个代理的bean使用的是`ProxyFactoryBean`， 这样需要对每一个需要代理的bean都配置一个代理bean。 Spring提供了自动代理，当 Spring 发现一个 bean 需要被切面织入的时候，Spring 会自动生成这个 bean 的一个代理来拦截方法的执行，确保定义的切面能被执行。

##### BeanNameAutoProxyCreator 
根据bean的名称来决定是否生成Proxy Bean，beanNames 中可以使用正则来匹配 bean 的名字

配置如下:
``` 
  <bean class="org.springframework.aop.framework.autoproxy.BeanNameAutoProxyCreator">
    <property name="interceptorNames">
      <list>
        <value>logArgsAdvice</value>
        <value>logResultAdvice</value>
      </list>
    </property>
    <!-- 可以通过正则匹配 -->
    <property name="beanNames" value="*ServiceImpl" />
  </bean>
```

在使用的时候，不在需要根据代理找bean：
``` 
UserService userService = (UserService) context.getBean(UserService.class);
OrderService orderService = (OrderService) context.getBean(OrderService.class);
```
[完整的xml配置](https://github.com/hongjiev/spring-aop-learning/blob/master/src/main/resources/spring_1_2_BeanNameAutoProxy.xml)

##### DefaultAdvisorAutoProxyCreator
通过配置 Advisor，精确定位到需要被拦截的方法，然后使用内部的 Advice 执行逻辑处理。之前我们配置advisor的时候是这样配置的:
``` 
  <bean id="logCreateAdvisor" class="org.springframework.aop.support.NameMatchMethodPointcutAdvisor">
    <property name="advice" ref="logArgsAdvice" />
    <property name="mappedNames" value="createUser" />
  </bean>
```
Advisor 还有一个更加灵活的实现类 RegexpMethodPointcutAdvisor，它能实现正则匹配，如：
``` 
<bean id="logArgsAdvisor" class="org.springframework.aop.support.RegexpMethodPointcutAdvisor">
    <property name="advice" ref="logArgsAdvice" />
    <property name="pattern" value="com.javadoop.*.service.*.create.*" />
</bean>
```
之后，我们需要配置 DefaultAdvisorAutoProxyCreator，它会使得所有的 Advisor 自动生效，无须其他配置。
``` 
 <bean class="org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator" />
```

### @AspectJ配置
@AspectJ 和 AspectJ 没多大关系，并不是说基于 AspectJ 实现的，而仅仅是使用了 AspectJ 中的概念，包括使用的注解也是直接来自于 AspectJ 的包。

#### 使用@AspectJ

##### 引入依赖
需要依赖 aspectjweaver.jar 这个包，这个包来自于 AspectJ：
``` 
<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjweaver</artifactId>
    <version>1.8.11</version>
</dependency>
```

如果是使用 Spring Boot 的话，添加以下依赖即可：
```
<dependency>
   <groupId>org.springframework.boot</groupId>
   <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

之所以要引入 aspectjweaver 并不是因为我们需要使用 AspectJ 的处理功能，而是因为 Spring 使用了 AspectJ 提供的一些注解，实际上还是纯的 Spring AOP 代码

##### 开启@AspectJ
开启 @AspectJ 的注解配置方式，有两种方式：
- 在 xml 中配置：
``` 
<aop:aspectj-autoproxy/>
```
- 使用 @EnableAspectJAutoProxy
``` 
@Configuration
@EnableAspectJAutoProxy
public class AppConfig {

}
```

一旦开启了上面的配置，那么所有使用`@Aspect` 注解的 bean 都会被 Spring 当做用来实现 AOP 的配置类，我们称之为一个 Aspect。

##### 配置pointcut
定义切点，用于定义哪些方法需要被增强或者说需要被拦截，有点类似于之前介绍的 Advisor 的方法匹配。

配置方法:
``` 
@Pointcut("execution(* transfer(..))")// the pointcut expression
private void anyOldTransfer() {}// the pointcut signature
```
@Pointcut 中使用了 execution 来正则匹配方法签名, 除了execution，还有几种比较常见的匹配方式:
- within：指定所在类或所在包下面的方法
``` 
如 @Pointcut("within(aop.springaoplearning.service..*)")
```
- @annotation：方法上具有特定的注解，如 @Subscribe 用于订阅特定的事件
```
如 @Pointcut("execution( .*(..)) && @annotation(aop.annotation.Subscribe)")
```
- bean(idOrNameOfBean)：匹配 bean 的名字
``` 
如 @Pointcut("bean(*Service)")
```

以上匹配中通常` .` 代表一个包名，`..` 代表包及其子包，方法参数任意匹配使用两个点 `..`

示例定义一个pointcut类, 定义出我们需要的切点
``` 
@Aspect
public class SystemArchitecture {

    @Pointcut("within(aop.service..*)")
    public void inServiceLayer() {}

    @Pointcut("within(aop.dao..*)")
    public void inDataAccessLayer() {}

    @Pointcut("execution(* aop.service.*.*(..))")
    public void businessService() {}

    @Pointcut("execution(* aop.dao.*.*(..))")
    public void dataAccessOperation() {}

}
```

##### 定义advice
定义好切点之后， 就需要定义advice， 配置需要对这些被拦截的方法做些什么

常用方法示例:
``` 
@Aspect
public class AdviceExample {

    // 下面方法就是写拦截 "dao层实现"
    @Before("aop.SystemArchitecture.dataAccessOperation()")
    public void doAccessCheck() {
        // ... 实现代码
    }

    // 当然，我们也可以直接"内联"Pointcut，直接在这里定义 Pointcut
    // 把 Advice 和 Pointcut 合在一起了，但是这两个概念我们还是要区分清楚的
    @Before("execution(* aop.dao.*.*(..))")
    public void doAccessCheck() {
        // ... 实现代码
    }

    @AfterReturning("aop.aop.SystemArchitecture.dataAccessOperation()")
    public void doAccessCheck() {
        // ...
    }

    @AfterReturning(
        pointcut="com.javadoop.aop.SystemArchitecture.dataAccessOperation()",
        returning="retVal")
    public void doAccessCheck(Object retVal) {
        // 这样，进来这个方法的处理时候，retVal 就是相应方法的返回值，是不是非常方便
        //  ... 实现代码
    }

    // 异常返回
    @AfterThrowing("com.javadoop.aop.SystemArchitecture.dataAccessOperation()")
    public void doRecoveryActions() {
        // ... 实现代码
    }

    @AfterThrowing(
        pointcut="com.javadoop.aop.SystemArchitecture.dataAccessOperation()",
        throwing="ex")
    public void doRecoveryActions(DataAccessException ex) {
        // ... 实现代码
    }

    // 注意理解它和 @AfterReturning 之间的区别，这里会拦截正常返回和异常的情况
    @After("com.javadoop.aop.SystemArchitecture.dataAccessOperation()")
    public void doReleaseLock() {
        // 通常就像 finally 块一样使用，用来释放资源。
        // 无论正常返回还是异常退出，都会被拦截到
    }

    // 感觉这个很有用吧，既能做 @Before 的事情，也可以做 @AfterReturning 的事情
    @Around("com.javadoop.aop.SystemArchitecture.businessService()")
    public Object doBasicProfiling(ProceedingJoinPoint pjp) throws Throwable {
        // start stopwatch
        Object retVal = pjp.proceed();
        // stop stopwatch
        return retVal;
    }
}
```
### 参考资料
- [Spring aop 前世今生](https://javadoop.com/post/spring-aop-intro)
- [深入分析java web技术内幕]
- [spring 技术内幕]