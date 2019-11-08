import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class TrafficSimulation {
    private static final String rawTrafficData = "trafficData/kathmanduTraffic.txt";
    private static final int numVehicles = 10;

    private static ArrayList<Vehicle> vehicleArrayList = new ArrayList();
    /*
    private static ArrayList<String> testingVehiclePath = new ArrayList(){
        "trafficData/vehicleData1.txt","trafficData/vehicleData2.txt","trafficData/vehicleData3.txt","trafficData/vehicleData4.txt"
                "trafficData/vehicleData5.txt", "trafficData/vehicleData6.txt", "trafficData/vehicleData7.txt", "trafficData/vehicleData8.txt"
                "trafficData/vehicleData9.txt","trafficData/vehicleData10.txt"
    };*/


    public static void main(String[] args){

        try{
            ArrayList<String> createdVehicleData = readTrafficData(rawTrafficData);
            createVehicles(createdVehicleData);
        }catch(IOException e){
            System.out.println(e.getMessage());
        }catch(ParseVehicleDataException e){
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
        while((row = csvReader.readLine()) != null && countVehicles<=numVehicles){
            String[] data = row.split(",");

            //if we encounter a car id, stop writing to current file & write to new file.
            //note, this skips the current line
            if (!data[0].isEmpty() && countVehicles!=numVehicles){
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

    private static void createVehicles(ArrayList<String> vehicleCSVs) throws ParseVehicleDataException{
        for(String csvFile: vehicleCSVs){
            Vehicle vehicle = new Vehicle(csvFile);
            vehicleArrayList.add(vehicle);
        }

        if(vehicleArrayList.size()!=numVehicles){
            throw new ParseVehicleDataException(String.format("Raw traffic data was parsed, but not into %s individual vehicles!", numVehicles));
        }
    }


}
