import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class TrafficSimulation {
    int numVehicles = 0;







    private void readTrafficData(){
        String row = "";
        BufferedReader csvReader = new BufferedReader((new FileReader(pathToCsv)));
        while((row = csvReader.readLine()) != null){
            String[] data = row.split(",");

            //error handling
            if (data.length == 0) continue;

            //skip first trajector
            if (data[0].contains("ID")) continue;

            //handle new car
            if (Character.isDigit(data[0].charAt(0))){
                count++;
                continue;
            }
        }
    }
}
