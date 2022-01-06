package uk.ac.ed.inf;

//inside menus.json we have a lot of shop offering different menus. The class Shop hold information for each one of them.
import java.util.ArrayList;

public class Shop {
    private String name; //name of the shop
    private String location; // location format will be based on What3Words app
    ArrayList<Item_and_cost> menu;//each shop has its own menu consisted of different items paired with their costs. The Item_and_cost class
    //holds together an item and the associated value. The menu field is a list of Item_and_cost objects



    public static class Item_and_cost{
        private String item;
        private int pence; // make them private for security reasons. We dont want hackers to modify the items and costs. To access them now we introduce Getters
        public String getItem() {
            return item;
        }

        public int getPence() {
            return pence;
        }


    }
    public String getName(){
        return this.name;
    }
    public String getLocation(){
        return this.location;
    }

}
