/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package aop.service.impl;

import aop.DO.Order;
import aop.service.OrderService;

/**
 * @author HaoBin
 * @version $Id: OrderServiceImpl.java, v0.1 2018/11/26 22:46 HaoBin
 */
public class OrderServiceImpl implements OrderService {


    public Order createOrder(String username, String product) {
        Order order = new Order();
        order.setUsername(username);
        order.setProduct(product);
        return order;
    }

    public Order queryOrder(String username) {
        Order order = new Order();
        order.setUsername("test");
        order.setProduct("test");
        return order;
    }
}