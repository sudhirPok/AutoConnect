import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class TrafficSimulation {
    private static final String INPUT_TRAFFIC_DATA = "trafficData/kingstonTraffic.txt";
    private static final String OUTPUT_TRAFFIC_DATA = "trafficData/simulation.txt";
    private static final String OUTPUT_COLUMN_HEADERS = "Connections ID, Vehicle ID, Time Stamp, Current Open Connections";

    private static final int NUM_VEHICLES = 116; //total vehicles in simulation
    private static boolean CREATE_VEHICLE_FILES = false; //flag on how to generate individual vehicle data

    private static HashMap<Float, List<Vehicle>> parsedVehicles = new HashMap<>();
    private static ArrayList<Float> vehicleCreationTime = new ArrayList<>();
    private static ArrayList<Thread> vehicleThreads = new ArrayList<>();

    public static void main(String[] args){
        executeSimulation();
    }

    private static void executeSimulation(){
        try{
            //prepare simulation vehicle data
            ArrayList<String> createdVehicleData = generateTestVehicleData();
            createVehicles(createdVehicleData);

            //run simulation
            generateSimulationVehicles();
            waitForSimulationToFinish();

            //generate output for visuals
            generateSimulationOutput();
        }catch(IOException e){
            System.out.println(e.getMessage());
        }catch (InterruptedException e){
            System.out.println(e.getMessage());
        }catch(AutoConnectException e){
            System.out.println(e.getMessage());
        }
    }

    //Returns list of file paths of each vehicle's traffic data, by either generating the traffic data itself or using existing traffic data
    private static ArrayList<String> generateTestVehicleData() throws IOException{
        ArrayList<String> filepaths = null;
        if(CREATE_VEHICLE_FILES){
            filepaths = readCreatedTrafficData();
        }else{
            filepaths = readExistingTrafficData();
        }
        return filepaths;
    }

    //Returns list of file paths of all vehicle traffic data, given they already exist
    private static ArrayList<String> readExistingTrafficData(){
        System.out.println("Reading existing traffic data!\n");

        ArrayList<String> vehiclePaths = new ArrayList();
        for(int i=1; i<=NUM_VEHICLES; i++){
            String path = "trafficData/vehicleData" + i + ".txt";
            vehiclePaths.add(path);
        }
        return vehiclePaths;
    }

    //Method takes in a CSV file containing the raw traffic simulation data for n vehicles,
    //and creates n smaller CSV files per vehicle. Returns file path of created CSV files.
    private static ArrayList<String> readCreatedTrafficData() throws IOException{
        System.out.println("Generating new traffic data!\n");

        ArrayList<String> vehicleData = new ArrayList<>();

        String row;

        BufferedReader csvReader = new BufferedReader((new FileReader(INPUT_TRAFFIC_DATA)));
        csvReader.readLine(); //skip first line

        FileWriter writer = null;

        int countVehicles = 0;
        while((row = csvReader.readLine()) != null && countVehicles<=NUM_VEHICLES){
            String[] data = row.split(",");


            //if we encounter a car id and we've made enough cars, simply exit
            if (!data[0].isEmpty() && countVehicles==NUM_VEHICLES) {
                break;
            }
            //if we encounter a car id and we haven't yet made enough, stop writing to current file & write to new file
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
            }
            //continue writing to current file
            else{
                writer.write(row + "\n");
            }
        }

        writer.flush();
        writer.close();

        return vehicleData;
    }

    private static void createVehicles(ArrayList<String> vehicleCSVs) throws IOException, AutoConnectException {
        HashSet<Float> setVehicleCreationTimes = new HashSet<>();
        int numVehicles=0;

        for(int i=0; i<NUM_VEHICLES; i++){
            String csvFile = vehicleCSVs.get(i);
            Vehicle vehicle = new Vehicle(csvFile);

            //add this vehicle to the hashmap entry: initialize empty list before adding, if needed
            List<Vehicle> prevVehiclesAtTime= parsedVehicles.get(vehicle.getStartingTime());
            if(prevVehiclesAtTime==null){
                prevVehiclesAtTime = new ArrayList();
            }
            prevVehiclesAtTime.add(vehicle);
            numVehicles++;
            parsedVehicles.put(vehicle.getStartingTime(), prevVehiclesAtTime);

            setVehicleCreationTimes.add(vehicle.getStartingTime());
        }

        //sort vehicleCreationTime in ascending order
        vehicleCreationTime = new ArrayList<Float>(setVehicleCreationTimes);
        Collections.sort(vehicleCreationTime);

        if(!(numVehicles==NUM_VEHICLES && parsedVehicles.size()==vehicleCreationTime.size())){
            throw new AutoConnectException(String.format("Raw traffic data was parsed, but not into %s individual vehicles!\n", NUM_VEHICLES));
        }
    }


    private static void generateSimulationVehicles(){
        System.out.println("Simulation should have " +NUM_VEHICLES + " cars running!\n" );

        //activate system vehicles by creation time
        for(int i=0; i<vehicleCreationTime.size()-1; i++){
            List<Vehicle> activeCars = parsedVehicles.get(vehicleCreationTime.get(i));
            for(Vehicle activeCar: activeCars){
                Thread systemCar = new Thread(activeCar);
                vehicleThreads.add(systemCar);
                systemCar.start();
            }

            Float waitTilNextCarInMilliseconds = round((vehicleCreationTime.get(i+1)-vehicleCreationTime.get(i)))*1000;
            try{
                System.out.println("Simulation is waiting " + waitTilNextCarInMilliseconds/1000 + "s for next car!\n");
                Thread.sleep(waitTilNextCarInMilliseconds.longValue());
            }catch (InterruptedException e){
                System.out.println("Simulation encountered CRITICAL error in waiting for upcoming car to active!\n");
            }
        }

        //create list of cars
        List<Vehicle> activeCars = parsedVehicles.get(vehicleCreationTime.get(vehicleCreationTime.size()-1));
        for(Vehicle activeCar: activeCars){
            Thread systemCar = new Thread(activeCar);
            vehicleThreads.add(systemCar);
            systemCar.start();
        }
    }

    private static void generateSimulationOutput() throws IOException{
        Path vehicleFilePath = Paths.get(OUTPUT_TRAFFIC_DATA);
        Files.createFile(vehicleFilePath);
        FileWriter writer = new FileWriter(OUTPUT_TRAFFIC_DATA);
        writer.write(OUTPUT_COLUMN_HEADERS + "\n");

        for(int i=0; i<vehicleCreationTime.size(); i++){
            List<Vehicle> vehicles = parsedVehicles.get(vehicleCreationTime.get(i));

            for(Vehicle vehicle: vehicles){
                List<String> alphaConnections = vehicle.getLifetimeAlphaConnections();
                for(int j=0; j<alphaConnections.size(); j++){
                    writer.write(alphaConnections.get(j));
                }
            }
        }

        writer.flush();
        writer.close();
        System.out.println("All vehicles' data has been outputted!\n");
    }

    //Although all vehicles will have started running in simulation, it's possible not all will have finished.
    //Thus, simply wait for all to complete before proceeding, and do so in the order they were ran (not really required)
    private static void waitForSimulationToFinish() throws InterruptedException{
        for(int i=0; i<vehicleThreads.size(); i++){
            Thread vehicleThread = vehicleThreads.get(i);
            vehicleThread.join();
        }

        System.out.println("Simulation has finished!\n");
    }

    //rounds float to one decimal place
    private static Float round(Float f){
        BigDecimal bd = new BigDecimal(Float.toString(f));
        bd = bd.setScale(1, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }
}
