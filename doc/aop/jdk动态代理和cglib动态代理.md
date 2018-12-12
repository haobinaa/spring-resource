### JDK动态代理(对实现了接口的对象进行代理)

#### jdk动态代理的实现

jdk动态代理依赖两个重要的接口(类)：

#####  InvocationHandler
每一个动态代理类都必须要实现InvocationHandler这个接口，并且每个代理类（Proxy）的实例都关联到了一个handler，当我们通过代理对象调用一个方法的时候，这个方法的调用就会被转发为由InvocationHandler这个接口的 invoke 方法来进行调用。

``` 
public interface InvocationHandler { 
    public Object invoke(Object proxy,Method method,Object[] args) throws Throwable; 
} 
```
- Object proxy： 被代理的对象
- Method method:  我们所要调用被代理对象的某个方法的Method对象
- Object[] args：被代理对象某个方法调用时所需要的参数 

##### Proxy类
 Proxy类是专门完成代理的操作类，可以通过此类为一个或多个接口动态地生成实现类, 我们使用`newProxyInstance`生成代理代理类实例
 ``` 
 public static Object newProxyInstance(ClassLoader loader,Class<?>[] interfaces,InvocationHandler h)
 ```
 - ClassLoader loader：类加载器，定义了由哪个ClassLoader对象来对生成的代理对象进行加
 - Class<?>[] interfaces：得到被代理类全部的接口，如果我提供了一组接口给它，那么这个代理对象就宣称实现了该接口，这样我就能调用这组接口中的方法了
 - InvocationHandler h：得到InvocationHandler接口的子类实例 
 
 #### JDK动态代理使用示例
 
 1.定义一个接口
 
 ``` 
public interface Subject {
    public void visit();
}
 ```
2.接口的实现类,也是我们被代理的对象 
``` 

public class RealSubject implements Subject {
 
	@Override
	public void visit() {
	     System.out.println("I am 'RealSubject',I am the execution method");
	}
}
```
3.定义一个代理类(实现InvocationHandler)
``` 

public class DynamicProxy implements InvocationHandler {
 
	// 我们要代理的真实对象(委托对象)
	private Object subject;
	
	// 构造方法，给我们要代理的真实对象赋初值
	public DynamicProxy(Object obj){
		this.subject = obj;
	}
	
	@Override
	public Object invoke(Object object, Method method, Object[] args)
			throws Throwable {
		// 在代理真实对象操作前 我们可以添加一些自己的操作
		System.out.println("before proxy invoke");
		
		// 当代理对象调用真实对象的方法时，其会自动的跳转到代理对象关联的handler对象的invoke方法来进行调用
		method.invoke(subject, args);
		
		// 在代理真实对象操作后 我们也可以添加一些自己的操作
		System.out.println("after proxy invoke");
		return null;
	}
}
```
4.生成代理类
``` 
public static void main(String[] args) {
		// 我们要代理的真实对象
		Subject realSubject = new RealSubject();
		// 我们要代理哪个真实对象，就将该对象传进去，最后是通过该真实对象调用方法的
		InvocationHandler handler = new DynamicProxy(realSubject);
		Subject proxyInstance = (Subject)Proxy.newProxyInstance(handler.getClass().getClassLoader(), 
				RealSubject.class.getInterfaces(), 
				handler);
		
		System.out.println(proxyInstance.getClass().getName());
		proxyInstance.visit();
 
	}
```

### Cglib（Code Generation Library）动态代理 (对没有实现接口的普通类做代理)

#### 概述

 Cglib是一个优秀的动态代理框架，它的底层使用ASM（JAVA字节码处理框架）在内存中动态的生成被代理类的子类。使CGLIB即使被代理类没有实现任何接口也可以实现动态代理功能。但是不能对final修饰的类进行代理。
 


#### 使用示例
1.首先定义一个需要被代理的对象
``` 
public class RealObj {
    public void visit(){
        System.out.println("real obj method");
    }
}
```
2.代理类(需要实现MethodInterceptor)
``` 
public class ProxyObj implements MethodInterceptor {

    public Object intercept(Object object, Method method, Object[] objects, MethodProxy proxy) throws Throwable {
        System.out.println("Before Method Invoke");
        proxy.invokeSuper(object, objects);
        System.out.println("After Method Invoke");
        return object;
    }
}
```
3.生成代理对象并使用
``` 
public static void main(String[] args) {
      ProxyObj proxyObj = new ProxyObj();
      Enhancer enhancer = new Enhancer();
      enhancer.setSuperclass(RealObj.class);
      enhancer.setCallback(proxyObj);

      RealObj obj = (RealObj) enhancer.create();
      obj.visit();
}
```

 #### 原理
 
cglib通过字节码技术为一个类创建子类，并在子类中采用方法拦截的技术拦截(绑定call back)所有父类方法的调用

总结：
- CGlib可以传入接口也可以传入普通的类，接口使用实现的方式,普通类使用会使用继承的方式生成代理类(通常cglib用于代理类而非接口)
- 由于是继承方式,如果是 static方法,private方法,final方法等描述的方法是不能被代理的
- CGLIB会默认代理Object中equals,toString,hashCode,clone等方法。比JDK代理多了clone
