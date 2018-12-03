/**
 * BrandBigData.com Inc. Copyright (c) 2018 All Rights Reserved.
 */
package aop.DO;

/**
 *
 *
 * @author HaoBin
 * @version $Id: User.java, v0.1 2018/11/26 22:43 HaoBin 
 */
public class User {

    private String firstName;

    private String lastName;

    private int age;

    private String address;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"firstName\":\"")
                .append(firstName).append('\"');
        sb.append(",\"lastName\":\"")
                .append(lastName).append('\"');
        sb.append(",\"age\":")
                .append(age);
        sb.append(",\"address\":\"")
                .append(address).append('\"');
        sb.append('}');
        return sb.toString();
    }
}