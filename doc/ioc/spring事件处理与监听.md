### Spring事件监听

Spring中事件机制是基于ApplicationContext的观察者模式实现的，通过`ApplicationEvent`类和`ApplicationListener`接口，可以实现 ApplicationContext事件处理。如果容器中有一个ApplicationListener Bean，每当ApplicationContext发布ApplicationEvent时，ApplicationListener Bean将自动被触发。

- `ApplicationEvent`: 容器事件，必须由 ApplicationContext 发布
- `ApplicationListener`: 监听器，可由容器中的任何监听器Bean担任
- `ApplicationContext`: 发布者， 用来发布事件

- [示例代码-ApplicationContext事件](../../src/main/java/base/event/Test.java)


### 源码分析

实现一个事件机制， 需要上面三种角色参与即可， Spring 将事件发布给专门的监听器是由 `ApplicationEventMulticaster` 接口的实现来表示的， 这个接口由三种类型的方法:
- 添加新的监听器：定义了两种方法来添加新的监听器：`addApplicationListener(ApplicationListener<?> listener)`和`addApplicationListenerBean(String listenerBeanName)`。当监听器对象已知时，可以应用第一个。如果使用第二个，我们需要将beanName 得到 listener对象(依赖查找DL)，然后再将其添加到listener列表中。

- 删除监听器：添加方法一样，我们可以通过传递对象来删除一个监听器: `removeApplicationListener(ApplicationListener<?> listener)` 或通过传递bean名称 `removeApplicationListenerBean(String listenerBeanName)`， 和第三种方法：removeAllListeners()用来删除所有已注册的监听器

- 将事件发送到已注册的监听器: `multicastEvent(ApplicationEvent event)` 向所有注册的监听器发发布事件

#### 事件类型

根据 `ApplicationContextEvent` 的实现类， 可以分为两种事件类型：

- 与应用上下文相关： 在 `ApplicationContext` 中触发。`ContextStartedEvent`在上下文启动时被启动，当它停止时启动`ContextStoppedEvent`，当上下文被刷新时产生`ContextRefreshedEvent`，最后在上下文关闭时产生`ContextClosedEvent`。 整个流程跟 `Spring LifeCycle` 一样
```
protected void finishRefresh() {
	clearResourceCaches();
	initLifecycleProcessor();
	getLifecycleProcessor().onRefresh();

	// 发布 ContextRefreshedEvent 事件， 此时是 IOC 容器重新初始化完
	publishEvent(new ContextRefreshedEvent(this));

	LiveBeansView.registerApedplicationContext(this);
}


protected void doClose() {
	if (this.active.get() && this.closed.compareAndSet(false, true)) {
		if (logger.isInfoEnabled()) {
			logger.info("Closing " + this);
		}

		LiveBeansView.unregisterApplicationContext(this);

		try {
			// 发布 ContextClosedEvent 事件， 上下文关闭触发
			publishEvent(new ContextClosedEvent(this));
		}
		catch (Throwable ex) {
			logger.warn("Exception thrown from ApplicationListener handling ContextClosedEvent", ex);
		}

		// Stop all Lifecycle beans, to avoid delays during individual destruction.
		try {
			getLifecycleProcessor().onClose();
		}
         ...
       }      


public void start() {
	getLifecycleProcessor().start();
    // 发布 ContextStartedEvent 事件， 上下文启动时触发
	publishEvent(new ContextStartedEvent(this));
}

@Override
public void stop() {
	getLifecycleProcessor().stop();
    // 发布 ContextStoppedEvent 事件
	publishEvent(new ContextStoppedEvent(this));
}
```

- 与request 请求相关联：由 `org.springframework.web.context.support.RequestHandledEvent`实例来表示，当在 ApplicationContext 中处理请求时，它们被引发。

#### 事件发布

spring 正常事件(上面描述的上下文相关事件)随着 Spring LifeCycle 会调用 `AbstractApplicationContext#publishEvent` 来发布对应事件， 也可手动触发:
``` 
@Override
public void publishEvent(ApplicationEvent event) {
    publishEvent(event, null);
}


@Override
public void publishEvent(Object event) {
    publishEvent(event, null);
}

protected void publishEvent(Object event, @Nullable ResolvableType eventType) {
    Assert.notNull(event, "Event must not be null");
    if (logger.isTraceEnabled()) {
        logger.trace("Publishing event in " + getDisplayName() + ": " + event);
    }

    ApplicationEvent applicationEvent;
    if (event instanceof ApplicationEvent) {
        applicationEvent = (ApplicationEvent) event;
    }
    else {
        applicationEvent = new PayloadApplicationEvent<>(this, event);
        if (eventType == null) {
            eventType = ((PayloadApplicationEvent)applicationEvent).getResolvableType();
        }
    }

    // Multicast right now if possible - or lazily once the multicaster is initialized
    if (this.earlyApplicationEvents != null) {
        this.earlyApplicationEvents.add(applicationEvent);
    }
    else {
        // SimpleApplicationEventMulticaster 发布事件
        getApplicationEventMulticaster().multicastEvent(applicationEvent, eventType);
    }

    // 通过 parent context 也发布一下事件
    if (this.parent != null) {
        if (this.parent instanceof AbstractApplicationContext) {
            ((AbstractApplicationContext) this.parent).publishEvent(event, eventType);
        }
        else {
            this.parent.publishEvent(event);
        }
    }
}
```

`SimpleApplicationEventMulticaster#multicastEvent`实现：
``` 
@Override
public void multicastEvent(ApplicationEvent event) {
	multicastEvent(event, resolveDefaultEventType(event));
}

@Override
public void multicastEvent(final ApplicationEvent event, @Nullable ResolvableType eventType) {
	ResolvableType type = (eventType != null ? eventType : resolveDefaultEventType(event));
	for (final ApplicationListener<?> listener : getApplicationListeners(event, type)) {
        // 判断是否用异步的 executor 去执行(默认是同步执行上下文事件)
		Executor executor = getTaskExecutor();
		if (executor != null) {
			executor.execute(() -> invokeListener(listener, event));
		}
		else {
            // 执行 listener
			invokeListener(listener, event);
		}
	}
}

private ResolvableType resolveDefaultEventType(ApplicationEvent event) {
	return ResolvableType.forInstance(event);
}


@SuppressWarnings({"unchecked", "rawtypes"})
protected void invokeListener(ApplicationListener listener, ApplicationEvent event) {
	ErrorHandler errorHandler = getErrorHandler();
	if (errorHandler != null) {
		try {
			listener.onApplicationEvent(event);
		}
		catch (Throwable err) {
			errorHandler.handleError(err);
		}
	}
	else {
		try {
            // 执行 listner 
			listener.onApplicationEvent(event);
		}
		catch (ClassCastException ex) {
			String msg = ex.getMessage();
			if (msg == null || msg.startsWith(event.getClass().getName())) {
				// Possibly a lambda-defined listener which we could not resolve the generic event type for
				Log logger = LogFactory.getLog(getClass());
				if (logger.isDebugEnabled()) {
					logger.debug("Non-matching event type for listener: " + listener, ex);
				}
			}
			else {
				throw ex;
			}
		}
	}
}
```