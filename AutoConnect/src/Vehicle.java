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


    public Vehicle(String file){
        ID = UUID.randomUUID();
        timestamp = new HashMap<>();
    }

    private void parsePointData(String file) throws IOException, ParseVehicleDataException{
        BufferedReader csvReader = new BufferedReader(new FileReader(file));
        String row = "";
        while ((row = csvReader.readLine()) != null) {
            String[] data = row.split(",", 4);
            if(data.length!=4){
                throw new ParseVehicleDataException("Raw vehicle data should contain empty space" +
                        "and time, latitude, longitude");
            }

            Coordinate point = new Coordinate(Double.parseDouble(data[2]), Double.parseDouble(data[3]));
            timestamp.put(Float.parseFloat(data[1]), point);
        }
        csvReader.close();

    }

    public void updatePositionToServer(String currentTime){
        //we need to be careful -- given current time may JUST be off 0.1 s off our known times
        if(timestamp.containsKey(Float.parseFloat(currentTime))){
            //send position to server
        }else{
            //need a way to handle this
        }
    }

}
