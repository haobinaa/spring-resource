### Spring事件监听

Spring中事件机制是基于ApplicationContext的观察者模式实现的，通过`ApplicationEvent`类和`ApplicationListener`接口，可以实现ApplicationContext事件处理。如果容器中有一个ApplicationListener Bean，每当ApplicationContext发布ApplicationEvent时，ApplicationListener Bean将自动被触发。

- `ApplicationEvent`: 容器事件，必须由ApplicationContext发布
- 'ApplicationListener': 监听器，可由容器中的任何监听器Bean担任

- [示例代码-ApplicationContext事件](../../src/main/java/base/event/Test.java)

可以看到`ApplicationContext`通过`pushEvent`触发了事件
