package design_pattern.pipeline;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.sound.midi.SoundbankResource;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * spring 中实现管道(责任链)模式
 * 应对代码中有多个子步骤的情况(参考 netty pipeline)
 *
 *
 * @Date 2021/2/19 9:51 下午
 * @author: leobhao
 */
public class SpringPipelineMain {

    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("design_pattern.xml");
        InstanceBuildContext data = createPipelineData();
        PipelineExecutor executor = context.getBean(PipelineExecutor.class);
        boolean success = executor.acceptSync(data);
        if (success) {
            System.out.println("创建成功");
            return;
        }
        System.out.println("创建失败");
        return;
    }

    public static InstanceBuildContext createPipelineData() {
        InstanceBuildContext data = new InstanceBuildContext();
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("key", "value");
        data.setFormInput(inputData);
        return data;
    }
}
