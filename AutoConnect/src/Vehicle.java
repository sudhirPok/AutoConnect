import io.jenetics.jpx.GPX;

import java.util.*;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

public class Vehicle {
    private LinkedHashMap<Float, Coordinate> timestamp = null;
    private final UUID ID;
    private final Coordinate destination;


    private class Coordinate{
        private final double latitude;
        private final double longitude;

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
    }


    public Vehicle(String file) throws IOException, ParseVehicleDataException{
        ID = UUID.randomUUID();
        timestamp = new HashMap<>();
        destination = parsePointData(file);
    }

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
            timestamp.put(Float.parseFloat(data[2]), point);
            lastPoint = point;
        }
        csvReader.close();
        return lastPoint;
    }

    public void initializeConnection(){}

    public void updatePositionToServer(String currentTime){
        //we need to be careful -- given current time may JUST be off 0.1 s off our known times
        if(timestamp.containsKey(Float.parseFloat(currentTime))){
            //insert here
        }else{
            //need a way to handle this
        }
    }

    //need to just build my code here!
    private void generateGPX(){

        //timestamp.forEach((i)-> System.out.print(ints.get(i-1)+ " "));
        final GPX gpx = GPX.builder()
                .addTrack(track -> track
                        .addSegment(segment -> {

                                for(Map.Entry<Float, Coordinate> entry: timestamp.entrySet()) {
                                    Coordinate location = entry.getValue();

                                    segment.addPoint(p -> p.lat(location.getLatitute()).lon(location.getLatitute()).ele(0));
                                }
                        }) )
                .build();
        }

    }

}
