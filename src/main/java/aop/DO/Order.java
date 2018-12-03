/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package aop.DO;

/**
 *
 *
 * @author HaoBin
 * @version $Id: Order.java, v0.1 2018/11/26 22:45 HaoBin 
 */
public class Order {
    private String username;

    private String product;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"username\":\"")
                .append(username).append('\"');
        sb.append(",\"product\":\"")
                .append(product).append('\"');
        sb.append('}');
        return sb.toString();
    }
}