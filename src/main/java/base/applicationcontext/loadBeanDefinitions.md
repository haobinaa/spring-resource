## xml 解析过程 loadBeanDefinitions

运行流程：

refresh-> obtainFreshBeanFactory -> refreshBeanFactory ->  loadBeanDefinitions

方法：
``` 
@Override
protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException, IOException {
   // 给这个 BeanFactory 实例化一个 XmlBeanDefinitionReader
   XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);

   // Configure the bean definition reader with this context's
   // resource loading environment.
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

loadBeanDefinitions(beanDefinitionReader)实现过程：

// AbstractXmlApplicationContext.java 120
``` 
protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws BeansException, IOException {
   Resource[] configResources = getConfigResources();
   if (configResources != null) {
      // 往下看
      reader.loadBeanDefinitions(configResources);
   }
   String[] configLocations = getConfigLocations();
   if (configLocations != null) {
      // 2
      reader.loadBeanDefinitions(configLocations);
   }
}

// 上面虽然有两个分支，不过第二个分支很快通过解析路径转换为 Resource 以后也会进到这里
@Override
public int loadBeanDefinitions(Resource... resources) throws BeanDefinitionStoreException {
   Assert.notNull(resources, "Resource array must not be null");
   int counter = 0;
   // 注意这里是个 for 循环，也就是每个文件是一个 resource
   for (Resource resource : resources) {
      // 继续往下看
      counter += loadBeanDefinitions(resource);
   }
   // 最后返回 counter，表示总共加载了多少的 BeanDefinition
   return counter;
}

// XmlBeanDefinitionReader 303
@Override
public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
   return loadBeanDefinitions(new EncodedResource(resource));
}

// XmlBeanDefinitionReader 314
public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
   Assert.notNull(encodedResource, "EncodedResource must not be null");
   if (logger.isInfoEnabled()) {
      logger.info("Loading XML bean definitions from " + encodedResource.getResource());
   }
   // 用一个 ThreadLocal 来存放配置文件资源
   Set<EncodedResource> currentResources = this.resourcesCurrentlyBeingLoaded.get();
   if (currentResources == null) {
      currentResources = new HashSet<EncodedResource>(4);
      this.resourcesCurrentlyBeingLoaded.set(currentResources);
   }
   if (!currentResources.add(encodedResource)) {
      throw new BeanDefinitionStoreException(
            "Detected cyclic loading of " + encodedResource + " - check your import definitions!");
   }
   try {
      InputStream inputStream = encodedResource.getResource().getInputStream();
      try {
         InputSource inputSource = new InputSource(inputStream);
         if (encodedResource.getEncoding() != null) {
            inputSource.setEncoding(encodedResource.getEncoding());
         }
         // 核心部分是这里，往下面看
         return doLoadBeanDefinitions(inputSource, encodedResource.getResource());
      }
      finally {
         inputStream.close();
      }
   }
   catch (IOException ex) {
      throw new BeanDefinitionStoreException(
            "IOException parsing XML document from " + encodedResource.getResource(), ex);
   }
   finally {
      currentResources.remove(encodedResource);
      if (currentResources.isEmpty()) {
         this.resourcesCurrentlyBeingLoaded.remove();
      }
   }
}

