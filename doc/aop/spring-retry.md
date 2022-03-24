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
`RetryConfiguration` 继承于 `AbstractPointcutAdvisor`， 它有一个pointcut和一个advice。
