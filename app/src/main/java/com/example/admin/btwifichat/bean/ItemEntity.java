package com.example.admin.btwifichat.bean;

/**
 * Created by admin on 2017/4/13.
 */

public class ItemEntity {

    private String name,message;
    private int id=-1;

    public ItemEntity() {
    }

    public ItemEntity(String name, String message, int id) {
        this.name = name;
        this.message = message;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "ItemEntity{" +
                "name='" + name + '\'' +
                ", message='" + message + '\'' +
                ", id=" + id +
                '}';
    }
}
