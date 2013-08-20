package mei.tcd.ins;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import mei.tcd.util.Operations;

/**
 * Created by pessanha on 02-08-2013.
 */
public class InsClass {
    private static final float GYRO_FILTER_COEFFICIENT = 0.98f; //O quanto queremos que os resultados se ajustema este
    private static final float EPSILON = 0.000000001f;
    private static final float NS2S = 1.0f / 1000000000.0f; // Nanosegundos para segundos
    private DenseMatrix64F mInitialGpsPosition=new DenseMatrix64F(3,1); ///Vem da posi??o auferida pelo GPS(transfoma??o de wgs82->ecef->enu).
    private DenseMatrix64F mPosition=new DenseMatrix64F(3,1); // Vetor posi??o no sistema de coordenadas global (ENU)
    private DenseMatrix64F mInitialGpsVelocity=new DenseMatrix64F(3,1); // Vetor posi??o anterior para medir distancia entre os ultimos pontos
    private float mVelocityTotal; // Guarda valor da velocidade calculada
    private DenseMatrix64F mAcceleration=new DenseMatrix64F(3,1); // Vetor acelera??o no sistema de coordenadas local (Dispositivo)
    private DenseMatrix64F mVelocityBody=new DenseMatrix64F(3,1); // Vetor acelera??o no sistema de coordenadas local (Dispositivo)
    private DenseMatrix64F m_temp=new DenseMatrix64F(3,1); // array temporario
    private DenseMatrix64F m_tempdcm=new DenseMatrix64F(3,3); // DCM temporaria
    private DenseMatrix64F mGravity=new DenseMatrix64F(3,1); // vetor gravidade
    private float[] mCbnRotVet = new float[]{1,0,0,0,1,0,0,0,1}; // mCbnRotVet - Matriz rota??o usada para guardar valores do getrotationmatrix() do VetorRotacao
    private float[] mCbnAccMag = new float[]{1,0,0,0,1,0,0,0,1}; // mCbnAccMag - Matriz rota??o usada para guardar valores do getrotationmatrix() do AccMag
    private float[] mCbnFusion = new float[]{1,0,0,0,1,0,0,0,1}; // mCbnFusion - Matriz rota??o usada para guardar valores do getrotationmatrixFromOrientation(mAprFusao)
    private float[] mCbn = new float[]{1,0,0,0,1,0,0,0,1}; // mCbn - Matriz rotação usada para guardar valores das matrizes rotações anteriores
    private float[] mAprAccMag=new float[3]; // (Azimuth-yaw, Pitch, Roll) de Acelerometro e magnetometro
    private float[] mAprGyro=new float[]{0,0,0}; // (Azimuth-yaw, Pitch, Roll) de giroscopio
    private float[] mAprRotVet=new float[]{0,0,0}; // (Azimuth-yaw, Pitch, Roll) de Vetor Rota??o
    private float[] mAprFusao=new float[]{0,0,0}; // (Azimuth-yaw, Pitch, Roll) da fus?o (Filtro complementar)
    private float[] mAccLinear=new float[]{0,0,0}; // Vetor aceler??o Linear
    private boolean initState = true; // Verifica se na fus?o dos registos o estado ? inicial para se obter os registos inicias da AccMag orientation
    private float timestamp = 0; // Timestamp inicial da obten??o dos registos do giroscopio
    private float[] gyroOrientation = new float[3];// Angulos de orienta??o da matriz girosc?pio (Gyromatrix)
    private float[] gyro = new float[3]; // Guarda as velocidades angulares registadas pelo girosco+io
    private Operations operations;

