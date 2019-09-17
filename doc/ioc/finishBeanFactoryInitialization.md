### finishBeanFactoryInitialization

finishBeanFactoryInitialization(beanFactory)这里会负责初始化所有的 singleton beans。

到这一步为止，应该说 BeanFactory 已经创建完成，并且所有的实现了 BeanFactoryPostProcessor 接口的 Bean 都已经初始化并且其中的 postProcessBeanFactory(factory) 方法已经得到回调执行了。而且 Spring 已经“手动”注册了一些特殊的 Bean，如 ‘environment’、‘systemProperties’ 等。

剩下的就是初始化 singleton beans 了，我们知道它们是单例的，如果没有设置懒加载，那么 Spring 会在接下来初始化所有的 singleton beans。


``` 
// 初始化剩余的 singleton beans
protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {

   // 首先，初始化名字为 conversionService 的 Bean。ConversionService主要用来做不同Class类型转换
   // 初始化的动作封装在getBean()里面，下文会有讲解
   if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME) &&
         beanFactory.isTypeMatch(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class)) {
      beanFactory.setConversionService(
            beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
   }

   // Register a default embedded value resolver if no bean post-processor
   // (such as a PropertyPlaceholderConfigurer bean) registered any before:
   // at this point, primarily for resolution in annotation attribute values.
   if (!beanFactory.hasEmbeddedValueResolver()) {
      beanFactory.addEmbeddedValueResolver(new StringValueResolver() {
         @Override
         public String resolveStringValue(String strVal) {
            return getEnvironment().resolvePlaceholders(strVal);
         }
      });
   }

   // 先初始化 LoadTimeWeaverAware 类型的 Bean
   // 之前也说过，这是 AspectJ 相关的内容，放心跳过吧
   String[] weaverAwareNames = beanFactory.getBeanNamesForType(LoadTimeWeaverAware.class, false, false);
   for (String weaverAwareName : weaverAwareNames) {
      getBean(weaverAwareName);
   }

   // Stop using the temporary ClassLoader for type matching.
   beanFactory.setTempClassLoader(null);
   
   // 停止bean的配置
   // 没什么别的目的，因为到这一步的时候，Spring 已经开始预初始化 singleton beans 了，
   // 肯定不希望这个时候还出现 bean 定义解析、加载、注册。
   beanFactory.freezeConfiguration();

   // 开始初始化
   beanFactory.preInstantiateSingletons();
}
```

#### preInstantiateSingletons
初始化bean注册操作是由DefaultListableBeanFactory实现：
``` 
// DefaultListableBeanFactory 728
@Override
public void preInstantiateSingletons() throws BeansException {
   if (this.logger.isDebugEnabled()) {
      this.logger.debug("Pre-instantiating singletons in " + this);
   }
   // this.beanDefinitionNames 保存了所有的 beanNames
   List<String> beanNames = new ArrayList<String>(this.beanDefinitionNames);

   // 触发所有的非懒加载的 singleton beans 的初始化操作
   for (String beanName : beanNames) {

      // 合并父 Bean 中的配置，注意 <bean id="" class="" parent="" /> 中的 parent，用的不多吧，
      // 涉及到bean的继承
      RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);

      // 非抽象、非懒加载的 singletons。如果配置了 'abstract = true'，那是不需要初始化的
      if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
         // 处理 FactoryBean
         if (isFactoryBean(beanName)) {
            // FactoryBean 的话，在 beanName 前面加上 ‘&’ 符号。再调用 getBean，getBean 方法别急
            final FactoryBean<?> factory = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
            // 判断当前 FactoryBean 是否是 SmartFactoryBean 的实现，此处忽略，直接跳过
            boolean isEagerInit;
            if (System.getSecurityManager() != null && factory instanceof SmartFactoryBean) {
               isEagerInit = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                  @Override
                  public Boolean run() {
                     return ((SmartFactoryBean<?>) factory).isEagerInit();
                  }
               }, getAccessControlContext());
            }
            else {
               isEagerInit = (factory instanceof SmartFactoryBean &&
                     ((SmartFactoryBean<?>) factory).isEagerInit());
            }
            if (isEagerInit) {

               getBean(beanName);
            }
         }
         else {
            // 对于普通的 Bean，只要调用 getBean(beanName) 这个方法就可以进行初始化了
            getBean(beanName);
         }
      }
   }


   // 到这里说明所有的非懒加载的 singleton beans 已经完成了初始化
   // 如果我们定义的 bean 是实现了 SmartInitializingSingleton 接口的，那么在这里得到回调，忽略
   for (String beanName : beanNames) {
      Object singletonInstance = getSingleton(beanName);
      if (singletonInstance instanceof SmartInitializingSingleton) {
         final SmartInitializingSingleton smartSingleton = (SmartInitializingSingleton) singletonInstance;
         if (System.getSecurityManager() != null) {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
               @Override
               public Object run() {
                  smartSingleton.afterSingletonsInstantiated();
                  return null;
               }
            }, getAccessControlContext());
         }
         else {
            smartSingleton.afterSingletonsInstantiated();
         }
      }
   }
}
```

