package mei.tcd.util;

import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import com.google.android.gms.maps.model.LatLng;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

public class KmlWriterSmta
{
    public File file;
    private FileWriter fileWriter;
    private boolean writeInsOnly;

    public void closeFile()
    {
        try
        {
            this.fileWriter.write("</Document>");
            this.fileWriter.write("</kml>");
            this.fileWriter.close();
            return;
        }
        catch (IOException localIOException)
        {
            while (true)
            {
                localIOException.printStackTrace();
                Log.v("SMTA closeFile Error: ", localIOException.getStackTrace().toString());
            }
        }
    }

    public void closeFile_lg(ArrayList<LatLng> pointsGps, ArrayList<LatLng> pointsIns)
    {
        StringBuilder localStringBuilder1;
        StringBuilder localStringBuilder2 = null;
        try
        {
            localStringBuilder1 = new StringBuilder();
            localStringBuilder2 = new StringBuilder();
            Iterator localIterator1 = pointsGps.iterator();
            while (localIterator1.hasNext())
            {
                LatLng localLatLng2 = (LatLng)localIterator1.next();
                localStringBuilder1.append(localLatLng2.longitude + "," + localLatLng2.latitude + " ");
            }
            Iterator localIterator2 = pointsIns.iterator();
            while (localIterator2.hasNext())
            {
                LatLng localLatLng1 = (LatLng)localIterator2.next();
                localStringBuilder2.append(localLatLng1.longitude + "," + localLatLng1.latitude + " ");
            }
            this.fileWriter.write("<Folder>\n\t\t\t<name>Camada sem título</name>\n\t\t\t<Placemark>\n\t\t\t\t<styleUrl>#gps</styleUrl>\n\t\t\t\t<name>Trajeto Gps</name>\n\t\t\t\t<LineString>\n\t\t\t\t\t<tessellate>1</tessellate>\n\t\t\t\t\t<coordinates>" + localStringBuilder1.toString() + "</coordinates>\n" + "\t\t\t\t</LineString>\n" + "\t\t\t</Placemark>\n" + "\t\t\t<Placemark>\n" + "\t\t\t\t<styleUrl>#ins</styleUrl>\n" + "\t\t\t\t<name>Trajeto Ins</name>\n" + "\t\t\t\t<LineString>\n" + "\t\t\t\t\t<tessellate>1</tessellate>\n" + "\t\t\t\t\t<coordinates>" + localStringBuilder2.toString() + "</coordinates>\n" + "\t\t\t\t</LineString>\n" + "\t\t\t</Placemark>\n" + "\t\t</Folder>");
            this.fileWriter.write("<Style id='gps'>\n\t\t\t<LineStyle>\n\t\t\t\t<color>ff0000ff</color>\n\t\t\t\t<width>4</width>\n\t\t\t</LineStyle>\n\t\t</Style>\n\t\t<Style id='ins'>\n\t\t\t<LineStyle>\n\t\t\t\t<color>ffff0000</color>\n\t\t\t\t<width>4</width>\n\t\t\t</LineStyle>\n\t\t</Style>\n\t</Document>\n</kml>");
            this.fileWriter.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
            Log.v("SMTA closeFile Error: ", e.getStackTrace().toString());
        }




    }

    public void createFile(String subdir, String dir, String file, Date data)
    {
        File localFile1 = new File(Environment.getExternalStorageDirectory() + "/" + dir + "/");
        if (!localFile1.exists())
        {
            Log.v("SMTA Create File", "A criar directorio.");
            localFile1.mkdir();
        }
        File localFile2 = new File(Environment.getExternalStorageDirectory() + "/" + dir + "/" + "/" + subdir + "/");
        if (!localFile2.exists())
        {
            Log.v("SMTA Create File", "A criar subdirectorio.");
            localFile2.mkdir();
        }
        String str = DateFormat.format("yyyy_MM_dd_hh_mm_ss", new Date()).toString();
        this.file = new File(Environment.getExternalStorageDirectory() + "/" + dir + "/" + "/" + subdir + "/" + file + str + ".kml");
        try
        {
            this.fileWriter = new FileWriter(this.file);
            writeKmlHeader(data);
            return;
        }
        catch (IOException localIOException)
        {
            while (true)
            {
                localIOException.printStackTrace();
                Log.v("SMTA Create File Error: ", localIOException.getStackTrace().toString());
            }
        }
    }

    public void writeKmlHeader(Date data)
    {
        Calendar localCalendar = Calendar.getInstance();
        localCalendar.setTime(data);


            try
            {
                this.fileWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n");
                this.fileWriter.write("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\r\n");
                this.fileWriter.write("<Document>\r\n");
                this.fileWriter.write("<name>SMTA KML ROUTE</name>\r\n");
                this.fileWriter.write("<Style id=\"paddle-a\">\n      <IconStyle>\n        <Icon>\n          <href>http://maps.google.com/mapfiles/kml/paddle/A.png</href>\n        </Icon>\n        <hotSpot x=\"32\" y=\"1\" xunits=\"pixels\" yunits=\"pixels\"/>\n      </IconStyle>\n    </Style>\n    <Style id=\"paddle-b\">\n      <IconStyle>\n        <Icon>\n          <href>http://maps.google.com/mapfiles/kml/paddle/B.png</href>\n        </Icon>\n        <hotSpot x=\"32\" y=\"1\" xunits=\"pixels\" yunits=\"pixels\"/>\n      </IconStyle>\n    </Style>\n    <Style id=\"hiker-icon\">\n      <IconStyle>\n        <Icon>\n          <href>http://www.bullguard.com/img/dot.png</href>\n        </Icon>\n        <hotSpot x=\"0\" y=\".5\" xunits=\"fraction\" yunits=\"fraction\"/>\n      </IconStyle>\n    </Style>");
                this.fileWriter.write("<open>1</open>\r\n");

            }
            catch (IOException localIOException)
            {
                localIOException.printStackTrace();
            }


    }


}