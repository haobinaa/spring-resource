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
![](https://raw.githubusercontent.com/haobinaa/spring-resource/master/images/RequestHandlerMapping.png)

几个重要的接口，描述功能：
- ServletContextAware:保存ServletContext
- ApplicationContext:保存Spring上下文
- InitializingBean:初始化映射关系


看下代码：
``` 
//### RequestMappingHandlerMapping

// 1.实现InitializingBean的afterPropertiesSet， bean初始化的时候调用
public void afterPropertiesSet() {
  this.config = new RequestMappingInfo.BuilderConfiguration();
  this.config.setUrlPathHelper(getUrlPathHelper());
  this.config.setPathMatcher(getPathMatcher());
  this.config.setSuffixPatternMatch(this.useSuffixPatternMatch);
  this.config.setTrailingSlashMatch(this.useTrailingSlashMatch);
  this.config.setRegisteredSuffixPatternMatch(this.useRegisteredSuffixPatternMatch);
  this.config.setContentNegotiationManager(getContentNegotiationManager());
  // 调用父类的afterPropertiesSet
  super.afterPropertiesSet();
}
protected boolean isHandler(Class<?> beanType) {
  return (AnnotatedElementUtils.hasAnnotation(beanType, Controller.class) ||
      AnnotatedElementUtils.hasAnnotation(beanType, RequestMapping.class));
}	
protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
  RequestMappingInfo info = createRequestMappingInfo(method);
  if (info != null) {
    RequestMappingInfo typeInfo = createRequestMappingInfo(handlerType);
    if (typeInfo != null) {
      info = typeInfo.combine(info);
    }
  }
  return info;
}
	
	
	
//### AbstractHandlerMethodMapping	

// 父类的初始化钩子
public void afterPropertiesSet() {
		initHandlerMethods();
}

protected void initHandlerMethods() {
		if (logger.isDebugEnabled()) {
			logger.debug("Looking for request mappings in application context: " + getApplicationContext());
		}
		//从容器中获取所有object类型名
		String[] beanNames = (this.detectHandlerMethodsInAncestorContexts ?
				BeanFactoryUtils.beanNamesForTypeIncludingAncestors(obtainApplicationContext(), Object.class) :
				obtainApplicationContext().getBeanNamesForType(Object.class));

		for (String beanName : beanNames) {
		//抽象,过滤(在RequestMappingHandlerMapping中根据Controller和RequestMapping注解过滤)
			if (!beanName.startsWith(SCOPED_TARGET_NAME_PREFIX)) {
				Class<?> beanType = null;
				try {
					beanType = obtainApplicationContext().getType(beanName);
				}
				catch (Throwable ex) {
					// An unresolvable bean type, probably from a lazy bean - let's ignore it.
					if (logger.isDebugEnabled()) {
						logger.debug("Could not resolve target class for bean with name '" + beanName + "'", ex);
					}
				}
				if (beanType != null && isHandler(beanType)) {
				//探测类中定义的handler方法
					detectHandlerMethods(beanName);
				}
			}
		}
		handlerMethodsInitialized(getHandlerMethods());
}
protected void detectHandlerMethods(final Object handler) {
		Class<?> handlerType = (handler instanceof String ?
				obtainApplicationContext().getType((String) handler) : handler.getClass());

		if (handlerType != null) {
			final Class<?> userType = ClassUtils.getUserClass(handlerType);
			//得到符合条件的handler方法
			Map<Method, T> methods = MethodIntrospector.selectMethods(userType,
					(MethodIntrospector.MetadataLookup<T>) method -> {
						try {
						//抽象,得到映射信息(如RequestMappingInfo)
							return getMappingForMethod(method, userType);
						}
						catch (Throwable ex) {
							throw new IllegalStateException("Invalid mapping on handler class [" +
									userType.getName() + "]: " + method, ex);
						}
					});
			if (logger.isDebugEnabled()) {
				logger.debug(methods.size() + " request handler methods found on " + userType + ": " + methods);
			}
			//注册handler映射关系
			methods.forEach((method, mapping) -> {
				Method invocableMethod = AopUtils.selectInvocableMethod(method, userType);
				//保存映射路径和处理方法(还有跨域信息)
				registerHandlerMethod(handler, invocableMethod, mapping);
			});
		}
}
```
大概流程为:
1. 获取所有object子类
2. 根据条件过滤出handle处理类
3. 解析handle类中定义的处理方法
4. 注册映射关系


 `DispatcherServlet#getHander()`的实现：
 ``` 
 // AbstractHandlerMapping
 public final HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
 //抽象,调用子类实现得到一个handler(可以是任一对象,需要通过HandleAdapter来解析)
 //RequestMappingInfoHandlerMapping中具体实现就是匹配请求路径和RequestMapping注解
 		Object handler = getHandlerInternal(request);
 		if (handler == null) {
 			handler = getDefaultHandler();
 		}
 		if (handler == null) {
 			return null;
 		}
 		// Bean name or resolved handler?
 		if (handler instanceof String) {
 			String handlerName = (String) handler;
 			handler = obtainApplicationContext().getBean(handlerName);
 		}
    //包装handle成HandlerExecutionChain
 		HandlerExecutionChain executionChain = getHandlerExecutionChain(handler, request);
 		 //如果是跨域请求 则根据@CrossOrigin配置添加前置Intercept
 		if (CorsUtils.isCorsRequest(request)) {
 			CorsConfiguration globalConfig = this.globalCorsConfigSource.getCorsConfiguration(request);
 			CorsConfiguration handlerConfig = getCorsConfiguration(handler, request);
 			CorsConfiguration config = (globalConfig != null ? globalConfig.combine(handlerConfig) : handlerConfig);
 			executionChain = getCorsHandlerExecutionChain(request, executionChain, config);
 		}
 		return executionChain;
 	}
 	
 	
 	
 	
// AbstractHandlerMethodMapping
  protected HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
  //得到映射路径
  String lookupPath = getUrlPathHelper().getLookupPathForRequest(request);
  ...
  try {
  	//根据映射路径获取HandlerMethod
  	HandlerMethod handlerMethod = lookupHandlerMethod(lookupPath, request);
  	return (handlerMethod != null ? handlerMethod.createWithResolvedBean() : null);
  }
  ...
 }

 ```

#### HandlerAdapter

 HandlerAdapter根据HandlerMethod信息,对http请求进行参数解析,并完成调用
 
 接口定义:
 ``` 
 public interface HandlerAdapter {
    //判断是否支持该handler类型的解析
    boolean supports(Object handler);
    //参数解析 并调用handler完成过程调用 
    ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception;
    //用于处理http请求头中的last-modified
    long getLastModified(HttpServletRequest request, Object handler);
 
 }
 ```
 
 实现类`RequestMappingHandlerAdapter`的继承关系:
 ![](https://raw.githubusercontent.com/haobinaa/spring-resource/master/images/RequestMappingHandlerAdapter.png) 
 
 可以看出同样有ApplicationContextAware,ServletContextAware,InitializingBean三个生命周期接口
 
 InitializingBean：
 ``` 
 public void afterPropertiesSet() {
  // 1.装载@ControllerAdvice注解的类
  initControllerAdviceCache();
 // 2.装载ArgumentResolver(默认+自定义)
  if (this.argumentResolvers == null) {
     List<HandlerMethodArgumentResolver> resolvers = getDefaultArgumentResolvers();
     //包装成一个Composite对象
     this.argumentResolvers = new HandlerMethodArgumentResolverComposite().addResolvers(resolvers);
  }
  // 2.装载InitBinderArgumentResolvers(默认+自定义)
  if (this.initBinderArgumentResolvers == null) {
     List<HandlerMethodArgumentResolver> resolvers = getDefaultInitBinderArgumentResolvers();
     //包装成一个Composite对象
     this.initBinderArgumentResolvers = new HandlerMethodArgumentResolverComposite().addResolvers(resolvers);
  }
  // 3.装载ReturnValueHandlers(默认+自定义)
  if (this.returnValueHandlers == null) {
     List<HandlerMethodReturnValueHandler> handlers = getDefaultReturnValueHandlers();
     //包装成一个Composite对象
     this.returnValueHandlers = new HandlerMethodReturnValueHandlerComposite().addHandlers(handlers);
  }
 }
 ```
 
 ##### 1. 装载带有ControllerAdvices注解的对象
 ``` 
 private void initControllerAdviceCache() {
   //从容器中获取所有带有ControllerAdvices注解的类名 并包装成ControllerAdviceBean
   List<ControllerAdviceBean> beans = ControllerAdviceBean.findAnnotatedBeans(getApplicationContext());
   OrderComparator.sort(beans);
   List<Object> responseBodyAdviceBeans = new ArrayList<Object>();
   for (ControllerAdviceBean bean : beans) {
     //筛选出带有@ModelAttribute且不带@RequestMapping注解的方法
     Set<Method> attrMethods = HandlerMethodSelector.selectMethods(bean.getBeanType(), MODEL_ATTRIBUTE_METHODS);
     if (!attrMethods.isEmpty()) {
       //保存到modelAttributeAdviceCache中
       this.modelAttributeAdviceCache.put(bean, attrMethods);
     }
     //筛选出带InitBinder注解的方法 添加到initBinderAdviceCache中
     Set<Method> binderMethods = HandlerMethodSelector.selectMethods(bean.getBeanType(), INIT_BINDER_METHODS);
     if (!binderMethods.isEmpty()) {
       this.initBinderAdviceCache.put(bean, binderMethods);
     }
     //筛选实现RequestBodyAdvice接口 添加到requestResponseBodyAdviceBeans中
     if (RequestBodyAdvice.class.isAssignableFrom(bean.getBeanType())) {
       requestResponseBodyAdviceBeans.add(bean);
       if (logger.isInfoEnabled()) {
         logger.info("Detected RequestBodyAdvice bean in " + bean);
       }
     }
     //筛选实现ResponseBodyAdvice接口 添加到requestResponseBodyAdviceBeans中
     if (ResponseBodyAdvice.class.isAssignableFrom(bean.getBeanType())) {
       requestResponseBodyAdviceBeans.add(bean);
       if (logger.isInfoEnabled()) {
         logger.info("Detected ResponseBodyAdvice bean in " + bean);
       }
     }
   }
   //保存到全局变量
   if (!responseBodyAdviceBeans.isEmpty()) {
     this.responseBodyAdvice.addAll(0, responseBodyAdviceBeans);
   }
 }
 ```
 1. 获取所有带有ControllerAdvices注解的类名 并包装成ControllerAdviceBean
 2.  筛选出带有@ModelAttribute且不带@RequestMapping注解的方法
 3.  筛选出带InitBinder注解的方法 添加到initBinderAdviceCache中
 4.  筛选实现RequestBodyAdvice接口 添加到responseBodyAdvice中
 5.  筛选实现ResponseBodyAdvice接口 添加到responseBodyAdvice中
 
 
##### 2.装载ArgumentResolvers(默认+自定义)
##### 3.装载InitBinderArgumentResolvers(默认+自定义)
##### 4.装载ReturnValueHandlers(默认+自定义)
 
 
 #### DispatcherServelet调用HanderAdapter过程
 
 在`DispatcherServlet`的`doDispacth`中， 调用 `HanderAdapter`的部分是:
 ``` 
 //1.调用support()方法判断是否支持改handler的解析
 HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());
 // 如果是Get或Head请求 调用getLastModified()获取上次更新时间 
 String method = request.getMethod();
 boolean isGet = "GET".equals(method);
 if (isGet || "HEAD".equals(method)) {
    long lastModified = ha.getLastModified(request, mappedHandler.getHandler());
    if (logger.isDebugEnabled()) {
       logger.debug("Last-Modified value for [" + getRequestUri(request) + "] is: " + lastModified);
    }
   //如果小于浏览器缓存更新时间 则直接返回 浏览器使用本地缓存
    if (new ServletWebRequest(request, response).checkNotModified(lastModified) && isGet) {
       return;
    }
 }
 if (!mappedHandler.applyPreHandle(processedRequest, response)) {
    return;
 }
 // 调用handler完成过程调用
 mv = ha.handle(processedRequest, response, mappedHandler.getHandler());
 ```
DisPatcherServlet调用HandlerAdapter分为三步:
##### 1.调用support()方法判断是否支持改handler的解析
``` 
#org.springframework.web.servlet.DispatcherServlet
//在doDispatch()方法中调用了getHandlerAdapter(Object)方法来得到一个HandlerAdapter
protected HandlerAdapter getHandlerAdapter(Object handler) throws ServletException {
    //调用HandlerAdapter.support()方法 判断是否支持该handler对象的解析
   for (HandlerAdapter ha : this.handlerAdapters) {
        ...
      if (ha.supports(handler)) {
         return ha;
      }
   }
  ...
}
#org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter
@Override
public final boolean supports(Object handler) {
	return (handler instanceof HandlerMethod && supportsInternal((HandlerMethod) handler));
}
#org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
@Override
protected boolean supportsInternal(HandlerMethod handlerMethod) {
	return true;
}
``` 
 
 
##### 2.如果是Get或Head请求 调用getLastModified()获取上次更新时间

如果是Get或Head请求 调用getLastModified()获取上次更新时间, 如果小于浏览器缓存更新时间 则直接返回 浏览器使用本地缓存
``` 
if (isGet || "HEAD".equals(method)) {
					long lastModified = ha.getLastModified(request, mappedHandler.getHandler());
					...
					// 如果小于浏览器更新缓存时间
					if (new ServletWebRequest(request, response).checkNotModified(lastModified) && isGet) {
						return;
					}
}
```

##### 3.调用handler()方法完成过程调用(参数解析 返回值解析)
``` 
# DispatcherServlet
 mv = ha.handle(processedRequest, response, mappedHandler.getHandler());
 
#org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter
@Override
public final ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
   return handleInternal(request, response, (HandlerMethod) handler);
}

#org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
@Override
protected ModelAndView handleInternal(HttpServletRequest request,
		HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {
	//对http协议缓存方面的请求头的处理(expire,cache-control)
	if (getSessionAttributesHandler(handlerMethod).hasSessionAttributes()) {
		// Always prevent caching in case of session attribute management.
		checkAndPrepare(request, response, this.cacheSecondsForSessionAttributeHandlers, true);
	}
	else {
		// Uses configured default cacheSeconds setting.
		checkAndPrepare(request, response, true);
	}
	// Execute invokeHandlerMethod in synchronized block if required.
	if (this.synchronizeOnSession) {//是否使用session锁
		HttpSession session = request.getSession(false);
		if (session != null) {
          	//得到互斥量
			Object mutex = WebUtils.getSessionMutex(session);
			synchronized (mutex) {//执行过程调用
				return invokeHandleMethod(request, response, handlerMethod);
			}
		}
	}
	//执行过程调用
	return invokeHandleMethod(request, response, handlerMethod);
}
//根据HandlerMethod解析参数 并完成过程调用得到一个ModelAndView
private ModelAndView invokeHandleMethod(HttpServletRequest request,
		HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {
	ServletWebRequest webRequest = new ServletWebRequest(request, response);
	//使用initBinderAdviceCache对@initBinder进行处理
	WebDataBinderFactory binderFactory = getDataBinderFactory(handlerMethod);
    // 使用modelAttributeAdviceCache对@ModelAttribute进行处理
	ModelFactory modelFactory = getModelFactory(handlerMethod, binderFactory);
	ServletInvocableHandlerMethod requestMappingMethod = createRequestMappingMethod(handlerMethod, binderFactory);
	ModelAndViewContainer mavContainer = new ModelAndViewContainer();
mavContainer.addAllAttributes(RequestContextUtils.getInputFlashMap(request));
	modelFactory.initModel(webRequest, mavContainer, requestMappingMethod);		mavContainer.setIgnoreDefaultModelOnRedirect(this.ignoreDefaultModelOnRedirect);
	//对异步的处理 暂时不管 TODO后面再分析
  	...
  	//1 完成过程调用
	requestMappingMethod.invokeAndHandle(webRequest, mavContainer);
	if (asyncManager.isConcurrentHandlingStarted()) {
		return null;
	}
	//2 包装ModelAndView
	return getModelAndView(mavContainer, modelFactory, webRequest);
}
```
============ invokeAndHandler
```
#org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod
public void invokeAndHandle(ServletWebRequest webRequest,
                    ModelAndViewContainer mavContainer, Object... providedArgs) throws Exception {
  // 1.1 参数解析 并完成过程调用
  Object returnValue = invokeForRequest(webRequest, mavContainer, providedArgs);
  setResponseStatus(webRequest);
  ...
    try {
      //1.2 使用returnValueHandlers对返回结果进行处理 讲结果塞到mavContainer中 过程类似参数解析
      this.returnValueHandlers.handleReturnValue(
        returnValue, getReturnValueType(returnValue), mavContainer, webRequest);
    }
}
#org.springframework.web.method.support.InvocableHandlerMethod
        public Object invokeForRequest(NativeWebRequest request, ModelAndViewContainer mavContainer,
                                       Object... providedArgs) throws Exception {
      //1.1.1 参数解析并得到绑定的结果
      Object[] args = getMethodArgumentValues(request, mavContainer, providedArgs);
      ...
        //1.1.2 反射完成过程调用 
        Object returnValue = doInvoke(args);
      ...
        return returnValue;
    }
    private Object[] getMethodArgumentValues(NativeWebRequest request, ModelAndViewContainer mavContainer,
                                             Object... providedArgs) throws Exception {
      //参数信息
      MethodParameter[] parameters = getMethodParameters();
      Object[] args = new Object[parameters.length];
      for (int i = 0; i < parameters.length; i++) {
        //调用HandlerMethodArgumentResolver#supportsParameter判断是否支持
        if (this.argumentResolvers.supportsParameter(parameter)) {
          try {
            //调用HandlerMethodArgumentResolver#resolveArgument进行解析
            args[i] = this.argumentResolvers.resolveArgument(
              parameter, mavContainer, request, this.dataBinderFactory);
            continue;
          }
          ...
        }
        ...
      }
      return args;
    }
```
============ getModelAndView
``` 
//从mavContainer取出结果 包装成ModelAndView
private ModelAndView getModelAndView(ModelAndViewContainer mavContainer,
                                     ModelFactory modelFactory, NativeWebRequest webRequest) throws Exception {
  modelFactory.updateModel(webRequest, mavContainer);
  if (mavContainer.isRequestHandled()) {
    return null;
  }
  ModelMap model = mavContainer.getModel();
  ModelAndView mav = new ModelAndView(mavContainer.getViewName(), model);
  if (!mavContainer.isViewReference()) {
    mav.setView((View) mavContainer.getView());
  }
  //如果是redirect请求
  if (model instanceof RedirectAttributes) {
    Map<String, ?> flashAttributes = ((RedirectAttributes) model).getFlashAttributes();
    HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
    RequestContextUtils.getOutputFlashMap(request).putAll(flashAttributes);
  }
  return mav;
}
```

流程总览：
![](https://raw.githubusercontent.com/haobinaa/spring-resource/master/images/spring_mvc_code_process.png)

### 参考资料
- [spring mvc 思维导图](http://developer.51cto.com/art/201707/545155.htm)
- [spring mvc 示例](http://www.cnblogs.com/sunniest/p/4555801.html)
- [spring mvc 源码分析-精简](https://juejin.im/post/5aaf4c556fb9a028b547af83)