#### getBean
普通bean的真正初始化的地方

``` 
@Override
public Object getBean(String name) throws BeansException {
   return doGetBean(name, null, null, false);
}

// 已经初始化过了就从容器中直接返回，否则就先初始化再返回
@SuppressWarnings("unchecked")
protected <T> T doGetBean(
      final String name, final Class<T> requiredType, final Object[] args, boolean typeCheckOnly)
      throws BeansException {
   // 获取一个 “正统的” beanName，处理两种情况，一个是前面说的 FactoryBean(前面带 ‘&’)，
   // 一个是别名问题，因为这个方法是 getBean，获取 Bean 用的，你要是传一个别名进来，是完全可以的
   final String beanName = transformedBeanName(name);

   // 注意跟着这个，这个是返回值
   Object bean; 

   // 检查下是不是已经创建过了
   Object sharedInstance = getSingleton(beanName);

   // 前面我们一路进来的时候都是 getBean(beanName)，
   // 所以 args 传参其实是 null 的，但是如果 args 不为空的时候，那么意味着调用方不是希望获取 Bean，而是创建 Bean
   if (sharedInstance != null && args == null) {
      if (logger.isDebugEnabled()) {
         if (isSingletonCurrentlyInCreation(beanName)) {
            logger.debug("...");
         }
         else {
            logger.debug("Returning cached instance of singleton bean '" + beanName + "'");
         }
      }
      // 下面这个方法：如果是普通 Bean 的话，直接返回 sharedInstance，
      // 如果是 FactoryBean 的话，返回它创建的那个实例对象
      // (FactoryBean 知识，读者若不清楚请移步附录)
      bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
   }

   else {
      if (isPrototypeCurrentlyInCreation(beanName)) {
         // 创建过了此 beanName 的 prototype 类型的 bean，那么抛异常，
         // 往往是因为陷入了循环引用
         throw new BeanCurrentlyInCreationException(beanName);
      }

      // 检查一下这个 BeanDefinition 在容器中是否存在
      BeanFactory parentBeanFactory = getParentBeanFactory();
      if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
         // 如果当前容器不存在这个 BeanDefinition，试试父容器中有没有
         String nameToLookup = originalBeanName(name);
         if (args != null) {
            // 返回父容器的查询结果
            return (T) parentBeanFactory.getBean(nameToLookup, args);
         }
         else {
            // No args -> delegate to standard getBean method.
            return parentBeanFactory.getBean(nameToLookup, requiredType);
         }
      }

      if (!typeCheckOnly) {
         // typeCheckOnly 为 false，将当前 beanName 放入一个 alreadyCreated 的 Set 集合中。
         markBeanAsCreated(beanName);
      }

      /*
       * 稍稍总结一下：
       * 到这里的话，要准备创建 Bean 了，对于 singleton 的 Bean 来说，容器中还没创建过此 Bean；
       * 对于 prototype 的 Bean 来说，本来就是要创建一个新的 Bean。
       */
      try {
         final RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
         checkMergedBeanDefinition(mbd, beanName, args);

         // 先初始化依赖的所有 Bean，这个很好理解。
         // 注意，这里的依赖指的是 depends-on 中定义的依赖（depends-on标签对应的bean）
         // depends-on 一般来指定 Bean初始化和销毁时的顺序
         String[] dependsOn = mbd.getDependsOn();
         if (dependsOn != null) {
            for (String dep : dependsOn) {
               // 检查是不是有循环依赖，这里的循环依赖是depends-on之间的循环依赖，这里肯定是不允许出现的
               if (isDependent(beanName, dep)) {
                  throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                        "Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
               }
               // 将depend-on属性值注册到dependentBeanMap
               registerDependentBean(dep, beanName);
               // 先初始化被依赖项
               getBean(dep);
            }
         }

         // 如果是 singleton scope 的，创建 singleton 的实例
         if (mbd.isSingleton()) {
            sharedInstance = getSingleton(beanName, new ObjectFactory<Object>() {
               @Override
               public Object getObject() throws BeansException {
                  try {
                     // 执行创建 Bean，详情后面再说
                     return createBean(beanName, mbd, args);
                  }
                  catch (BeansException ex) {
                     destroySingleton(beanName);
                     throw ex;
                  }
               }
            });
            bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
         }

         // 如果是 prototype scope 的，创建 prototype 的实例
         else if (mbd.isPrototype()) {
            // It's a prototype -> create a new instance.
            Object prototypeInstance = null;
            try {
               beforePrototypeCreation(beanName);
               // 执行创建 Bean
               prototypeInstance = createBean(beanName, mbd, args);
            }
            finally {
               afterPrototypeCreation(beanName);
            }
            bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
         }

         // 如果不是 singleton 和 prototype 的话，需要委托给相应的实现类来处理
         else {
            String scopeName = mbd.getScope();
            final Scope scope = this.scopes.get(scopeName);
            if (scope == null) {
               throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
            }
            try {
               Object scopedInstance = scope.get(beanName, new ObjectFactory<Object>() {
                  @Override
                  public Object getObject() throws BeansException {
                     beforePrototypeCreation(beanName);
                     try {
                        // 执行创建 Bean
                        return createBean(beanName, mbd, args);
                     }
                     finally {
                        afterPrototypeCreation(beanName);
                     }
                  }
               });
               bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
            }
            catch (IllegalStateException ex) {
               throw new BeanCreationException(beanName,
                     "Scope '" + scopeName + "' is not active for the current thread; consider " +
                     "defining a scoped proxy for this bean if you intend to refer to it from a singleton",
                     ex);
            }
         }
      }
      catch (BeansException ex) {
         cleanupAfterBeanCreationFailure(beanName);
         throw ex;
      }
   }

   // 最后，检查一下类型对不对，不对的话就抛异常，对的话就返回了
   if (requiredType != null && bean != null && !requiredType.isInstance(bean)) {
      try {
         return getTypeConverter().convertIfNecessary(bean, requiredType);
      }
      catch (TypeMismatchException ex) {
         if (logger.isDebugEnabled()) {
            logger.debug("Failed to convert bean '" + name + "' to required type '" +
                  ClassUtils.getQualifiedName(requiredType) + "'", ex);
         }
         throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
      }
   }
   return (T) bean;
}
```



