package mei.tcd.util;

/**
 * Created by pessanha on 27-07-2013.
 */
public class VectorAverage {
    float[] arrayX;
    float[] arrayY;
    float[] arrayZ;
    float[] accCalibratedData = new float[3];
    int observations;
    int index = 0;
    public boolean hasDetermined = false;
    Operations operations;

    public VectorAverage(int _observacoes){
        operations = new Operations();
        arrayX = new float[_observacoes];
        arrayY = new float[_observacoes];
        arrayZ = new float[_observacoes];
        observations = _observacoes;
        index = 0;
        resetCalibration();
    }


    /***
     * Vai buscar a média dos valores gravados em cada um dos eixos
     * @return o vetor média dos valores
     */
    public float[] getAveragedVetor()
    {
        return accCalibratedData;
    }
    public void resetCalibration()
    {
        for (int i = 0;i<3;i++)
            accCalibratedData[i] = 0;
        index = 0;
    }
    /***
     * Adiciona o valor ao array de valores de cada eixo para quando finalizar, então calcular a média
     * @param _xyz
     */
    public void addVector(float[] _xyz)
    {
        if(index==this.observations-1)
        {
            hasDetermined = true;
            accCalibratedData[0] = operations.getAverage(arrayX);
            accCalibratedData[1] = operations.getAverage(arrayY);
            accCalibratedData[2] = operations.getAverage(arrayZ);
        }
        arrayX[index] =  _xyz[0];
        arrayY[index] =  _xyz[1];
        arrayZ[index] =  _xyz[2];
        index ++;
    }
}
