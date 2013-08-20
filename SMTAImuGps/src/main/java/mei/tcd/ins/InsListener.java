package mei.tcd.ins;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;

import java.util.Date;

import mei.tcd.filtros.LowPassFilter;
import mei.tcd.filtros.MovingAverage;
import mei.tcd.smta.R;
import mei.tcd.util.Operations;
import mei.tcd.util.SensorWriterSmta;

/**
 * Created by pessanha on 03-08-2013.
 */
public class InsListener implements SensorEventListener {
    // Variaveis estaticas
    private static final float NS2S = 1.0f / 1000000000.0f; //Para calcular segundos de nanosegundos (multiplicar este valor pelo tempo)
    private static final float RAD2GRAUS = 57.2957795f;// Valor para passar de radianos para graus
    private static final Handler mHandler = new Handler(); // Agendar mensagens na thread para envio dos callbacks
    // Tipo de retorno do callback implementado
    private Context context;
    private Resources resources;// Aceder aos resource (Strings, etc...)
    private SharedPreferences preferences;// Gestor de preferencias

    // Vetores
    private float[] mAccData = new float[3]; // Vetor acelera��o obtida do Acelerometro nos 3 eixos (X, Y e Z)
    private float[] mGravityData = new float[3]; // Vetor acelera��o de gravidade nativo Android nos 3 eixos (X, Y e Z) (Sensor Virtual)
    private float[] mAccCalibratedData = new float[3]; // Vetor acelera��o com calibra��o nos 3 eixos (X, Y e Z)
    private float[] mAccFilteredData = new float[3]; // Vetor acelera��o filtrada (lpf ou mv) obtida do Acelerometro nos 3 eixos (X, Y e Z)
    private float[] mGyroData = new float[3]; // Vetor velocidade angular obtida do Giroscopio  nos 3 eixos (X, Y e Z)
    private float[] mMagData = new float[3]; // Vetor Micro Teslas obtida do magnetometro  nos 3 eixos (X, Y e Z)
    private float[] mMyLinearAccData = new float[3]; // Acelerometro linear retirando a gravidade com DCM na classe InsClass com m?todo getAccLinear
    private float[] mMyGravityData = new float[3]; // Vetor acelera��o gravidade com filtro passa baixas para retirar gravidade ao acelerometro
    private float[] mLinearAccData = new float[3]; // Acelerometro linear nativo android (Sensor Virtual)
    private float[] mAccAvgData = new float[3]; // Vetor acelea��o com filtro m�dia m�vel
    private float[] mAccLpfData = new float[3]; // Vetor acelea��o com filtro low Pass filter
    private float[] mRotvectorData = new float[3]; // Vetor rota��o obtido do sensor virtual Rotation Vector.
    private float[] mAziPitRol = new float[3]; // Azimuth pith e roll de retorno
    private float[] mCalibVetor_scale = new float[3]; // Vai guardar os valores obtidos da calibra��o do acelerometro e usa-los na obten��o de valores Acc melhorados.
    private float[] mCalibVetor_bias = new float[3]; // Vai guardar os valores obtidos da calibra��o do acelerometro e usa-los na obten��o de valores Acc melhorados.
    private float mVelocity; // Velocidade em Km ((float) ins.getVelocidadeTotal() / MSTOKM;)

    // Preferencias (SharedPreferences)
    private int prefSensorFrequency; // Frequencia dos sensores definida nas preferencias e sugeridas nos listeners dos sensores.
    private String prefOrientationType; // Tipo de orienta��o definida (Acc+Mag, Rotvet, Fusao)
    private String prefNoiseFilter; // Tipo de filtro para atenuar o ru�do (lpf, mv)
    private float prefLpfAlpha; // Alpha para filtro LPF no tratamento da acelera��o Ru�do
    private float prefGravityAlpha;
    private int prefSizeSma; // Numero de pontos associados ao filtro m�dia m�vel
    private boolean prefUsesCalibration; // Indica se os valores obtidos da calibra��o v�o ser utilizados
    private int prefCalibrationRecordsCount; // Numero de registo para efetuar a calibra��o (avg)
    private int prefThresholdVelocityCompute; // N�mero de registos para determinar o threshold de c�lculo da velocidade
    private boolean prefLogSensors; // Efetuar log dos sensores ou n�o
    private boolean prefLogGps; // Efetuar log do GPS ou n�o
    private int prefIpPort; // Porta uDP
    private String prefIpAddress; // Endere�o IP para envio pacotes UDP
    private boolean prefUdpsend; // Envia UDP ou n�o

