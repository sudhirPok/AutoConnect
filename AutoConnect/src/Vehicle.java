//import io.jenetics.jpx.GPX;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.HttpResponse;
import org.json.JSONObject;


public class Vehicle implements Runnable{
    private static final int VEHICLES_IN_SPEED = 3;
    private static final Double CONNECTION_RADIUS_CAR = 100.0;
    private static final int BETA_BOUND = 3;
    private static final int BETA_REQUEST_INTERVALS = 3; //After how many position updates should vehicle ask for betas?

    //design of the vehicle class is such that the very first entry of the collection futureRoute ALWAYS
    //represents its CURRENT positional data (ie. must constantly prune this collection to remove all previous
    //entries with timestamps in the past)
    private LinkedHashMap<Float, Coordinate> futureRoute = null;
    private final UUID ID;
    private final Coordinate destination;
    private int updateInterval;
    private int AutoConnectID;


    public Vehicle(String file) throws IOException, AutoConnectException {
        ID = UUID.randomUUID();
        futureRoute = new LinkedHashMap<>();
        destination = parsePointData(file);
    }

    //this function guarantees vehicle has proper time-stamped coordinate data
    private Coordinate parsePointData(String file) throws IOException, AutoConnectException {
        BufferedReader csvReader = new BufferedReader(new FileReader(file));
        String row = "";
        Coordinate lastPoint = null;
        while ((row = csvReader.readLine()) != null) {
            String[] data = row.split(",");

            if(data.length!=5){
                throw new AutoConnectException(String.format("Vehicle file %s was not parsed correctly", file));
            }

            Coordinate point = new Coordinate(Double.parseDouble(data[3]), Double.parseDouble(data[4]));
            futureRoute.put(Float.parseFloat(data[2]), point);
            lastPoint = point;
        }
        csvReader.close();
        return lastPoint;
    }

    public Coordinate getStartingCoordinate(){
        Collection<Coordinate> coordinates = futureRoute.values();
        return coordinates.iterator().next();
    }

    public Float getStartingTime(){
        Collection<Float> times = futureRoute.keySet();
        return times.iterator().next();
    }

    // Calculates speed of vehicle by looking ahead into its project path by @VEHICLES_IN_SPEED amount
    //NOTE: does not handle case where simulation time is end of this vehicle's path!
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

    //NOTE: does not handle case where simulation time is end of this vehicle's path!
    private double getDirection() {
        Iterator<Float> times = futureRoute.keySet().iterator();
        Coordinate currentPoint = futureRoute.get(times.next());
        Coordinate nextPoint = futureRoute.get(times.next());

        return currentPoint.getDirection(nextPoint);
    }


    public void run(){

        try{
            //initialize vehicle's connection with server
            initializeConnection();

            //counter for interval duration to connect to beta vehicles
            int alphaVehicleCounter = 0;

            while(true){
                //update vehicle's position to server
                updatePositionToServer();

                //after every BETA_REQUEST_INTERVALS updates to server, ask for candidate beta vehicles
                alphaVehicleCounter++;
                if(alphaVehicleCounter%BETA_REQUEST_INTERVALS == 0){
                    getBetaVehicles();
                }
            }
        }catch (AutoConnectException e){
            System.out.println(e.getMessage());
        }
    }


    //initialize vehicle's connection with server. Successful request will receive an AutoConnect-registered ID
    //and an updateInterval with which to wait til updating its position.
    public void initializeConnection() throws AutoConnectException{
        //set parameters
        String VIN = this.ID.toString();
        String GPX = "dummyGPX";
        Double PositionX = getStartingCoordinate().getLatitude();
        Double PositionY = getStartingCoordinate().getLongitude();
        Double DestinationX = this.destination.getLatitude();
        Double DestinationY = this.destination.getLongitude();
        Double Speed = getSpeed();
        Double Direction = getDirection();
        String Time = Clock.systemUTC().instant().toString();

        //JSONify parameters, and build payload
        JSONObject myJson = new JSONObject();
        myJson.put("VIN", VIN);
        myJson.put("RouteXML", GPX);
        myJson.put("PositionX", PositionX);
        myJson.put("PositionY", PositionY);
        myJson.put("DestinationX", DestinationX);
        myJson.put("DestinationY", DestinationY);
        myJson.put("Speed", Speed);
        myJson.put("Direction", Direction);
        myJson.put("Time", Time);
        HttpEntity entity = EntityBuilder.create()
                .setText(myJson.toString())
                .setContentType(ContentType.create("application/json", StandardCharsets.UTF_8)).build();

        //execute POST request
        try{
            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpPost request = new HttpPost("http://192.168.0.104:4000/initconnect");
            request.setEntity(entity);
            HttpResponse response = httpClient.execute(request);
            System.out.println(response.getStatusLine());
            System.out.println(response.toString());
        }catch (IOException e){
            throw new AutoConnectException("VIN# " + this.ID.toString() + "could not initialize connection with server!");
        }

        //updateInterval = response back!
        //initialize an AutoConnect issued ID!!
    }

