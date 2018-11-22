## obtainFreshBeanFactory

// AbstractApplicationContext.java 
``` 
protected ConfigurableListableBeanFactory obtainFreshBeanFactory() {
   // 关闭旧的 BeanFactory (如果有)，创建新的 BeanFactory，加载 Bean 定义、注册 Bean 等等
   refreshBeanFactory();

   // 返回刚刚创建的 BeanFactory
   ConfigurableListableBeanFactory beanFactory = getBeanFactory();
   if (logger.isDebugEnabled()) {
      logger.debug("Bean factory for " + getDisplayName() + ": " + beanFactory);
   }
   return beanFactory;
}
```

### refreshBeanFactory

// AbstractApplicationContext.java

``` 
@Override
protected final void refreshBeanFactory() throws BeansException {
   // 如果 ApplicationContext 中已经加载过 BeanFactory 了，销毁所有 Bean，关闭 BeanFactory
   if (hasBeanFactory()) {
      destroyBeans();
      closeBeanFactory();
   }
   try {
      // 初始化一个 DefaultListableBeanFactory， ApplicationContext的beanFacorty将引用它
      DefaultListableBeanFactory beanFactory = createBeanFactory();
      // 用于 BeanFactory 的序列化，大部分时候用不到
      beanFactory.setSerializationId(getId());
      // 设置 BeanFactory 的两个配置属性：是否允许 Bean 覆盖、是否允许循环引用
      customizeBeanFactory(beanFactory);
      // 加载 Bean 到 BeanFactory 中
      loadBeanDefinitions(beanFactory);
      synchronized (this.beanFactoryMonitor) {
         this.beanFactory = beanFactory;
      }
   }
   catch (IOException ex) {
      throw new ApplicationContextException("I/O error parsing bean definition source for " + getDisplayName(), ex);
   }
}
```

这里ApplicationContext持有的beanFactory引用是实例化的DefaultListableBeanFactory, 
从下图可以看到，DefaultListableBeanFactory基本上实现了所有beanFactory的功能， 所以选择使用这个类来实例化

