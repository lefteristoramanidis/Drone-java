package uk.ac.ed.inf;

import com.google.gson.Gson;

import javax.xml.crypto.Data;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.util.ArrayList;

public class DataAccess {

    String name;
    String port;
    Connection conn;
    Statement statement;
    DatabaseMetaData databaseMetaData;
    Date date;

    public Menus menus;
    public ResultSet resultSet;
    public DataAccess(String name, String port, Menus menus, Date date) throws SQLException {
        this.name = name;
        this.port = port;
        this.menus = menus;
        this.date = date;
        String dataServer = "jdbc:derby://" + this.name +":"+ this.port +"/derbyDB";
        try {
            this.conn = DriverManager.getConnection(dataServer);
            this.statement = conn.createStatement();//used to run sql commands
            this.databaseMetaData = this.conn.getMetaData();
            this.resultSet = databaseMetaData.getTables(null,null,"ORDERS",null);
            while(resultSet.next()){

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createNewTable() throws SQLException {
        PreparedStatement statement =this.conn.prepareStatement("create table flightpath(orderNo char(8)," +
                "fromLongitude double," +
                "fromLatitude double," +
                "angle integer," +
                "toLongitude double," +
                "toLatitude double)");
        statement.execute();
    }
    public void insertRow(String orderNo, LongLat from, int angle, LongLat to) throws SQLException {
        PreparedStatement ps = this.conn.prepareStatement("INSERT INTO flightpath (orderNo,fromLongitude , fromLatitude, angle, toLongitude, toLatitude)" +
                "VALUES((?),(?),(?),(?),(?),(?));" );
        double fromLongitude = from.lng;
        double fromLatitude = from.lat;
        double toLongitude = to.lng;
        double toLatitude = to.lat;
        ps.setString(1,orderNo);
        ps.setDouble(2,fromLongitude);
        ps.setDouble(3,fromLatitude);
        ps.setInt(4,angle);
        ps.setDouble(5,toLongitude);
        ps.setDouble(6,toLatitude);
        ps.execute();
    }





    public ArrayList<String> getItemsFromOrderNo(String orderNo) throws SQLException {
        ArrayList<String> items = new ArrayList<String>();
        PreparedStatement ps = this.conn.prepareStatement("select * from orderDetails where orderNo=(?)");
        ps.setString(1,orderNo);
        ResultSet rs = ps.executeQuery();
        while(rs.next()){
            items.add(rs.getString("item"));
        }

        return items;

    }

    protected void writeFile(String writeString) throws IOException {
        String filename = "drone-"+ this.date.getDay()+ "-"+this.date.getMonth()+"-"+
                this.date.getYear()+".geojson";

        var writer = new FileWriter(filename);
        writer.append(writeString);
        writer.close();
    }

    public LongLat getDeliveryPointLongLatsFromOrderNumber(String orderNo) throws SQLException, IOException, InterruptedException {
        String deliveryPointString ;
        System.out.println("Enter!!!!!!!!!!!!!!!");
        LongLat deliveryPoint = null;
        ArrayList<LongLat> deliveryLongLats = new ArrayList<>();


        PreparedStatement ps = this.conn.prepareStatement("select * from orders where orderNo=(?)");
        ps.setString(1,orderNo);
        ResultSet rs = ps.executeQuery();
        while(rs.next()){
            deliveryPointString=(rs.getString("deliverTo"));
            deliveryPoint = this.convertCoordinatesToLongLat(this.coordinatesFromW3W(deliveryPointString));

        }

        return deliveryPoint;
    }
    public ArrayList<Shop> getShopsFromItems(ArrayList<String> items){
        ArrayList<Shop> shops = new ArrayList<Shop>();
        int countLocatedItems = 0;
        for(Shop shop:this.menus.getShops()){// for loop that iterates through all shops
            for(Shop.Item_and_cost item_and_cost: shop.menu){ // for loop that iterates through each item inside the shop class
                for(String order_item : items){ // for loop that will iterate through all order items to check if they are equal to the one we are looking in the shop
                    if (order_item.equals(item_and_cost.getItem())){
                        if(!shops.contains(shop)){
                            shops.add(shop);
                        }
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
        if(shops.size()==1){
            System.out.println("the size is 1");
        }else if(shops.size()==2){
            System.out.println("The size is 2");
        }
        return shops;
    }



    public ArrayList<String> getOrderNoFromDates(java.sql.Date date) throws SQLException {

        ArrayList<String> items= new ArrayList<String>();
        PreparedStatement ps = this.conn.prepareStatement("select * from orders where deliveryDate=(?)");
        ps.setDate(1,date);
        ResultSet rs = ps.executeQuery();
        ArrayList<String> orderNumbers = new ArrayList<String>();

        while(rs.next()){
            orderNumbers.add(rs.getString("orderNo"));
//            System.out.println(rs.getString("orderNo"));

        }
        return orderNumbers;

    }

    public ArrayList<LongLat> getLongLatsForShopsOfOrder(String orderNo) throws SQLException, IOException, InterruptedException {
        ArrayList<LongLat> longLatsOfShops = new ArrayList<>();
        ArrayList<String> items = getItemsFromOrderNo(orderNo);
        ArrayList<Shop> shops = getShopsFromItems(items);
        for(Shop shop:shops){
            CoordinatesFromThreeWords.Coordinates coos = coordinatesFromW3W(shop.getLocation());
            LongLat lngLat = new LongLat(coos.lng,coos.lat);
            longLatsOfShops.add(lngLat);
        }

        return longLatsOfShops;
    }

    public CoordinatesFromThreeWords.Coordinates coordinatesFromW3W(String threewordsNotSplit) throws IOException, InterruptedException {
        String[] threewords = threewordsNotSplit.split("\\.");
        for(String s:threewords){
            System.out.println(s);
        }
        String threeWordsFile = "http://"+ this.menus.getName() +":" + this.menus.getPort() + "/words/"+threewords[0]+"/"+
                threewords[1]+"/"+threewords[2]+"/details.json";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(threeWordsFile)).build();
        HttpResponse<String> response = menus.getClient().send(request , HttpResponse.BodyHandlers.ofString());
        CoordinatesFromThreeWords coordinates = new Gson().fromJson(response.body(), CoordinatesFromThreeWords.class);

        System.out.println(coordinates.coordinates.lng);

        return coordinates.coordinates;
    }

    public LongLat convertCoordinatesToLongLat(CoordinatesFromThreeWords.Coordinates coordinates){
        LongLat longLat = new LongLat(coordinates.lng,coordinates.lat);
        return longLat;
    }
}
