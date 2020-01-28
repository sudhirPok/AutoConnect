import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

public class TrafficSimulation {
    private static final String rawTrafficData = "trafficData/kathmanduTraffic.txt";
    private static final int NUM_VEHICLES = 10;

    private static HashMap<Float, Vehicle> parsedVehicles = new HashMap<>();
    private static ArrayList<Float> vehicleCreationTime = new ArrayList<>();

    private static ArrayList<String> testingVehiclePath = new ArrayList<>(
            Arrays.asList("trafficData/vehicleData1.txt","trafficData/vehicleData2.txt","trafficData/vehicleData3.txt","trafficData/vehicleData4.txt",
            "trafficData/vehicleData5.txt", "trafficData/vehicleData6.txt", "trafficData/vehicleData7.txt", "trafficData/vehicleData8.txt",
            "trafficData/vehicleData9.txt","trafficData/vehicleData10.txt"));

    public static void main(String[] args){

        initializeTraffic();

        System.out.println("Yeah, works so far!");

    }

    private static void initializeTraffic(){

        //create vehicles
        try{
            //ArrayList<String> createdVehicleData = readTrafficData(rawTrafficData);
            ArrayList<String> createdVehicleData = testingVehiclePath;
            createVehicles(createdVehicleData);
            generateSystemVehicles();
        }catch(IOException e){
            System.out.println(e.getMessage());
        }catch(AutoConnectException e){
            System.out.println(e.getMessage());
        }
    }

    //This method takes in a CSV file containing the raw traffic simulation data for n vehicles,
    //and creates n smaller CSV files per vehicle. Returns file path of created CSV files.
    private static ArrayList<String> readTrafficData(String pathToCsv) throws IOException{
        ArrayList<String> vehicleData = new ArrayList<>();

        String row;

        BufferedReader csvReader = new BufferedReader((new FileReader(pathToCsv)));
        csvReader.readLine(); //skip first line

        FileWriter writer = null;

        int countVehicles = 0;
        while((row = csvReader.readLine()) != null && countVehicles<=NUM_VEHICLES){
            String[] data = row.split(",");

            //if we encounter a car id, stop writing to current file & write to new file.
            //note, this skips the current line
            if (!data[0].isEmpty() && countVehicles!=NUM_VEHICLES){
                if(writer!=null){
                    writer.flush();
                    writer.close();
                }

                String filePath = "trafficData/vehicleData" + Integer.toString(++countVehicles) + ".txt";
                Path vehicleFilePath = Paths.get(filePath);
                Files.createFile(vehicleFilePath);
                vehicleData.add(filePath);
                writer = new FileWriter(filePath);
            }else{
                writer.write(row + "\n");
            }
        }

        writer.flush();
        writer.close();

        return vehicleData;
    }

    private static void createVehicles(ArrayList<String> vehicleCSVs) throws IOException, AutoConnectException {
        for(String csvFile: vehicleCSVs){
            Vehicle vehicle = new Vehicle(csvFile);
            parsedVehicles.put(vehicle.getStartingTime(), vehicle);
            vehicleCreationTime.add(vehicle.getStartingTime());
        }

        if(!(parsedVehicles.size()==NUM_VEHICLES && vehicleCreationTime.size()==NUM_VEHICLES)){
            throw new AutoConnectException(String.format("Raw traffic data was parsed, but not into %s individual vehicles!", NUM_VEHICLES));
        }

        //sort vehicleCreationTime in ascending order
        Collections.sort(vehicleCreationTime);
    }

    /*
    private static void generateSystemVehicles(){

        //activate system vehicles by creation time
        for(int i=0; i<vehicleCreationTime.size()-1; i++){
            Vehicle activeCar = parsedVehicles.get(vehicleCreationTime.get(i));
            Thread systemCar = new Thread(activeCar);
            systemCar.start();

            Float waitTilNextCarInMilliseconds = (vehicleCreationTime.get(i+1)-vehicleCreationTime.get(i))*1000;
            try{
                Thread.sleep(waitTilNextCarInMilliseconds.longValue());
            }catch (InterruptedException e){
                System.out.println("Simulation encountered error in waiting for upcoming car to active! Critical error!");
            }
        }

        //create last car
        Vehicle activeCar = parsedVehicles.get(vehicleCreationTime.get(parsedVehicles.size()-1));
        Thread systemCar = new Thread(activeCar);
        systemCar.start();
    }*/

    private static void generateSystemVehicles(){
        //create first car
        Vehicle activeCar = parsedVehicles.get(vehicleCreationTime.get(0));
        Thread systemCar = new Thread(activeCar);
        systemCar.start();
    }

}
