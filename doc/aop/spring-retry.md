### retry 的切入点

使用 spring-retry 会用到以下注解:
- @EnableRetry - 表示开启重试机制
- @Retryable - 表示这个方法需要重试，它有很丰富的参数，可以满足你对重试的需求
- @Backoff - 表示重试中的退避策略
- @Recover - 兜底方法，即多次重试后还是失败就会执行这个方法



#### EnableRetry 开启重试
``` 
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@EnableAspectJAutoProxy(proxyTargetClass = false)
@Import(RetryConfiguration.class)
@Documented
public @interface EnableRetry {

	/**
	 * Indicate whether subclass-based (CGLIB) proxies are to be created as opposed
	 * to standard Java interface-based proxies. The default is {@code false}.
	 *
	 * @return whether to proxy or not to proxy the class
	 */
	boolean proxyTargetClass() default false;

}
```
- @EnableAspectJAutoProxy(proxyTargetClass = false): 打开 aop 功能
- @Import(RetryConfiguration.class): 注册 RetryConfiguration 为一个bean

```
public class RetryConfiguration extends AbstractPointcutAdvisor
		implements IntroductionAdvisor, BeanFactoryAware, InitializingBean {
	private Advice advice;
	private Pointcut pointcut;
	.....
```
`RetryConfiguration` 继承于 `AbstractPointcutAdvisor`， 它有一个pointcut和一个advice。继承于`InitializingBean`, 初始化方法如下:
``` 
@Override
public void afterPropertiesSet() throws Exception {
    retryContextCache = findBean(RetryContextCache.class);
    methodArgumentsKeyGenerator = findBean(MethodArgumentsKeyGenerator.class);
    newMethodArgumentsIdentifier = findBean(NewMethodArgumentsIdentifier.class);
    retryListeners = findBeans(RetryListener.class);
    sleeper = findBean(Sleeper.class);
    Set<Class<? extends Annotation>> retryableAnnotationTypes = new LinkedHashSet<Class<? extends Annotation>>(1);
    retryableAnnotationTypes.add(Retryable.class);
    this.pointcut = buildPointcut(retryableAnnotationTypes);
    this.advice = buildAdvice();
    if (this.advice instanceof BeanFactoryAware) {
        ((BeanFactoryAware) this.advice).setBeanFactory(beanFactory);
    }
}

protected Pointcut buildPointcut(Set<Class<? extends Annotation>> retryAnnotationTypes) {
    ComposablePointcut result = null;
    for (Class<? extends Annotation> retryAnnotationType : retryAnnotationTypes) {
        // 切入点过滤
        Pointcut filter = new AnnotationClassOrMethodPointcut(retryAnnotationType);
        if (result == null) {
            result = new ComposablePointcut(filter);
        }
        else {
            result.union(filter);
        }
    }
    return result;
}


protected Advice buildAdvice() {
    // 创建 advice 拦截器
    AnnotationAwareRetryOperationsInterceptor interceptor = new AnnotationAwareRetryOperationsInterceptor();
    if (retryContextCache != null) {
        interceptor.setRetryContextCache(retryContextCache);
    }
    if (retryListeners != null) {
        interceptor.setListeners(retryListeners);
    }
    if (methodArgumentsKeyGenerator != null) {
        interceptor.setKeyGenerator(methodArgumentsKeyGenerator);
    }
    if (newMethodArgumentsIdentifier != null) {
        interceptor.setNewItemIdentifier(newMethodArgumentsIdentifier);
    }
    if (sleeper != null) {
        interceptor.setSleeper(sleeper);
    }
    return interceptor;
}

```

