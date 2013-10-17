package mei.tcd.smta;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.location.GpsSatellite;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;
import android.view.WindowManager;
import android.os.Bundle;
import android.support.v4.app.*;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.google.android.gms.common.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import mei.tcd.gps.GpsListener;
import mei.tcd.ins.InsListener;
import mei.tcd.ins.InsOrientationView;
import mei.tcd.util.KmlWriterSmta;

/**
 * Created by pessanha on 07-08-2013.
 */
public class InsGpsActivity extends Activity implements GpsListener.InterfaceGps,InsListener.InterfaceIns {
    private static final Handler mHandler = new Handler();
    private static final float RAD2GRAUS = 57.2957795f;// Valor para passar de radianos para graus
    private static final float MSTOKM = 3.6f; //MSTOKM - Metros por segundo para KM/H
    private boolean gpsReady = false; // Guarda se recebeu um fix do GPS do onLocationChanged e se está pronto
    private boolean insReady = false; // Guarda ins está pronto a ser usado
    private boolean startSystemSaving;
    private boolean isCorrectlyOriented;
    // Google Maps V2
    private GoogleMap mapView;
    private float accuracy;
    private float bearingGps;
    Circle circle;
    // Listeners INS e GPS
    private InsListener insListener;
    private GpsListener gpsListener;
    // UI objectos- Não necessito de instanciar os botões porque já têm o onClick definido.
    private ViewSwitcher mViewSwitcher;
    private InsOrientationView orientationView;// A minha views para o INS
    private FrameLayout rootLayout;
    private View warningMessage;
    private LayoutInflater factory;
    private ImageButton btn_start, btn_stop,btn_stopGps; // Botões do interface UI
    private TextView textVelocity;

    double currentAltitudeGps;
    double currentAltitudeIns;
    double currentLatitudeGps;
    double currentLatitudeIns;
    double currentLongitudeGps;
    double currentLongitudeIns;

    double previousAltitudeGps;
    double previousAltitudeIns;
    double previousLatitudeGps;
    double previousLatitudeIns;
    double previousLongitudeGps;
    double previousLongitudeIns;

