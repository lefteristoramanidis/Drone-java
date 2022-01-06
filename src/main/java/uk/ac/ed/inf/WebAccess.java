package uk.ac.ed.inf;

import com.google.gson.Gson;
import com.mapbox.geojson.FeatureCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class WebAccess {
    private String name;
    private String port;
    public Menus menus;
    public DataAccess dataAccess;
    public NoFlyZones noFlyZones;
    private Landmarks landmarks;

    public WebAccess(String name, String port, DataAccess dataAccess) throws IOException, InterruptedException, SQLException {
        this.name = name;
        this.port = port;
        this.menus = new Menus(name, port);
        this.dataAccess = dataAccess;
        this.noFlyZones = new NoFlyZones(name, port);
        this.landmarks = new Landmarks(name, port);

    }

    public NoFlyZones getNoFlyZones() {
        return this.noFlyZones;
    }

}
