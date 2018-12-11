### Spring mvc 运行流程

![](https://raw.githubusercontent.com/haobinaa/spring-resource/master/images/spring_mvc.png)

(1) Http请求：客户端请求提交到DispatcherServlet。

(2) 寻找处理器：由DispatcherServlet控制器查询一个或多个HandlerMapping，找到处理请求的Controller。

(3) 调用处理器：DispatcherServlet将请求提交到Controller。

(4)(5)调用业务处理和返回结果：Controller调用业务逻辑处理后，返回ModelAndView。

(6)(7)处理视图映射并返回模型： DispatcherServlet查询一个或多个ViewResoler视图解析器，找到ModelAndView指定的视图。

(8) Http响应：视图负责将结果显示到客户端。

### Spring mvc 源码解析

springboot 给我们提供开箱即用的体验， 但也封装了很多细节， 在这里回顾一下Spring mvc的使用流程：

配置web.xml-> 配置init-param扫描的Spring mvc配置文件-> 定义controller接收和响应请求


一个常见的web.xml大概配置如下:
``` 
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://java.sun.com/xml/ns/javaee"
         xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd"
         id="WebApp_ID" version="3.0">
    <display-name>spring-mvc</display-name>
    <welcome-file-list>
        <welcome-file>index.jsp</welcome-file>
    </welcome-file-list>
    <context-param>
        <param-name>contextConfigLocation</param-name>
      	# spring的配置
        <param-value>classpath:config/spring/applicationContext.xml</param-value>
    </context-param>
    <listener>
        <listener-class>org.springframework.web.context.ContextLoaderListener</listener-class>
    </listener>
    
    
    <servlet>
        <servlet-name>controller</servlet-name>
        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
        <init-param>
            <param-name>contextConfigLocation</param-name>
          	# springmvc的配置
            <param-value>classpath:config/spring/spring-controller.xml</param-value>
        </init-param>
        <load-on-startup>1</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>controller</servlet-name>
        <url-pattern>/</url-pattern>
    </servlet-mapping>
    
    
</web-app>
```

### 启动过程

#### WebApplicationContext的创建
首先ContextLoadListener创建WebApplicationContext作为spring的容器上下文
``` 
public class ContextLoaderListener extends ContextLoader implements ServletContextListener 
```
ContextLoaderListener实现了ServletContextListener:
``` 
public interface ServletContextListener extends EventListener {
  //容器初始化完成
    public void contextInitialized(ServletContextEvent sce);
  //容器停止
    public void contextDestroyed(ServletContextEvent sce);
}
```
由servlet标准可知ServletContextListener定义了容器的生命周期方法,springmvc就借助其启动与停止
ContextLoadListener调用了initWebApplicationContext方法,创建WebApplicationContext作为spring的容器上下文
``` 
// org.springframework.web.context.ContextLoader
public WebApplicationContext initWebApplicationContext(ServletContext servletContext) {
	... 省略部分
	...
	try {
		if (this.context == null) {//判空 (以注解方式配置时非空)
			this.context = createWebApplicationContext(servletContext);
		}
		if (this.context instanceof ConfigurableWebApplicationContext) {
			ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) this.context;
			if (!cwac.isActive()) {
				...
				//读取contextConfigLocation配置并refresh()
				configureAndRefreshWebApplicationContext(cwac, servletContext);
			}
		}
        //将applicationContext设置到servletContext中
   	servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.context);
		... 省略部分
		...
		return this.context;
	}
}
```

而DispatcherServlet创建WebApplicationContext作为springmvc的上下文 并将ContextLoadListener创建的上下文设置为自身的parent
``` 
// DispatcherServlet 

protected final void initServletBean() throws ServletException {
	...
	try {
		this.webApplicationContext = initWebApplicationContext();
		initFrameworkServlet();
	}
	...
}
protected WebApplicationContext initWebApplicationContext() {
	WebApplicationContext rootContext =
			WebApplicationContextUtils.getWebApplicationContext(getServletContext());
	WebApplicationContext wac = null;
	...
	if (wac == null) {
		//创建applicationContext
		wac = createWebApplicationContext(rootContext);
	}
	...
	return wac;
}
protected WebApplicationContext createWebApplicationContext(ApplicationContext parent) {
	//XmlWebApplicationContext 
	Class<?> contextClass = getContextClass();
	...
	//创建applicationContext
	ConfigurableWebApplicationContext wac =
			(ConfigurableWebApplicationContext) BeanUtils.instantiateClass(contextClass);
	wac.setEnvironment(getEnvironment());
	//设置parent(ContextLoadListener中创建的applicationContext)
	wac.setParent(parent);
	//读取contextConfigLocation配置
	wac.setConfigLocation(getContextConfigLocation());
	//refresh()
	configureAndRefreshWebApplicationContext(wac);
	return wac;
}
```
springmvc的applicationContext会去读取配置文件, 解析标签， 加载bean(跟ioc流程类似)

#### DispatcherServlet
DispatcherServlet的类层次结构如图:
![]()
``` 
protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
	...
	try {
		ModelAndView mv = null;
		Exception dispatchException = null;
		try {
			processedRequest = checkMultipart(request);
			multipartRequestParsed = (processedRequest != request);
			//1.调用handlerMapping获取handlerChain
			mappedHandler = getHandler(processedRequest);
			if (mappedHandler == null || mappedHandler.getHandler() == null) {
				noHandlerFound(processedRequest, response);
				return;
			}
			// 2.获取支持该handler解析的HandlerAdapter
			HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());
			...
			// 3.使用HandlerAdapter完成handler处理
			mv = ha.handle(processedRequest, response, mappedHandler.getHandler());
			if (asyncManager.isConcurrentHandlingStarted()) {
				return;
			}
			// 4.视图处理(页面渲染)
			applyDefaultViewName(request, mv);
			mappedHandler.applyPostHandle(processedRequest, response, mv);
		}
		catch (Exception ex) {
			dispatchException = ex;
		}
		processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);
	}
	...
}
```









流程总览：
![]()

### 参考资料
- [spring mvc 思维导图](http://developer.51cto.com/art/201707/545155.htm)
- [spring mvc 示例](http://www.cnblogs.com/sunniest/p/4555801.html)
- [spring mvc 源码分析-精简](https://juejin.im/post/5aaf4c556fb9a028b547af83)

