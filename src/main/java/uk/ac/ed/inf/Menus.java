package uk.ac.ed.inf;

import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;


public class Menus {
    private String name;
    private String port;
    private static final HttpClient client = HttpClient.newHttpClient();
    private ArrayList<Shop> shops;


    public Menus(String name, String port ) throws IOException, InterruptedException {
        this.name = name;
        this.port = port;

        String server = "http://"+ this.name +":" + this.port + "/Menus/menus.json"; // that is the format of the URI used to create our client
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(server)).build(); //create a request
        Type listType = new TypeToken<ArrayList<Shop>>() {}.getType();
        HttpResponse<String> response = client.send(request , BodyHandlers.ofString()); // the response body will be saved to shops for later access
        if(response.statusCode() !=200){
            System.out.println("Please make sure the name and the port of the server you are trying to access matches the server you opened.");
        }

        this.shops = new Gson().fromJson(response.body() , listType); //
    }


    protected int getDeliveryCost(String... a){ // returns delivery cost including 50p for delivery

        int total_cost = 50; // initialize to 50 due to the delivery charge
        int countLocatedItems = 0;
        for(Shop shop:this.shops){// for loop that iterates through all shops
            for(Shop.Item_and_cost item_and_cost: shop.menu){ // for loop that iterates through each item inside the shop class
                for(String order_item : a){ // for loop that will iterate through all order items to check if they are equal to the one we are looking in the shop
                    if (order_item.equals(item_and_cost.getItem())){
                        countLocatedItems+=1;
                        total_cost+= item_and_cost.getPence();


                    }
                }
                if(countLocatedItems ==4){ //if all 4 items have been found we need to exit without executing any other iterations in for loop
                    break;
                }
            }
            if(countLocatedItems ==4){
                break;
            }
        }

        return total_cost;
    }

    protected HttpClient getClient(){
        return this.client;
    }
    protected String getName(){
        return this.name;
    }
    protected String getPort(){
        return this.port;
    }
    protected ArrayList<Shop> getShops(){
        return this.shops;
    }
}