    TimerTask drawMapTask;
    TimerTask showInfoTask;
    private float filteredVelocity;
    private GroundOverlay groundOverlay;
    private GroundOverlayOptions groundOverlayOptions;
    private BitmapDescriptor image;
    private float insCurrentPosition;
    private float insPreviousPosition;
    private KmlWriterSmta kml = new KmlWriterSmta();
    private NumberFormat nf;
    private Polyline p1;
    private Polyline p2;
    ArrayList<LatLng> pointsGps = new ArrayList();
    ArrayList<LatLng> pointsGpsSave = new ArrayList();
    ArrayList<LatLng> pointsIns = new ArrayList();
    ArrayList<LatLng> pointsInsSave = new ArrayList();
    private PolylineOptions polylineOptionsGps;
    private PolylineOptions polylineOptionsIns;
    private boolean prefLogKml;
    private boolean prefMapView;
    private SharedPreferences preferences;
    private TextView textGpsBearing;
    private TextView textGpsInsVelocity;
    private TextView textGpsVelocity;
    private TextView textInsBearing;
    private float velocityGps;
    private float velocityIns;



    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
       // getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.act_insgps);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setUpMapIfNeeded(); // Verifica se o mapa está instanciado. Se não estiver, então instancia.
        gpsListener = new GpsListener(getApplicationContext(),this); // Vai aplicar os métodos
        insListener = new InsListener(getApplicationContext(),this); // Vai aplicar os métodos
        mViewSwitcher = (ViewSwitcher)findViewById(R.id.viewSwitcher1); //Viewswitcher para slider
        // instancio as minha views
        orientationView = (InsOrientationView) this
                .findViewById(R.id.orientacaoView); // indico a minha view para compasso
        // Get root Element da view lifaga á minha atividade sem conhecer o seu nome, tipo ou ID
        rootLayout = (FrameLayout )findViewById(android.R.id.content); // instancio a minha layout. Podia instanciar directamente a LinerLayout e alterar o android.R.id.content para R.id.idLayout
        factory = LayoutInflater.from(this); // Obter o layoutInflater do contexto
        warningMessage = factory.inflate(R.layout.warning_not_oriented, null,false); // a view a carregar (inflate=render)
        this.isCorrectlyOriented = true; // Controla se está orientado ou não
        startSystemSaving = false; // Variavel que alerta o inicio da gravação
        btn_start = (ImageButton)findViewById(R.id.btngpsinsstart);
        btn_stop = (ImageButton)findViewById(R.id.btngpsinsstop);
        textVelocity = (TextView)findViewById(R.id.velocidade);
    }

    /**
     * Iniciar o SystemCheck sempre que é resumido após onCreate ou Pause
     */
    @Override
    protected void onResume() {
        super.onResume();
        startSystemSaving = false;
        SystemCheck();

    }

    /**
     * Quando voltar atrás ao ecra anterior temos de certificar que temos tudo limpo
     */
    @Override
    protected void onPause()
    {
        super.onPause();
        startSystemSaving = false;
       if(gpsListener!=null) // verifica se já não está no GC
            gpsListener.stopGps();
       //     gpsListener=null; // Pronto para GC
        if(insListener!=null)
            insListener.stopIns();
        //    insListener=null;
    }

    /**
     * Vai verificar se já existe uma instancia do mapView, se não existir vai carregar
     */
    private void setUpMapIfNeeded() {
        // Verificar null para confirmar se já não foi instanciado o mapa.
        if((this.mapView==null) && ((MapFragment)getFragmentManager().findFragmentById(R.id.mapview1)!=null))
        {
            this.mapView = ((MapFragment)getFragmentManager().findFragmentById(R.id.mapview1)).getMap();
            if (mapView == null) {
                this.image = BitmapDescriptorFactory.fromResource(R.drawable.icon_map);
                this.mapView.animateCamera(CameraUpdateFactory.zoomTo(17));
               /* // Tentar obter o mapa do SupportMapFragment.
                if (((MapFragment) getFragmentManager().findFragmentById(R.id.mapview1)) != null) {
                    mapView = ((MapFragment) getFragmentManager().findFragmentById(R.id.mapview1))
                            .getMap();
                }*/
            }
        }

    }
    /**
     * Click Lisntener do botão próximo ecrã para o ViewSwitcher
     * O onClick está definido no botão no layout
     */
    public void onNextScreenClick(View view)
    {
        mViewSwitcher.showNext();
    }
    /**
     * Click Lisntener do botão ecrã anterior para o ViewSwitcher
     * O onClick está definido no botão no layout
     */
    public void onPreviousScreenClick(View view)
    {
        mViewSwitcher.showPrevious();
    }
    /**
     * Click Lisntener do botão Começar
     * O onClick está definido no botão no layout
     *
     * Vai iniciar o processo de GPSINS.
     */
    public void onStartClick(View view)
    {
        Date data = new Date();
        String subDirName = android.text.format.DateFormat.format("yyyy_MM_dd_hh_mm_ss", data).toString();
        gpsListener.start(subDirName); // Inicia a gravação
        insListener.start(subDirName); // Inicia a gravação
        startSystemSaving = true;
        btn_start.setVisibility(ImageButton.GONE);
        btn_stop.setVisibility(ImageButton.VISIBLE);
    }
    /**
     * Click Lisntener do botão Começar
     * O onClick está definido no botão no layout
     *
     * Vai iniciar o processo de GPSINS.
     */
    public void onStopClick(View view)
    {
        startSystemSaving = false;
        gpsListener.stop(); // Inicia a gravação
        insListener.stop(); // Inicia a gravação
        btn_start.setVisibility(ImageButton.VISIBLE);
        btn_stop.setVisibility(ImageButton.GONE);
    }

    /**
     * Check system ready providencia um conjunto de operações para verificar se todos os preceitos
     * satisfazem o inicio do sistema
     *
     * 1º Verificar se o GPS está Ligado
     * 2º Verificar se tem zona fixa
     * 3º Verifica se o INS já está ligado e funcional. Deverá esperar um tempo para que os registos
     * dos filtros estejam estabilizados.
     */
    private void SystemCheck(){

        /* ------------ GPS LIGADO? ----------------------------------------------*/
        if(gpsListener.isGpsEnabled())
        {
            gpsListener.startGps(); // Se GPS enable então começar GPS (Registar Listener do GPS e callbacks do GPS)
            new CheckGpsIns().execute(); // inicia os listeners do INS caso GPS ativo
        }
        else
        {
            showAlertMessageNoGps(); // Se GPS não está enable, então mostra erro e  retorna falso.

        }


    }

    /**
     * Mostra mensagem alerta quando o GPS aparece como disable. Possibilita ao utilizador aceder
     * à configuração do dipositivo.
     */
    private void showAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("O GPS parece estar desligado, deseja ligar?")
                .setCancelable(false)
                .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));

                    }
                })
                .setNegativeButton("Não", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }
    private void DrawMapTask()
    {
        Timer localTimer = new Timer();
        this.drawMapTask = new TimerTask(){
            public void run(){
                InsGpsActivity.this.drawMap();
            }
        };
        localTimer.schedule(this.drawMapTask,1000,2000);

    }
    private void ShowInfoTask(){
        Timer localTimer = new Timer();
        this.showInfoTask = new TimerTask(){
            public void run(){
                InsGpsActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        InsGpsActivity.this.textGpsBearing.setText(InsGpsActivity.this.nf.format(InsGpsActivity.this.bearingGps));
                        InsGpsActivity.this.textGpsVelocity.setText(InsGpsActivity.this.nf.format(MSTOKM*InsGpsActivity.this.velocityGps));
                        InsGpsActivity.this.textInsBearing.setText(InsGpsActivity.this.nf.format((360.0f + RAD2GRAUS * InsGpsActivity.this.insListener.getAziPitRoll()[0])%360.0f));
                        InsGpsActivity.this.nf.format(MSTOKM * InsGpsActivity.this.insListener.getVelocity());
                        InsGpsActivity.this.textGpsInsVelocity.setText(InsGpsActivity.this.nf.format(MSTOKM * InsGpsActivity.this.velocityIns));
                    }
                });
            }
        };
        localTimer.schedule(this.showInfoTask,1000,100);
    }
    /**
     * Actualizo a view de acordo com os parametros Azimuth, Pitch e Roll passados em array
     * @param apr array Azimuth, pitch e roll
     */
    private void updateOrientationView(float[] apr) {
        float[] copiaApr = new float[3];
        copiaApr = apr.clone();
        // Para passar em graus
        copiaApr[0] *= RAD2GRAUS;
        copiaApr[1] *= RAD2GRAUS;
        copiaApr[2] *= RAD2GRAUS;
        if (orientationView != null) {
            orientationView.setAzimuth(copiaApr[0]);
            orientationView.setPitch(copiaApr[1]);
            orientationView.setRoll(copiaApr[2]);
            // Obriga a chamar o onDraw
            orientationView.invalidate();
        }

    }
    public void drawMap(){
        runOnUiThread(new Runnable() {
            @Override
            public synchronized void  run() {
                if((InsGpsActivity.this.polylineOptionsGps != null) && (InsGpsActivity.this.pointsGps.size() % 600 !=0))
                    polylineOptionsGps = new PolylineOptions()
                            .addAll(InsGpsActivity.this.pointsGps)
                            .width(5.0f)
                            .color(-16776961)
                            .zIndex(2.0f);
                if(InsGpsActivity.this.p1 != null)
                    InsGpsActivity.this.p1.remove();

                InsGpsActivity.this.p1 = mapView.addPolyline(InsGpsActivity.this.polylineOptionsGps);
                InsGpsActivity.this.pointsGps.clear();
                //---------------------------------------------
                if((InsGpsActivity.this.polylineOptionsIns != null) && (InsGpsActivity.this.pointsIns.size() % 600 !=0))
                    polylineOptionsIns = new PolylineOptions()
                            .addAll(InsGpsActivity.this.pointsIns)
                            .width(5.0f)
                            .color(-65536)
                            .zIndex(2.0f);
                if(InsGpsActivity.this.p2 != null)
                    InsGpsActivity.this.p2.remove();

                InsGpsActivity.this.p2 = mapView.addPolyline(InsGpsActivity.this.polylineOptionsIns);
                InsGpsActivity.this.pointsIns.clear();

                if (InsGpsActivity.this.groundOverlayOptions == null)
                    InsGpsActivity.this.initializeGroundOverlayOptions();
                

            }
        });
    }
    public void initializeGroundOverlayOptions()
    {
        if((this.gpsListener.isGpsEnabled()) && (this.gpsListener.isGpsStarted())){
            this.groundOverlayOptions = new GroundOverlayOptions()
                    .image(this.image).transparency(0.5f)
                    .position(new LatLng(this.currentLatitudeGps,this.currentLongitudeGps),30.0f)
                    .bearing((360.0f + 57.29578f * this.insListener.getAziPitRoll()[0] % 360.0f));

        }
    }
    /* ------------   Calback do GPS Change ----------------------------*/
    @Override
    public void onLocationChanged(Location location) {
        if(!this.gpsReady)
            this.gpsReady = true; // Coloca campo da classe HasFix=true para SystemCheck Dialogprogress tester
    }

    @Override
    public void onGpsStatusChanged(int event) {

    }

    @Override
    public void sateliteStatus(ArrayList<GpsSatellite> arraySatelites) {

    }
    /* ------------   Calback do INS Change ----------------------------*/
    @Override
    public void onInsReady() {
        if(!this.insReady)
            this.insReady=true;
    }

    @Override
    public void onOrientationChange() {
        runOnUiThread(new Runnable() { // Para correr na UI visto que os sensores estão a correr numa threadHandler
            public void run() {
                updateOrientationView(insListener.getAziPitRoll());
            }
        });

    }

    @Override
    public void onVelocityChange() {
        runOnUiThread(new Runnable() { // Para correr na UI visto que os sensores estão a correr numa threadHandler
            public void run() {
                NumberFormat nf = NumberFormat.getInstance();
                nf.setMinimumFractionDigits(2);
                nf.setMaximumFractionDigits(2);
                String output = nf.format(insListener.getVelocity()*MSTOKM);
                textVelocity.setText(output + " K/h");
                //textVelocity.setText(Float.toString(insListener.getVelocity()*MSTOKM));
            }
        });
    }

    @Override
    public void onPositionChange() {

    }

    /**
     * Verifica o pitch e roll. É uma restrição necessária por forma a que tenhamos certeza na
     * determinação da orientação e velocidade.
     */
    @Override
    public void onBeforeStartNotOriented() {
       // ((ViewStub) findViewById(R.id.viewStub)).setVisibility(View.VISIBLE);
    if(isCorrectlyOriented && !startSystemSaving){
        runOnUiThread(new Runnable() {
            public synchronized  void run() {
                rootLayout.addView(warningMessage);
                btn_start.setEnabled(false);
             }
        });
        isCorrectlyOriented = false;
    }
    }
    /**
     * Verifica o pitch e roll. É uma restrição necessária por forma a que tenhamos certeza na
     * determinação da orientação e velocidade.
     */
    @Override
    public void onBeforeStartOriented() {
        if(!isCorrectlyOriented && !startSystemSaving){
        runOnUiThread(new Runnable() {
            public synchronized void run() {
                rootLayout.removeView(warningMessage);
                btn_start.setEnabled(true);
            }
        });
        isCorrectlyOriented = true;
        }
    }

    @Override
    public void onVehicleStopDetected() {

    }

    /**
     * AsyncTask para progress em background do system Check.
     */
    public class CheckGpsIns extends AsyncTask<Void, Void, Void>
    {
        ProgressDialog dialog = new ProgressDialog(InsGpsActivity.this);
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();

            dialog.setCancelable(true);
            dialog.setMessage("A inicializar o sistema...");
            // Coloca botão cancelar no dialog
            dialog.setButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    gpsListener.stopGps();
                    if(insListener!=null)
                        insListener.stopIns();
                    dialog.dismiss();

                }
            });
            // Permite exeutar ações quando BackButton android clicado
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener(){
                @Override
                public void onCancel(DialogInterface dialog){
                    gpsListener.stopGps();
                    if(insListener!=null)
                        insListener.stopIns();
                    dialog.dismiss();
                }});
            // dialog.setOnCancelListener(cancelDialog());
            dialog.show();
            dialog.setMessage("A iniciar o GPS...");

        }



        @Override
        protected void onProgressUpdate(Void... values) { // Para correr no UI
            super.onProgressUpdate(values);
           if(gpsReady)
            {
                insListener.startIns();// Inicio agora o INS
                dialog.setMessage("A iniciar o INS...");
            }
        }

        @Override
        protected void onPostExecute(Void result)
        {
            dialog.dismiss();
            Toast.makeText(getApplicationContext(), (String) "Sistema está pronto para arrancar!!", Toast.LENGTH_LONG).show();
        }
        @Override
        protected Void doInBackground(Void... msg) {
            while(!gpsReady)
            {
            }
            publishProgress(); // Gps pronto, iniciar INS
            while(!insReady)
            {
            }
            return null;
        }
    }
}
