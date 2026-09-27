package com.library.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "employee")
public class Employee extends Person {

    private Double salary;

    private String department;

    public Employee() {
    }

    public Employee(String name, String email, Double salary, String department) {
        super(name, email);
        this.salary = salary;
        this.department = department;
    }

    public Double getSalary() {
        return salary;
    }

    public void setSalary(Double salary) {
        this.salary = salary;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
