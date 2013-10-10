package mei.tcd.gps;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.google.android.gms.maps.model.Circle;

import java.util.ArrayList;
import java.util.Iterator;

import mei.tcd.util.SensorWriterSmta;

/**
 * Created by pessanha on 08-08-2013.
 */
public class GpsListener  implements LocationListener, GpsStatus.Listener  {
    private static final long MINIMAL_DISTANCE_BETWEEN_UPDATES = 0; // Em metros
    private static final long MINIMAL_TIME_BETWEEN_UPDATES = 0; // Em milisegundos

    private ArrayList<GpsSatellite> arraySatelites = new ArrayList<GpsSatellite>();// Arrays de envio de informa??o satelite (GpsSatellite - representa o estado atual de um satelite

    private boolean gpsEnabled = false;

    private LocationManager locationManager;// Location Manager providencia acesso aos servi?os de localiza??o
    private InterfaceGps iGps; // Instancia o interface nested nesta classe
    private boolean prefLogGps; // Efetuar log do GPS ou n?o
    private SharedPreferences preferences;// Gestor de preferencias
    // Variaveis para determina??o da localiza??o
    double newLatE6,  newLongE6, newAltE6; // localiza??o actual
    double previousLatE6,  previousLongE6, previousAltE6; // localiza??o anterior
    private float accuracy; // Precis?o indicada pelo GPS
    double gpsVelocity; // Velocidade indicada pelo GPS
    boolean startSaving; // Indica se ? para come?ar a gravar
    private SensorWriterSmta gps = new SensorWriterSmta();


    /**
     * M?todo construtor
     * Recebe o Context da actividade que o chamou e a instancia do interface iGPS
     *
     * @param _iGps Interface aplicado pela atividade
     * @param context Contexto da actividade que o chama
     *
     */
    public GpsListener(Context context,InterfaceGps _iGps){
        this.iGps = _iGps;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationManager.addGpsStatusListener(this); // Register Listener
        startSaving=  false;
        preferences = PreferenceManager.getDefaultSharedPreferences(context); //instancio as preferencias de modo static , sem new()
    }

    /**
     * Inicia o GPS com requisi??es efetuadas ao LocationManager usando um namedProvier e um PendingIntent (this)
     */
    public void startGps()
    {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,MINIMAL_TIME_BETWEEN_UPDATES, MINIMAL_DISTANCE_BETWEEN_UPDATES, this);
        prefLogGps = preferences.getBoolean("logGps", false); // Efetua log do GPS ou n?o

    }
    /**
     * Inicia a grava??o
     */
    public void start(String subdir)
    {
        startSaving = true;
        if(prefLogGps)
            gps.createFile(subdir,"smta_isep","gps");

    }
    /**
     * Parar a grava??o
     */
    public void stop()
    {
        startSaving = false;
        if(gps.file!=null)
            gps.closeFile();

    }
    /**
     * Finaliza as requisitos GPS
     */
    public void stopGps()
    {
        locationManager.removeUpdates(this); // Deixam de existir atualiza??es
        locationManager.removeGpsStatusListener(this); // Remove GPS listener
        this.gpsEnabled = false;
    }
    public boolean isGpsEnabled()
    {
        if ( !locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            return false;
        }else
        {
            return true;
        }
    }
    public boolean isGpsStarted()
    {
        if ( !this.gpsEnabled ) {
            return false;
        }else
        {
            return true;
        }
    }
    @Override
    public void onGpsStatusChanged(int event) {
        GpsStatus gpsStatus = locationManager.getGpsStatus(null); // Representa o estado atual do satelite
        switch (event) {
            case GpsStatus.GPS_EVENT_FIRST_FIX: // Primeiro fix desde que come?ou
                this.iGps.onGpsStatusChanged(event);
                break;
            case GpsStatus.GPS_EVENT_STARTED: // Quando o GPS come?a
                this.gpsEnabled = true;
                break;
            case GpsStatus.GPS_EVENT_STOPPED: // Quando o GPS acabou
                this.gpsEnabled = false;
                break;
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                Iterable<GpsSatellite>satellites = gpsStatus.getSatellites();
                Iterator<GpsSatellite> satI = satellites.iterator();
                if(!arraySatelites.isEmpty()) // Causa exfep??o quando vazio ao limpar
                    arraySatelites.clear();
                while (satI.hasNext()) {
                    GpsSatellite satellite = satI.next();
                    arraySatelites.add(satellite);
                }
                this.iGps.sateliteStatus(arraySatelites); // Guarda o estado num array para ser enviado pelo callback
                break;
        }
        this.iGps.onGpsStatusChanged(event); // Callback com o estado
    }

    @Override
    public void onLocationChanged(Location location) {
        this.iGps.onLocationChanged(location); // Callback implementado pelo interface
        accuracy = location.getAccuracy();
        newLatE6 = location.getLatitude();
        newLongE6 = location.getLongitude();
        newAltE6 = location.getAltitude();
        gpsVelocity = location.getSpeed();
        if(startSaving)
        {
            gps.writeThis(location.getTime() + "," + newLatE6+ "," + newLongE6 + "," + newAltE6 + "," + location.getBearing()+ ","+location.getSpeed()+"\n");
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }



    /**
     * Interface a implementar pelas actividades
     * As actividade dever?o implementar a seguinte interface.
     * Envia apenas quando altera??o de location (locationChange) ou gpsStatusChange.
     */
    public interface InterfaceGps {
        public void onLocationChanged(Location location);// LocationListener
        public void onGpsStatusChanged(int event); // GpsStatus Listener
        public void sateliteStatus( ArrayList<GpsSatellite> arraySatelites);

    }
}
