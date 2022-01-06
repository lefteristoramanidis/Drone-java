package uk.ac.ed.inf;
import org.locationtech.jts.geom.Coordinate;

import static java.lang.Math.PI;
import static java.lang.Math.sqrt;

public class LongLat {

    public double lng;
    public double lat;
//    public int moves_remaining;

    public LongLat(double longitude, double latitude) {
        this.lng = longitude;
        this.lat = latitude;
//        this.moves_remaining = 1500;
    }

    public boolean isConfined(){ //returns true if we are in the confined area and false if we are out of it
        boolean longConfined = this.lng>=(-3.192473) && this.lng<= -3.184319;
        boolean latConfined = this.lat>= 55.942617 && this.lat<=55.946233;
        return longConfined && latConfined;
    }

    public double distanceTo(LongLat destination){
        double differenceInLong = this.lng - destination.lng;
        double differenceInLat = this.lat - destination.lat;
        return sqrt(differenceInLat*differenceInLat + differenceInLong*differenceInLong);
    }

    public boolean closeTo(LongLat secondPoint){

        return distanceTo(secondPoint)< 0.00015;
    }

    public LongLat nextPosition(int angle){
        if(angle == -999){
            return new LongLat(this.lng, this.lat);
        }else if(angle % 10 != 0){
            int a = angle%10;
            if(a>=5){
                angle = angle+10-a;
            }else{
                angle-=a;
            }
        }

        double newLongitude = this.lng + 0.00015* Math.cos(angle*PI/180);
        double newLatitude  = this.lat + 0.00015*Math.sin(angle* PI/180);
        return new LongLat(newLongitude, newLatitude);


    }


    public Coordinate longLatToCoordinate(){
        Coordinate coordinate = new Coordinate(this.lng, this.lat);
        return coordinate;
    }
    public LongLat extendLongLat(){
        return new LongLat(this.lng*10, this.lat*10);
    }


}
