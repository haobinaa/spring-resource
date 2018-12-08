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