    //update vehicle's current position with server. Successful request will receive next updateInterval by which to wait.
    public void updatePositionToServer() throws AutoConnectException{
        try {
            //wait certain intervals
            Thread.currentThread().sleep(updateInterval * 1000);

            //prune GPX
            removePastLocations();

            //set parameters
            String AutoID = String.valueOf(this.AutoConnectID);
            String GPX = "dummyGPX";
            Double PositionX = getStartingCoordinate().getLatitude();
            Double PositionY = getStartingCoordinate().getLongitude();
            Double DestinationX = this.destination.getLatitude();
            Double DestinationY = this.destination.getLongitude();
            Double Speed = getSpeed();
            Double Direction = getDirection();
            String Time = Clock.systemUTC().instant().toString();

            //JSONify parameters, and build payload
            JSONObject myJson = new JSONObject();
            myJson.put("AutoID", AutoID);
            myJson.put("RouteXML", GPX);
            myJson.put("PositionX", PositionX);
            myJson.put("PositionY", PositionY);
            myJson.put("DestinationX", DestinationX);
            myJson.put("DestinationY", DestinationY);
            myJson.put("Speed", Speed);
            myJson.put("Direction", Direction);
            myJson.put("Time", Time);
            HttpEntity entity = EntityBuilder.create()
                    .setText(myJson.toString())
                    .setContentType(ContentType.create("application/json", StandardCharsets.UTF_8)).build();

            //Execute PATCH request
            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpPatch request = new HttpPatch("http://192.168.0.104:4000/updateconnect");
            request.setEntity(entity);
            HttpResponse response = httpClient.execute(request);
            System.out.println(response.getStatusLine());
            System.out.println(response.toString());
        } catch (InterruptedException e){
            throw new AutoConnectException("Vehicle " + this.AutoConnectID + " failed to wait for next updating session!");
        } catch (AutoConnectException e) {
            throw new AutoConnectException("Vehicle " + this.AutoConnectID + " failed to remove its past positions!");
        } catch (IOException e){
            throw new AutoConnectException("Vehicle " + this.AutoConnectID + " failed to update its position with server!");
        }

    }

    public void getBetaVehicles() throws AutoConnectException{
        //set parameters
        String AutoID = String.valueOf(this.AutoConnectID);
        Double PositionX = getStartingCoordinate().getLatitude();
        Double PositionY = getStartingCoordinate().getLongitude();
        Double Speed = getSpeed();
        Double Direction = getDirection();
        String Time = Clock.systemUTC().instant().toString();
        Double ConnectionRadius = CONNECTION_RADIUS_CAR;
        int BetaBound = BETA_BOUND;

        //JSONify parameters, and build payload
        JSONObject myJson = new JSONObject();
        myJson.put("AutoID", AutoID);
        myJson.put("PositionX", PositionX);
        myJson.put("PositionY", PositionY);
        myJson.put("Speed", Speed);
        myJson.put("Direction", Direction);
        myJson.put("Time", Time);
        myJson.put("ConnectionRadius", ConnectionRadius);
        myJson.put("BetaBound", BetaBound);
        HttpEntity entity = EntityBuilder.create()
                .setText(myJson.toString())
                .setContentType(ContentType.create("application/json", StandardCharsets.UTF_8)).build();

        try{
            //Execute POST request
            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpPost request = new HttpPost("http://192.168.0.104:4000/getbetas");
            request.setEntity(entity);
            HttpResponse response = httpClient.execute(request);
            System.out.println(response.getStatusLine());
            System.out.println(response.toString());
        } catch (IOException e) {
            throw new AutoConnectException("Vehicle " + this.AutoConnectID + " failed to obtain Beta candidate vehicles!");
        }
    }


    //prune's futureRoute such that all past timestamps are erased (ie. first entry of collection is CURRENT)
    private void removePastLocations() throws AutoConnectException {
        Float updatedCurrentTime = this.getStartingTime()+updateInterval;

        if(!futureRoute.containsKey(updatedCurrentTime)) throw new
                AutoConnectException(Thread.currentThread().getName() + "does not have position coordinate for time "
                +this.getStartingTime()+updateInterval + "s from " + this.getStartingTime() + "s");

        //remove past locations of vehicle
        for(Float time: futureRoute.keySet()){
            if(time.equals(updatedCurrentTime)){
                return;
            }
            futureRoute.remove(time);
        }

    }

    //just need to convert this GPX object to a string to send..
    /*
    private String generateGPX(){
        //timestamp.forEach((i)-> System.out.print(ints.get(i-1)+ " "));
         GPX gpx = GPX.builder()
                .addTrack(track -> track
                        .addSegment(segment -> {
                                for(Map.Entry<Float, Coordinate> entry: futureRoute.entrySet()) {
                                    Coordinate location = entry.getValue();
                                    segment.addPoint(p -> p.lat(location.getLongitude()).lon(location.getLongitude()).ele(0));
                                }
                        }) )
                .build();
    }*/
}
