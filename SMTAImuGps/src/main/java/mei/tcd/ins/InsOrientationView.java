package mei.tcd.ins;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.content.res.Resources;

import mei.tcd.smta.R;

/**
 * Created by pessanha on 11-08-2013.
 */
public class InsOrientationView  extends View {
    // Paints para desenhar
    private Paint marcasPaint;
    private Paint textPaint;
    private Paint circuloForaPaint;
    private Paint circuloDentroPaint;
    private Paint pitchRollPaint;

    // Pontos cardeais s?o strings
    private String nString;
    private String eString;
    private String sString;
    private String oString;

    private float pitch;
    private float roll;
    private float azimuth;
    // Vai guardar a altura do texto
    private int textHeight;

    // getter para pitch
    public float getPitch() {
        return pitch;
    }
    // setter para pitch
    public void setPitch(float pitch) {
        this.pitch = pitch;
    }
    // getter para roll
    public float getRoll() {
        return roll;
    }
    // setter para roll
    public void setRoll(float roll) {
        this.roll = roll;
    }
    // setter para direcao
    public void setAzimuth(float _azimuth) {
        azimuth = _azimuth;
    }
    //getter para direcao
    public float getAzimuth() {
        return azimuth;
    }


    // M?todos contrutores da view
    public InsOrientationView(Context context) {
        super(context);
        initOrientacaoView();
    }

    public InsOrientationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initOrientacaoView();
    }

    public InsOrientationView(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
        initOrientacaoView();
    }
    // Iniciliaza??o de variaveis
    protected void initOrientacaoView() {
        // N?o ? focusable (pode receber touchs)
        setFocusable(true);
        // Referencia aos resources para carregar as strings
        Resources r = this.getResources();
        nString = r.getString(R.string.norte);
        eString = r.getString(R.string.este);
        sString = r.getString(R.string.sul);
        oString = r.getString(R.string.oeste);

        // paints para os circulos
        circuloForaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circuloForaPaint.setColor(r.getColor(R.color.circulos_grandes));
        circuloForaPaint.setStrokeWidth(3);
        circuloForaPaint.setStyle(Paint.Style.STROKE);

        circuloDentroPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circuloDentroPaint.setColor(r.getColor(R.color.circulos_pequenos));
        circuloDentroPaint.setStrokeWidth(1);
        circuloDentroPaint.setStyle(Paint.Style.STROKE);

        pitchRollPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pitchRollPaint.setColor(r.getColor(R.color.pitch_roll));
        pitchRollPaint.setStrokeWidth(1);
        pitchRollPaint.setStyle(Paint.Style.FILL);
        // paint para as marcas
        marcasPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        marcasPaint.setStrokeWidth(2);
        marcasPaint.setColor(r.getColor(R.color.marcas));

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(r.getColor(R.color.text_color));
        textPaint.setTextSize(13);
        textHeight = (int)textPaint.measureText("yY");// inteiro

    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // para ocupar o maximo espa?o disponivel
        int measuredWidth = measure(widthMeasureSpec);
        int measuredHeight = measure(heightMeasureSpec);

        int d = Math.min(measuredWidth, measuredHeight);

        setMeasuredDimension(d, d);
    }

    private int measure(int measureSpec) {
        int result = 0;


        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.UNSPECIFIED) {
            // por defeito fica 200
            result = 200;
        } else {
            // retorna sempre o m?ximo
            result = specSize;
        }
        return result;
    }
    @Override
    protected void onDraw(Canvas canvas) {
        int px = getMeasuredWidth() / 2;
        int py = getMeasuredHeight() / 2;

        int radius = Math.min(px, py);

        canvas.drawCircle(px, py, radius, circuloForaPaint);
        // antes de rodar fazer save para mais tarde efectuat o restore e assim poder desenhar po cima
        canvas.save();
        //- azimuth para rodar no sentido correcto
        canvas.rotate(-azimuth, px, py);
        int textWidth = (int)textPaint.measureText("W");
        int cardinalX = px-textWidth/2;
        int cardinalY = py-radius+textHeight;

        // a cada 15 graus
        for (int i = 0; i < 24; i++) {
            // marcador
            canvas.drawLine(px, py-radius, px, py-radius+10, 		marcasPaint);

            canvas.save();
            canvas.translate(0, textHeight);

            if (i % 6 == 0) {
                String dirString = "";
                switch (i) {
                    case(0)  : {
                        dirString = nString;
                        int arrowY = 2*textHeight;
                        canvas.drawLine(px, arrowY, px-5, 	3*textHeight, marcasPaint);
                        canvas.drawLine(px, arrowY, px+5, 	3*textHeight, marcasPaint);
                        break;
                    }
                    case(6)  : dirString = eString; break;
                    case(12) : dirString = sString; break;
                    case(18) : dirString = oString; break;
                }

                canvas.drawText(dirString, cardinalX, 	cardinalY, textPaint);
            }

            else if (i % 3 == 0) {
                // cada 45 degraus
                String angle = String.valueOf(i*15);
                float angleTextWidth = 	textPaint.measureText(angle);

                int angleTextX = (int)(px-angleTextWidth/2);
                int angleTextY = py-radius+textHeight;
                canvas.drawText(angle, angleTextX, angleTextY, 	textPaint);
            }
            canvas.restore();
            canvas.rotate(15, px, py);
        }
        canvas.restore();
        RectF rollOval = new RectF((getMeasuredWidth()/3)-	getMeasuredWidth()/7,
                (getMeasuredHeight()/2)-getMeasuredWidth()/7,
                (getMeasuredWidth()/3)+getMeasuredWidth()/7,
                (getMeasuredHeight()/2)+getMeasuredWidth()/7);
        marcasPaint.setStyle(Paint.Style.STROKE);
        canvas.drawOval(rollOval, circuloDentroPaint);
        marcasPaint.setStyle(Paint.Style.FILL);
        canvas.save();
        canvas.rotate(roll, getMeasuredWidth()/3, 	getMeasuredHeight()/2);
        canvas.drawArc(rollOval, 0, 180, false, pitchRollPaint);
        marcasPaint.setStyle(Paint.Style.STROKE);
        canvas.restore();

        RectF pitchOval = new RectF((2*getMeasuredWidth()/3)-	getMeasuredWidth()/7,
                (getMeasuredHeight()/2)-getMeasuredWidth()/7,
                (2*getMeasuredWidth()/3)+getMeasuredWidth()/7,
                (getMeasuredHeight()/2)+getMeasuredWidth()/7);
        canvas.drawOval(pitchOval, circuloDentroPaint);
        marcasPaint.setStyle(Paint.Style.FILL);
        canvas.drawArc(pitchOval, 0-pitch/2, 180+(pitch), false, 	pitchRollPaint);
        marcasPaint.setStyle(Paint.Style.STROKE);



        canvas.restore();
    }


}
