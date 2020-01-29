//import io.jenetics.jpx.GPX;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
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
    private int AutoConnectId ;
    private Float updateInterval;
    private List<Integer> betaCandidates;


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

    // Calculates speed of vehicle by looking (VEHICLES_IN_SPEED-1) future positions ahead (ie. includes current position)
    private Double getSpeed(){
        int numVehicles=0;
        double totalSpeed = 0;
        Coordinate previousPoint = null;

        Iterator<Float> times = futureRoute.keySet().iterator();
        while(numVehicles<VEHICLES_IN_SPEED){
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

    private Double getDirection() {
        Iterator<Float> times = futureRoute.keySet().iterator();
        Coordinate currentPoint = futureRoute.get(times.next());
        Coordinate nextPoint = futureRoute.get(times.next());

        return currentPoint.getDirection(nextPoint);
    }


    public void run(){
        //counter for interval duration to connect to beta vehicles
        int alphaVehicleCounter = 0;

        try{
            //initialize vehicle's connection with server
            initializeConnection();

            while(true){
                //update vehicle's position to server
                updatePositionToServer();

                //after every BETA_REQUEST_INTERVALS updates to server, ask for candidate beta vehicles
                alphaVehicleCounter++;
                if(alphaVehicleCounter%BETA_REQUEST_INTERVALS==0) {
                    getBetaVehicles();
                }
            }
        }catch (AutoConnectException e){
            System.out.println("After " + alphaVehicleCounter + " loops, Vehicle " + this.AutoConnectId + " exits --> " + e.getMessage() + "\n");
        }
    }


    //initialize vehicle's connection with server. Successful request will receive an AutoConnect-registered ID
    //and an updateInterval with which to wait til updating its position.
    public void initializeConnection() throws AutoConnectException{
        try {
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
            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpPost request = new HttpPost("http://192.168.0.104:4000/initconnect");
            request.setEntity(entity);
            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();

            //Check REST call went through
            if (statusCode != HttpStatus.SC_CREATED) {
                throw new AutoConnectException("VIN# " + this.ID.toString() + " failed to successively initialize connection with server!");
            }

            //Obtain response data
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject responseJson = new JSONObject(responseBody);

            //Obtain AutoConnectId and updateInterval
            this.AutoConnectId = (int) responseJson.get("AutoId");
            this.updateInterval = ((Double) responseJson.get("TimeCheck")).floatValue();
        } catch (IOException e){
            throw new AutoConnectException("VIN# " + this.ID.toString() + "was unable to attempt an initialization connection with server!");
        }
    }

    //update vehicle's current position with server. Successful request will receive next updateInterval by which to wait.
    public void updatePositionToServer() throws AutoConnectException{
        try {
            //wait certain intervals
            Thread.currentThread().sleep((long) (updateInterval * 1));

            //prune GPX
            moveVehicleForward();

            //check if vehicle is no longer in simulation
            if(isVehicleLifeOver()){
                killVehicle();
            }

            //set parameters
            int AutoId = this.AutoConnectId;
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
            myJson.put("AutoId", AutoId);
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
            int statusCode = response.getStatusLine().getStatusCode();

            //Check REST call went through
            if(statusCode != HttpStatus.SC_OK) {
                throw new AutoConnectException("Vehicle " + this.AutoConnectId + " failed to successively update position with server!");
            }

            //Obtain response data
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject responseJson = new JSONObject(responseBody);
            int responseAutoId = (int) responseJson.get("AutoId");
            String responseStatus = (String) responseJson.get("Status");

            //Perform error checks on server's response
            if (responseAutoId != this.AutoConnectId){
                throw new AutoConnectException("Vehicle " + this.AutoConnectId + " received mismatched AutoConnectId from server in update attempt!");
            }
            if (!responseStatus.equals("Success")){
                throw new AutoConnectException("Server was unsuccessful in updating position of Vehicle " + this.AutoConnectId +"!");
            }
        } catch (InterruptedException e){
            throw new AutoConnectException("Vehicle " + this.AutoConnectId + " has exited the simulation!");
        } catch (IOException e){
            throw new AutoConnectException("Vehicle " + this.AutoConnectId + " was unable to attempt an updating of its position with server!");
        }

    }

    public void getBetaVehicles() throws AutoConnectException{
        try {
            //check if vehicle is no longer in simulation
            if(isVehicleLifeOver()){
                killVehicle();
            }

            //set parameters
            int AutoId = this.AutoConnectId;
            Double PositionX = getStartingCoordinate().getLatitude();
            Double PositionY = getStartingCoordinate().getLongitude();
            Double Speed = getSpeed();
            Double Direction = getDirection();
            String Time = Clock.systemUTC().instant().toString();
            Double ConnectionRadius = CONNECTION_RADIUS_CAR;
            int BetaBound = BETA_BOUND;

            //JSONify parameters, and build payload
            JSONObject myJson = new JSONObject();
            myJson.put("AutoId", AutoId);
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


            //Execute POST request
            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpPost request = new HttpPost("http://192.168.0.104:4000/getbetas");
            request.setEntity(entity);
            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();

            //Check REST call went through
            if (statusCode != HttpStatus.SC_CREATED) {
                throw new AutoConnectException("Vehicle " + this.AutoConnectId + " failed to successively initialize connection with server!");
            }

            //Obtain response data
            String responseBody = EntityUtils.toString(response.getEntity());
            JSONObject responseJson = new JSONObject(responseBody);
            String responseStatus = (String) responseJson.get("Status");

            //Perform error checks
            if (!responseStatus.equals("Success")) {
                throw new AutoConnectException("Server was unsuccessful in generating candidate Beta cars for Vehicle " + this.AutoConnectId + "!");
            }

            //Obtain beta candidates
            JSONArray priorityMatrix = (JSONArray) responseJson.get("PriorityMatrix");
            this.betaCandidates = (List<Integer>) (List<?>) priorityMatrix.toList();
        } catch (InterruptedException e){
            throw new AutoConnectException("Vehicle " + this.AutoConnectId + " has exited the simulation!");
        } catch (IOException e) {
            throw new AutoConnectException("Vehicle " + this.AutoConnectId + " failed to obtain Beta candidate vehicles!");
        }
    }


    //As very first position entry of 'futureRoute' is at current moment, we must prune the collection of past positions
    private void moveVehicleForward() throws InterruptedException{
        Float updatedCurrentTime = round(this.getStartingTime() + updateInterval);

        //if vehicle doesn't exist at current moment (ie. no longer a running vehicle), simply exit simulation
        if(!futureRoute.containsKey(updatedCurrentTime)){
            killVehicle();
        }

        //remove past locations of vehicle
        Iterator<Map.Entry<Float, Coordinate>> entryIterator = futureRoute.entrySet().iterator();
        while(entryIterator.hasNext()){
            Map.Entry entry = entryIterator.next();
            if(entry.getKey().equals(updatedCurrentTime)){
                break;
            }
            entryIterator.remove();
        }
    }

    //Check if vehicle is nearing or has passed its lifecyle
    private boolean isVehicleLifeOver(){
        return (futureRoute.isEmpty() || futureRoute.size()<VEHICLES_IN_SPEED);
    }

    private void killVehicle() throws InterruptedException{
        Thread.currentThread().interrupt();
        throw new InterruptedException();
    }

    //rounds float to one decimal place
    private Float round(Float f){
        BigDecimal bd = new BigDecimal(Float.toString(f));
        bd = bd.setScale(1, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
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
