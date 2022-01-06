package uk.ac.ed.inf;

import com.mapbox.geojson.*;
import org.locationtech.jts.geom.Coordinate;

import org.locationtech.jts.geom.GeometryFactory;

import org.locationtech.jts.geom.LineString;


import java.io.IOException;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;



public class Drone {

    private WebAccess webAccess;

    private GeometryFactory geom = new GeometryFactory();
    private LongLat location ;
    private int moves_remaining = 1500;

    private Path pathOfDrone = new Path(new ArrayList<LongLat>(),0);
    private Date date;
    private LongLat appletonTower = new LongLat(-3.1869,	55.9445);
    private ArrayList<TotalOrder> totalOrders= new ArrayList<>();
    private ArrayList<Integer> angles;
    public Drone( WebAccess webAccess, Date date) throws SQLException, IOException, InterruptedException {
        this.location =new LongLat(-3.186874,55.944494);
        this.webAccess = webAccess;
        this.date = date;

        ArrayList<String> allOrderNumbers = webAccess.dataAccess.getOrderNoFromDates(this.date);
        for(String orderNo: allOrderNumbers){
            this.totalOrders.add(getTotalOrder(orderNo));
        }
        System.out.println("AMOUNT OF ORDERS IS  :  " + this.totalOrders.size());
        start();
    }

    /**Main algorithm, and writing to the output files*/
    protected void start() throws IOException, InterruptedException, SQLException {
        ArrayList<Integer> angles = new ArrayList<>();
        ArrayList<TotalOrder> totalOrders  = this.totalOrders;
        ArrayList<String> orderNumbersForFlightPath = new ArrayList<>();
        LongLat lastPositionFromPreviousOrder = new LongLat(0,0);
        while(!this.totalOrders.isEmpty()){
            TotalOrder totalOrder = findBestOrder(this.totalOrders);
            if(isTotalOrderPossible(totalOrder)){
                System.out.println("MOVES REQUQIRED  : "+getBestPathForTotalOrder(totalOrder).getMoves());
                this.moves_remaining-=getBestPathForTotalOrder(totalOrder).getMoves();
                ArrayList<LongLat> path = getBestPathForTotalOrder(totalOrder).getPath();
                if(!(angles.isEmpty())){
                    angles.add(calculateAngle(lastPositionFromPreviousOrder,path.get(0)));
                }
                this.pathOfDrone.addToPath(path);

                for(int i = 0;i<path.size() ;i++){
                    orderNumbersForFlightPath.add(totalOrder.getOrder().getOrderNo());
                    if(!(i==path.size()-1)){
                        angles.add(calculateAngle(path.get(i),path.get(i+1)));
                    }

                }
                lastPositionFromPreviousOrder = path.get(path.size()-1);
                this.totalOrders.remove(totalOrder);
                int lastIndex = this.pathOfDrone.getPath().size()-1;
                this.location = this.pathOfDrone.getPath().get(lastIndex);
            }else {
                this.totalOrders.remove(totalOrder);
            }

        }
        int sizeBeforeReturningToAppleton = this.pathOfDrone.getPath().size();

        Path goToAppleton = getBestPathForDestination(this.location, this.appletonTower);

        this.pathOfDrone.addToPath(goToAppleton.getPath());
        var ps = webAccess.dataAccess.conn.prepareStatement("drop table flightpath");
        ps.execute();
        this.webAccess.dataAccess.createNewTable();
        for(int i = 0;i<(sizeBeforeReturningToAppleton-1);i++){
            if(i==(sizeBeforeReturningToAppleton-1)){
                this.webAccess.dataAccess.insertRow(orderNumbersForFlightPath.get(i),this.pathOfDrone.getPath().get(i),angles.get(i),this.pathOfDrone.getPath().get(i+1));
            }
        }for(int i = 0;i<goToAppleton.getPath().size()-1;i++){
            if(i==(goToAppleton.getPath().size()-1)){
                int angle = calculateAngle(goToAppleton.getPath().get(i), goToAppleton.getPath().get(i+1));
                this.webAccess.dataAccess.insertRow("FINISHED",goToAppleton.getPath().get(i),angles.get(i),goToAppleton.getPath().get(i+1));
            }
        }

        this.moves_remaining-=goToAppleton.getMoves();
        System.out.println("MOVES TO APPLETON  : " + goToAppleton.getMoves());


        System.out.println("AMOUNT OF MOVES REMAINING  :  "+this.moves_remaining);

        ps = this.webAccess.dataAccess.conn.prepareStatement("select * from flightpath");
        ResultSet rs = ps.executeQuery();
        while(rs.next()){
            String from = "Long from  "+(rs.getString("fromLongitude")) + "  Lat from   "+(rs.getDate("fromLatitude"));
            String to = "Long To  "+(rs.getString("toLongitude")) + "  Lat To   "+(rs.getDate("toLatitude"));
//            System.out.println(rs.getString("fromLatitude"));
            System.out.println(from);
            System.out.println(to);
        }


        this.webAccess.dataAccess.writeFile(createGeojsonMap());



    }

