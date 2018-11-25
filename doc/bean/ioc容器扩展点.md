## IOC容器的扩展点

Spring让Bean对象有一定的扩展性，可以让用户加入一些自定义的操作。

在构建BeanFactory的时候，有BeanFactoryPostProcessor

在构建Bean的时候，有BeanPostProcessor

在创建和销毁Bean的时候有InitializingBean（在BPP的调用栈附近）和DisposableBean

还有一个就是[FactoryBean](https://github.com/haobinaa/spring-resource/blob/master/doc/bean/FactoryBean.md)，这种特殊的Bean可以被用户更多的控制


### BeanFactoryPostProcessor

实现该接口，可以在spring的bean创建之前，修改bean的定义属性。也就是说，Spring允许BeanFactoryPostProcessor在容器实例化任何其它bean之前读取配置元数据，并可以根据需要进行修改，例如可以把bean的scope从singleton改为prototype，也可以把property的值给修改掉。可以同时配置多个BeanFactoryPostProcessor，并通过设置'order'属性来控制各个BeanFactoryPostProcessor的执行次序。


```
public interface BeanFactoryPostProcessor {
	void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException;
}
```


BeanFactoryPostProcessor是在spring容器加载了bean的定义文件之后，在bean实例化之前执行的。接口方法的入参是ConfigurrableListableBeanFactory
，使用该参数，可以获取到相关bean的定义信息，如：[例子](https://github.com/haobinaa/spring-resource/blob/master/src/main/java/base/beanfactorypostprocessor/BFPP.java)

#### Spring调用过程

在ioc加载过程`refresh()`中：
``` 
// Invoke factory processors registered as beans in the context.
 invokeBeanFactoryPostProcessors(beanFactory);
```
这里调用， 实现如下:
``` 
public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		//如果存在BeanDefinitionRegistryPostProcessor, 先处理
		// BeanDefinitionRegistryPostProcessor是对BeanFactoryPostProcessor的扩展
		// 在BeanFactoryPostProcessor启动之前检查注册bean的定义
		// 用于对BeanDefinition的修改
		Set<String> processedBeans = new HashSet<>();
		if (beanFactory instanceof BeanDefinitionRegistry) {
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
			List<BeanFactoryPostProcessor> regularPostProcessors = new LinkedList<>();
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new LinkedList<>();

			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					registryProcessors.add(registryProcessor);
				}
				else {
					regularPostProcessors.add(postProcessor);
				}
			}

			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement
			// PriorityOrdered, Ordered, and the rest.
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			registryProcessors.addAll(currentRegistryProcessors);
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			currentRegistryProcessors.clear();

			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			registryProcessors.addAll(currentRegistryProcessors);
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			currentRegistryProcessors.clear();

			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
			boolean reiterate = true;
			while (reiterate) {
				reiterate = false;
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				for (String ppName : postProcessorNames) {
					if (!processedBeans.contains(ppName)) {
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						processedBeans.add(ppName);
						reiterate = true;
					}
				}
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				registryProcessors.addAll(currentRegistryProcessors);
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
				currentRegistryProcessors.clear();
			}

			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}

		else {
			// Invoke factory processors registered with the context instance.
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
			}
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// 调用实现了PriorityOrdered接口的BeanFactoryPostProcessor
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// 调用实现了Ordered接口的BeanFactoryPostProcessor
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>();
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// 调用所有 BeanFactoryPostProcessors.
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>();
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
		beanFactory.clearMetadataCache();
	}
```

#### 总结
BeanFactoryPostProcessor提供了一个无比强大的针对BeanFactory实例的回调接口， 从设计初衷上而言，主要是用来修改 bean definitions，进而影响 bean 的实例化过程。

### BPP(BeanPostProcessor)

如果我们需要在Spring容器完成Bean的实例化、配置和其他的初始化前后添加一些自己的逻辑处理，我们就可以定义一个或者多个BeanPostProcessor接口的实现，然后注册到容器中。

，它是针对已经instantiated的 beans 进行的回调，也就是说是对实例化好以后的 beans 进行的回调

``` 
public interface BeanPostProcessor {

  //实例化、依赖注入完毕，在调用显示的初始化之前完成一些定制的初始化任务
	Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException;


  //实例化、依赖注入、初始化完毕时执行
	Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException;

}
```

[使用例子](https://github.com/haobinaa/spring-resource/blob/master/src/main/java/base/beanprocessor/App.java)


#### 注册BPP

在Ioc的加载过程`refresh`中的`registerBeanPostProcessors()`注册了所有的BPP， 过程如下:
```
	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {
    // // 从 bean definitions 找出实现了 BeanPostProcessor 接口的类，
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		 // Ordered, and the rest.
     // 下面的逻辑是将 BeanPostProcessors 根据实现了 PriorityOrdered，Ordered 接口和其它没有实现相应接口的实例进行分类；
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// Separate between BeanPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				priorityOrderedPostProcessors.add(pp);
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					internalPostProcessors.add(pp);
				}
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		 // 对 priorityOrderedPostProcessors 进行排序，并注册到 BeanPostProcessors 中
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

		// 然后注册实现了 Ordered 接口的 BeanPostProcessors
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>();
		for (String ppName : orderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			orderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);

		 // 现在，注册所有普通的 BeanPostProcessors
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>();
		for (String ppName : nonOrderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			nonOrderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		// 最后，注册所有的 internalPostProcessors
		sortPostProcessors(internalPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// Re-register post-processor for detecting inner beans as ApplicationListeners,
		// moving it to the end of the processor chain (for picking up proxies etc).
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}
```
这段代码。通过getBean实例化了所有的BeanPostProcessor, 然后根据BeanPostProcessor使用的不同分类， 设置了优先级依次注入bean的容器中：
1. 实现了 PriorityOrdered 的 bean-post-processors 最先被注册，对应分类 priorityOrderedPostProcessors
2. 实现了 Ordered 的 bean-post-processors 其次被注册，对应分类 orderedPostProcessorNames
3. 没有实现任何排序接口的普通的 bean-post-processors 再其次被注册，对应分类 nonOrderedPostProcessorNames
4. 最后注册 internal-bean-post-processors，对应分类 internalPostProcessors（interal-bean-post-processor 指的是实现了 MergedBeanDefinitionPostProcessor 接口的 bean-post-processor）

#### 调用BPP
上述是BPP的注册过程，BPP的具体调用逻辑是：
doCreateBean->initializeBean->invokeInitMethods

源码如下：
``` 
 AbstractAutowireCapableBeanFactory#initializeBean 
 
 protected Object initializeBean(final String beanName, final Object bean, RootBeanDefinition mbd) {
   
     invokeAwareMethods(beanName, bean); // 调用实现了 *Aware 接口的方法，比如注入 ApplicationContext... 
     
 
     Object wrappedBean = bean;
     if (mbd == null || !mbd.isSynthetic()) {
         // 调用 bean-post-processor 的 before initialization 回调方法
         wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName); 
     }
 
     try {
         invokeInitMethods(beanName, wrappedBean, mbd); // 调用 InitializingBean#afterPropertiesSet 回调 
     }
     catch (Throwable ex) {
         throw new BeanCreationException(
                 (mbd != null ? mbd.getResourceDescription() : null),
                 beanName, "Invocation of init method failed", ex);
     }
 
     if (mbd == null || !mbd.isSynthetic()) {
         // 调用 bean-post-processor 的 after initialization 回调方法
         wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName); 
     }
 
     return wrappedBean; // 这里的 wrapped bean 指的是被 bean-post-processor 修饰以后的包装 bean
 }
```
这里大概分为三步：
1. 注入 Aware 对象
2. 回调 bean-post-processors 接口方法
3. 回调 InitializingBean 接口方法

##### 注入Aware对象

##### 回调BPP接口方法
BPP两个接口分别对应:
- AbstractAutowireCapableBeanFactory.java#applyBeanPostProcessorsBeforeInitialization
- AbstractAutowireCapableBeanFactory.java#applyBeanPostProcessorsAfterInitialization 

在`initializeBean`中如下：
``` 
	protected Object initializeBean(final String beanName, final Object bean, @Nullable RootBeanDefinition mbd) {
		if (System.getSecurityManager() != null) {
			AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
				invokeAwareMethods(beanName, bean);
				return null;
			}, getAccessControlContext());
		}
		else {
			invokeAwareMethods(beanName, bean);
		}

		Object wrappedBean = bean;
		if (mbd == null || !mbd.isSynthetic()) {
			wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
		}

		try {
			invokeInitMethods(beanName, wrappedBean, mbd);
		}
		catch (Throwable ex) {
			throw new BeanCreationException(
					(mbd != null ? mbd.getResourceDescription() : null),
					beanName, "Invocation of init method failed", ex);
		}
		if (mbd == null || !mbd.isSynthetic()) {
			wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
		}

		return wrappedBean;
	}
```

applyBeanPostProcessorsBeforeInitialization：
``` 
	public Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName)
			throws BeansException {

		Object result = existingBean;
		for (BeanPostProcessor beanProcessor : getBeanPostProcessors()) {
			Object current = beanProcessor.postProcessBeforeInitialization(result, beanName);
			if (current == null) {
				return result;
			}
			result = current;
		}
		return result;
	}
```

这里可以看到如果有一个BPP返回null，剩下的就不执行了。我想到了我们项目中也有这种设计，在评级之前和评级之后都有类似的回调

##### 回调 InitializingBean 接口方法
该步骤对应的是`initializeBean`中的`invokeInitMethods`, 回调 InitializingBean 接口方法, 源码：
``` 
protected void invokeInitMethods(String beanName, final Object bean, RootBeanDefinition mbd)
        throws Throwable {

    boolean isInitializingBean = (bean instanceof InitializingBean);
    if (isInitializingBean && (mbd == null || !mbd.isExternallyManagedInitMethod("afterPropertiesSet"))) {
        if (logger.isDebugEnabled()) {
            logger.debug("Invoking afterPropertiesSet() on bean with name '" + beanName + "'");
        }
        .....
        ((InitializingBean) bean).afterPropertiesSet();
        .....
    }

    if (mbd != null) {
        String initMethodName = mbd.getInitMethodName();
        if (initMethodName != null && !(isInitializingBean && "afterPropertiesSet".equals(initMethodName)) &&
                !mbd.isExternallyManagedInitMethod(initMethodName)) {
            invokeCustomInitMethod(beanName, bean, mbd);
        }
    }
}
```

可以看到，主要回调的是InitializingBean接口的afterPropertiesSet方法，所以，我们可以让某个 bean 实现 InitializingBean 接口，并通过该接口实现一些当 bean 实例化好以后的回调方法，注意afterPropertiesSet并不返回任何值，所以，这里不是像 bean-post-processor 那样对 bean 起到修饰的作用，而是起到纯粹的调用作用；


### DisposableBean


### 参考资料
- [Spring的BeanFactoryPostProcessor和BeanPostProcessor](https://blog.csdn.net/caihaijiang/article/details/35552859)
- [Spring Core Container：BeanPostProcessor 和 BeanFactoryPostProcessor](https://www.shangyang.me/2017/04/02/spring-core-container-sourcecode-analysis-bean-and-bean-factory-post-processors/#BeanFactoryPostProcessor)
- [BeanPostProcessor回调](https://www.shangyang.me/2017/04/01/spring-core-container-sourcecode-analysis-beans-instantiating-process/#%E5%9B%9E%E8%B0%83-bean-post-processors-%E6%8E%A5%E5%8F%A3%E6%96%B9%E6%B3%95)