    private boolean hasStarted; // Indica se j� come?ou ou n�o

    // Pacotes UDP para controlo
    private int accUdpCount; // Controla pacotes UDP para envio de registos de Acelera??o
    private int gyroUdpCount; // Controla pacotes UDP para envio de registos de Girosc?pio
    private int gravityUdpCount; // Controla pacotes UDP para envio de registos de Gravidade (gravidade filtrada do aceler?metro com LPF)
    private int magUdpCount; // Controla pacotes UDP para envio de registos de Magnet?metro
    private int linAccUdpCount; // Controla pacotes UDP para envio de registos de Acelera??o Linear
    private int myLinAccUdpCount; // Controla pacotes UDP para envio de registos de Minha Acelera??o Linear
    private int orientationUdpCount; // Controla pacotes UDP para envio de registos de Orienta??o APR

    // Instanciar interfaces e classes
    private InterfaceIns iIns; // Interface para comunicar com quem instancia e enviar os retornos quando prontos.
    private SensorManager sensorManager = null;// sensorManager - Instancia do sensorManager
    private InsClass ins;// ins - Instancia classe INS
    private Operations operations;// ops - Instancia da classe de opera??es gen?ricas

    // Filtro m�dia m�vel
    private MovingAverage mSmaX; //=new MovingAverage(SMA_SIZE);
    private MovingAverage mSmaY; //= new MovingAverage(SMA_SIZE);
    private MovingAverage mSmaZ;// = new MovingAverage(SMA_SIZE);

    // Filtro LowPassFilter
    private LowPassFilter lowPassFilter;
    private float AccAlpha; // Alpha para filtro do acelerometro
    private LowPassFilter gravityLowPassFilter;
    private float gravityAlpha; // Alpha para filtro gravidade

    // Log sensors
    private SensorWriterSmta accWriter = new SensorWriterSmta(); // Escreve registos provenientes do acelerometro
    private SensorWriterSmta accLinearWriter = new SensorWriterSmta(); // Escreve registos provenientes do acelerometro linear
    private SensorWriterSmta accMeuLinearWriter = new SensorWriterSmta(); // Escreve registos provenientes do acelerometro linear calculado pela DCM
    private SensorWriterSmta magnetometroWriter = new SensorWriterSmta(); // Escreve registo provenientes do magnet�metro
    private SensorWriterSmta giroscopioWriter = new SensorWriterSmta(); // Escreve registo provenientes do girosc+opio
    private SensorWriterSmta gravidadeWriter = new SensorWriterSmta(); // Escreve registos provenientes do sensor virtual gravidade
    private SensorWriterSmta accmagaprWriter = new SensorWriterSmta(); // Escreve registos proveninetes do Azimuth, Pith e Roll do Acc+Mag
    private SensorWriterSmta rvaprWriter = new SensorWriterSmta(); // Escreve registos proveninetes do Azimuth, Pith e Roll do RotVet
    private SensorWriterSmta fusaoaprWriter = new SensorWriterSmta(); // // Escreve registos proveninetes do Azimuth, Pith e Roll do filtro complementar
    private String sensorDataToSave; // String que guarda os registos dos sensores para serem gravados num string

