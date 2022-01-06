package uk.ac.ed.inf;

public class CoordinatesFromThreeWords {
    String country;
    Square square;
    public class Square{
        Coordinates southwest;
        Coordinates northeast;
    }
    String nearestPlace;
    Coordinates coordinates;
    public static class Coordinates{
        double lng;
        double lat;
        public Coordinates(double lng, double lat){
            this.lng = lng;
            this.lat =lat;
        }
    }
    String words;
    String language;
    String map;



}