    /**Creates json string used to map
     * */


   public String createGeojsonMap() throws InterruptedException{
        ArrayList<Point> points = new ArrayList<>();

        for(LongLat longLat : this.pathOfDrone.getPath()){
            var point = Point.fromLngLat(longLat.lng,longLat.lat);
            points.add(point);

        }

        com.mapbox.geojson.LineString lineString = com.mapbox.geojson.LineString.fromLngLats(points);
        ArrayList<Feature> features = new ArrayList<Feature>();
        Feature line = Feature.fromGeometry((Geometry) lineString);
        features.add(line);

        FeatureCollection fc = FeatureCollection.fromFeatures(features);
        String jsonString = fc.toJson();
        return jsonString;
    }




    /**@[param] [Total orders remaining]
     * Finds the best ration delivery cost over moves required
     * @return the best ration*/
    public TotalOrder findBestOrder(ArrayList<TotalOrder> totalOrders){
        TotalOrder totalOrder = totalOrders.get(0);
        double ratio = calculateRatio(totalOrders.get(0));
        for(TotalOrder totalOrder1:totalOrders){
            if(calculateRatio(totalOrder1)>=ratio){
                totalOrder=totalOrder1;
                ratio = calculateRatio(totalOrder1);
            }
        }
        return totalOrder;

    }

    public double calculateRatio(TotalOrder totalOrder){
        double ratio = (totalOrder.getDeliveryCost())/(getBestPathForTotalOrder(totalOrder).getMoves());
        return ratio;
    }

    public GeometryFactory getGeom() {
        return this.geom;
    }

    /**@param start , the starting location
     * @param destination , the desired location
     * @return if the straight line between the two points intersects with no fly zones*/
    public boolean intersectsWithNoFlyZones(LongLat start,
                                            LongLat destination){

//
        Coordinate initial = start.longLatToCoordinate();//make it jts coordinates
        Coordinate finalCo = destination.longLatToCoordinate();
        Coordinate[] lineCoordinates = new Coordinate[]{initial,finalCo};
        LineString line = getGeom().createLineString(lineCoordinates);

        for(org.locationtech.jts.geom.Polygon nfz: this.webAccess.getNoFlyZones().jtsPolygon){
            if(line.intersects(nfz)){
                return true;
            }

        }
        return false;
    }
    public boolean isPathBlocked(LongLat position,LongLat destination){
        while(position.isConfined()){
            int angle = calculateAngle(position,destination);
            LongLat nextMove = position.nextPosition(angle);
            if(position.closeTo(destination)){
                return false;
            }
            if(intersectsWithNoFlyZones(position,nextMove)){
                return true;
            }
            position = nextMove;
        }
        return false;
    }

    public boolean isBlocked(LongLat starting, int angle){//find if a straight line from one point to the other intersects with no fly zones
            LongLat ending =starting;
            while(ending.isConfined()){
                ending = ending.nextPosition(angle);
                if(intersectsWithNoFlyZones(starting,ending)){
                    return true;
                }

            }
            return false;
    }


    public int calculateAngle(LongLat starting, LongLat ending){
        double xDif = ending.lng - starting.lng;
        double yDif = ending.lat - starting.lat;
        double angle = Math.toDegrees(Math.atan2(yDif, xDif));
        if(angle % 10 != 0){ //  if the angle is not a multiple of 10 then we round it and the package will again be droped off close
            angle = Math.round(angle/10)*10;
        }
        int angleInt = (int)angle;
        angleInt = correctAngle(angleInt);
        return angleInt;
    }

