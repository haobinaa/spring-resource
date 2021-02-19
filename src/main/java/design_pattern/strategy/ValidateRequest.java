package design_pattern.strategy;

import java.util.Map;

/**
 * 需要校验的对象
 * @Date 2021/2/19 9:14 下午
 * @author: leobhao
 */
public class ValidateRequest {

    public ValidateRequest() {
    }

    public ValidateRequest(String type) {
        this.type = type;
    }

    /**
     * 提交的类型
     */
    private String type;

    /**
     * 需要校验的数据
     */
    private Map<String, Object> properties;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}
