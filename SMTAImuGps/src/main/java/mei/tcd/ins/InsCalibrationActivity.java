package mei.tcd.ins;
import mei.tcd.smta.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import mei.tcd.util.VectorAverage;

/**
 * Created by pessanha on 27-07-2013.
 */
public class InsCalibrationActivity  extends Activity implements SensorEventListener{
    static final int OBSERVATIONS = 500; // Número de registos por defeito para usar na calibração em cada eixo para cada limite
    float[] accData = new float[3]; // Guarda os valores retornado pelo sensorEvent
    VectorAverage avg; // Classe para calcular a média de registos
    String axeTypeHighLowLimit = ""; // Qual o eixo e limite que está a ser calibrado
    static final int OFFSET=50;//Para assentar o acelerometro, necessitamos de deitar fora os primeiros 50 registos
    private String calibrationAxis = "";
    // mensagens para calibração dos eixos
    static String mensagemX_high = "Calibração eixo X (9.8), o telemovel deverá estar " +
            "pousado numa superficie plana virado para o lado esquerdo longitudinalmente!\n";
    static String mensagemX_low = "Calibração eixo X (-9.8), o telemovel deverá estar " +
            "pousado numa superficie plana virado para o lado direito longitudinalmente!\n";

    static String mensagemY_high = "Calibração eixo Y (9.8), o telemovel deverá estar " +
            "pousado numa superficie plana de pé!\n";
    static String mensagemY_low = "Calibração eixo Y (-9.8), o telemovel deverá estar " +
            "pousado numa superficie plana de pé virado ao contrário!\n";