// 还在这个文件中，第 388 行
protected int doLoadBeanDefinitions(InputSource inputSource, Resource resource)
      throws BeanDefinitionStoreException {
   try {
      // 这里就不看了，将 xml 文件转换为 Document 对象
      Document doc = doLoadDocument(inputSource, resource);
      // 继续
      return registerBeanDefinitions(doc, resource);
   }
   catch (...
}
// 还在这个文件中，第 505 行
// 返回值：返回从当前配置文件加载了多少数量的 Bean
public int registerBeanDefinitions(Document doc, Resource resource) throws BeanDefinitionStoreException {
   BeanDefinitionDocumentReader documentReader = createBeanDefinitionDocumentReader();
   int countBefore = getRegistry().getBeanDefinitionCount();
   // 这里
   documentReader.registerBeanDefinitions(doc, createReaderContext(resource));
   return getRegistry().getBeanDefinitionCount() - countBefore;
}
// DefaultBeanDefinitionDocumentReader 90
@Override
public void registerBeanDefinitions(Document doc, XmlReaderContext readerContext) {
   this.readerContext = readerContext;
   logger.debug("Loading bean definitions");
   Element root = doc.getDocumentElement();
   // 从 xml 根节点开始解析文件
   doRegisterBeanDefinitions(root);
}
```

经过漫长的链路，一个配置文件终于转换为一颗 DOM 树了，注意，这里指的是其中一个配置文件，不是所有的我们可以看到上面有个 for 循环的。

下面开始从根节点开始解析dom树：
``` 
// DefaultBeanDefinitionDocumentReader 116
protected void doRegisterBeanDefinitions(Element root) {
   // 我们看名字就知道，BeanDefinitionParserDelegate 必定是一个重要的类，它负责解析 Bean 定义，
   // 这里为什么要定义一个 parent? 看到后面就知道了，是递归问题，
   // 因为 <beans /> 内部是可以定义 <beans /> 的，所以这个方法的 root 其实不一定就是 xml 的根节点，也可以是嵌套在里面的 <beans /> 节点，从源码分析的角度，我们当做根节点就好了
   BeanDefinitionParserDelegate parent = this.delegate;
   this.delegate = createDelegate(getReaderContext(), root, parent);

   if (this.delegate.isDefaultNamespace(root)) {
      // 这块说的是根节点 <beans ... profile="dev" /> 中的 profile 是否是当前环境需要的，
      // 如果当前环境配置的 profile 不包含此 profile，那就直接 return 了，不对此 <beans /> 解析
      String profileSpec = root.getAttribute(PROFILE_ATTRIBUTE);
      if (StringUtils.hasText(profileSpec)) {
         String[] specifiedProfiles = StringUtils.tokenizeToStringArray(
               profileSpec, BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS);
         if (!getReaderContext().getEnvironment().acceptsProfiles(specifiedProfiles)) {
            if (logger.isInfoEnabled()) {
               logger.info("Skipped XML bean definition file due to specified profiles [" + profileSpec +
                     "] not matching: " + getReaderContext().getResource());
            }
            return;
         }
      }
   }

   preProcessXml(root); // 钩子
   // 下面解释
   parseBeanDefinitions(root, this.delegate);
   postProcessXml(root); // 钩子

   this.delegate = parent;
}
```
核心解析方法：parseBeanDefinitions(root, this.delegate) :
``` 
// default namespace 涉及到的就四个标签 <import />、<alias />、<bean /> 和 <beans />，
// 其他的属于 custom 的
protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
   if (delegate.isDefaultNamespace(root)) {
      NodeList nl = root.getChildNodes();
      for (int i = 0; i < nl.getLength(); i++) {
         Node node = nl.item(i);
         if (node instanceof Element) {
            Element ele = (Element) node;
            if (delegate.isDefaultNamespace(ele)) {
               // 解析 default namespace 下面的几个元素
               parseDefaultElement(ele, delegate);
            }
            else {
               // 解析其他 namespace 的元素
               delegate.parseCustomElement(ele);
            }
         }
      }
   }
   else {
      delegate.parseCustomElement(root);
   }
}
```

从上面的代码，我们可以看到，对于每个配置来说，分别进入到 parseDefaultElement(ele, delegate); 和 delegate.parseCustomElement(ele); 这两个分支了。

parseDefaultElement(ele, delegate) 代表解析的节点是 <import />、<alias />、<bean />、<beans /> 这几个。

parseCustomElement(ele)代表着解析其他标签（除了http://www.springframework.org/schema/beans之外的namespace标签）


解析default的过程：
``` 
private void parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) {
   if (delegate.nodeNameEquals(ele, IMPORT_ELEMENT)) {
      // 处理 <import /> 标签
      importBeanDefinitionResource(ele);
   }
   else if (delegate.nodeNameEquals(ele, ALIAS_ELEMENT)) {
      // 处理 <alias /> 标签定义
      // <alias name="fromName" alias="toName"/>
      processAliasRegistration(ele);
   }
   else if (delegate.nodeNameEquals(ele, BEAN_ELEMENT)) {
      // 处理 <bean /> 标签定义，这也算是我们的重点吧
      processBeanDefinition(ele, delegate);
   }
   else if (delegate.nodeNameEquals(ele, NESTED_BEANS_ELEMENT)) {
      // 如果碰到的是嵌套的 <beans /> 标签，需要递归
      doRegisterBeanDefinitions(ele);
   }
}
```

以处理`<bean>`标签为例：
``` 
protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
   // 将 <bean /> 节点中的信息提取出来，然后封装到一个 BeanDefinitionHolder 中，细节往下看
   BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);

   // 下面的几行先不要看，跳过先，跳过先，跳过先，后面会继续说的

   if (bdHolder != null) {
      bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
      try {
         // Register the final decorated instance.
         BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
      }
      catch (BeanDefinitionStoreException ex) {
         getReaderContext().error("Failed to register bean definition with name '" +
               bdHolder.getBeanName() + "'", ele, ex);
      }
      // Send registration event.
      getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
   }
}
```
先看一下，<bean/>标签中有那些属性：

| property |  解释 |
|------|-------|
|class | 	类的全限定名 |
|name	  | 可指定 id、name(用逗号、分号、空格分隔) |
|scope	|   作用域 |
|constructor  |  arguments	指定构造参数 |
|properties | 	设置属性的值  |
| autowiring  | mode	no(默认值)、byName、byType、 constructor |
| lazy-initialization mode  |	是否懒加载(如果被非懒加载的bean依赖了那么其实也就不能懒加载了) |
| initialization method	bean |  属性设置完成后，会调用这个方法 |
| destruction method |	bean 销毁后的回调方法 |

一个完整的bean的xml描述类似：
``` 
<bean id="exampleBean" name="name1, name2, name3" class="com.javadoop.ExampleBean"
      scope="singleton" lazy-init="true" init-method="init" destroy-method="cleanup">

    <!-- 可以用下面三种形式指定构造参数 -->
    <constructor-arg type="int" value="7500000"/>
    <constructor-arg name="years" value="7500000"/>
    <constructor-arg index="0" value="7500000"/>

    <!-- property 的几种情况 -->
    <property name="beanOne">
        <ref bean="anotherExampleBean"/>
    </property>
    <property name="beanTwo" ref="yetAnotherBean"/>
    <property name="integerProperty" value="1"/>
