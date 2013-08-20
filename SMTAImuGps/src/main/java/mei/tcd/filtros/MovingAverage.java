package mei.tcd.filtros;
import java.util.LinkedList;
/**
 * Created by pessanha on 03-08-2013.
 * http://en.wikipedia.org/wiki/Moving_average.
 */
public class MovingAverage {
    private LinkedList values = new LinkedList();

    private int length;

    private double sum = 0;

    private double average = 0;

    /**
     * M?todo construtor
     *
     * @param length n?mero de pontos
     */
    public MovingAverage(int length)
    {
        if (length <= 0)
        {
            throw new IllegalArgumentException("Tamanho dever? ser maior que zero");
        }
        this.length = length;
    }

    /**
     * Retorna a m?dia atual
     *
     * @return average
     */
    public double currentAverage()
    {
        return average;
    }

    /**
     * Calcula a m?dia m?vel.
     * Synchronised por forma a que nenhuma altera??o seja efetuada nos dados durante o calculo.
     *
     * @param value o valor
     * @return A m?dia
     */
    public synchronized double compute(double value)
    {
        if (values.size() == length && length > 0)
        {
            sum -= ((Double) values.getFirst()).doubleValue();
            values.removeFirst();
        }
        sum += value;
        values.addLast(new Double(value));
        average = sum / values.size();
        return average;
    }
}
