package mei.tcd.util;

/**
 * Created by pessanha on 15-10-2013.
 */
public class Wsg84 {
    public static double EarthRadius = 6378137.0D;

    public static double[] FindPointAtDistanceFrom(double paramDouble1, double paramDouble2, double paramDouble3, double paramDouble4)
    {
        double[] arrayOfDouble = new double[2];
        double d1 = paramDouble4 / EarthRadius;
        double d2 = Math.toRadians(paramDouble1);
        double d3 = Math.toRadians(paramDouble2);
        double d4 = Math.asin(Math.sin(d2) * Math.cos(d1) + Math.cos(d2) * Math.sin(d1) * Math.cos(paramDouble3));
        double d5 = d3 + Math.atan2(Math.sin(paramDouble3) * Math.sin(d1) * Math.cos(d2), Math.cos(d1) - Math.sin(d2) * Math.sin(d4));
        arrayOfDouble[0] = d4;
        arrayOfDouble[1] = d5;
        return arrayOfDouble;
    }

    public static double getLastDistance(float paramFloat1, float paramFloat2)
    {
        return paramFloat2 - paramFloat1;
    }
}
