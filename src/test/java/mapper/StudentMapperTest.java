package mapper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import mybatis.mapper.StudentMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Before;
import org.junit.Test;

/**
 * @Author HaoBin
 * @Create 2020/5/14 15:57
 * @Description: mapper test
 **/
public class StudentMapperTest {


    private SqlSessionFactory factory;

    @Before
    public void setUp() throws Exception {
        factory = new SqlSessionFactoryBuilder().build(Resources.getResourceAsReader("mybatis-config.xml"));
    }

    @Test
    public void findOneSql() {
        SqlSession sqlSession = factory.openSession(true);
        StudentMapper studentMapper = sqlSession.getMapper(StudentMapper.class);
        System.out.println(studentMapper.findOne(1));
    }

    /**
     * 查询参数解析测试
     */
    @Test
    public void paramNameResolver() throws NoSuchMethodException,NoSuchFieldException,IllegalAccessException {
        Configuration config = new Configuration();
        // 如果不设置为false，并且没有 @Param注解， 那么参数名称则是 arg0,arg1... 形式
        config.setUseActualParamName(false);
        Method method = StudentMapper.class.getMethod("findOne", Integer.class);
        ParamNameResolver resolver = new ParamNameResolver(config, method);
        Field field = resolver.getClass().getDeclaredField("names");
        field.setAccessible(true);
        Object id = field.get(resolver);
        System.out.println(id);
    }
}
