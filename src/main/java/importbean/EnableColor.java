package importbean;

import importbean.beans.RedBean;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/**
 * @Author HaoBin
 * @Create 2019/12/26 9:58
 * @Description: @import 手动导入 bean 的方式
 * 1. 导入普通类 red
 * 2. 导入配置类 YellowRegisterConfiguration
 * 3. ImportSelector 导入
 * 4. ImportBeanDefinitionRegistrar 导入
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import({RedBean.class, YellowRegisterConfiguration.class, BlueImportSelect.class, BlackImportBeanDefinitionRegister.class})
public @interface EnableColor {

}
