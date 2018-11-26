/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package aop.service;

import aop.DO.User;

/**
 *
 *
 * @author HaoBin
 * @version $Id: UserService.java, v0.1 2018/11/26 22:42 HaoBin 
 */
public interface UserService {

    User createUser(String firstName, String lastName, int age);

    User queryUser();
}