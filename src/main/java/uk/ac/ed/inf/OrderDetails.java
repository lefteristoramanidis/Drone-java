package uk.ac.ed.inf;

import java.util.ArrayList;

public class OrderDetails {

    private String orderNo;
    private ArrayList<String> items;

    public OrderDetails(String orderNo ,  ArrayList<String> items){
        this.orderNo = orderNo;
        this.items = items;
    }


}
