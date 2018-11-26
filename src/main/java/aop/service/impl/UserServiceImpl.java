/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package aop.service.impl;

import aop.DO.User;
import aop.service.UserService;

/**
 *
 *
 * @author HaoBin
 * @version $Id: UserServiceImpl.java, v0.1 2018/11/26 22:44 HaoBin 
 */
public class UserServiceImpl implements UserService {

    public User createUser(String firstName, String lastName, int age) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setAge(age);
        return user;
    }

    public User queryUser() {
        User user = new User();
        user.setFirstName("test");
        user.setLastName("test");
        user.setAge(20);
        return user;
    }
}