![](https://javadoop.com/blogimages/spring-context/3.png)


引申一个知识点，如果我们需要动态的往容器里面注册新的bean， 就会使用到这个类。

如何在运行的时候获得DefaultListableBeanFactory 的实例呢。之前我们说过 ApplicationContext 接口能获取到 AutowireCapableBeanFactory，然后它向下转型就能得到 DefaultListableBeanFactory 了。

例如,在SpringBoot中动态的注册一个bean到容器中：
 ``` 
 ApplicationContext ctx =  (ApplicationContext) SpringApplication.run(App.class, args);
 // 获取如何在运行的时候获得DefaultListableBeanFactory
 DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) ctx.getAutowireCapableBeanFactory();  
 //创建bean信息.  
 BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(TestService.class);    
 eanDefinitionBuilder.addPropertyValue("name","张三");  
 //动态注册bean. 
 defaultListableBeanFactory.registerBeanDefinition("testService", beanDefinitionBuilder.getBeanDefinition());  
 //获取动态注册的bean.  
 TestService testService =ctx.getBean(TestService.class);、testService.print();
 ```
 
 #### loadBeanDefinitions
 这个方法将根据配置，加载各个 Bean，然后放到 BeanFactory 中。
 
 
 // AbstractXmlApplicationContext.java 80
 ``` 
 /** 我们可以看到，此方法将通过一个 XmlBeanDefinitionReader 实例来加载各个 Bean。*/
 @Override
 protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {
    // 给这个 BeanFactory 实例化一个 XmlBeanDefinitionReader
    XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
 
    // 设置beanDefinitionReader的上下文
    beanDefinitionReader.setEnvironment(this.getEnvironment());
    beanDefinitionReader.setResourceLoader(this);
    beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));
 
    // 初始化 BeanDefinitionReader，其实这个是提供给子类覆写的，
    // 我看了一下，没有类覆写这个方法，我们姑且当做不重要吧
    initBeanDefinitionReader(beanDefinitionReader);
    // 加载xml的配置
    loadBeanDefinitions(beanDefinitionReader);
 }
 ```
 
实例化了一个beanDefinitionReader来读取配置， BeanDefinition中保存了Bean的信息， Bean可以认为就是BeanDefinition的实现
 
 ##### 将xml中<bean>标签解析成BeanDefinition
接下来就用这个Reader来加载xml配置， 具体实现流程： [reader过程-解析BeanDefinition](https://github.com/haobinaa/spring-resource/blob/master/doc/ioc/loadBeanDefinitions.md)

会将配置文件解析成一个`BeanDefinitionHolder `实例， 接下来就是注册， 源码如下:
``` 
protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
   // 将 <bean /> 节点转换为 BeanDefinitionHolder，就是上面说的一堆
   BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
   if (bdHolder != null) {
      // 如果有自定义属性的话，进行相应的解析，先忽略
      bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
      try {
         // 我们把这步叫做 注册Bean 吧
         BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
      }
      catch (BeanDefinitionStoreException ex) {
         getReaderContext().error("Failed to register bean definition with name '" +
               bdHolder.getBeanName() + "'", ele, ex);
      }
      // 注册完成后，发送事件，本文不展开说这个
      getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
   }
}
```


##### 注册BeanDefinition

接下来的步骤是注册BeanDefinition， 然后将这个注册事件发送出去
 
 // BeanDefinitionReaderUtils.registerBeanDefinition
 ``` 
 public static void registerBeanDefinition(
       BeanDefinitionHolder definitionHolder, BeanDefinitionRegistry registry)
       throws BeanDefinitionStoreException {
 
    String beanName = definitionHolder.getBeanName();
    // 注册这个 Bean
    registry.registerBeanDefinition(beanName, definitionHolder.getBeanDefinition());
 
    // 如果还有别名的话，也要根据别名全部注册一遍，不然根据别名就会找不到 Bean 了
    String[] aliases = definitionHolder.getAliases();
    if (aliases != null) {
       for (String alias : aliases) {
          // alias -> beanName 保存它们的别名信息，这个很简单，用一个 map 保存一下就可以了，
          // 获取的时候，会先将 alias 转换为 beanName，然后再查找
          registry.registerAlias(beanName, alias);
       }
    }
 }
 ```
 这里的`registerBeanDefinition`方法由`DefaultListableBeanFactory `实现了`BeanDefinitionRegistry`接口来实现这个方法：
 ``` 
  DefaultListableBeanFactory 793
  @Override
  public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
        throws BeanDefinitionStoreException {
  
     Assert.hasText(beanName, "Bean name must not be empty");
     Assert.notNull(beanDefinition, "BeanDefinition must not be null");
  
     if (beanDefinition instanceof AbstractBeanDefinition) {
        try {
           ((AbstractBeanDefinition) beanDefinition).validate();
        }
        catch (BeanDefinitionValidationException ex) {
           throw new BeanDefinitionStoreException(...);
        }
     }
  
     // old? 还记得 “允许 bean 覆盖” 这个配置吗？allowBeanDefinitionOverriding
     BeanDefinition oldBeanDefinition;
  
     // 之后会看到，所有的 Bean 注册后会放入这个 beanDefinitionMap 中
     oldBeanDefinition = this.beanDefinitionMap.get(beanName);
  
     // 处理重复名称的 Bean 定义的情况
     if (oldBeanDefinition != null) {
        if (!isAllowBeanDefinitionOverriding()) {
           // 如果不允许覆盖的话，抛异常
           throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription()...
        }
        else if (oldBeanDefinition.getRole() < beanDefinition.getRole()) {
           // 用框架定义的 Bean 覆盖用户自定义的 Bean 
        }
        else if (!beanDefinition.equals(oldBeanDefinition)) {
           // .用新的 Bean 覆盖旧的 Bean
        }
        else {
           // 用同等的 Bean 覆盖旧的 Bean，这里指的是 equals 方法返回 true 的 Bean
        }
        // 覆盖
        this.beanDefinitionMap.put(beanName, beanDefinition);
     }
     else {
        // 判断是否已经有其他的 Bean 开始初始化了.
        // 注意，"注册Bean" 这个动作结束，Bean 依然还没有初始化
        // 在 Spring 容器启动的最后，会 预初始化 所有的 singleton beans
        if (hasBeanCreationStarted()) {
           // Cannot modify startup-time collection elements anymore (for stable iteration)
           synchronized (this.beanDefinitionMap) {
              this.beanDefinitionMap.put(beanName, beanDefinition);
              List<String> updatedDefinitions = new ArrayList<String>(this.beanDefinitionNames.size() + 1);
              updatedDefinitions.addAll(this.beanDefinitionNames);
              updatedDefinitions.add(beanName);
              this.beanDefinitionNames = updatedDefinitions;
              if (this.manualSingletonNames.contains(beanName)) {
                 Set<String> updatedSingletons = new LinkedHashSet<String>(this.manualSingletonNames);
                 updatedSingletons.remove(beanName);
                 this.manualSingletonNames = updatedSingletons;
              }
           }
        }
        else {
           // 最正常的应该是进到这个分支, 并没有其他bean开始初始化
  
           // 将 BeanDefinition 放到这个 map 中，这个 map 保存了所有的 BeanDefinition
           this.beanDefinitionMap.put(beanName, beanDefinition);
           // 这是个 ArrayList，所以会按照 bean 配置的顺序保存每一个注册的 Bean 的名字
           this.beanDefinitionNames.add(beanName);
           // 这是个 LinkedHashSet，代表的是手动注册的 singleton bean，
           // 注意这里是 remove 方法，到这里的 Bean 当然不是手动注册的
           // 手动指的是通过调用以下方法注册的 bean ：
           //     registerSingleton(String beanName, Object singletonObject)
           // 这不是重点，解释只是为了不让大家疑惑。Spring 会在后面"手动"注册一些 Bean，
           // 如 "environment"、"systemProperties" 等 bean，我们自己也可以在运行时注册 Bean 到容器中的
           this.manualSingletonNames.remove(beanName);
        }
        // 这个不重要，在预初始化的时候会用到，不必管它。
        this.frozenBeanDefinitionNames = null;
     }
  
     if (oldBeanDefinition != null || containsSingleton(beanName)) {
        resetBeanDefinition(beanName);
     }
  }
 ```
 
 总结一下，到这里已经初始化了 Bean 容器，<bean /> 配置也相应的转换为了一个个 BeanDefinition，放入了beanDefinitionMap(beanName->beanDefinition)