    // Variveis de apoio
    private long previousEventTimeGravity = 0;
    private long previousEventTimeAcc = 0;
    private long previousEventTimeGyro = 0;
    private long previousEventTimeMag = 0;
    private long previousEventTimeRotVet = 0;
    private long previousEventLinearAcc = 0;
    private float filterCoeficient; // Valor do filtro quando LPF ou MV para gravar
    private boolean startSaving;

    // Obten��o do threshold
    private boolean hasThreshold; // Verifica se j� tem o threshold para o limite alto e baixo da velocidade
    private float highLimitThresholdAcc; // Magnitude de limite superior da acc - Obten��o threshold
    private float lowLimitThresholdAcc; // Magnitude de limite inferior da acc - Obten��o threshold
    private int thresholdRecordCount; // Contador de registos a serem udados na m�dia do threshold



    /**
     * M�todo contrutor.
     *
     * @param _context interface com a informa??o global sobre a aplica??o.
     * @param _iIns    interface do tipo OnInsChanged para comunicar os eventos. Esta interface deve estar implementada na actividade que o chama.
     */
    public InsListener(Context _context, InterfaceIns _iIns) {
        this.context = _context;
        this.setVelocidadeZero(); // Inicializar a velocidade a zero.
        resources = context.getResources();// Tenho de ir buscar os resources, caso contrario devolve nullpointer
        // Referenciar o sensorManager
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);// Para referencia o servi?o de sistema para os sensores
        ins = new InsClass(); // instnacio a classe INS
        operations = new Operations();
        preferences = PreferenceManager.getDefaultSharedPreferences(context); //instancio as preferencias de modo static , sem new()
        iIns = _iIns; // O listener referencia ? o que vem por referencia quando inicializo o InsListener.
        // Em java os arrays s�o inicializados a zero por defeito, logo desnecess?rio inicializar a zero.
        mCalibVetor_scale[0] = 1;
        mCalibVetor_scale[1] = 1;
        mCalibVetor_scale[2] = 1;
        mCalibVetor_bias[0] = 0;
        mCalibVetor_bias[1] = 0;
        mCalibVetor_bias[2] = 0;
        sensorDataToSave ="";
        startSaving = false;
        hasThreshold = false;
        thresholdRecordCount=0;
        highLimitThresholdAcc=0;
        lowLimitThresholdAcc=0;
    }

    /**
     * Coloca a velocidade em Zero, fazendo com que a posi��o n�o aumente.
     */
    public void setVelocidadeZero() {
        mVelocity = 0.0f;

    }
    /**
     * Update � velocidade com acelera��o - gravidade filtro
     */
    public void setVelocity(float[] filteredAccData,float[] gravityData, float dt)
    {
        // Veriicar se vale a pena registar como altera��o de velocidade

        if(operations.getMagnitude(filteredAccData)>highLimitThresholdAcc || operations.getMagnitude(filteredAccData)<lowLimitThresholdAcc){
            ins.updateVelocity(filteredAccData,gravityData,dt);
            mHandler.post(new Runnable() {

                public void run() {
                    iIns.onVelocityChange();
                }
            });
        }

        //iIns.onOrientationChange(); //Se j� tiver o Azimuth, o pitch e rool ent�o envio informa��o pelo callback
    }
    /**
     * Update � velocidade
     */
    public float getVelocity()
    {

        //mVelocity = operations.getMagnitude(ins.getVelocity());
        mVelocity = ins.getVelocity();
        return mVelocity;
    }
    /**
     * Verifica se o sistema j� inicializou aquando do startHandler ap?s 10 segundos.
     *
     * @return inicioIns boolean true ou false
     */
    public boolean getInsState() {
        return hasStarted;
    }

    /**
     * Altera a variavel mInicioIns conforme o estado do Ins.
     */
    private void setInsState(boolean _inicioIns) {
        hasStarted = _inicioIns;
    }
    /*------------- Get e Set do Aimute, pitch e roll para orientationView ----------------------------*/
    /**
     * Retorna o Azimuth, pitch e roll.
     * Azimute magn�tico - Medida horizontal em graus sobre o norte magn�tico. Medido sobre o Z
     * Pitch - Rota��o sobre o eixo do X
     * Roll - Rota��o sobre o eixo do Y
     *
     * @return AziPitRoll array float[] valor[0] - Azimute, Valor[1] - Pitch, Valor[2] - Roll (Valores em radianos)
     *
     */
    public float[] getAziPitRoll(){
        return mAziPitRol;

    }
    /**
     * Define o valor Azimuth, pitch e roll.
     * Azimute magn�tico - Medida horizontal em graus sobre o norte magn�tico. Medido sobre o Z
     * Pitch - Rota��o sobre o eixo do X (0� -> -90� -> 0)
     * Roll - Rota��o sobre o eixo do Y (0� -> 90� ->
     *
     * Aqui vai activar um evento sobre o interface entretanto inicializado na classe que o instancia.
     *
     */
    private void setAziPitRoll(float[] _AziPitRol){
        mAziPitRol = _AziPitRol;
        mHandler.post(new Runnable() {

            public void run() {
                iIns.onOrientationChange();
            }
        });
        //iIns.onOrientationChange(); //Se j� tiver o Azimuth, o pitch e rool ent�o envio informa��o pelo callback
        //|| (Math.abs(mAziPitRol[2]*RAD2GRAUS)<70 || Math.abs(mAziPitRol[2]*RAD2GRAUS)>110)
        if((mAziPitRol[1]*RAD2GRAUS>-10 || mAziPitRol[1]*RAD2GRAUS<-80) ){ // testa o pitch
            iIns.onBeforeStartNotOriented();
        }else
        {
            iIns.onBeforeStartOriented();
        }

    }
    /*------------- FIM Get e Set do Aimute, pitch e roll para orientationView -------------------------*/
    /**
     * Inicializa as defini��es para os sensores. Carrega preferencias e timers para executar a primeira actualiza��o.
     * <p/>
     * Apenas provicendia acesso ao m?todo:
     * {@link #loadDefinitions()}
     */
    public void startIns() {
        loadDefinitions(); // Para carregar prefs e inicializar os listeners dos sensores
        accUdpCount = gyroUdpCount = gravityUdpCount = magUdpCount = linAccUdpCount = myLinAccUdpCount = orientationUdpCount = 0;// Inicializa contadores pacotes UDP a zero
        //startSaving = true;
    }
    public void start(String subdir){

        if(prefLogSensors) //se for para gravar entao criar ficheiros
        {

            /*Date data = new Date();
            String subDirName = android.text.format.DateFormat.format("yyyy_MM_dd_hh_mm_ss", data).toString();*/
            accWriter.createFile(subdir,"smta_isep","Acc");
            accLinearWriter.createFile(subdir,"smta_isep", "AccLinear");
            accMeuLinearWriter.createFile(subdir,"smta_isep", "AccMeulinear");
            magnetometroWriter.createFile(subdir,"smta_isep", "mag");
            giroscopioWriter.createFile(subdir,"smta_isep", "gyr");
            gravidadeWriter.createFile(subdir,"smta_isep", "gravidade");
            accmagaprWriter.createFile(subdir,"smta_isep", "apr_accmag");
            rvaprWriter.createFile(subdir,"smta_isep", "apr_rv");
            fusaoaprWriter.createFile(subdir,"smta_isep", "apr_fusao");
            startSaving = true;

        }

    }
    public void stop(){
        startSaving = false;

        if(accWriter.file!=null && accMeuLinearWriter.file!=null && magnetometroWriter.file!=null && giroscopioWriter.file!=null){
            accWriter.closeFile();
            accLinearWriter.closeFile();
            accMeuLinearWriter.closeFile();
            magnetometroWriter.closeFile();
            giroscopioWriter.closeFile();
            gravidadeWriter.closeFile();
            accmagaprWriter.closeFile();
            rvaprWriter.closeFile();
            fusaoaprWriter.closeFile();

        }
    }
    /**
     * Inicializa as defini��es para os sensores. Carrega preferencias e timers para executar a primeira actualiza??o.
     * <p/>
     * Apenas provicendia acesso ao m?todo:
     * {@link #loadDefinitions()}
     */
    public void stopIns() {
        startSaving = false;
        hasThreshold=false;
        thresholdRecordCount=0;
        if(sensorManager!=null)
            sensorManager.unregisterListener(this); // Retiro os listeners dos sensores
        if(accWriter.file!=null && accMeuLinearWriter.file!=null && magnetometroWriter.file!=null && giroscopioWriter.file!=null){
            accWriter.closeFile();
            accLinearWriter.closeFile();
            accMeuLinearWriter.closeFile();
            magnetometroWriter.closeFile();
            giroscopioWriter.closeFile();
            gravidadeWriter.closeFile();
            accmagaprWriter.closeFile();
            rvaprWriter.closeFile();
            fusaoaprWriter.closeFile();

        }
    }
    /**
     * Determinar quais os sensores que dever?o estar activos. Respeitando o ciclo de vida de uma activadade no android,
     * corre sempre que o estado da actividade entra em onResume()
     * Carregar defini??es das prefs e efectar os posts dos handlers.
     */
    private void loadDefinitions() {
        // reset �s variaveis caso esteja a recome�ar
        previousEventTimeGravity = 0;
        previousEventTimeAcc = 0;
        previousEventTimeGyro = 0;
        previousEventTimeMag = 0;
        previousEventTimeRotVet = 0;
        previousEventLinearAcc = 0;
        this.setInsState(false);
        // Vai carregar as preferencioas
        prefSensorFrequency = Integer.parseInt(preferences.getString("sensorfrequency", "0")); // Velocidade do sensor (Fastest=0, Game=1, Normal=2, UI=3)
        prefOrientationType = preferences.getString("orientationtype", "fusao"); // Qual o tipo de orienta��o (accmag, rotvet, fusao)
        prefNoiseFilter = preferences.getString("filtroRuido", "lpf"); // Qual o filtro para atenuar o ru�do (lpf=Low Pass, mv=mediamovel)
        prefGravityAlpha = Float.parseFloat(preferences.getString("thresholdGravityLPF", "0.999f")); // Filtro alpha para determina��o da gravidade em movimento
        prefLpfAlpha = Float.parseFloat(preferences.getString("thresholdLPF", "0.4f")); // Valor de alpha para filtro de ru?do acelera��o
        prefSizeSma = Integer.parseInt(preferences.getString("thresholdSMA", "5")); // N?mero de registos para SMA filtro de ru�do acelera��o
        prefUsesCalibration = preferences.getBoolean("usaCalibracao", false); // Usa valores de calibra��o para aceler�metro
        prefCalibrationRecordsCount = Integer.parseInt(preferences.getString("thresholdCalibracao", "500")); // N�mero de registos para Calibra��o
        prefThresholdVelocityCompute = Integer.parseInt(preferences.getString("thresholdComputeVelocity", "500")); // N�mero de registos para determina��o do threshold
        // Log Sensores
        prefLogSensors = preferences.getBoolean("logSensor", false); // Efetua log dos sensores
        prefLogGps = preferences.getBoolean("logGps", false); // Efetua log do GPS ou n?o
        // Udp Send
        prefIpPort = Integer.parseInt(preferences.getString("udpporta", "49000")); // Porta UDP para envio
        prefIpAddress = preferences.getString("udpip", "0.0.0.0");// IP para envio
        prefUdpsend = preferences.getBoolean("udpsend", false); // Envia dados por UDP?
        // Inicializa��o da media m�vel com numero de pontos definidos nas configura��es
        mSmaX = new MovingAverage(prefSizeSma);
        mSmaY = new MovingAverage(prefSizeSma);
        mSmaZ = new MovingAverage(prefSizeSma);
        // Inicializa��o do filtro Low/High PAss Acelerometro
        lowPassFilter = new LowPassFilter(prefLpfAlpha);
        gravityLowPassFilter = new LowPassFilter(prefGravityAlpha);
        // Se usa calibra��o, ent�o carrego o vetor calibra��o dos valores calibrados
        if (prefUsesCalibration && preferences.contains("k_X")) {
            mCalibVetor_scale[0] = preferences.getFloat("k_X", 1.0f);
            mCalibVetor_scale[1] = preferences.getFloat("k_Y", 1.0f);
            mCalibVetor_scale[2] = preferences.getFloat("k_Z", 1.0f);
            mCalibVetor_bias[0] = preferences.getFloat("b_X", 0.0f);
            mCalibVetor_bias[1] = preferences.getFloat("b_Y", 0.0f);
            mCalibVetor_bias[2] = (float) (preferences.getFloat("b_Z", 0.0f));
        }
        // Inicializa��o dos sensores com handler e handlerthread para correr os eventos noutra thread
        HandlerThread mHandlerThread = new HandlerThread("sensorThread"); // Cria��o da HandlerThread
        mHandlerThread.start(); // Inicio da thread
        Handler handler = new Handler(mHandlerThread.getLooper()); // Ir buscar o looper da mHandlerThread
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                prefSensorFrequency,handler); // Sensor Virtual Aceler�metro Linear
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
                prefSensorFrequency,handler); // Sensor Virtual Gravidade
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                prefSensorFrequency,handler); // Sensor Aceler�metro
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                prefSensorFrequency,handler); // Sensor Magnet�metro
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                prefSensorFrequency,handler); // Sensor Gisrosc�pio
        if (prefOrientationType.equals(resources
                .getString(R.string.RotationVector))) {
            sensorManager
                    .registerListener(this, sensorManager
                            .getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                            prefSensorFrequency,handler); // Se for usado o Sensor Virtual RotationVector
        }



    }
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(!hasThreshold){
            processThreshold(sensorEvent);
        }else{
            processData(sensorEvent);
            processOrientation(sensorEvent);
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
    private void processThreshold(SensorEvent sensorEvent){
        int sensor = sensorEvent.sensor.getType();// guarda o tipo de sensor que gerou o evento

        switch (sensor) {
            case Sensor.TYPE_ACCELEROMETER:
                float tempDataMagnitude ;
                tempDataMagnitude = operations.getMagnitude(sensorEvent.values);
                if(highLimitThresholdAcc!=0 && lowLimitThresholdAcc!=0){
                    if(highLimitThresholdAcc<tempDataMagnitude)
                    {
                        highLimitThresholdAcc = tempDataMagnitude;
                    }
                    if(lowLimitThresholdAcc>tempDataMagnitude)
                    {
                        lowLimitThresholdAcc = tempDataMagnitude;
                    }
                }else
                {
                    highLimitThresholdAcc = tempDataMagnitude;
                    lowLimitThresholdAcc = tempDataMagnitude;
                }
                thresholdRecordCount ++;
                if(thresholdRecordCount==prefThresholdVelocityCompute)
                {
                    hasThreshold = true;
                    // agendo para post na thread
                    mHandler.post(new Runnable() {

                        public void run() {
                            iIns.onInsReady();
                        }
                    });
                }
                break;
        }
    }
    private void processData(SensorEvent sensorEvent) {
        int sensor = sensorEvent.sensor.getType();// guarda o tipo de sensor que gerou o evento
        long eventTime = sensorEvent.timestamp; // Guarda o tempo em nanosegundos quando o evento aconteceu
        float dt = 0; // Intervalo de tempo entre o evento actual e o anterior (posterior passagem para segundos)
        switch (sensor) {
            case Sensor.TYPE_ACCELEROMETER:
                // Calcula do dt tempo entre eventos
                if (previousEventTimeAcc != 0)
                    dt = (eventTime - previousEventTimeAcc) * NS2S;
                previousEventTimeAcc = eventTime;
                mAccData = sensorEvent.values.clone(); // Vetor acelera��o
                mAccCalibratedData = getCalibratedAccVector(sensorEvent.values.clone()); // Vetor calibrado
                if(prefNoiseFilter.trim().equals("lpf"))
                {
                    mAccLpfData = lowPassFilter.compute(mAccCalibratedData,mAccLpfData);
                    mAccFilteredData = mAccLpfData.clone();
                    filterCoeficient = prefLpfAlpha;
                }
                if(prefNoiseFilter.trim().equals("mv"))
                {
                    mAccAvgData[0] = (float)mSmaX.compute(mAccCalibratedData[0]); // Vetor acelera��o com filtro m�dia m�vel Eixo X
                    mAccAvgData[1] = (float)mSmaY.compute(mAccCalibratedData[1]); // Vetor acelera��o com filtro m�dia m�vel Eixo Y
                    mAccAvgData[2] = (float)mSmaZ.compute(mAccCalibratedData[2]); // Vetor acelera��o com filtro m�dia m�vel Eixo Z
                    mAccFilteredData = mAccAvgData.clone();
                    filterCoeficient = (float)prefSizeSma;
                }
                if(operations.getMagnitude(mMyGravityData)==0) // Se o vetor gravidade estiver a zero, ent�o preencher com dados do acc
                {
                    mMyGravityData = mAccFilteredData.clone();
                }
                mMyGravityData = gravityLowPassFilter.compute(mAccFilteredData,mMyGravityData);
                sensorDataToSave = eventTime + "," + dt + "," + mAccData[0] + "," + mAccData[1] + "," + mAccData[2] + "," +
                        mAccFilteredData[0] + "," + mAccFilteredData[1] + "," + mAccFilteredData[2]+ ","+ prefNoiseFilter + "," + filterCoeficient + "," +
                        mCalibVetor_scale[0] + "," + mCalibVetor_scale[1] + "," + mCalibVetor_scale[2] + "," +
                        mCalibVetor_bias[0] + "," + mCalibVetor_bias[1] + "," + mCalibVetor_bias[2] + "," +
                        mAccCalibratedData[0] + "," + mAccCalibratedData[1] + "," + mAccCalibratedData[2] + "," +
                        mMyGravityData[0] + "," + mMyGravityData[1] + "," + mMyGravityData[2] +"\n";
                if(prefLogSensors && startSaving)
                    accWriter.writeThis(sensorDataToSave);
                this.setVelocity(mAccFilteredData,mMyGravityData,dt);
                break;
            case Sensor.TYPE_GYROSCOPE:
                if (previousEventTimeGyro != 0)
                    dt = (eventTime - previousEventTimeGyro) * NS2S;
                previousEventTimeGyro = eventTime;
                mGyroData =  sensorEvent.values.clone();
                sensorDataToSave = eventTime + "," + dt + "," + mGyroData[0] + "," + mGyroData[1] + "," + mGyroData[2]+ "\n";
                if(prefLogSensors && startSaving)
                    giroscopioWriter.writeThis(sensorDataToSave);
                break;
            case Sensor.TYPE_GRAVITY:
                if (previousEventTimeGravity != 0)
                    dt = (eventTime - previousEventTimeGravity) * NS2S;
                previousEventTimeGravity = eventTime;
                mGravityData= sensorEvent.values.clone();
                sensorDataToSave = eventTime + "," + dt + "," + mGravityData[0] + "," + mGravityData[1] + "," + mGravityData[2]+ "\n";
                if(prefLogSensors && startSaving)
                    gravidadeWriter.writeThis(sensorDataToSave);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                if (previousEventTimeMag != 0)
                    dt = (eventTime - previousEventTimeMag) * NS2S;
                previousEventTimeMag = eventTime;
                mMagData = sensorEvent.values.clone();
                sensorDataToSave = eventTime + "," + dt + "," + mMagData[0] + "," + mMagData[1] + "," + mMagData[2]+ "\n";
                if(prefLogSensors && startSaving)
                    magnetometroWriter.writeThis(sensorDataToSave);
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                if (previousEventTimeRotVet != 0)
                    dt = (eventTime - previousEventTimeRotVet) * NS2S;
                previousEventTimeRotVet = eventTime;
                mRotvectorData =  sensorEvent.values.clone();
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                if (previousEventLinearAcc != 0)
                    dt = (eventTime - previousEventLinearAcc) * NS2S;
                previousEventLinearAcc = eventTime;
                mLinearAccData = sensorEvent.values.clone();
                sensorDataToSave = eventTime + "," + dt + "," + mLinearAccData[0] + "," + mLinearAccData[1] + "," + mLinearAccData[2]+"," + "\n";
                if(prefLogSensors && startSaving)
                    accLinearWriter.writeThis(sensorDataToSave);
                break;

        }
        if(operations.getMagnitude(ins.getAprAccMag())!=0 || operations.getMagnitude(ins.getAprFusao())!=0 || operations.getMagnitude(ins.getAprRotVet())!=0)
        {
            if (sensor == Sensor.TYPE_ACCELEROMETER) {
                mMyLinearAccData = ins.getAccLinear(mAccFilteredData);
                sensorDataToSave = eventTime + "," + dt + "," + mMyLinearAccData[0] + "," + mMyLinearAccData[1] + "," + mMyLinearAccData[2]+"," + "\n";
                if(prefLogSensors && startSaving)
                    accMeuLinearWriter.writeThis(sensorDataToSave);
            }
        }

    }
    private void processOrientation(SensorEvent sensorEvent) {
        int sensor = sensorEvent.sensor.getType();// guarda o tipo de sensor que gerou o evento
        if (prefOrientationType.equals(resources.
                getString(R.string.OrientationTypeAccMag))) {
            if (sensor == Sensor.TYPE_ACCELEROMETER || sensor == Sensor.TYPE_MAGNETIC_FIELD) {
                ins.computeAccMagOrientation(mMagData, mAccCalibratedData);
                this.setAziPitRoll(ins.getAprAccMag());
            }
        }
        if (prefOrientationType.equals(resources.
                getString(R.string.OrientationTypeRotationVector))) {
            if (sensor == Sensor.TYPE_ROTATION_VECTOR) {
                ins.computeRotVetOrientation(mRotvectorData);
                this.setAziPitRoll(ins.getAprRotVet());
            }
        }
        if (prefOrientationType.equals(resources.
                getString(R.string.OrientationTypeFusion))) {
            if (sensor == Sensor.TYPE_ACCELEROMETER) {
                ins.computeAccMagOrientation(mMagData, mAccCalibratedData);
                this.setAziPitRoll(ins.getAprFusao());
            }
            if (sensor == Sensor.TYPE_GYROSCOPE) {

                ins.computeGyroOrientation(sensorEvent);
            }
        }
    }
    private void sendDataUdp(){

    }
    /**
     * Determina o vetor acelera��o calibrado com os coeficientes estimados durante o processo autom�tico
     * ou manual de calibra��o.
     *
     *
     * @param accData Vetor a ser calibrado
     *
     * @return O vetor calibrado
     */
    private float[] getCalibratedAccVector(float[] accData)
    {
        float[] tempVector = new float[3];
        for(int i=0;i<accData.length;i++)
        {
            tempVector[i] = (accData[i] * mCalibVetor_scale[i])-mCalibVetor_bias[i];
        }
        return tempVector;
    }

    /**
     * Interface para comunicar com a classe que implmenta esta interface e assim aceder aos callbacks efectuados
     */
    public interface InterfaceIns {
        public void onInsReady(); // Ins est� pronto para arrancar

        public void onOrientationChange();

        public void onVelocityChange();

        public void onPositionChange();

        public void onBeforeStartNotOriented();
        public void onBeforeStartOriented();
    }
}
