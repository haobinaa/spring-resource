### FactoryBean
``` 
public interface FactoryBean<T> {
  	//返回由FactoryBean创建的bean实例,如果isSingleton()返回true,则该实例会放到Spring容器中单实例缓存池中
    T getObject() throws Exception;
	//返回FactoryBean创建的bean类型
    Class<?> getObjectType();
	//返回bean实例的作用域是singleton还是prototype
    boolean isSingleton();
}
```

当配置文件的class属性配置的实现类是FactoryBean 时,通过getBean() 方法返回的不是FactoryBean 本身,而是FactoryBean.getObject() 方法所返回的对象. 相当于FactoryBean.getObject() 代理了getBean() 方法.


当使用ApplicationContext的getBean()方法获取FactoryBean实例本身而不是它所产生的bean，则要使用&符号+id。比如，现有FactoryBean，它有id，在容器上调用getBean("myBean")将返回FactoryBean所产生的bean，调用getBean("&myBean")将返回FactoryBean它本身的实例。


#### 例子
``` 
public class Car {
  private int maxSpeed;
  private String brand;
  private double price;
  //get/set
}
```
如果使用传统方式配置Car的bean,Car的每个属性对应一个 元素标签

如果使用FactoryBean来配置：
``` 
public class CarFactoryBean implements FactoryBean<Car> {
    private String carInfo;

    @Override
    public Car getObject() throws Exception {
        Car car = new Car();
        // 假设car的信息通过逗号隔开的string
        String[] infos = carInfo.split(",");
        car.setBrand(infos[0]);
        car.setMaxSpeed(Integer.valueOf(infos[1]));
        car.setPrice(Double.valueOf(infos[2]));
        return car;
    }

    @Override
    public Class<?> getObjectType() {
        return Car.class;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    public String getCarInfo() {
        return carInfo;
    }

    public void setCarInfo(String carInfo) {
        this.carInfo = carInfo;
    }
}
```

配置文件中配置CarBean
``` 
<bean id="car" class="com.test.factoryBean.CarFactoryBean">
  <property name = "carInfo" value ="超级跑车,400,3000"/>
</bean>
```

调用 getBean(“car”) , Spring并不会返回CarFactoryBean 而是返回car

如果希望返回CarFactoryBean 那就 getBean(“&car”)

### 应用场景

当bean的产生比较复杂的时候，常用的xml配置等方式已经无法描述出bean的信息，需要代理bean的生成方式，在mybatis，dubbo等框架中，都用到了FactoryBean

myabtis的`org.mybatis.spring.SqlSessionFactoryBean`:

``` 
    <bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
        <property name="dataSource" ref="dataSource"/>
        <!-- 自动扫描mapping.xml文件 -->
        <property name="mapperLocations" value="classpath:mapper/*.xml"></property>
    </bean>
    
    

public class SqlSessionFactoryBean implements FactoryBean<SqlSessionFactory>, InitializingBean, ApplicationListener<ApplicationEvent> {
    private static final Log LOGGER = LogFactory.getLog(SqlSessionFactoryBean.class);
...
public SqlSessionFactory getObject() throws Exception {
        if (this.sqlSessionFactory == null) {
            this.afterPropertiesSet();
        }
 
        return this.sqlSessionFactory;
    }
...
}
    
```
## 参考
- [what is a FactoryBean](https://spring.io/blog/2011/08/09/what-s-a-factorybean)
- [FactoryBean的实现原理与作用](https://blog.csdn.net/u013185616/article/details/52335864)