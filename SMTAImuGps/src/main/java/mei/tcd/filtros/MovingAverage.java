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
     * Método construtor
     *
     * @param length_ número de pontos
     */
    public MovingAverage(int length_)
    {
        if (length_ <= 0)
        {
            throw new IllegalArgumentException("Tamanho deverá ser maior que zero");
        }
        this.length = length_;
    }

    /**
     * Retorna a média atual
     *
     * @return average
     */
    public double currentAverage()
    {
        return this.average;
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
            this.sum -= ((Double) values.getFirst()).doubleValue();
            values.removeFirst();
        }
        this.sum += value;
        values.addLast(new Double(value));
        this.average = this.sum / values.size();
        return this.average;
    }
}