`AnnotationAwareRetryOperationsInterceptor` 中 `invoke` 方法:
``` 
public Object invoke(MethodInvocation invocation) throws Throwable {
    // 获取委托类
    MethodInterceptor delegate = getDelegate(invocation.getThis(), invocation.getMethod());
    if (delegate != null) {
        return delegate.invoke(invocation);
    }
    else {
        return invocation.proceed();
    }
}
// 根据配置返回 statefulInterceptor 或 statelessInterceptor
private MethodInterceptor getDelegate(Object target, Method method) {
    ConcurrentMap<Method, MethodInterceptor> cachedMethods = this.delegates.get(target);
    if (cachedMethods == null) {
        cachedMethods = new ConcurrentHashMap<Method, MethodInterceptor>();
    }
    MethodInterceptor delegate = cachedMethods.get(method);
    if (delegate == null) {
        MethodInterceptor interceptor = NULL_INTERCEPTOR;
        Retryable retryable = AnnotatedElementUtils.findMergedAnnotation(method, Retryable.class);
        if (retryable == null) {
            // 获取注解信息
            retryable = AnnotatedElementUtils.findMergedAnnotation(method.getDeclaringClass(), Retryable.class);
        }
        if (retryable == null) {
            retryable = findAnnotationOnTarget(target, method, Retryable.class);
        }
        if (retryable != null) {
            // 自定义 MethodInterceptor 优先级最高
            if (Strin
            
            gUtils.hasText(retryable.interceptor())) {
                interceptor = this.beanFactory.getBean(retryable.interceptor(), MethodInterceptor.class);
            }
            // 得到有状态的 interceptor
            else if (retryable.stateful()) {
                interceptor = getStatefulInterceptor(target, method, retryable);
            }
            // 得到无状态的 interceptor
            else {
                interceptor = getStatelessInterceptor(target, method, retryable);
            }
        }
        cachedMethods.putIfAbsent(method, interceptor);
        delegate = cachedMethods.get(method);
    }
    this.delegates.putIfAbsent(target, cachedMethods);
    return delegate == NULL_INTERCEPTOR ? null : delegate;
}
```

`getStatefulInterceptor`和`getStatelessInterceptor`都是差不多，比较简单的`getStatelessInterceptor`如下:
``` 
private MethodInterceptor getStatelessInterceptor(Object target, Method method, Retryable retryable) {
        // 创建 retryTemplate
		RetryTemplate template = createTemplate(retryable.listeners());
		// 设置 retryPolicy
		template.setRetryPolicy(getRetryPolicy(retryable));
		// 设置 backOffPolicy 
		template.setBackOffPolicy(getBackoffPolicy(retryable.backoff()));
		return RetryInterceptorBuilder.stateless().retryOperations(template).label(retryable.label())
				.recoverer(getRecoverer(target, method)).build();
	}
```

`RetryInterceptorBuilder`其实就是为了生成`RetryOperationsInterceptor`。
`RetryOperationsInterceptor`是一个 MethodInterceptor，我invoke方法如下:
``` 
public Object invoke(final MethodInvocation invocation) throws Throwable {
    String name;
    if (StringUtils.hasText(label)) {
        name = label;
    } else {
        name = invocation.getMethod().toGenericString();
    }
    final String label = name;

    //定义了一个RetryCallback，其实看它的doWithRetry方法，调用了invocation的proceed()方法，是不是有点眼熟，这就是AOP的拦截链调用，如果没有拦截链，那就是对原来方法的调用。
    RetryCallback<Object, Throwable> retryCallback = new RetryCallback<Object, Throwable>() {
        public Object doWithRetry(RetryContext context) throws Exception {
            context.setAttribute(RetryContext.NAME, label);
            if (invocation instanceof ProxyMethodInvocation) {
                try {
                    return ((ProxyMethodInvocation) invocation).invocableClone().proceed();
                }
                catch (Exception e) {
                    throw e;
                }
                catch (Error e) {
                    throw e;
                }
                catch (Throwable e) {
                    throw new IllegalStateException(e);
                }
            }
            else {
                throw new IllegalStateException(
                        "MethodInvocation of the wrong type detected - this should not happen with Spring AOP, " +
                                "so please raise an issue if you see this exception");
            }
        }

    };

    if (recoverer != null) {
        ItemRecovererCallback recoveryCallback = new ItemRecovererCallback(
                invocation.getArguments(), recoverer);
        return this.retryOperations.execute(retryCallback, recoveryCallback);
    }
    //最终还是进入到retryOperations的execute方法，这个retryOperations就是在之前的 builder set 进来的RetryTemplate。
    return this.retryOperations.execute(retryCallback);
}
```