进入创建bean操作：`createBean(String beanName, RootBeanDefinition mbd, Object[] args) throws BeanCreationException;`（第三个参数 args 数组代表创建实例需要的参数，不就是给构造方法用的参数，或者是工厂 Bean 的参数）

``` 
protected Object createBean(String beanName, RootBeanDefinition mbd, Object[] args) throws BeanCreationException {
   if (logger.isDebugEnabled()) {
      logger.debug("Creating instance of bean '" + beanName + "'");
   }
   RootBeanDefinition mbdToUse = mbd;

   // 确保 BeanDefinition 中的 Class 被加载
   Class<?> resolvedClass = resolveBeanClass(mbd, beanName);
   if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
      mbdToUse = new RootBeanDefinition(mbd);
      mbdToUse.setBeanClass(resolvedClass);
   }

   // 准备方法覆写，这里又涉及到一个概念：MethodOverrides，它来自于 bean 定义中的 <lookup-method /> 
   // 和 <replaced-method />，如果读者感兴趣，回到 bean 解析的地方看看对这两个标签的解析。
   // 我在附录中也对这两个标签的相关知识点进行了介绍，读者可以移步去看看
   try {
      mbdToUse.prepareMethodOverrides();
   }
   catch (BeanDefinitionValidationException ex) {
      throw new BeanDefinitionStoreException(mbdToUse.getResourceDescription(),
            beanName, "Validation of method overrides failed", ex);
   }

   try {
      // 让 InstantiationAwareBeanPostProcessor 在这一步有机会返回代理，
      // 在 《Spring AOP 源码分析》那篇文章中有解释，这里先跳过
      Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
      if (bean != null) {
         return bean; 
      }
   }
   catch (Throwable ex) {
      throw new BeanCreationException(mbdToUse.getResourceDescription(), beanName,
            "BeanPostProcessor before instantiation of bean failed", ex);
   }
   // 重头戏，创建 bean
   Object beanInstance = doCreateBean(beanName, mbdToUse, args);
   if (logger.isDebugEnabled()) {
      logger.debug("Finished creating instance of bean '" + beanName + "'");
   }
   return beanInstance;
}
```

