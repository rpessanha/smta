package mei.tcd.util;

/**
 * Created by pessanha on 28-07-2013.
 */
public class Operations {
    /**
     * Obtenção da média de arrays float
     *
     * @param dados array vetor float
     * @return média
     */
    public float getAverage(float[] dados)
    {
        float retval = 0;
        for(int i =0;i<dados.length; i++)
            retval +=  (dados[i]);
        return retval/dados.length;
    }
    /**
     * Retorna verdadeiro ou false se magnitude > ou < 0
     *
     * @param dados array vetor float
     * @return true ou false
     */
    public boolean hasMagnitude(float[] dados)
    {
        if( (this.getMagnitude(dados))>0){
            return true;
        }
        else return false;
    }
    /**
     * Obtem a magnitude de um vetor array float
     *
     * @param dados array vetor float
     * @return magnitude
     */
    public float getMagnitude(float[] dados)
    {
        float SomaQuadrada = 0;
        for(int i=0;i<dados.length;i++){
            SomaQuadrada +=Math.abs(dados[i])*Math.abs(dados[i]);

        }
        return (float) Math.sqrt(SomaQuadrada);

    }
}
