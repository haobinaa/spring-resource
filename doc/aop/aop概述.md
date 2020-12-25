### Spring AOP 简介 与 AspectJ

#### Spring Aop

- 它基于动态代理来实现。默认地，如果基于接口则用`JDK动态代理`实现，如果没有接口则使用 `CGLIB` 实现
- Spring AOP 只能作用于 Spring 容器中的 Bean，它是使用纯粹的 Java 代码实现的，只能作用于 bean 的方法
- Spring 提供了 AspectJ 的支持，一般来说我们用纯的 Spring AOP 就够了
- 很多人会对比 Spring AOP 和 AspectJ 的性能，Spring AOP 是基于代理实现的，在容器启动的时候需要生成代理实例，在方法调用上也会增加栈的深度，使得 Spring AOP 的性能不如 AspectJ 那么好


#### AspectJ

- 属于静态织入，它是通过修改代码来实现的，它的织入时机可以是：
  - Compile-time weaving：编译期织入，如类 A 使用 AspectJ 添加了一个属性，类 B 引用了它，这个场景就需要编译期的时候就进行织入，否则没法编译类 B
  - Post-compile weaving：编译后织入，也就是已经生成了 `.class` 文件，或已经打成 jar 包了，这种情况我们需要增强处理的话，就要用到编译后织入
  - Load-time weaving：指的是在加载类的时候进行织入，要实现这个时期的织入，有几种常见的方法:
    1. 自定义类加载器来干这个，这个应该是最容易想到的办法，在被织入类加载到 JVM 前去对它进行加载，这样就可以在加载的时候定义行为了
    2. 在 JVM 启动的时候指定 AspectJ 提供的 `agent：-javaagent:xxx/xxx/aspectjweaver.jar`
- AspectJ 在代码运行前已经完成了织入， 所以它生成的类是没有额外开销的


### xml配置aop的使用

#### 普通的advice配置

定义advice，在方法调用前和调用后进行拦截:
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

我们在代理的bean配置`interceptor`，就可以对指定的bean进行拦截， 被代理的bean所有的方法都会执行我们定义好的advice，代理bean的配置如下:
``` 
 <!-- 代理 bean -->
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

#### advisor配置

advice 的拦截实现了类级别的拦截，而 advisor 则可以是方法级别的拦截，只拦截特定的方法
``` 
<bean id="logCreateAdvisor" class="org.springframework.aop.support.NameMatchMethodPointcutAdvisor">
    <!-- advisor 需要指定 advice 来做具体的拦截动作 ->
    <property name="advice" ref="logArgsAdvice" />
    <!-- 只匹配方法名 createUser -->
    <property name="mappedNames" value="createUser" />
</bean>

<bean id="userServiceProxy" class="org.springframework.aop.framework.ProxyFactoryBean">
    <!--代理的接口-->
    <property name="proxyInterfaces">
      <list>
        <value>aop.service.UserService</value>
      </list>
    </property>
    
    <!--代理的具体实现-->
    <property name="target" ref="userServiceImpl"/>
    
    <!--配置拦截器，这里可以配置 advice、advisor、interceptor-->
    <property name="interceptorNames">
      <list>
        <value>logCreateAdvisor</value>
      </list>
    </property>
</bean>
```
 Advisor 内部需要指定一个 Advice，Advisor 决定该拦截哪些方法，拦截后需要完成的工作还是内部的 Advice 来做。
 
 Advisor 有好几个实现类，上面使用实现类 `NameMatchMethodPointcutAdvisor` 来演示，从名字上就可以看出来，它需要我们给它提供方法名字，这样符合该配置的方法才会做拦截。

#### AutoProxy

在上面的 advice 配置当中， 我们配置出一个代理的bean使用的是 `ProxyFactoryBean`， 这样需要对每一个需要代理的 bean 都配置一个代理 bean。 

Spring 提供了自动代理，当 Spring 发现一个 bean 需要被切面织入的时候，Spring 会自动生成这个 bean 的一个代理来拦截方法的执行，确保定义的切面能被执行。

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
    <!-- 可以通过正则匹配 beanName -->
    <property name="beanNames" value="*ServiceImpl" />
  </bean>
```

在使用的时候，不在需要根据代理找bean(不需要配置需要代理的 interface )：
``` 
UserService userService = (UserService) context.getBean(UserService.class);
OrderService orderService = (OrderService) context.getBean(OrderService.class);
```

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
    <property name="pattern" value="com.haobin.*.service.*.create.*" />
</bean>
```
之后，我们需要配置 DefaultAdvisorAutoProxyCreator，它会使得所有的 Advisor 自动生效，无须其他配置。
``` 
 <bean class="org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator" />
```

### @AspectJ配置

@AspectJ 和 AspectJ 没多大关系，并不是说基于 AspectJ 实现的，而仅仅是使用了 AspectJ 中的概念，包括使用的注解也是直接来自于 AspectJ 的包。

#### 开启@AspectJ

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
一旦开启了上面的配置，那么所有使用`@Aspect` 注解的 bean 都会被 Spring 当做用来实现 AOP 的配置类，我们称之为一个 `Aspect`

#### 配置 pointcut 表达式

定义切点，用于定义哪些方法需要被增强或者说需要被拦截, 这里介绍几个常用的

- execution：一般用于指定方法的执行

``` 
execution(modifiers-pattern? ret-type-pattern declaring-type-pattern? 
	name-pattern(param-pattern) throws-pattern?)
	
- modifiers-pattern 表示方法的访问类型, 如 public
- ret-type-pattern 表示方法的返回值类型
- declaring-type-pattern 表示方法的声明类
- name-pattern 表示方法的名称
- throws-parttern 可以省略
- * 表示所有
- .. 表示包以及子包


eg:
1. “execution(* add())”匹配所有的不带参数的add()方法。
2. “execution(public * com.elim..*.add*(..))”匹配所有com.elim包及其子包下所有类的以add开头的所有public方法。
3. “execution(* *(..) throws Exception)”匹配所有抛出Exception的方法
```

- within：指定某些类型的全部方法执行，也可用来指定一个包
``` 
eg:
1. “within(com.elim.spring.aop.service.UserServiceImpl)” 匹配UserServiceImpl类对应对象的所有方法外部调用，而且这个对象只能是UserServiceImpl类型，不能是其子类型。
2. “within(com.elim..*)” 匹配com.elim包及其子包下面所有的类的所有方法的外部调用。
```

- @within：@within用于匹配被代理的目标对象对应的类型或其父类型拥有指定的注解的情况，但只有在调用拥有指定注解的类上的方法时才匹配(既只有加了该注解的类才会被代理)
``` 
eg：
“@within(com.elim.spring.support.MyAnnotation)” 匹配被调用的方法声明的类上拥有 MyAnnotation 才匹配该 pointcut
```

- @annotation：当执行的方法上拥有指定的注解时生效。
```
@annotation(com.elim.spring.support.MyAnnotation)”匹配所有的方法上拥有MyAnnotation注解的方法外部调用 
```

#### 定义advice

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
- [pointcut 介绍](https://www.iteye.com/blog/elim-2395255)