    public Path antiClockwise(LongLat position, LongLat destination ){
        int totalMoves = 0;
        ArrayList<LongLat> path = new ArrayList<>();
        path.add(position);
        int angleToDestination = calculateAngle(position,destination);
        int angleOfCurrentMove = turnRightUntilNotBlocked(position,angleToDestination);

        while(isPathBlocked(position,destination)){

            if(!isLeftTurnPossible(position,angleOfCurrentMove)){

                position = position.nextPosition(angleOfCurrentMove);

                path.add(position);
                totalMoves++;
                angleToDestination = calculateAngle(position,destination);
            }else{
                angleOfCurrentMove = turnLeftUntilBlocked(position,angleOfCurrentMove);

            }
        }
        while(!position.closeTo(destination)){

            position = position.nextPosition(angleToDestination);

            totalMoves++;
            path.add(position);
            angleToDestination = calculateAngle(position,destination);
        }
        totalMoves++;//for hovering
        Path finalPath =  new Path(path,totalMoves);
        return finalPath;

    }

    public Path clockwise(LongLat position, LongLat destination ){
        int totalMoves = 0;

        ArrayList<LongLat> path = new ArrayList<>();
        path.add(position);
        int angleToDestination = calculateAngle(position,destination);
        int angleOfCurrentMove = turnLeftUntilNotBlocked(position,angleToDestination);

        while(isPathBlocked(position,destination)){

            if(!isRightTurnPossible(position,angleOfCurrentMove)){

                position = position.nextPosition(angleOfCurrentMove);
                totalMoves++;
                path.add(position);
                angleToDestination = calculateAngle(position,destination);
            }else{
                angleOfCurrentMove = turnRightUntilBlocked(position,angleOfCurrentMove);

            }
        }
        while(!position.closeTo(destination)){
            position = position.nextPosition(angleToDestination);
            path.add(position);
            totalMoves++;
            angleToDestination = calculateAngle(position,destination);
        }
        totalMoves++;
        Path finalPath = new Path(path,totalMoves);
        return finalPath;
    }
    public Path getBestPathForDestination(LongLat starting, LongLat ending){
        Path clockwise = clockwise(starting,ending);
        Path anticlockwise = antiClockwise(starting,ending);
        int movesClockwise = clockwise.getMoves();
        int movesAntiClockwise = anticlockwise.getMoves();
        if(movesAntiClockwise<=movesClockwise){
            return anticlockwise;
        }else{
            return clockwise;
        }
    }

    public Path getBestPathForTotalOrder(TotalOrder totalOrder){
        if(totalOrder.getShopsToVisit().size()==2){
            Path tryFirst = goToFirstShopFirst(totalOrder);
            Path trySecond = goToSecondShopFirst(totalOrder);
            if(tryFirst.getMoves()<trySecond.getMoves()){
                return tryFirst;
            }else {
                return trySecond;
            }

        }else{
            Path toShop = getBestPathForDestination(this.getLocation(),totalOrder.getShopsToVisit().get(0));
            Path toDeliveryPoint = getBestPathForDestination(totalOrder.getShopsToVisit().get(0), totalOrder.getDeliveryPoint());
            Path totalPath = toShop;
            totalPath.addToPath(toDeliveryPoint.getPath());
            int moves = toShop.getMoves();
            moves+=toDeliveryPoint.getMoves();
            totalPath.setMoves(moves);
            return totalPath;
        }
    }
    public Path goToFirstShopFirst(TotalOrder totalOrder){
        Path goToFirstShop = getBestPathForDestination(this.getLocation(),totalOrder.getShopsToVisit().get(0));
        Path fromFirstToSecondShop = getBestPathForDestination(totalOrder.getShopsToVisit().get(0),
                                            totalOrder.getShopsToVisit().get(1));
        Path secondShopToDeliveryPoint = getBestPathForDestination(totalOrder.getShopsToVisit().get(1),
                                            totalOrder.getDeliveryPoint());
        Path totalPath= goToFirstShop;
        totalPath.addToPath(fromFirstToSecondShop.getPath());
        totalPath.addToPath(secondShopToDeliveryPoint.getPath());
        int totalMoves = goToFirstShop.getMoves() + fromFirstToSecondShop.getMoves()+secondShopToDeliveryPoint.getMoves();
        totalPath.setMoves(totalMoves);
        return  totalPath;
    }
    public Path goToSecondShopFirst(TotalOrder totalOrder){
        Path goToSecondShop = getBestPathForDestination(this.getLocation(),totalOrder.getShopsToVisit().get(1));
        Path fromSecondToFirstShop = getBestPathForDestination(totalOrder.getShopsToVisit().get(1),
                                                totalOrder.getShopsToVisit().get(0));
        Path secondShopToDeliveryPoint = getBestPathForDestination(totalOrder.getShopsToVisit().get(0),
                                                                totalOrder.getDeliveryPoint());
        Path totalPath= goToSecondShop;
        totalPath.addToPath(fromSecondToFirstShop.getPath());
        totalPath.addToPath(secondShopToDeliveryPoint.getPath());
        int totalMoves = goToSecondShop.getMoves() + fromSecondToFirstShop.getMoves()+secondShopToDeliveryPoint.getMoves();
        totalPath.setMoves(totalMoves);
        return totalPath;
    }

