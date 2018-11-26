/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package aop.service;

import aop.DO.Order;

/**
 *
 *
 * @author HaoBin
 * @version $Id: OrderService.java, v0.1 2018/11/26 22:46 HaoBin 
 */
public interface OrderService {
    Order createOrder(String username, String product);

    Order queryOrder(String username);
}
