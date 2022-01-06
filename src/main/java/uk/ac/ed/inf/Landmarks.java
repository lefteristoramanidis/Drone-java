package uk.ac.ed.inf;

import com.mapbox.geojson.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;

public class Landmarks {

    private String name;
    private String port;
    private static final HttpClient client = HttpClient.newHttpClient();
    private Coordinate[] jtsPoints ;

    public Landmarks(String name, String port) throws IOException, InterruptedException {
        this.name = name;
        this.port = port;
        String server = "http://"+ this.name +":" + this.port + "/buildings/landmarks.geojson";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(server)).build();
        HttpResponse<String> response = this.client.send(request , HttpResponse.BodyHandlers.ofString());
        if(response.statusCode() !=200){
            System.out.println("Please make sure the name and the port of the server you are trying to access matches the server you opened.");
        }

        FeatureCollection fc = FeatureCollection.fromJson(response.body());


        Geometry geometry;
        this.jtsPoints = new Coordinate[fc.features().size()];
        int j = 0;

        for(Feature f : fc.features()) {
            geometry = f.geometry();
            var point = (Point)geometry;
            this.jtsPoints[j] =new Coordinate(point.longitude(),point.latitude());
            j++;

        }


    }
    public Coordinate[] getCoordinates(){
        return this.jtsPoints;
    }

}
