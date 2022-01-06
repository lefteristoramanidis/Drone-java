package uk.ac.ed.inf;

import java.util.ArrayList;

public class TotalOrder {

    private Order order;
    private OrderDetails orderDetails;
    private ArrayList<LongLat> shopsToVisit;
    private LongLat deliveryPoint;
    private int deliveryCost;
    private ArrayList<Integer> angles;

    public TotalOrder(Order order, OrderDetails orderDetails, ArrayList<LongLat> shopsToVisit,LongLat deliveryPoint,int deliveryCost){
        this.order = order;
        this.orderDetails =orderDetails;
        this.shopsToVisit = shopsToVisit;
        this.deliveryPoint = deliveryPoint;
        this.deliveryCost = deliveryCost;

    }

    public Order getOrder() {
        return order;
    }


    public ArrayList<LongLat> getShopsToVisit() {
        return shopsToVisit;
    }


    public LongLat getDeliveryPoint() {
        return deliveryPoint;
    }



    public int getDeliveryCost() {
        return deliveryCost;
    }


}