#### 创建Bean
``` 
protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final Object[] args)
      throws BeanCreationException {

   // Instantiate the bean.
   BeanWrapper instanceWrapper = null;
   if (mbd.isSingleton()) {
      instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
   }
   if (instanceWrapper == null) {
      // 说明不是 FactoryBean，这里实例化 Bean，这里非常关键，细节之后再说
      instanceWrapper = createBeanInstance(beanName, mbd, args);
   }
   // 这个就是 Bean 里面的 我们定义的类 的实例，很多地方我直接描述成 "bean 实例"
   final Object bean = (instanceWrapper != null ? instanceWrapper.getWrappedInstance() : null);
   // 类型
   Class<?> beanType = (instanceWrapper != null ? instanceWrapper.getWrappedClass() : null);
   mbd.resolvedTargetType = beanType;

   // 建议跳过吧，涉及接口：MergedBeanDefinitionPostProcessor
   synchronized (mbd.postProcessingLock) {
      if (!mbd.postProcessed) {
         try {
            // MergedBeanDefinitionPostProcessor，这个我真不展开说了，直接跳过吧，很少用的
            applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
         }
         catch (Throwable ex) {
            throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                  "Post-processing of merged bean definition failed", ex);
         }
         mbd.postProcessed = true;
      }
   }

   // Eagerly cache singletons to be able to resolve circular references
   // even when triggered by lifecycle interfaces like BeanFactoryAware.
   // 下面这块代码是为了解决循环依赖的问题，以后有时间，我再对循环依赖这个问题进行解析吧
   boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
         isSingletonCurrentlyInCreation(beanName));
   if (earlySingletonExposure) {
      if (logger.isDebugEnabled()) {
         logger.debug("Eagerly caching bean '" + beanName +
               "' to allow for resolving potential circular references");
      }
      addSingletonFactory(beanName, new ObjectFactory<Object>() {
         @Override
         public Object getObject() throws BeansException {
            return getEarlyBeanReference(beanName, mbd, bean);
         }
      });
   }

   // Initialize the bean instance.
   Object exposedObject = bean;
   try {
      // 这一步也是非常关键的，这一步负责属性装配，因为前面的实例只是实例化了，并没有设值，这里就是设值
      populateBean(beanName, mbd, instanceWrapper);
      if (exposedObject != null) {
         // 还记得 init-method 吗？还有 InitializingBean 接口？还有 BeanPostProcessor 接口？
         // 这里就是处理 bean 初始化完成后的各种回调
         exposedObject = initializeBean(beanName, exposedObject, mbd);
      }
   }
   catch (Throwable ex) {
      if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
         throw (BeanCreationException) ex;
      }
      else {
         throw new BeanCreationException(
               mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
      }
   }

   if (earlySingletonExposure) {
      // 
      Object earlySingletonReference = getSingleton(beanName, false);
      if (earlySingletonReference != null) {
         if (exposedObject == bean) {
            exposedObject = earlySingletonReference;
         }
         else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
            String[] dependentBeans = getDependentBeans(beanName);
            Set<String> actualDependentBeans = new LinkedHashSet<String>(dependentBeans.length);
            for (String dependentBean : dependentBeans) {
               if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
                  actualDependentBeans.add(dependentBean);
               }
            }
            if (!actualDependentBeans.isEmpty()) {
               throw new BeanCurrentlyInCreationException(beanName,
                     "Bean with name '" + beanName + "' has been injected into other beans [" +
                     StringUtils.collectionToCommaDelimitedString(actualDependentBeans) +
                     "] in its raw version as part of a circular reference, but has eventually been " +
                     "wrapped. This means that said other beans do not use the final version of the " +
                     "bean. This is often the result of over-eager type matching - consider using " +
                     "'getBeanNamesOfType' with the 'allowEagerInit' flag turned off, for example.");
            }
         }
      }
   }

   // Register bean as disposable.
   try {
      registerDisposableBeanIfNecessary(beanName, bean, mbd);
   }
   catch (BeanDefinitionValidationException ex) {
      throw new BeanCreationException(
            mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
   }

   return exposedObject;
}
```

 doCreateBean 中的三个细节:
 1. 创建 Bean 实例的 createBeanInstance 方法，
 2. 依赖注入的 populateBean 方法，
 3. 回调方法 initializeBean。
 
 #### createBeanInstance 创建 Bean 实例
 
 此方法的目的就是实例化我们指定的类。
 
 ``` 
 protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, Object[] args) {
    // 确保已经加载了此 class
    Class<?> beanClass = resolveBeanClass(mbd, beanName);
 
    // 校验一下这个类的访问权限
    if (beanClass != null && !Modifier.isPublic(beanClass.getModifiers()) && !mbd.isNonPublicAccessAllowed()) {
       throw new BeanCreationException(mbd.getResourceDescription(), beanName,
             "Bean class isn't public, and non-public access not allowed: " + beanClass.getName());
    }
 
    if (mbd.getFactoryMethodName() != null)  {
       // 采用工厂方法实例化，不熟悉这个概念的读者请看附录，注意，不是 FactoryBean
       return instantiateUsingFactoryMethod(beanName, mbd, args);
    }
 
    // 如果不是第一次创建，比如第二次创建 prototype bean。
    // 这种情况下，我们可以从第一次创建知道，采用无参构造函数，还是构造函数依赖注入 来完成实例化
    boolean resolved = false;
    boolean autowireNecessary = false;
    if (args == null) {
       synchronized (mbd.constructorArgumentLock) {
          if (mbd.resolvedConstructorOrFactoryMethod != null) {
             resolved = true;
             autowireNecessary = mbd.constructorArgumentsResolved;
          }
       }
    }
    if (resolved) {
       if (autowireNecessary) {
          // 构造函数依赖注入
          return autowireConstructor(beanName, mbd, null, null);
       }
       else {
          // 无参构造函数
          return instantiateBean(beanName, mbd);
       }
    }
 
    // 判断是否采用有参构造函数
    Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
    if (ctors != null ||
          mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_CONSTRUCTOR ||
          mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args))  {
       // 构造函数依赖注入
       return autowireConstructor(beanName, mbd, ctors, args);
    }
 
    // 调用无参构造函数
    return instantiateBean(beanName, mbd);
 }
 ```
 
 #### populateBean处理bean 属性注入
 该方法负责进行属性设值，处理依赖。
 ``` 
 // AbstractAutowireCapableBeanFactory 1203
 protected void populateBean(String beanName, RootBeanDefinition mbd, BeanWrapper bw) {
    // bean 实例的所有属性都在这里了
    PropertyValues pvs = mbd.getPropertyValues();
 
    if (bw == null) {
       if (!pvs.isEmpty()) {
          throw new BeanCreationException(
                mbd.getResourceDescription(), beanName, "Cannot apply property values to null instance");
       }
       else {
          // Skip property population phase for null instance.
          return;
       }
    }
 
    // 到这步的时候，bean 实例化完成（通过工厂方法或构造方法），但是还没开始属性设值，
    // InstantiationAwareBeanPostProcessor 的实现类可以在这里对 bean 进行状态修改，
    // 我也没找到有实际的使用，所以我们暂且忽略这块吧
    boolean continueWithPropertyPopulation = true;
    if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
       for (BeanPostProcessor bp : getBeanPostProcessors()) {
          if (bp instanceof InstantiationAwareBeanPostProcessor) {
             InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
             // 如果返回 false，代表不需要进行后续的属性设值，也不需要再经过其他的 BeanPostProcessor 的处理
             if (!ibp.postProcessAfterInstantiation(bw.getWrappedInstance(), beanName)) {
                continueWithPropertyPopulation = false;
                break;
             }
          }
       }
    }
 
    if (!continueWithPropertyPopulation) {
       return;
    }
 
    if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_NAME ||
          mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_TYPE) {
       MutablePropertyValues newPvs = new MutablePropertyValues(pvs);
 
       // 通过名字找到所有属性值，如果是 bean 依赖，先初始化依赖的 bean。记录依赖关系
       if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_NAME) {
          autowireByName(beanName, mbd, bw, newPvs);
       }
 
       // 通过类型装配。复杂一些
       if (mbd.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_TYPE) {
          autowireByType(beanName, mbd, bw, newPvs);
       }
 
       pvs = newPvs;
    }
 
    boolean hasInstAwareBpps = hasInstantiationAwareBeanPostProcessors();
    boolean needsDepCheck = (mbd.getDependencyCheck() != RootBeanDefinition.DEPENDENCY_CHECK_NONE);
 
    if (hasInstAwareBpps || needsDepCheck) {
       PropertyDescriptor[] filteredPds = filterPropertyDescriptorsForDependencyCheck(bw, mbd.allowCaching);
       if (hasInstAwareBpps) {
          for (BeanPostProcessor bp : getBeanPostProcessors()) {
             if (bp instanceof InstantiationAwareBeanPostProcessor) {
                InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) bp;
                // 这里有个非常有用的 BeanPostProcessor 进到这里: AutowiredAnnotationBeanPostProcessor
                // 对采用 @Autowired、@Value 注解的依赖进行设值，这里的内容也是非常丰富的，不过本文不会展开说了，感兴趣的读者请自行研究
                pvs = ibp.postProcessPropertyValues(pvs, filteredPds, bw.getWrappedInstance(), beanName);
                if (pvs == null) {
                   return;
                }
             }
          }
       }
       if (needsDepCheck) {
          checkDependencies(beanName, mbd, filteredPds, pvs);
       }
    }
    // 设置 bean 实例的属性值
    applyPropertyValues(beanName, mbd, bw, pvs);
 }
 ```
 
 #### initializeBean 处理各种回调
 BeanPostProcessor（BPP） 的两个回调都发生在这边
 ``` 
 protected Object initializeBean(final String beanName, final Object bean, RootBeanDefinition mbd) {
    if (System.getSecurityManager() != null) {
       AccessController.doPrivileged(new PrivilegedAction<Object>() {
          @Override
          public Object run() {
             invokeAwareMethods(beanName, bean);
             return null;
          }
       }, getAccessControlContext());
    }
    else {
       // 如果 bean 实现了 BeanNameAware、BeanClassLoaderAware 或 BeanFactoryAware 接口，回调
       invokeAwareMethods(beanName, bean);
    }
 
    Object wrappedBean = bean;
    if (mbd == null || !mbd.isSynthetic()) {
       // BeanPostProcessor 的 postProcessBeforeInitialization 回调
       wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
    }
 
    try {
       // 处理 bean 中定义的 init-method，
       // 或者如果 bean 实现了 InitializingBean 接口，调用 afterPropertiesSet() 方法
       invokeInitMethods(beanName, wrappedBean, mbd);
    }
    catch (Throwable ex) {
       throw new BeanCreationException(
             (mbd != null ? mbd.getResourceDescription() : null),
             beanName, "Invocation of init method failed", ex);
    }
 
    if (mbd == null || !mbd.isSynthetic()) {
       // BeanPostProcessor 的 postProcessAfterInitialization 回调
       wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
    }
    return wrappedBean;
 }
 ```
 
 #### 补充说明
 
 - [ConversionService](https://blog.csdn.net/u013485533/article/details/47296361): 用来做Class类型转换
 - [bean的继承](https://blog.csdn.net/u013468917/article/details/51888619): child bean会继承parent bean的所有配置， parent  bean一般被设置为abstract 来当模板
 - [spring循环依赖](https://blog.csdn.net/u010853261/article/details/77940767): 对bean的循环依赖的说明和解决方案