package com.library.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer")
public class Customer extends Person {

    private String address;

    private String phone;

    public Customer() {
    }

    public Customer(String name, String email, String address, String phone) {
        super(name, email);
        this.address = address;
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
