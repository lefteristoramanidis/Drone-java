package uk.ac.ed.inf;


import java.io.IOException;

import java.sql.*;
import java.util.ArrayList;



public class App{

    public static void main( String[] args ) throws IOException, InterruptedException, SQLException {
        String webPort = "9898";
        String dataPort = "9876";
        String dataServer = "jdbc:derby://" + "localhost" + ":" + dataPort + "/derbyDB";
        ArrayList<java.sql.Date> deliveryDates = new ArrayList<Date>();

        Connection conn = DriverManager.getConnection(dataServer);
        String query = "select deliveryDate from orders";
        PreparedStatement ps = conn.prepareStatement(query);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            if (!deliveryDates.contains(rs.getDate("deliveryDate"))) {
                Date date = rs.getDate("deliveryDate");
                deliveryDates.add(date);

            }
        }
        java.sql.Date date = deliveryDates.get(145);

            Menus menus = new Menus("localhost", webPort);
            DataAccess dataAccess = new DataAccess("localhost", webPort, menus, date);


            System.out.println("DELIVERY DATE  " + date.toString());
            WebAccess webAccess = new WebAccess("localhost", "9898", dataAccess);

            Drone drone = new Drone(webAccess, (java.sql.Date) date);


        }

}