    public InsClass()
    {
        mGravity.set(2,-SensorManager.GRAVITY_EARTH); //{0,0,-g} - Este vector servir? como base para calculo da varia??o do sistema de coordenadas disposito para navega??o
        mAcceleration.zero();
        mPosition.zero();// Aqui tenho de colocar a posicao inicial do GPS. Isso ? efectuado com um setter
        mVelocityBody.zero();
        mInitialGpsVelocity.zero();
		/*Filtro Complementar*/
        gyroOrientation[0] = 0.0f;
        gyroOrientation[1] = 0.0f;
        gyroOrientation[2] = 0.0f;
        mVelocityTotal = 0;
        operations = new Operations();
        //TODO: Inicializar os CBN
    }
    /**
     * Calculo da velocidade linear. Removo a gravidade da aceleração aproximadamente fazendo aceleracao += dcm_T x gravidade (no relatorio está a -dct_T porque o vetor gravidade está como positivo).
     * Posso usar mais, pois o vetor gravidade está com sinal menos. Visto que a DCM é ortogonal, a inversa é apenas a transposta.
     * Com esta transformação, passo o vetor gravidade do referencial da terra para
     * o referencial do dispositivo elaborando o calculo de dcm_T*g e assim tenho o peso da gravidade no dispositivo.
     *
     * @param data array float valores do acelerometro
     * @return array float da velocidade linear.
     *
     */
    public float[] getAccLinear(float[] data)
    {
        m_temp.set(0,data[0]); //x
        m_temp.set(1,data[1]); //y
        m_temp.set(2,data[2]); //z
        // Dcm em float, tenho de passar para densematrix64f
        for (int i = 0;i<9;i++)
            m_tempdcm.set(i,mCbn[i]);
        //m_temp = m_temp + m_tempdcm * gravidade
        CommonOps.multAddTransA(m_tempdcm, mGravity, m_temp);
        for(int i=0;i<3;i++)
            mAccLinear[i] = (float)m_temp.get(i);
        //return new float[]{(float) m_temp.get(0),(float) m_temp.get(1),(float) m_temp.get(2)};
        return mAccLinear;
    }
    public float getVelocity(){
        return mVelocityTotal;
    }
    /**
     * Retorna o vetor Azimuth, pitch  roll (Z, X, Y) referente ao getOrientation() obtido do acelerometro e magnetometro.
     *
     * @return array float Azimuth, pitch  roll (Z, X, Y)
     */
    public float[] getAprAccMag()
    {
        return  this.mAprAccMag;
    }
    /**
     * Retorna o vetor Azimuth, pitch  roll (Z, X, Y) referente ao getOrientation() com o sensor Giroscopio.
     * Vai ser usado para calcular o aprFusao.
     *
     * @return array float Azimuth, pitch  roll (Z, X, Y)
     */
    public float[] getAprGyro()
    {
        return  this.mAprGyro;
    }
    /**
     * Retorna o vetor Azimuth, pitch  roll (Z, X, Y) referente ao getOrientation() com o sensor RotationVector e getRotationMatrixFromVetor().
     *
     *
     * @return array float Azimuth, pitch  roll (Z, X, Y)
     */
    public float[] getAprRotVet()
    {
        return  this.mAprRotVet;
    }
    /**
     * Retorna o vetor Azimuth, pitch  roll (Z, X, Y) referente ao getOrientation() da fus?o Acc+Mag+Gyro.
     *
     *
     * @return array float Azimuth, pitch  roll (Z, X, Y)
     */
    public float[] getAprFusao()
    {
        return  this.mAprFusao;
    }
    /**
     * Calculo da velocidade.Integração numerica regra dos trapézios não funciona pois os intervalos de tempo não são constantes....
     *
     * @param acc array float dados do meu acelerometro linear
     * @param gravity float de aceleração de gravidade
     * @param dt intervalo de tempo entre registos
     */
    public void updateVelocity(float[] acc,float[] gravity, float dt)
    {
        mAcceleration.set(0,acc[0]-gravity[0]); //x - Aceleração menos gravidade
        mAcceleration.set(1,acc[1]-gravity[1]); //y - Aceleração menos gravidade
        mAcceleration.set(2,acc[2]-gravity[2]); //z - Aceleração menos gravidade
        if(acc[1]-gravity[1]>0)
            mVelocityTotal = mVelocityTotal + (operations.getMagnitude(getFloatFromDenseMatrix(mAcceleration))*dt);
        if(acc[1]-gravity[1]<0)
            mVelocityTotal = mVelocityTotal - (operations.getMagnitude(getFloatFromDenseMatrix(mAcceleration))*dt);

    }
    /**
     * Actualiza o Azimuth, pitch e roll de acordo com o getrotationmatrix() e Acc+Mag
     *
     *
     * @param dMag array float vetor dados do magnetometro
     * @param dAcc array float vetor dados do acelerometro calibrado
     *
     */
    public void computeAccMagOrientation(float[] dMag, float[] dAcc)
    {
        //azimuth_anterior = aprAccMag[0]; // Para calculo XYVIEW
        if(dMag!=null && dAcc!=null)
        {
            if(SensorManager.getRotationMatrix(mCbnAccMag, null, dAcc, dMag)) { //devolve ENU
                SensorManager.getOrientation(mCbnAccMag, this.mAprAccMag);
                mCbn = mCbnAccMag.clone();

            }
        }
    }