</bean>
```
 
 如何从上面的描述元素，转换成一个`BeanDefinitionHolder`：
 ``` 
 // BeanDefinitionParserDelegate 428
 public BeanDefinitionHolder parseBeanDefinitionElement(Element ele) {
     return parseBeanDefinitionElement(ele, null);
 }
 
 public BeanDefinitionHolder parseBeanDefinitionElement(Element ele, BeanDefinition containingBean) {
    String id = ele.getAttribute(ID_ATTRIBUTE);
    String nameAttr = ele.getAttribute(NAME_ATTRIBUTE);
 
    List<String> aliases = new ArrayList<String>();
 
    // 将 name 属性的定义按照 “逗号、分号、空格” 切分，形成一个 别名列表数组，
    // 当然，如果你不定义 name 属性的话，就是空的了
    // 我在附录中简单介绍了一下 id 和 name 的配置，大家可以看一眼，有个20秒就可以了
    if (StringUtils.hasLength(nameAttr)) {
       String[] nameArr = StringUtils.tokenizeToStringArray(nameAttr, MULTI_VALUE_ATTRIBUTE_DELIMITERS);
       aliases.addAll(Arrays.asList(nameArr));
    }
 
    String beanName = id;
    // 如果没有指定id, 那么用别名列表的第一个名字作为beanName
    if (!StringUtils.hasText(beanName) && !aliases.isEmpty()) {
       beanName = aliases.remove(0);
       if (logger.isDebugEnabled()) {
          logger.debug("No XML 'id' specified - using '" + beanName +
                "' as bean name and " + aliases + " as aliases");
       }
    }
 
    if (containingBean == null) {
       checkNameUniqueness(beanName, aliases, ele);
    }
 
    // 根据 <bean ...>...</bean> 中的配置创建 BeanDefinition，然后把配置中的信息都设置到实例中,
    // 细节后面细说，先知道下面这行结束后，一个 BeanDefinition 实例就出来了。
    AbstractBeanDefinition beanDefinition = parseBeanDefinitionElement(ele, beanName, containingBean);
 
    // 到这里，整个 <bean /> 标签就算解析结束了，一个 BeanDefinition 就形成了。
    if (beanDefinition != null) {
       // 如果都没有设置 id 和 name，那么此时的 beanName 就会为 null，进入下面这块代码产生
       // 如果读者不感兴趣的话，我觉得不需要关心这块代码，对本文源码分析来说，这些东西不重要
       if (!StringUtils.hasText(beanName)) {
          try {
             if (containingBean != null) {// 按照我们的思路，这里 containingBean 是 null 的
                beanName = BeanDefinitionReaderUtils.generateBeanName(
                      beanDefinition, this.readerContext.getRegistry(), true);
             }
             else {
                // 如果我们不定义 id 和 name，那么我们引言里的那个例子：
                //   1. beanName 为：com.javadoop.example.MessageServiceImpl#0
                //   2. beanClassName 为：com.javadoop.example.MessageServiceImpl
 
                beanName = this.readerContext.generateBeanName(beanDefinition);
 
                String beanClassName = beanDefinition.getBeanClassName();
                if (beanClassName != null &&
                      beanName.startsWith(beanClassName) && beanName.length() > beanClassName.length() &&
                      !this.readerContext.getRegistry().isBeanNameInUse(beanClassName)) {
                   // 把 beanClassName 设置为 Bean 的别名
                   aliases.add(beanClassName);
                }
             }
             if (logger.isDebugEnabled()) {
                logger.debug("Neither XML 'id' nor 'name' specified - " +
                      "using generated bean name [" + beanName + "]");
             }
          }
          catch (Exception ex) {
             error(ex.getMessage(), ele);
             return null;
          }
       }
       String[] aliasesArray = StringUtils.toStringArray(aliases);
       // 返回 BeanDefinitionHolder
       return new BeanDefinitionHolder(beanDefinition, beanName, aliasesArray);
    }
 
    return null;
 }
 ```
 到这里就创建BeanDefinitionHolder 实例， 重新回到解析<bean/>标签的入口：
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
           // log...用框架定义的 Bean 覆盖用户自定义的 Bean 
        }
        else if (!beanDefinition.equals(oldBeanDefinition)) {
           // log...用新的 Bean 覆盖旧的 Bean
        }
        else {
           // log...用同等的 Bean 覆盖旧的 Bean，这里指的是 equals 方法返回 true 的 Bean
        }
        // 覆盖
        this.beanDefinitionMap.put(beanName, beanDefinition);
     }
     else {
        // 判断是否已经有其他的 Bean 开始初始化了.
        // 注意，"注册Bean" 这个动作结束，Bean 依然还没有初始化，我们后面会有大篇幅说初始化过程，
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
           // 最正常的应该是进到这个分支。
  
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
 
 总结一下，到这里已经初始化了 Bean 容器，<bean /> 配置也相应的转换为了一个个 BeanDefinition，然后注册了各个 BeanDefinition 到注册中心，并且发送了注册事件。