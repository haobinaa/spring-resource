package importbean;

import importbean.beans.BlueBean;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @Author HaoBin
 * @Create 2019/12/26 10:15
 * @Description: ImportSelector 导入 bean
 **/
public class BlueImportSelect implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        return new String[] {BlueBean.class.getName()};
    }
}
