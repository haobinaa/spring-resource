/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package aspectJ;

/**
 *
 *
 * @author HaoBin
 * @version $Id: Account.java, v0.1 2018/12/5 23:37 HaoBin 
 */
public class Account {

    int balance = 20;

    public boolean pay(int amount) {
        if (balance < amount) {
            return false;
        }
        balance -= amount;
        return true;
    }
}