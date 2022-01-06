package uk.ac.ed.inf;

import java.sql.Date;

public class Order {
    private String orderNo;
    private java.sql.Date date;
    private String matric;
    private String deliverTo;

    public Order(String orderNo, Date date, String matric, String deliverTo ){
        this.orderNo = orderNo;
        this.date = date;
        this.matric = matric;
        this.deliverTo =  deliverTo;
    }


    protected String getOrderNo() {
        return orderNo;
    }

    protected void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    protected Date getDate() {
        return date;
    }


}