    /**
     * Actualiza o Azimuth, pitch e roll de acordo com o getrotationmatrix() e sensor RotationVector
     *
     *
     * @param dRotVet  array float vetor dados do RotationVector
     *
     */
    public void computeRotVetOrientation(float[] dRotVet) {
        //azimuth_anterior = aprRotVet[0]; // Para calculo XYVIEW
        SensorManager.getRotationMatrixFromVector(mCbnRotVet, dRotVet);
        //SensorManager.remapCoordinateSystem(m_dcm, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y, m_dcmposicao);
        SensorManager.getOrientation(mCbnRotVet, this.mAprRotVet);
        mCbn = mCbnRotVet.clone();
        // Tenho de calcular o offset do azimuth calcuado para que aponte sempre para Y o inicio.


    }
    /**
     * Calcula a orienta??o com a fus?o dos registos Acc+Mag+Gyro
     *
     *
     * @param dGyro  array float vetor dados do Giroscopio
     *
     */
    public void computeGyroOrientation(SensorEvent dGyro) {
        // Não começar até que a primeira orientação acc+mag seja obtida
        if (mAprAccMag == null)
            return;

        // Inicializa??o da matriz rota??o baseada no giroscopio e vai ser inicializada com o acc+mag
        if(initState) {
            float[] initMatrix = new float[9];
            //SensorManager.getRotationMatrixFromVector(initMatrix, mAprAccMag);
            initMatrix = getRotationMatrixFromOrientation(mAprAccMag);//Pode ser mudado
            float[] test = new float[3];
            SensorManager.getOrientation(initMatrix, test);
            mCbnFusion = matrixMultiplication(mCbnFusion, initMatrix);
            initState = false;
        }

        // Copiar os valores do giroscopio para o array
        // Converter os valores originais para o vetor rota??o
        float[] deltaVector = new float[4];
        if(timestamp != 0) {
            final float dT = (dGyro.timestamp - timestamp) * NS2S;
            System.arraycopy(dGyro.values, 0, gyro, 0, 3);

            getRotationVectorFromGyro(gyro, deltaVector, dT / 2.0f);
        }

        // Observa??o tomada, guardar o tempo para o proximo intervalo
        timestamp = dGyro.timestamp;

        // Converter o vetor rota??o para a matriz rota??o
        float[] deltaMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector);

        // Aplicar o novo intervalor de rota??o no giroscopio baseado na matriz rota??o

        mCbnFusion = matrixMultiplication(mCbnFusion, deltaMatrix);
        //Log.d("smta-gyroFunction2", Float.toString(mCbn[1]));

        // Ir buscar a orienta??o baseado n giroscopio da matriz rota??o
        SensorManager.getOrientation(mCbnFusion, gyroOrientation);

