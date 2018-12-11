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
![](https://raw.githubusercontent.com/haobinaa/spring-resource/master/images/DispatcherServlest.png)

有几个生命周期接口：
- ApplicationContextAware保存了spring上下文
- EnvironmentAware保存了环境变量对象
- 继承了Servlet即包含了servlet的生命周期


##### DispatcherServlet调用过程
``` 
protected void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {
	//将相关配置设置到request作用于中
	request.setAttribute(WEB_APPLICATION_CONTEXT_ATTRIBUTE, getWebApplicationContext());
	request.setAttribute(LOCALE_RESOLVER_ATTRIBUTE, this.localeResolver);
	request.setAttribute(THEME_RESOLVER_ATTRIBUTE, this.themeResolver);
	request.setAttribute(THEME_SOURCE_ATTRIBUTE, getThemeSource());
  	...
	request.setAttribute(OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());
	request.setAttribute(FLASH_MAP_MANAGER_ATTRIBUTE, this.flashMapManager);

	try {
		doDispatch(request, response);
	}
}


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
private void processDispatchResult(HttpServletRequest request, HttpServletResponse response,
		HandlerExecutionChain mappedHandler, ModelAndView mv, Exception exception) throws Exception {
	boolean errorView = false;
  	//如果HandlerAdapter.handler()执行异常 则进行异常处理
	if (exception != null) {
		...
          Object handler = (mappedHandler != null ? mappedHandler.getHandler() : null);
      //4.1
        mv = processHandlerException(request, response, handler, exception);
        errorView = (mv != null);
	}


	if (mv != null && !mv.wasCleared()) {
      	//4.2 视图解析 渲染返回
		render(mv, request, response);
		if (errorView) {
			WebUtils.clearErrorRequestAttributes(request);
		}
	}
}
//异常处理
protected ModelAndView processHandlerException(HttpServletRequest request, HttpServletResponse response,
		Object handler, Exception ex) throws Exception {
	ModelAndView exMv = null;
	for (HandlerExceptionResolver handlerExceptionResolver : this.handlerExceptionResolvers) {
      	//使用异常解析器解析异常(类似HandlerAdapter参数解析,调用)
		exMv = handlerExceptionResolver.resolveException(request, response, handler, ex);
		if (exMv != null) {
			break;
		}
    }
  	...
}
protected void render(ModelAndView mv, HttpServletRequest request, HttpServletResponse response) throws Exception {
	//国际化
	Locale locale = this.localeResolver.resolveLocale(request);
	response.setLocale(locale);
	View view;
	if (mv.isReference()) {
		//4.2.1视图解析
		view = resolveViewName(mv.getViewName(), mv.getModelInternal(), locale, request);
		...
	}
	else {
		view = mv.getView();
	}
	....
	try {
		if (mv.getStatus() != null) {
			response.setStatus(mv.getStatus().value());
		}
      	//渲染返回
		view.render(mv.getModelInternal(), request, response);
	}
	...
}
protected View resolveViewName(String viewName, Map<String, Object> model, Locale locale,
		HttpServletRequest request) throws Exception {
	//视图解析
	for (ViewResolver viewResolver : this.viewResolvers) {
		View view = viewResolver.resolveViewName(viewName, locale);
		if (view != null) {
			return view;
		}
	}
	return null;
}
```
流程概述：
1. 调用HandlerMapping得到HandlerChain(Handler+Intercept)
2. 调用HandlerAdapter执行handle过程(参数解析 过程调用)
3. 异常处理(过程类似HanderAdapter)
4. 调用ViewResolver进行视图解析
5. 渲染视图

#### HandleMapping

HandlerMapping是对： 请求路径->处理映射过程的一个管理, 接口定义如下:
``` 
public interface HandlerMapping {
  //根据request获取处理链
   HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception;
}
```
以`RequestHandlerMapping`为例， 继承关系:










流程总览：
![]()

### 参考资料
- [spring mvc 思维导图](http://developer.51cto.com/art/201707/545155.htm)
- [spring mvc 示例](http://www.cnblogs.com/sunniest/p/4555801.html)
- [spring mvc 源码分析-精简](https://juejin.im/post/5aaf4c556fb9a028b547af83)

