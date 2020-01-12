import io.jenetics.jpx.GPX;

import java.util.*;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

public class Vehicle {
    private LinkedHashMap<Float, Coordinate> futureRoute = null;
    private final UUID ID;
    private final Coordinate source;
    private final Coordinate destination;
    private final float inception;


    private static final int VEHICLES_IN_SPEED = 3;
    private static final double SPEED_DURATION = 0.2;

    private class Coordinate{
        private final double latitude;
        private final double longitude;
        private static final int KM_IN_DEGREE = 111;

        private Coordinate(double x, double y){
            latitude = x;
            longitude = y;
        }

        public double getLatitute(){
            return latitude;
        }

        public double getLongitute(){
            return longitude;
        }

        //Returns absolute distance between two coordinates in kilometres
        public double getDistance(Coordinate target){
            double xComponent = Math.pow(KM_IN_DEGREE * Math.abs(this.latitude-target.latitude), 2);
            double yComponent = Math.pow(KM_IN_DEGREE * Math.abs(this.longitude-target.longitude), 2);
            return Math.sqrt(xComponent+yComponent);
        }

        //Returns travelling speed of current coordinate to target coordinate,
        // over an interval of SPEED_DURATION seconds, in km/s
        public double getSpeed(Coordinate target){
            double distance = this.getDistance(target);
            return distance/SPEED_DURATION;
        }

        //Returns angle of vector between current and target coordinate in degrees
        public double getDirection(Coordinate target){
            //since angle is simply a ratio measurement, no need to convert to kilometres
            //additionally, we MUST keep the original positive/negative values of x- and y-components
            double xComponent = target.latitude - this.latitude;
            double yComponent = target.longitude - this.longitude;
            return Math.toDegrees(Math.atan(yComponent/xComponent));
        }
    }


    public Vehicle(String file) throws IOException, ParseVehicleDataException{
        ID = UUID.randomUUID();
        futureRoute = new LinkedHashMap<>();
        destination = parsePointData(file);
        source = getStartingCoordinate();
        inception = getStartingTime();
    }

    private Coordinate getStartingCoordinate(){
        Collection<Coordinate> coordinates = futureRoute.values();
        return coordinates.iterator().next();
    }

    private float getStartingTime(){
        Collection<Float> times = futureRoute.keySet();
        return times.iterator().next();
    }

    //this function gaurantees vehicle has proper time-stamped coordinate data
    private Coordinate parsePointData(String file) throws IOException, ParseVehicleDataException{
        BufferedReader csvReader = new BufferedReader(new FileReader(file));
        String row = "";
        Coordinate lastPoint = null;
        while ((row = csvReader.readLine()) != null) {
            String[] data = row.split(",");

            if(data.length!=5){
                throw new ParseVehicleDataException(String.format("Vehicle file %s was not parsed correctly", file));
            }

            Coordinate point = new Coordinate(Double.parseDouble(data[3]), Double.parseDouble(data[4]));
            futureRoute.put(Float.parseFloat(data[2]), point);
            lastPoint = point;
        }
        csvReader.close();
        return lastPoint;
    }

    public void initializeConnection(){
        String VIN = this.ID.toString();
        String GPX = generateGPX();
        String PositionX = Double.toString(getStartingCoordinate().getLatitute());
        String PositionY = Double.toString(getStartingCoordinate().getLongitute());
        String DestinationX = Double.toString(this.destination.getLatitute());
        String DestinationY = Double.toString(this.destination.getLongitute());
    }

    /**
     * Calculates speed of vehicle by looking ahead into its project path by @VEHICLES_IN_SPEED amount
     * @return
     */
    private double getSpeed(){
        int numVehicles=0;
        double totalSpeed = 0;
        Coordinate previousPoint = null;

        Iterator<Float> times = futureRoute.keySet().iterator();
        while(numVehicles<=VEHICLES_IN_SPEED){
            float time = times.next();

            if(previousPoint==null){
                previousPoint = futureRoute.get(time);
            }else{
                Coordinate currentPoint = futureRoute.get(time);
                totalSpeed += previousPoint.getSpeed(currentPoint);

                previousPoint = currentPoint;
            }

            numVehicles++;
        }

        return totalSpeed/(VEHICLES_IN_SPEED); //return average of the speeds
    }

    private double getDirection(){
        Iterator<Float> times = futureRoute.keySet().iterator();
        Coordinate currentPoint = futureRoute.get(times.next());
        Coordinate nextPoint = futureRoute.get(times.next());

        return currentPoint.getDirection(nextPoint);
    }

    public void updatePositionToServer(String currentTime){
        //prune GPX
        removePastLocations(currentTime);
    }

    private void removePastLocations(String currentTime){
        if(futureRoute.containsKey(Float.parseFloat(currentTime))){
            Coordinate currentLocation = futureRoute.get(Float.parseFloat(currentTime));

            //remove past locations of vehicle
            for(Float time: futureRoute.keySet()){
                if(time.equals(currentLocation)){
                    break;
                }

                futureRoute.remove(time);
            }
        }
    }

    //just need to convert this GPX object to a string to send..
    private String generateGPX(){

        //timestamp.forEach((i)-> System.out.print(ints.get(i-1)+ " "));
         GPX gpx = GPX.builder()
                .addTrack(track -> track
                        .addSegment(segment -> {

                                for(Map.Entry<Float, Coordinate> entry: futureRoute.entrySet()) {
                                    Coordinate location = entry.getValue();

                                    segment.addPoint(p -> p.lat(location.getLatitute()).lon(location.getLatitute()).ele(0));
                                }
                        }) )
                .build();


    }

}
