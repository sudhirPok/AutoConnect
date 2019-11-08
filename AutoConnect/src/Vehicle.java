import java.util.HashMap;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.UUID;

public class Vehicle {
    private HashMap<Float, Coordinate> timestamp = null;
    private final UUID ID;

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
        parsePointData(file);
    }

    private void parsePointData(String file) throws IOException, ParseVehicleDataException{
        BufferedReader csvReader = new BufferedReader(new FileReader(file));
        String row = "";
        while ((row = csvReader.readLine()) != null) {
            String[] data = row.split(",");
            if(data.length!=5){
                throw new ParseVehicleDataException(String.format("Vehicle file %s was not parsed correctly", file));
            }

            Coordinate point = new Coordinate(Double.parseDouble(data[3]), Double.parseDouble(data[4]));
            timestamp.put(Float.parseFloat(data[2]), point);
        }
        csvReader.close();

    }

    public void updatePositionToServer(String currentTime){
        //we need to be careful -- given current time may JUST be off 0.1 s off our known times
        if(timestamp.containsKey(Float.parseFloat(currentTime))){
            //insert here
        }else{
            //need a way to handle this
        }
    }

}
