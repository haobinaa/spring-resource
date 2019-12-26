package importbean;

import importbean.beans.BlackBean;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @Author HaoBin
 * @Create 2019/12/26 10:19
 * @Description: ImportBeanRegister 注册 Bean
 **/
public class BlackImportBeanDefinitionRegister implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata,
        BeanDefinitionRegistry registry) {
        registry.registerBeanDefinition("black", new RootBeanDefinition(BlackBean.class));
    }
}
