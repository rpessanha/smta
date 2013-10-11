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
    private static final float GRAUS2RAD = 0.01745329f;
    private static final int ACCEL_DEVIATION_LENGTH = 80;
    private static final Handler mHandler = new Handler(); // Agendar mensagens na thread para envio dos callbacks
    // Tipo de retorno do callback implementado
    private Context context;
    private Resources resources;// Aceder aos resource (Strings, etc...)
    private SharedPreferences preferences;// Gestor de preferencias

    // Vetores
    private float[] mAccData = new float[3]; // Vetor aceleração obtida do Acelerometro nos 3 eixos (X, Y e Z)
    private float[] mGravityData = new float[3]; // Vetor aceleração de gravidade nativo Android nos 3 eixos (X, Y e Z) (Sensor Virtual)
    private float[] mAccCalibratedData = new float[3]; // Vetor aceleração com calibração nos 3 eixos (X, Y e Z)
    private float[] mAccFilteredData = new float[3]; // Vetor aceleração filtrada (lpf ou mv) obtida do Acelerometro nos 3 eixos (X, Y e Z)
    private float[] mGyroData = new float[3]; // Vetor velocidade angular obtida do Giroscopio  nos 3 eixos (X, Y e Z)
    private float[] mMagData = new float[3]; // Vetor Micro Teslas obtida do magnetometro  nos 3 eixos (X, Y e Z)
    private float[] mMagLpfData = new float[3]; // Vetor Micro Teslas obtida do magnetometro  nos 3 eixos (X, Y e Z)
    private float[] mMyLinearAccData = new float[3]; // Acelerometro linear retirando a gravidade com DCM na classe InsClass com m?todo getAccLinear
    private float[] mMyGravityData = new float[3]; // Vetor aceleração gravidade com filtro passa baixas para retirar gravidade ao acelerometro
    private float[] mMyGravityDataFusion = new float[3]; // Vetor aceleração gravidade com filtro passa baixas para retirar gravidade ao acelerometro
    private float[] mLinearAccData = new float[3]; // Acelerometro linear nativo android (Sensor Virtual)
    private float[] mAccAvgData = new float[3]; // Vetor aceleação com filtro média móvel
    private float[] mAccLpfData = new float[3]; // Vetor aceleação com filtro low Pass filter
    private float[] mAccLpfDataForGravity = new float[3]; // Vetor aceleação com filtro low Pass filter
    private float[] mRotvectorData = new float[3]; // Vetor rotação obtido do sensor virtual Rotation Vector.
    private float[] mAziPitRol = new float[3]; // Azimuth pith e roll de retorno
    private float[] mCalibVetor_scale = new float[3]; // Vai guardar os valores obtidos da calibração do acelerometro e usa-los na obtenção de valores Acc melhorados.
    private float[] mCalibVetor_bias = new float[3]; // Vai guardar os valores obtidos da calibração do acelerometro e usa-los na obtenção de valores Acc melhorados.
    private float mVelocity; // Velocidade em Km ((float) ins.getVelocidadeTotal() / MSTOKM;)
    private float mPosition; // Velocidade em Km ((float) ins.getVelocidadeTotal() / MSTOKM;)

    // Preferencias (SharedPreferences)
    private int prefSensorFrequency; // Frequencia dos sensores definida nas preferencias e sugeridas nos listeners dos sensores.
    private String prefOrientationType; // Tipo de orientação definida (Acc+Mag, Rotvet, Fusao)
    private String prefNoiseFilter; // Tipo de filtro para atenuar o ruído (lpf, mv)
    private float prefLpfAlpha; // Alpha para filtro LPF no tratamento da aceleração Ruído
    private float prefGravityAlpha;
    private String prefGravityFilter;
    private int prefSizeSma; // Numero de pontos associados ao filtro média móvel
    private boolean prefUsesCalibration; // Indica se os valores obtidos da calibração vão ser utilizados
    private boolean prefUsesVelocityThreshold;
    private int prefCalibrationRecordsCount; // Numero de registo para efetuar a calibração (avg)
    private float prefGyroFilterCoefficient;
    private float prefGyroNoiseLimit;
    private float prefMagneticDeclination;
    private boolean prefUsaCentripeta;
    private int prefThresholdVelocityCompute; // Número de registos para determinar o threshold de cálculo da velocidade
    private float prefThresholdLimit;
    private boolean prefLogSensors; // Efetuar log dos sensores ou não
    private boolean prefLogGps; // Efetuar log do GPS ou não
    private int prefIpPort; // Porta uDP
    private String prefIpAddress; // Endereço IP para envio pacotes UDP
    private boolean prefUdpsend; // Envia UDP ou não

    private boolean hasStarted; // Indica se já come?ou ou não

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

    // Filtro média móvel
    private MovingAverage mSmaX; //=new MovingAverage(SMA_SIZE);
    private MovingAverage mSmaY; //= new MovingAverage(SMA_SIZE);
    private MovingAverage mSmaZ;// = new MovingAverage(SMA_SIZE);

    // Filtro LowPassFilter
    private LowPassFilter lowPassFilter;
    private float AccAlpha; // Alpha para filtro do acelerometro
    private LowPassFilter gravityLowPassFilter;
    private LowPassFilter lowPassFilterForGravity;
    private float gravityAlpha; // Alpha para filtro gravidade
    private LowPassFilter magnetometerLowPassFilter;
    private float magAlpha;
    // Log sensors
    private SensorWriterSmta accWriter = new SensorWriterSmta(); // Escreve registos provenientes do acelerometro
    private SensorWriterSmta accLinearWriter = new SensorWriterSmta(); // Escreve registos provenientes do acelerometro linear
    private SensorWriterSmta accMeuLinearWriter = new SensorWriterSmta(); // Escreve registos provenientes do acelerometro linear calculado pela DCM
    private SensorWriterSmta magnetometroWriter = new SensorWriterSmta(); // Escreve registo provenientes do magnetómetro
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

    private float accMagnitude;

    // Obtenção do threshold
    private boolean hasThreshold; // Verifica se já tem o threshold para o limite alto e baixo da velocidade
    private float highLimitThresholdAcc; // Magnitude de limite superior da acc - Obtenção threshold
    private float lowLimitThresholdAcc; // Magnitude de limite inferior da acc - Obtenção threshold
    private int thresholdRecordCount; // Contador de registos a serem udados na média do threshold
    private float[] avgAccThreshold = new float[3];
    private int countRecordsForThreshold;
    private int gravityAccelLimitLen;
    private boolean gravityCalibrated;
    private int gravityThresholdCount;
    private boolean hasGravityCalibrated;


    /**
     * Método contrutor.
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
        preferences = PreferenceManager.getDefaultSharedPreferences(context); //instancio as preferencias de modo static , sem new()
        this.prefGyroFilterCoefficient = Float.parseFloat(this.preferences.getString("gyroFilterCoefficient","0.98f"));

        ins = new InsClass(this.prefGyroFilterCoefficient); // instnacio a classe INS
        operations = new Operations();

        iIns = _iIns; // O listener referencia ? o que vem por referencia quando inicializo o InsListener.
        // Em java os arrays são inicializados a zero por defeito, logo desnecess?rio inicializar a zero.
        mCalibVetor_scale[0] = 1;
        mCalibVetor_scale[1] = 1;
        mCalibVetor_scale[2] = 1;
        mCalibVetor_bias[0] = 0;
        mCalibVetor_bias[1] = 0;
        mCalibVetor_bias[2] = 0;
        mMyGravityData[0] = 0.0f;
        mMyGravityData[1] = 0.0f;
        mMyGravityData[2] = 0.0f;
        sensorDataToSave ="";
        startSaving = false;
        hasThreshold = false;
        thresholdRecordCount=0;
        highLimitThresholdAcc=0;
        lowLimitThresholdAcc=0;
        countRecordsForThreshold =0;
        gravityAccelLimitLen = -1;
        gravityThresholdCount = 0;
        magAlpha = 0.3f;
    }

    /**
     * Coloca a velocidade em Zero, fazendo com que a posição não aumente.
     */
    public void setVelocidadeZero() {
        mVelocity = 0.0f;

    }
    /**
     * Update á velocidade com aceleração - gravidade filtro
     */
    public void setVelocity(float[] filteredAccData,float[] gravityData, float dt)
    {
        if((this.prefUsesVelocityThreshold) && ((this.operations.getMagnitude(filteredAccData) < this.highLimitThresholdAcc) || (this.operations.getMagnitude(filteredAccData)>this.lowLimitThresholdAcc)))
            this.countRecordsForThreshold += countRecordsForThreshold;


        if((this.prefUsesVelocityThreshold) || ((this.operations.getMagnitude(filteredAccData) > this.highLimitThresholdAcc + this.prefThresholdLimit) || this.operations.getMagnitude(filteredAccData)<this.lowLimitThresholdAcc-this.prefThresholdLimit))
        {
            this.countRecordsForThreshold=0;
            this.gravityCalibrated = false;
            this.hasGravityCalibrated = true;
            this.gravityThresholdCount = 0;
            this.ins.updateVelocity(filteredAccData,gravityData,dt,this.prefUsaCentripeta);
            mHandler.post(new Runnable() {

                public void run() {
                    iIns.onVelocityChange();
                }
            });
        }
        if((this.operations.getMagnitude(filteredAccData)<=this.highLimitThresholdAcc+this.prefThresholdLimit) && (this.operations.getMagnitude(filteredAccData)>=this.lowLimitThresholdAcc-this.prefThresholdLimit) && (this.countRecordsForThreshold>=80))
        {
            if(!this.gravityCalibrated)
            {
                this.avgAccThreshold[0] = 0.0f;
                this.avgAccThreshold[1] = 0.0f;
                this.avgAccThreshold[2] = 0.0f;
                this.hasGravityCalibrated = false;
            }
            this.countRecordsForThreshold=0;
            this.ins.setInitialVelocityFromFilteredGps(0.0f);
            this.iIns.onVehicleStopDetected();
        }

        //iIns.onOrientationChange(); //Se já tiver o Azimuth, o pitch e rool então envio informação pelo callback
    }
    /**
     * Update á velocidade com aceleração - gravidade filtro
     */
    public void setVelocityRotation(float[] filteredAccData, float dt)
    {
        if((this.prefUsesVelocityThreshold) && ((this.operations.getMagnitude(filteredAccData) < this.highLimitThresholdAcc) || (this.operations.getMagnitude(filteredAccData)>this.lowLimitThresholdAcc)))
            this.countRecordsForThreshold += countRecordsForThreshold;


        if((this.prefUsesVelocityThreshold) || ((this.operations.getMagnitude(filteredAccData) > this.highLimitThresholdAcc + this.prefThresholdLimit) || this.operations.getMagnitude(filteredAccData)<this.lowLimitThresholdAcc-this.prefThresholdLimit))
        {
            this.countRecordsForThreshold=0;
            this.gravityCalibrated = false;
            this.hasGravityCalibrated = true;
            this.gravityThresholdCount = 0;
            this.ins.updateVelocityRotation(filteredAccData,dt);
            mHandler.post(new Runnable() {

                public void run() {
                    iIns.onVelocityChange();
                }
            });
        }
        if((this.operations.getMagnitude(filteredAccData)<=this.highLimitThresholdAcc+this.prefThresholdLimit) && (this.operations.getMagnitude(filteredAccData)>=this.lowLimitThresholdAcc-this.prefThresholdLimit) && (this.countRecordsForThreshold>=80))
        {
            if(!this.gravityCalibrated)
            {
                this.avgAccThreshold[0] = 0.0f;
                this.avgAccThreshold[1] = 0.0f;
                this.avgAccThreshold[2] = 0.0f;
                this.hasGravityCalibrated = false;
            }
            this.countRecordsForThreshold=0;
            this.ins.setInitialVelocityFromFilteredGps(0.0f);
            this.iIns.onVehicleStopDetected();
        }

        //iIns.onOrientationChange(); //Se já tiver o Azimuth, o pitch e rool então envio informação pelo callback
    }
    /**
     * Update á velocidade
     */
    public float getVelocity()
    {

        //mVelocity = operations.getMagnitude(ins.getVelocity());
        mVelocity = ins.getVelocity();
        return mVelocity;
    }
    /**
     * Verifica se o sistema já inicializou aquando do startHandler ap?s 10 segundos.
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
     * Azimute magnético - Medida horizontal em graus sobre o norte magnético. Medido sobre o Z
     * Pitch - Rotação sobre o eixo do X
     * Roll - Rotação sobre o eixo do Y
     *
     * @return AziPitRoll array float[] valor[0] - Azimute, Valor[1] - Pitch, Valor[2] - Roll (Valores em radianos)
     *
     */
    public float[] getAziPitRoll(){
        return mAziPitRol;

    }
    /**
     * Define o valor Azimuth, pitch e roll.
     * Azimute magnético - Medida horizontal em graus sobre o norte magnético. Medido sobre o Z
     * Pitch - Rotação sobre o eixo do X (0º -> -90º -> 0)
     * Roll - Rotação sobre o eixo do Y (0º -> 90º ->
     *
     * Aqui vai activar um evento sobre o interface entretanto inicializado na classe que o instancia.
     *
     */
    private void setAziPitRoll(float[] _AziPitRol){
        mAziPitRol = _AziPitRol.clone();
        this.mAziPitRol[0] +=GRAUS2RAD * this.prefMagneticDeclination;
        mHandler.post(new Runnable() {

            public void run() {
                iIns.onOrientationChange();
            }
        });
        //iIns.onOrientationChange(); //Se já tiver o Azimuth, o pitch e rool então envio informação pelo callback
        //|| (Math.abs(mAziPitRol[2]*RAD2GRAUS)<70 || Math.abs(mAziPitRol[2]*RAD2GRAUS)>110)
        /*if((mAziPitRol[1]*RAD2GRAUS>-10 || mAziPitRol[1]*RAD2GRAUS<-80) ){ // testa o pitch
            iIns.onBeforeStartNotOriented();
        }else
        {
            iIns.onBeforeStartOriented();
        }*/

    }
    public float getPosition(){
        this.mPosition = this.ins.getPosition();
        return this.mPosition;
    }
    /*------------- FIM Get e Set do Aimute, pitch e roll para orientationView -------------------------*/
    /**
     * Inicializa as definições para os sensores. Carrega preferencias e timers para executar a primeira actualização.
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
     * Inicializa as definições para os sensores. Carrega preferencias e timers para executar a primeira actualiza??o.
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
        // reset às variaveis caso esteja a recomeçar
        previousEventTimeGravity = 0;
        previousEventTimeAcc = 0;
        previousEventTimeGyro = 0;
        previousEventTimeMag = 0;
        previousEventTimeRotVet = 0;
        previousEventLinearAcc = 0;
        this.setInsState(false);
        // Vai carregar as preferencioas
        prefSensorFrequency = Integer.parseInt(preferences.getString("sensorfrequency", "0")); // Velocidade do sensor (Fastest=0, Game=1, Normal=2, UI=3)
        prefOrientationType = preferences.getString("orientationtype", "fusao"); // Qual o tipo de orientação (accmag, rotvet, fusao)
        prefNoiseFilter = preferences.getString("filtroRuido", "lpf"); // Qual o filtro para atenuar o ruído (lpf=Low Pass, mv=mediamovel)
        prefGravityAlpha = Float.parseFloat(preferences.getString("thresholdGravityLPF", "0.999f")); // Filtro alpha para determinação da gravidade em movimento
        prefLpfAlpha = Float.parseFloat(preferences.getString("thresholdLPF", "0.4f")); // Valor de alpha para filtro de ru?do aceleração
        prefSizeSma = Integer.parseInt(preferences.getString("thresholdSMA", "5")); // N?mero de registos para SMA filtro de ruído aceleração
        prefUsesCalibration = preferences.getBoolean("usaCalibracao", false); // Usa valores de calibração para acelerómetro
        prefCalibrationRecordsCount = Integer.parseInt(preferences.getString("thresholdCalibracao", "500")); // Número de registos para Calibração
        prefUsesVelocityThreshold = this.preferences.getBoolean("usaThresholdVelocidade",false);
        prefThresholdVelocityCompute = Integer.parseInt(preferences.getString("thresholdComputeVelocity", "500")); // Número de registos para determinação do threshold
        prefThresholdLimit = Float.parseFloat(this.preferences.getString("thresholdLimit","0.0f"));
        prefMagneticDeclination = Float.parseFloat(this.preferences.getString("compasscalibration","0.0f"));
        prefUsaCentripeta = this.preferences.getBoolean("usaCentripeta",false);
        prefGyroNoiseLimit = Float.parseFloat(this.preferences.getString("gyro_noise_limit","0.005f"));
         // Log Sensores
        prefLogSensors = preferences.getBoolean("logSensor", false); // Efetua log dos sensores
        prefLogGps = preferences.getBoolean("logGps", false); // Efetua log do GPS ou n?o
        // Udp Send
        prefIpPort = Integer.parseInt(preferences.getString("udpporta", "49000")); // Porta UDP para envio
        prefIpAddress = preferences.getString("udpip", "0.0.0.0");// IP para envio
        prefUdpsend = preferences.getBoolean("udpsend", false); // Envia dados por UDP?
        // Inicialização da media móvel com numero de pontos definidos nas configurações
        mSmaX = new MovingAverage(prefSizeSma);
        mSmaY = new MovingAverage(prefSizeSma);
        mSmaZ = new MovingAverage(prefSizeSma);
        // Inicialização do filtro Low/High PAss Acelerometro
        lowPassFilter = new LowPassFilter(prefLpfAlpha);
        gravityLowPassFilter = new LowPassFilter(prefGravityAlpha);
        lowPassFilterForGravity = new LowPassFilter(0.1f);
        // Se usa calibração, então carrego o vetor calibração dos valores calibrados
        if (prefUsesCalibration && preferences.contains("k_X")) {
            mCalibVetor_scale[0] = preferences.getFloat("k_X", 1.0f);
            mCalibVetor_scale[1] = preferences.getFloat("k_Y", 1.0f);
            mCalibVetor_scale[2] = preferences.getFloat("k_Z", 1.0f);
            mCalibVetor_bias[0] = preferences.getFloat("b_X", 0.0f);
            mCalibVetor_bias[1] = preferences.getFloat("b_Y", 0.0f);
            mCalibVetor_bias[2] = (float) (preferences.getFloat("b_Z", 0.0f));
        }
        // Inicialização dos sensores com handler e handlerthread para correr os eventos noutra thread
        HandlerThread mHandlerThread = new HandlerThread("sensorThread"); // Criação da HandlerThread
        mHandlerThread.start(); // Inicio da thread
        Handler handler = new Handler(mHandlerThread.getLooper()); // Ir buscar o looper da mHandlerThread
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                prefSensorFrequency,handler); // Sensor Virtual Acelerómetro Linear
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
                prefSensorFrequency,handler); // Sensor Virtual Gravidade
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                prefSensorFrequency,handler); // Sensor Acelerómetro
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                prefSensorFrequency,handler); // Sensor Magnetómetro
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                prefSensorFrequency,handler); // Sensor Gisroscópio
        if (prefOrientationType.equals(resources
                .getString(R.string.RotationVector))) {
            sensorManager
                    .registerListener(this, sensorManager
                            .getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                            prefSensorFrequency,handler); // Se for usado o Sensor Virtual RotationVector
        }



    }
    private void processCalibration(SensorEvent sensorEvent)
    {
        int sensor = sensorEvent.sensor.getType();// guarda o tipo de sensor que gerou o evento

        switch (sensor) {
            case Sensor.TYPE_ACCELEROMETER:
                if(this.gravityThresholdCount>19)
                {
                    this.mAccData = sensorEvent.values.clone();
                    this.mAccCalibratedData = getCalibratedAccVector(sensorEvent.values.clone());
                    if(this.prefNoiseFilter.trim().equals("lpf"))
                    {
                        if(this.gravityThresholdCount==0)
                            this.mAccLpfData = this.mAccCalibratedData.clone();
                        this.mAccLpfData = this.lowPassFilter.compute(this.mAccCalibratedData, this.mAccLpfData);
                        this.mAccFilteredData = this.mAccLpfData.clone();
                        this.filterCoeficient = this.prefLpfAlpha;
                    }
                    if(this.prefNoiseFilter.trim().equals("mv"))
                    {
                        this.mAccAvgData[0] = (float)this.mSmaX.compute(this.mAccCalibratedData[0]);
                        this.mAccAvgData[1] = (float)this.mSmaY.compute(this.mAccCalibratedData[1]);
                        this.mAccAvgData[2] = (float)this.mSmaZ.compute(this.mAccCalibratedData[2]);
                        this.mAccFilteredData = this.mAccAvgData.clone();
                        this.filterCoeficient = this.prefSizeSma;
                    }
                    this.avgAccThreshold[0] +=this.mAccFilteredData[0];
                    this.avgAccThreshold[1] +=this.mAccFilteredData[1];
                    this.avgAccThreshold[2] +=this.mAccFilteredData[2];

                }
                this.gravityThresholdCount ++;
                if(this.gravityThresholdCount==39)
                {
                    this.hasGravityCalibrated = true;
                    this.gravityCalibrated = true;
                    this.avgAccThreshold[0] /=20;
                    this.avgAccThreshold[1] /=20;
                    this.avgAccThreshold[2] /=20;
                    this.mMyGravityData = this.avgAccThreshold.clone();
                    this.gravityThresholdCount=0;
                }

                break;
        }
    }
    public boolean isVehicleMoving(float[] filteredAccData){
        if((this.operations.getMagnitude(filteredAccData) < this.highLimitThresholdAcc + this.prefThresholdLimit) && (this.operations.getMagnitude(filteredAccData)>this.lowLimitThresholdAcc - this.prefThresholdLimit)){
            return false;
        }else
        {
            return true;
        }


    }
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(!hasThreshold){
            processThreshold(sensorEvent);
        }else{
            if(this.hasGravityCalibrated)
            {
                processData(sensorEvent);
                processOrientation(sensorEvent);
            }else{
                processCalibration(sensorEvent);
            }

        }
    }
    public void setVelocidadeFromFilteredGps(float filteredVelocity){
        this.ins.setInitialVelocityFromFilteredGps(filteredVelocity);
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
    private void processThreshold(SensorEvent sensorEvent){
        int sensor = sensorEvent.sensor.getType();// guarda o tipo de sensor que gerou o evento

        switch (sensor) {
            case Sensor.TYPE_ACCELEROMETER:
                float tempDataMagnitude ;
                if(this.thresholdRecordCount>99){
                    mAccData = sensorEvent.values.clone(); // Vetor aceleração
                    mAccCalibratedData = getCalibratedAccVector(sensorEvent.values.clone()); // Vetor calibrado
                    if(prefNoiseFilter.trim().equals("lpf"))
                    {
                        if(this.operations.getMagnitude(this.mAccLpfData)==0.0f)
                            this.mAccLpfData = this.mAccCalibratedData.clone();
                        mAccLpfData = lowPassFilter.compute(mAccCalibratedData,mAccLpfData);
                        mAccFilteredData = mAccLpfData.clone();
                        filterCoeficient = prefLpfAlpha;
                    }
                    if(prefNoiseFilter.trim().equals("mv"))
                    {
                        mAccAvgData[0] = (float)mSmaX.compute(mAccCalibratedData[0]); // Vetor aceleração com filtro média móvel Eixo X
                        mAccAvgData[1] = (float)mSmaY.compute(mAccCalibratedData[1]); // Vetor aceleração com filtro média móvel Eixo Y
                        mAccAvgData[2] = (float)mSmaZ.compute(mAccCalibratedData[2]); // Vetor aceleração com filtro média móvel Eixo Z
                        mAccFilteredData = mAccAvgData.clone();
                        filterCoeficient = (float)prefSizeSma;
                    }

                }
                tempDataMagnitude = operations.getMagnitude(mAccFilteredData);
                avgAccThreshold[0] +=this.mAccFilteredData[0];
                avgAccThreshold[1] +=this.mAccFilteredData[1];
                avgAccThreshold[2] +=this.mAccFilteredData[2];

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
                if(thresholdRecordCount==prefThresholdVelocityCompute+100)
                {
                    hasThreshold = true;
                    this.avgAccThreshold[0] /=this.prefThresholdVelocityCompute;
                    this.avgAccThreshold[1] /=this.prefThresholdVelocityCompute;
                    this.avgAccThreshold[2] /=this.prefThresholdVelocityCompute;
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
        if(this.operations.getMagnitude(this.mMyGravityData)==0.0f)
            this.mMyGravityData = this.avgAccThreshold.clone();
        switch (sensor) {
            case Sensor.TYPE_ACCELEROMETER:

                // Calcula do dt tempo entre eventos
                if (previousEventTimeAcc != 0)
                    dt = (eventTime - previousEventTimeAcc) * NS2S;
                previousEventTimeAcc = eventTime;
                mAccData = sensorEvent.values.clone(); // Vetor aceleração
                mAccCalibratedData = getCalibratedAccVector(sensorEvent.values.clone()); // Vetor calibrado
                if(prefNoiseFilter.trim().equals("lpf"))
                {
                    if(this.operations.getMagnitude(this.mAccLpfData)==0.0f)
                        this.mAccLpfData = this.mAccCalibratedData.clone();
                    mAccLpfData = lowPassFilter.compute(mAccCalibratedData,mAccLpfData);
                    mAccFilteredData = mAccLpfData.clone();
                    filterCoeficient = prefLpfAlpha;
                }
                if(prefNoiseFilter.trim().equals("mv"))
                {
                    mAccAvgData[0] = (float)mSmaX.compute(mAccCalibratedData[0]); // Vetor aceleração com filtro média móvel Eixo X
                    mAccAvgData[1] = (float)mSmaY.compute(mAccCalibratedData[1]); // Vetor aceleração com filtro média móvel Eixo Y
                    mAccAvgData[2] = (float)mSmaZ.compute(mAccCalibratedData[2]); // Vetor aceleração com filtro média móvel Eixo Z
                    mAccFilteredData = mAccAvgData.clone();
                    filterCoeficient = (float)prefSizeSma;
                }
                // Qual o método de eterminaçao da gravidade
                if(this.prefGravityFilter.trim().equals("fglpf")) // LowPass
                {
                    this.mMyGravityData = this.gravityLowPassFilter.compute(this.mAccFilteredData,this.mMyGravityData);
                }
                if(this.prefGravityFilter.trim().equals("fgmrot")) // LowPass
                {
                    this.mMyGravityData = this.gravityLowPassFilter.compute(this.mAccFilteredData,this.mMyGravityData);
                    setVelocityRotation(this.mAccFilteredData,dt);
                    this.mMyGravityDataFusion = this.ins.getGravityFromRotationMatrix();

                }

                /*if(operations.getMagnitude(mMyGravityData)==0) // Se o vetor gravidade estiver a zero, então preencher com dados do acc
                {
                    mMyGravityData = mAccFilteredData.clone();
                }
                mMyGravityData = gravityLowPassFilter.compute(mAccFilteredData,mMyGravityData);*/
                sensorDataToSave = eventTime + "," + dt + "," + mAccData[0] + "," + mAccData[1] + "," + mAccData[2] + "," +
                        mAccFilteredData[0] + "," + mAccFilteredData[1] + "," + mAccFilteredData[2]+ ","+ prefNoiseFilter + "," + filterCoeficient + "," +
                        mCalibVetor_scale[0] + "," + mCalibVetor_scale[1] + "," + mCalibVetor_scale[2] + "," +
                        mCalibVetor_bias[0] + "," + mCalibVetor_bias[1] + "," + mCalibVetor_bias[2] + "," +
                        mAccCalibratedData[0] + "," + mAccCalibratedData[1] + "," + mAccCalibratedData[2] + "," +
                        mMyGravityData[0] + "," + mMyGravityData[1] + "," + mMyGravityData[2] + "," +
                        mMyGravityDataFusion[0] + "," + mMyGravityDataFusion[1] + "," + mMyGravityDataFusion[2] +"\n";
                if(prefLogSensors && startSaving)
                    accWriter.writeThis(sensorDataToSave);
                this.setVelocity(mAccFilteredData,mMyGravityData,dt);

                break;
            case Sensor.TYPE_GYROSCOPE:
                if (previousEventTimeGyro != 0)
                    dt = (eventTime - previousEventTimeGyro) * NS2S;
                previousEventTimeGyro = eventTime;
                mGyroData =  sensorEvent.values.clone();
                if(this.prefGravityFilter.equals("fggir")){
                    float dx = gyroNoiseLimiter(dt*this.mGyroData[0]);
                    float dy = gyroNoiseLimiter(dt*this.mGyroData[0]);
                    float dz = gyroNoiseLimiter(dt*this.mGyroData[0]);
                    this.ins.rotx(this.mMyGravityData,-dx);
                    this.ins.roty(this.mMyGravityData,-dy);
                    this.ins.rotz(this.mMyGravityData,-dz);
                }
                if(this.prefUsaCentripeta){
                    if(this.prefGravityFilter.equals("fgmrot")){
                        this.ins.updateVelocityGyroRotation(this.mGyroData,dt);
                        this.mMyGravityDataFusion = this.ins.getGravityFromRotationMatrix();
                    }else{
                        this.ins.updateVelocityGyro(this.mGyroData,dt);
                    }
                }
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
                this.mMagLpfData = this.magnetometerLowPassFilter.compute(this.mMagData, this.mMagLpfData);
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
                ins.computeAccMagOrientation(mMagLpfData, mMyGravityData);
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
                ins.computeAccMagOrientation(mMagLpfData, mMyGravityData);
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
     * Determina o vetor aceleração calibrado com os coeficientes estimados durante o processo automático
     * ou manual de calibração.
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
    private float gyroNoiseLimiter(float gyroValue ){
        float retValue=gyroValue;
        if(Math.abs(retValue)>this.prefGyroNoiseLimit)
            retValue = 0.0f;
        return retValue;
    }

    /**
     * Interface para comunicar com a classe que implmenta esta interface e assim aceder aos callbacks efectuados
     */
    public interface InterfaceIns {
        public void onInsReady(); // Ins está pronto para arrancar

        public void onOrientationChange();

        public void onVelocityChange();

        public void onPositionChange();

        public void onBeforeStartNotOriented();
        public void onBeforeStartOriented();
        public void onVehicleStopDetected();
    }
}
