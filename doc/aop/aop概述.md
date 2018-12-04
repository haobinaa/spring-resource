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

### 参考资料
- [Spring aop 前世今生](https://javadoop.com/post/spring-aop-intro)
- [深入分析java web技术内幕]
- [spring 技术内幕]