    static String mensagemZ_high = "Calibração eixo Z (9.8), o telemovel deverá estar " +
            "pousado numa superficie plana deitado com o ecrã virado para cima!\n";
    static String mensagemZ_low = "Calibração eixo Z (-9.8), o telemovel deverá estar " +
            "pousado numa superficie plana deitado com o ecrã virado para baixo!\n";
    // wigets
    TextView calibrationText; // Mostra valores de calibração
    TextView messages; // Mostra mensagens de ajuda para calibrar os eixos
    ProgressBar progressBar; // Barra de progresso
    ImageButton btn_exit, btn_continue, btn_calibrate; // Botões do interface UI
    SensorManager sensorManager; // Instancia do gestor de sensores
    SharedPreferences preferences; // Instancia das preferencias
    private int progressCounter = 0; // Conta o progresso da calibração de cada eixo
    private boolean startCalibration = false; // Variavel lógica para controlar o inicio da calibração ao clicar o botão
    private float xHigh=0; // Limite Superior do eixo X
    private float xLow=0; // Limite Inferior do eixo X
    private float yHigh=0; // Limite Superior do eixo Y
    private float yLow=0; // Limite Inferior do eixo Y
    private float zHigh=0; // Limite Superior do eixo Z
    private float zLow=0; // Limite Inferior do eixo Z
    AlertDialog.Builder alertBox ; // Criar uma caixa de alerta

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN); // Obriga a full screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // Obriga a ecrã sempre ligado
        setContentView(R.layout.act_ins_calibration); // Tenho de importar o R do meu package e não o do Android "Android.R"
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // Obriga ecrã em modo retrato
        alertBox= new AlertDialog.Builder(this);

        calibrationText = (TextView) this.findViewById(R.id.calibrationText);
        messages = (TextView) this.findViewById(R.id.mensagem);
        btn_calibrate = (ImageButton) this.findViewById(R.id.btncalibrate);
        btn_exit = (ImageButton) this.findViewById(R.id.btnexit);
        btn_continue = (ImageButton) this.findViewById(R.id.btncontinue);
        progressBar = (ProgressBar) this.findViewById(R.id.pbcount);
        // Referenciar o sensorManager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        avg = new VectorAverage(OBSERVATIONS);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        calibrationText.setText("A calibrar...");
        progressBar.setVisibility(View.INVISIBLE);
        progressBar.setMax(OBSERVATIONS+OFFSET);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int eventType = sensorEvent.sensor.getType();// guarda o tipo de sensor que gerou o evento (INT)
        if(eventType==Sensor.TYPE_ACCELEROMETER) {
            //vai calibrar de acordo com os valores da gravidade, pois este vetor é tratado.
            accData[0] = sensorEvent.values[0];
            accData[1] = sensorEvent.values[1];
            accData[2] = sensorEvent.values[2];
            if(startCalibration)
            {
                if(progressCounter>=OFFSET){
                    if(!avg.hasDetermined){
                        avg.addVector(accData);

                    }
                    else
                    {
                        sensorManager.unregisterListener(this);
                        progressBar.setVisibility(View.INVISIBLE);
                        if(calibrationAxis=="AccCalibX_high")
                        {
                            xHigh = avg.getAveragedVetor()[0];
                            showDialog("AccCalibX_low");
                        }
                        if(calibrationAxis=="AccCalibX_low")
                        {
                            xLow = avg.getAveragedVetor()[0];
                            calculateCoefficients(xHigh,xLow,"X");
                            showDialog("AccCalibY_high");
                        }

                        if(calibrationAxis=="AccCalibY_high")
                        {
                            yHigh = avg.getAveragedVetor()[1];
                            showDialog("AccCalibY_low");
                        }
                        if(calibrationAxis=="AccCalibY_low")
                        {
                            yLow = avg.getAveragedVetor()[1];
                            calculateCoefficients(yHigh,yLow,"Y");
                            showDialog("AccCalibZ_high");
                        }

                        if(calibrationAxis=="AccCalibZ_high")
                        {
                            zHigh = avg.getAveragedVetor()[2];
                            showDialog("AccCalibZ_low");
                        }
                        if(calibrationAxis=="AccCalibZ_low")
                        {
                            zLow = avg.getAveragedVetor()[2];
                            calculateCoefficients(zHigh,zLow,"Z");
                            showCalibratedValues();
                        }


                    }


                }

                progressBar.setProgress(progressCounter);
                progressCounter ++;

            }

        }
        if(eventType==Sensor.TYPE_GRAVITY) {
            calibrationText.setText("Progresso:" + progressCounter + "\r\nX:" + sensorEvent.values[0] + "\r\n"+"Y:" + sensorEvent.values[1] + "\r\n"+"Z:" + sensorEvent.values[2]);
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
    public void showCalibratedValues()
    {
        // 9.8 = value * coeficiente
        // coeficiente = 9.8/value
        //calibrationText.setVisibility(View.GONE);
        float X_escala =  preferences.getFloat("k_X", 0);
        float Y_escala = preferences.getFloat("k_Y", 0);
        float Z_escala =  preferences.getFloat("k_Z", 0);
        float X_bias =  preferences.getFloat("b_X", 0);
        float Y_bias = preferences.getFloat("b_Y", 0);
        float Z_bias =  preferences.getFloat("b_Z", 0);

        calibrationText.setText("Valores de calibração\r\n" +
                "X_scale:" + Float.toString(X_escala) + "\r\n"+
                "Y_scale:" + Float.toString(Y_escala) + "\r\n"+
                "Z_scale:" + Float.toString(Z_escala) + "\r\n"+
                "X_Bias:" + Float.toString(X_bias) + "\r\n"+
                "Y_Bias:" + Float.toString(Y_bias) + "\r\n"+
                "Z_Bias:" + Float.toString(Z_bias));
    }
    /**
     * Grava as preferencias em ficheiro.
     * /data/data/YOUR_PACKAGE_NAME/shared_prefs/YOUR_PREFS_NAME.xml
     *
     * @param k o valor encontrado para escala
     * @param b o valor encontrado para bias
     * @param tipo o tipo que grava X, Y ou Z
     */
    public void savePreferences(float k,float b,String tipo)
    {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putFloat("k_"+tipo, k);
        editor.putFloat("b_"+tipo, b);
        editor.commit();
    }
    /***
     * Calcula o BIAS e ESCALA, valores para serem coeficientes da determinação da aceleração
     * K = 2 * g / (Mhigh-Mlow)
     * X = Mlow * K  + g
     * @param high Limite superior
     * @param low Limite Inferior
     * @param tipo Tipo de eixo
     */
    public void calculateCoefficients(float high, float low, String tipo)
    {
        float k_temp = 0;
        float b_temp = 0;
        k_temp = (2*SensorManager.GRAVITY_EARTH) / (high-low);
        b_temp = (low * k_temp) + SensorManager.GRAVITY_EARTH;
        savePreferences(k_temp,b_temp,tipo);
    }
    public void showDialog(final String tipo)
    {
        btn_exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InsCalibrationActivity.this.finish();
            }
        });
        calibrationText.setText("");
        btn_calibrate.setVisibility(View.VISIBLE);
        avg.hasDetermined = false;
        avg.resetCalibration();
        startCalibration = false;
        progressCounter =0;
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
                SensorManager.SENSOR_DELAY_FASTEST);
        if(tipo=="AccCalibX_high")
        {
            messages.setText(mensagemX_high);
        }
        if(tipo=="AccCalibX_low")
        {
            messages.setText(mensagemX_low);
        }
        if(tipo=="AccCalibY_high")
        {
            messages.setText(mensagemY_high);
        }
        if(tipo=="AccCalibY_low")
        {
            messages.setText(mensagemY_low);
        }
        if(tipo=="AccCalibZ_high")
        {
            messages.setText(mensagemZ_high);
        }
        if(tipo=="AccCalibZ_low")
        {
            messages.setText(mensagemZ_low);
        }
        btn_calibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calibrationText.setVisibility(View.VISIBLE);
                calibrationText.setText("A calibrar...");
                startCalibration = true;
                progressBar.setVisibility(View.VISIBLE);
                calibrationAxis = tipo;
                btn_calibrate.setVisibility(View.GONE);
            }
        });

        //alertbox.show();

    }
    @Override
    public void onResume() {
        super.onResume();

        btn_calibrate.setVisibility(View.INVISIBLE);
        btn_exit.setVisibility(View.VISIBLE);
        messages.setVisibility(View.VISIBLE);
        messages.setText("Clique no botão continuar para proceder à calibração.");
        //vou verifiar se já temos valores para mostrar
        float X_escala =  preferences.getFloat("k_X", 0);
        float Y_escala = preferences.getFloat("k_Y", 0);
        float Z_escala =  preferences.getFloat("k_Z", 0);
        float X_bias =  preferences.getFloat("b_X", 0);
        float Y_bias = preferences.getFloat("b_Y", 0);
        float Z_bias =  preferences.getFloat("b_Z", 0);

        calibrationText.setText("Valores de calibração\r\n" +
                "X_scale:" + Float.toString(X_escala) + "\r\n"+
                "Y_scale:" + Float.toString(Y_escala) + "\r\n"+
                "Z_scale:" + Float.toString(Z_escala) + "\r\n"+
                "X_Bias:" + Float.toString(X_bias) + "\r\n"+
                "Y_Bias:" + Float.toString(Y_bias) + "\r\n"+
                "Z_Bias:" + Float.toString(Z_bias));
        btn_continue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_continue.setVisibility(View.INVISIBLE);
                btn_calibrate.setVisibility(View.VISIBLE);
                btn_exit.setVisibility(View.INVISIBLE);
                messages.setVisibility(View.VISIBLE);

                showDialog("AccCalibX_high");
            }
        });
        btn_exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
        // Tenho sempre de efetuar o unRegister para prevenir o gasto da bateria
        // device's battery.
        sensorManager.unregisterListener(this);

    }
}
