package mei.tcd.filtros;

/**
 * Created by pessanha on 03-08-2013.
 */
public class LowPassFilter {
    private float mAlpha;

    public LowPassFilter(float alpha_)
    {
        mAlpha = alpha_;

    }
    public float[] compute(float[] input, float[] output) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = mAlpha * input[i] + (1.0f - mAlpha) * output[i];

        }
        return output;
    }
}