        this.estimateFusion();
        mCbn = mCbnFusion.clone();
    }
    /**
     * Implementa o algoritmo do filtro complementar. http://web.mit.edu/scolton/www/filter.pdf
     * Pega no resultado do vetor fus?o aprfusao e acha a matriz rota??o atrav?s dos angulos de Euler para actualizar a matriz rota??o do giroscopio.
     * Crio a DCM com a nova orienta??o. Compenso o drift do giroscopio com a fus?o
     *
     *
     */
    private void estimateFusion()
    {
        float ACCMAG_FILTER_COEFFICIENT = 1.0f - GYRO_FILTER_COEFFICIENT;

        if (this.gyroOrientation[0] < -0.5 * Math.PI && this.mAprAccMag[0] > 0.0) {
            mAprFusao[0] = (float) (GYRO_FILTER_COEFFICIENT * (this.gyroOrientation[0] + 2.0 * Math.PI) + ACCMAG_FILTER_COEFFICIENT * this.mAprAccMag[0]);
            mAprFusao[0] -= (mAprFusao[0] > Math.PI) ? 2.0 * Math.PI : 0;
        }
        else if (this.mAprAccMag[0] < -0.5 * Math.PI && this.gyroOrientation[0] > 0.0) {
            mAprFusao[0] = (float) (GYRO_FILTER_COEFFICIENT * this.gyroOrientation[0] + ACCMAG_FILTER_COEFFICIENT * (this.mAprAccMag[0] + 2.0 * Math.PI));
            mAprFusao[0] -= (mAprFusao[0] > Math.PI)? 2.0 * Math.PI : 0;
        }
        else {
            mAprFusao[0] = GYRO_FILTER_COEFFICIENT * this.gyroOrientation[0] + ACCMAG_FILTER_COEFFICIENT * this.mAprAccMag[0];
        }
        // pitch
        if (this.gyroOrientation[1] < -0.5 * Math.PI && this.mAprAccMag[1] > 0.0) {
            mAprFusao[1] = (float) (GYRO_FILTER_COEFFICIENT * (this.gyroOrientation[1] + 2.0 * Math.PI) + ACCMAG_FILTER_COEFFICIENT * this.mAprAccMag[1]);
            mAprFusao[1] -= (mAprFusao[1] > Math.PI) ? 2.0 * Math.PI : 0;
        }
        else if (this.mAprAccMag[1] < -0.5 * Math.PI && this.gyroOrientation[1] > 0.0) {
            mAprFusao[1] = (float) (GYRO_FILTER_COEFFICIENT * this.gyroOrientation[1] + ACCMAG_FILTER_COEFFICIENT * (this.mAprAccMag[1] + 2.0 * Math.PI));
            mAprFusao[1] -= (mAprFusao[1] > Math.PI)? 2.0 * Math.PI : 0;
        }
        else {
            mAprFusao[1] = GYRO_FILTER_COEFFICIENT * this.gyroOrientation[1] + ACCMAG_FILTER_COEFFICIENT * this.mAprAccMag[1];
        }
        // roll
        if (this.gyroOrientation[2] < -0.5 * Math.PI && this.mAprAccMag[2] > 0.0) {
            mAprFusao[2] = (float) (GYRO_FILTER_COEFFICIENT * (this.gyroOrientation[2] + 2.0 * Math.PI) + ACCMAG_FILTER_COEFFICIENT * this.mAprAccMag[2]);
            mAprFusao[2] -= (mAprFusao[2] > Math.PI) ? 2.0 * Math.PI : 0;
        }
        else if (this.mAprAccMag[2] < -0.5 * Math.PI && this.gyroOrientation[2] > 0.0) {
            mAprFusao[2] = (float) (GYRO_FILTER_COEFFICIENT * this.gyroOrientation[2] + ACCMAG_FILTER_COEFFICIENT * (this.mAprAccMag[2] + 2.0 * Math.PI));
            mAprFusao[2] -= (mAprFusao[2] > Math.PI)? 2.0 * Math.PI : 0;
        }
        else {
            mAprFusao[2] = GYRO_FILTER_COEFFICIENT * this.gyroOrientation[2] + ACCMAG_FILTER_COEFFICIENT * this.mAprAccMag[2];
        }

        mCbnFusion = this.getRotationMatrixFromOrientation(mAprFusao);
        System.arraycopy(mAprFusao, 0, this.gyroOrientation, 0, 3); //copia de elementos de array
     }
    /*NOVO FILTRO*/
    public float[] getRotationMatrixFromOrientation(float[] o) {
        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];

        float sinX = (float)Math.sin(o[1]);
        float cosX = (float)Math.cos(o[1]);
        float sinY = (float)Math.sin(o[2]);
        float cosY = (float)Math.cos(o[2]);
        float sinZ = (float)Math.sin(o[0]);
        float cosZ = (float)Math.cos(o[0]);

        // rota??o em torno do X (pitch)
        xM[0] = 1.0f; xM[1] = 0.0f; xM[2] = 0.0f;
        xM[3] = 0.0f; xM[4] = cosX; xM[5] = sinX;
        xM[6] = 0.0f; xM[7] = -sinX; xM[8] = cosX;

        // rota??o em torno do y (roll)
        yM[0] = cosY; yM[1] = 0.0f; yM[2] = sinY;
        yM[3] = 0.0f; yM[4] = 1.0f; yM[5] = 0.0f;
        yM[6] = -sinY; yM[7] = 0.0f; yM[8] = cosY;

        // rota??o em torno do z (azimuth)
        zM[0] = cosZ; zM[1] = sinZ; zM[2] = 0.0f;
        zM[3] = -sinZ; zM[4] = cosZ; zM[5] = 0.0f;
        zM[6] = 0.0f; zM[7] = 0.0f; zM[8] = 1.0f;

        // a ordem de rota??o ? y, x, z (roll, pitch, azimuth)
        float[] resultMatrix = matrixMultiplication(xM, yM);
        resultMatrix = matrixMultiplication(zM, resultMatrix);
        return resultMatrix;
    }
    public float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];

        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];

        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];

        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];

        return result;
    }
    public void getRotationVectorFromGyro(float[] gyroValues,
                                          float[] deltaRotationVector,
                                          float timeFactor)
    {
        float[] normValues = new float[3];

        // Calcular a velocidade angular da amostra
        float omegaMagnitude =
                (float)Math.sqrt(gyroValues[0] * gyroValues[0] +
                        gyroValues[1] * gyroValues[1] +
                        gyroValues[2] * gyroValues[2]);

        // Normalizar o vetor rota??o se for suficientemente grande para obter um exo
        if(omegaMagnitude > EPSILON) {
            normValues[0] = gyroValues[0] / omegaMagnitude;
            normValues[1] = gyroValues[1] / omegaMagnitude;
            normValues[2] = gyroValues[2] / omegaMagnitude;
        }

        // Integrate around this axis with the angular speed by the timestep
        // in order to get a delta rotation from this sample over the timestep
        // We will convert this axis-angle representation of the delta rotation
        // into a quaternion before turning it into the rotation matrix.
        float thetaOverTwo = omegaMagnitude * timeFactor;
        float sinThetaOverTwo = (float)Math.sin(thetaOverTwo);
        float cosThetaOverTwo = (float)Math.cos(thetaOverTwo);
        deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
        deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
        deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
        deltaRotationVector[3] = cosThetaOverTwo;
    }
    /**
     * Pega num vetor 3x3 em DenseMatrix64F e transforma-o em Float.
     *
     * @param vetor DenseMatrix64F
     * @return retval float[]
     */
    public float[] getFloatFromDenseMatrix(DenseMatrix64F vetor)
    {
        float[] retval = new float[3];
        for(int i=0;i<3;i++)
            retval[i]=(float)vetor.get(i);
        return retval;
    }
}
