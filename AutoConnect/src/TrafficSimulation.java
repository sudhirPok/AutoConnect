import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class TrafficSimulation {

    ArrayList<Vehicle> vehicleArrayList = new ArrayList();


    /**
     *
     */
    //This method takes in a CSV file containing the raw traffic simulation data for n vehicles,
    //and creates n smaller CSV files per vehicle. Returns file path of created CSV files.
    private ArrayList<String> readTrafficData(String pathToCsv) throws IOException{
        ArrayList<String> vehicleData = new ArrayList<>();

        String row;

        BufferedReader csvReader = new BufferedReader((new FileReader(pathToCsv)));
        csvReader.skip(1);

        FileWriter writer = null;

        int counter = 0;
        while((row = csvReader.readLine()) != null){
            String[] data = row.split(",");

            //if we encounter a car id, stop writing to current file & write to new file.
            //note, this skips the current line
            if (Character.isDigit(data[0].charAt(0))){
                if(writer!=null){
                    writer.flush();
                    writer.close();
                }

                String filePath = "vehicleData" + Integer.toString(++counter);
                Path vehicleFilePath = Paths.get(filePath);
                Files.createFile(vehicleFilePath);
                vehicleData.add(filePath);
                writer = new FileWriter(filePath);
            }else{
                writer.write(row);
            }
        }

        writer.flush();
        writer.close();

        return vehicleData;
    }

    private void createVehicles(ArrayList<String> vehicleCSVs){
        for(String csvFile: vehicleCSVs){
            Vehicle vehicle = new Vehicle(csvFile);
            vehicleArrayList.add(vehicle);
        }
    }


}
