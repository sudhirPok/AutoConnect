public class Coordinate {
    private static final int KM_IN_DEGREE = 111;
    private static final double SPEED_DURATION = 0.2;
    private static final int SECONDS_IN_HOUR = 3600;

    private final double latitude;
    private final double longitude;

    public Coordinate(double x, double y){
        latitude = x;
        longitude = y;
    }

    public double getLatitude(){
        return latitude;
    }

    public double getLongitude(){
        return longitude;
    }

    //Returns absolute distance between two coordinates in kilometres
    public double getDistance(Coordinate target){
        double xComponent = Math.pow(KM_IN_DEGREE * Math.abs(this.latitude-target.latitude), 2);
        double yComponent = Math.pow(KM_IN_DEGREE * Math.abs(this.longitude-target.longitude), 2);
        return Math.sqrt(xComponent+yComponent);
    }

    //Returns travelling speed of current coordinate to target coordinate,
    // over an interval of SPEED_DURATION seconds, in km/hr
    public double getSpeed(Coordinate target){
        double distance = this.getDistance(target);
        return SECONDS_IN_HOUR*(distance/SPEED_DURATION);
    }

    //Returns angle of vector between current and target coordinate in degrees
    public double getDirection(Coordinate target){
        //since angle is simply a ratio measurement, no need to convert to kilometres
        //additionally, we MUST keep the original positive/negative values of x- and y-components
        double xComponent = target.latitude - this.latitude;
        double yComponent = target.longitude - this.longitude;
        return Math.toDegrees(Math.atan(yComponent/xComponent));
    }

}

