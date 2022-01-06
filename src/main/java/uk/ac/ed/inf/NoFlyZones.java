package uk.ac.ed.inf;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;

import com.mapbox.geojson.Polygon;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


public class NoFlyZones {

    private String name;
    private String port;
    private static final HttpClient client = HttpClient.newHttpClient();
    public org.locationtech.jts.geom.Polygon[] jtsPolygon;
    public com.mapbox.geojson.Polygon[] geoJsonPolygon;



    public NoFlyZones(String name, String port) throws IOException, InterruptedException{
        this.name = name;
        this.port = port;
        String server = "http://"+ this.name +":" + this.port + "/buildings/no-fly-zones.geojson";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(server)).build();
        HttpResponse<String> response = this.client.send(request , HttpResponse.BodyHandlers.ofString());
        if(response.statusCode() !=200){
            System.out.println("Please make sure the name and the port of the server you are trying to access matches the server you opened.");
        }

        FeatureCollection fc = FeatureCollection.fromJson(response.body());
        GeometryFactory geometryFactory = new GeometryFactory();
        this.geoJsonPolygon = new com.mapbox.geojson.Polygon[fc.features().size()];
        this.jtsPolygon = new org.locationtech.jts.geom.Polygon[fc.features().size()];
        int j = 0;
        for(Feature f : fc.features()){
            var geometry = f.geometry();
            var pol = (Polygon)geometry;
            this.geoJsonPolygon[j] = pol;




            var points = pol.coordinates().get(0);
            var jtsPoints = new Coordinate[points.size()];
            for(int i =0;i<points.size();i++){
                var currentPoint = points.get(i);
                var coordinates = new Coordinate(currentPoint.longitude(), currentPoint.latitude());
                jtsPoints[i] = coordinates;
            }
            this.jtsPolygon[j]=(geometryFactory.createPolygon(jtsPoints));
            j++;

        }

    }
}