    public boolean isTotalOrderPossible(TotalOrder totalOrder){
        Path path = getBestPathForTotalOrder(totalOrder);
        int lastIndex = path.getPath().size()-1;
        LongLat lastPoint = path.getPath().get(lastIndex);
        Path goToAppleton = getBestPathForDestination(lastPoint,this.appletonTower);
        int totalMovesRequired = path.getMoves() + goToAppleton.getMoves();
        return totalMovesRequired<=this.moves_remaining;
    }
    public TotalOrder getTotalOrder(String orderNo) throws SQLException, IOException, InterruptedException {
        ArrayList<String> items = this.webAccess.dataAccess.getItemsFromOrderNo(orderNo);
        OrderDetails orderDetails = new OrderDetails(orderNo, items);

        PreparedStatement ps = this.webAccess.dataAccess.conn.prepareStatement("select * from orders where deliveryDate=(?) AND orderNo=(?)");
        ps.setString(2, orderNo);
        ps.setDate(1, this.date);
        ResultSet rs = ps.executeQuery();
        String matric = "";
        String deliverTo = "";

        while(rs.next()) {


            matric = rs.getString("customer");

            deliverTo = rs.getString("deliverTo");


        }
        Order order = new Order(orderNo, this.date, matric, deliverTo);
        ArrayList<LongLat> shopsToVisit = webAccess.dataAccess.getLongLatsForShopsOfOrder(orderNo);
        LongLat deliveryPoint = webAccess.dataAccess.getDeliveryPointLongLatsFromOrderNumber(orderNo);
        String[] itemsArray = new String[items.size()];
        int i =0;
        for(String item:items) {
            itemsArray[i] = item;
            i++;
        }


        int deliveryCost = webAccess.menus.getDeliveryCost(itemsArray);
        System.out.println("TOTAL DELIVERY COST  " + deliveryCost);

        TotalOrder totalOrder = new TotalOrder(order, orderDetails, shopsToVisit,deliveryPoint,deliveryCost);
        return totalOrder;

    }



    public boolean isLeftTurnPossible(LongLat position , int angle){
        angle = turnLeftByTen(angle);
        return !isBlocked(position,angle);
    }
    public boolean isRightTurnPossible(LongLat position , int angle){
        angle = turnRightByTen(angle);
        return !isBlocked(position,angle);
    }
    public int turnRightUntilNotBlocked(LongLat position,int angle){
        while(isBlocked(position,angle)){
            angle = turnRightByTen(angle);
        }
        return angle;
    }
    public int turnLeftUntilNotBlocked(LongLat position,int angle){
        while(isBlocked(position,angle)){
            angle = turnLeftByTen(angle);
        }
        return angle;
    }

    public int turnRightUntilBlocked(LongLat position, int angle){
        while(!isBlocked(position,angle)){
            angle = turnRightByTen(angle);
        }
        return angle+10;
    }
    public int turnLeftUntilBlocked(LongLat position, int angle){
        while(!isBlocked(position,angle)){
            angle = turnLeftByTen(angle);
        }
        return angle-10;
    }
    public int turnRightByTen(int angle){
        angle-=10;
        angle = correctAngle(angle);
        return angle;
    }
    public int turnLeftByTen(int angle){
        angle+=10;
        angle =correctAngle(angle);

        return angle;
    }

    public LongLat getLocation(){
        return this.location;
    }


    public int correctAngle(int angle){
        if(angle>=360){
            return angle-360;
        }else if(angle <0){
            return angle+360;

        }else {
            return angle;
        }

    }


}
