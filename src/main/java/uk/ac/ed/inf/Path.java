package uk.ac.ed.inf;

import java.util.ArrayList;

public class Path {
    private ArrayList<LongLat> path;
    private int moves;


    public Path(ArrayList<LongLat> path , int moves){
        this.path = path;
        this.moves = moves;
    }

    public int getMoves(){
        return this.moves;
    }
    public ArrayList<LongLat> getPath(){
        return this.path;
    }



    public void setMoves(int moves){
        this.moves = moves;
    }
    public void addToPath(ArrayList<LongLat> path){
        for(LongLat longLat:path){
            this.path.add(longLat);
        }
    }

}
