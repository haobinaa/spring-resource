### Spring Lifecycle

Lifecycle接口定义Spring容器的生命周期，任何springbean都可以实现该接口。当ApplicationContext接收启动和停止信号时，spring容器将在容器上下文中找出所有实现了LifeCycle及其子类接口的类，并一一调用它们实现类对应的方法。

spring 是通过委托给生命周期处理器 `LifecycleProcessor` 来实现这一点的。


refresh 结束后 `finishRefresh`:
``` 
protected void finishRefresh() {
		clearResourceCaches();

		// 初始化当前 context 的 LifecycleProcessor
		initLifecycleProcessor();

		// 调用 LifecycleProcessor 的 onRefresh
		getLifecycleProcessor().onRefresh();

		publishEvent(new ContextRefreshedEvent(this));
		LiveBeansView.registerApplicationContext(this);
	}
```

#### AbstractApplicationContext 中 Lifecycle 的处理逻辑

`AbstractApplicationContext`关联了`LifecycleProcessor`，在其`refresh`的最后`finishRefresh`方法里面调用到了`onRefresh`方法触发`Lifecycle`的`start`方法：

``` 
// DefaultLifecycleProcessor#onRefresh
public void onRefresh() {
		startBeans(true);
		this.running = true;
}

private void startBeans(boolean autoStartupOnly) {
        // 获取所有实现了 Lifecycle 的 bean(一般是 SmartLifeCycle)
		Map<String, Lifecycle> lifecycleBeans = getLifecycleBeans();
		Map<Integer, LifecycleGroup> phases = new HashMap<>();
        // 遍历所有实现了 Lifecycle 的 bean
		lifecycleBeans.forEach((beanName, bean) -> {
		    // 默认autoStartupOnly=true，因此只能筛选出是SmartLifecycle.isAutoStartup()=true的bean
			if (!autoStartupOnly || (bean instanceof SmartLifecycle && ((SmartLifecycle) bean).isAutoStartup())) {
				int phase = getPhase(bean);
				LifecycleGroup group = phases.get(phase);
				if (group == null) {
					group = new LifecycleGroup(phase, this.timeoutPerShutdownPhase, lifecycleBeans, autoStartupOnly);
					phases.put(phase, group);
				}
				group.add(beanName, bean);
			}
		});
		if (!phases.isEmpty()) {
			List<Integer> keys = new ArrayList<>(phases.keySet());
			Collections.sort(keys);
			for (Integer key : keys) {
				phases.get(key).start();
			}
		}
}

// 
private void doStart(Map<String, ? extends Lifecycle> lifecycleBeans, String beanName, boolean autoStartupOnly) {
// 移除已经执行过start方法的bean，因为下面依赖原因，避免重复调用
    Lifecycle bean = lifecycleBeans.remove(beanName);
    if (bean != null && bean != this) {
        // 获取这个Bean依赖的其它Bean,在启动时先启动其依赖的Bean
        String[] dependenciesForBean = getBeanFactory().getDependenciesForBean(beanName);
        for (String dependency : dependenciesForBean) {
            doStart(lifecycleBeans, dependency, autoStartupOnly);
        }
        if (!bean.isRunning() &&
            (!autoStartupOnly || !(bean instanceof SmartLifecycle) || ((SmartLifecycle) bean).isAutoStartup())) {
            try {
                // 调用 bean 的 start 方法
                bean.start();
            }
            catch (Throwable ex) {
                throw new ApplicationContextException("Failed to start bean '" + beanName + "'", ex);
            }
        }
    }
}

```