`RetryTemplate` 的 doExecute 逻辑:
``` 
protected <T, E extends Throwable> T doExecute(RetryCallback<T, E> retryCallback,
			RecoveryCallback<T> recoveryCallback, RetryState state)
			throws E, ExhaustedRetryException {

		RetryPolicy retryPolicy = this.retryPolicy;
		BackOffPolicy backOffPolicy = this.backOffPolicy;

		//新建一个RetryContext来保存本轮重试的上下文
		RetryContext context = open(retryPolicy, state);
		if (this.logger.isTraceEnabled()) {
			this.logger.trace("RetryContext retrieved: " + context);
		}

		// Make sure the context is available globally for clients who need
		// it...
		RetrySynchronizationManager.register(context);

		Throwable lastException = null;

		boolean exhausted = false;
		try {

			//如果有注册RetryListener，则会调用它的open方法，给调用者一个通知。
			boolean running = doOpenInterceptors(retryCallback, context);

			if (!running) {
				throw new TerminatedRetryException(
						"Retry terminated abnormally by interceptor before first attempt");
			}

			// Get or Start the backoff context...
			BackOffContext backOffContext = null;
			Object resource = context.getAttribute("backOffContext");

			if (resource instanceof BackOffContext) {
				backOffContext = (BackOffContext) resource;
			}

			if (backOffContext == null) {
				backOffContext = backOffPolicy.start(context);
				if (backOffContext != null) {
					context.setAttribute("backOffContext", backOffContext);
				}
			}

			//判断能否重试，就是调用RetryPolicy的canRetry方法来判断。
			//这个循环会直到原方法不抛出异常，或不需要再重试
			while (canRetry(retryPolicy, context) && !context.isExhaustedOnly()) {

				try {
					if (this.logger.isDebugEnabled()) {
						this.logger.debug("Retry: count=" + context.getRetryCount());
					}
					//清除上次记录的异常
					lastException = null;
					//doWithRetry方法，一般来说就是原方法
					return retryCallback.doWithRetry(context);
				}
				catch (Throwable e) {
					//原方法抛出了异常
					lastException = e;

					try {
						//记录异常信息
						registerThrowable(retryPolicy, state, context, e);
					}
					catch (Exception ex) {
						throw new TerminatedRetryException("Could not register throwable",
								ex);
					}
					finally {
						//调用RetryListener的onError方法
						doOnErrorInterceptors(retryCallback, context, e);
					}
					//再次判断能否重试
					if (canRetry(retryPolicy, context) && !context.isExhaustedOnly()) {
						try {
							//如果可以重试则走退避策略
							backOffPolicy.backOff(backOffContext);
						}
						catch (BackOffInterruptedException ex) {
							lastException = e;
							// back off was prevented by another thread - fail the retry
							if (this.logger.isDebugEnabled()) {
								this.logger
										.debug("Abort retry because interrupted: count="
												+ context.getRetryCount());
							}
							throw ex;
						}
					}

					if (this.logger.isDebugEnabled()) {
						this.logger.debug(
								"Checking for rethrow: count=" + context.getRetryCount());
					}

					if (shouldRethrow(retryPolicy, context, state)) {
						if (this.logger.isDebugEnabled()) {
							this.logger.debug("Rethrow in retry for policy: count="
									+ context.getRetryCount());
						}
						throw RetryTemplate.<E>wrapIfNecessary(e);
					}

				}

				/*
				 * A stateful attempt that can retry may rethrow the exception before now,
				 * but if we get this far in a stateful retry there's a reason for it,
				 * like a circuit breaker or a rollback classifier.
				 */
				if (state != null && context.hasAttribute(GLOBAL_STATE)) {
					break;
				}
			}

			if (state == null && this.logger.isDebugEnabled()) {
				this.logger.debug(
						"Retry failed last attempt: count=" + context.getRetryCount());
			}

			exhausted = true;
			//重试结束后如果有兜底Recovery方法则执行，否则抛异常
			return handleRetryExhausted(recoveryCallback, context, state);

		}
		catch (Throwable e) {
			throw RetryTemplate.<E>wrapIfNecessary(e);
		}
		finally {
			//处理一些关闭逻辑
			close(retryPolicy, context, state, lastException == null || exhausted);
			//调用RetryListener的close方法
			doCloseInterceptors(retryCallback, context, lastException);
			RetrySynchronizationManager.clear();
		}

	}
```
Spring Retry采用了一个轻量级的做法，针对每一个需要重试的方法只new一个上下文Context对象，然后在重试时，把这个Context传到策略里，策略再根据这个Context做重试，而且Spring Retry还对这个Context做了cache。这样就相当于对重试的上下文做了优化。