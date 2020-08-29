package androidapp.com.bluetoothjoystick;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidapp.com.bluetoothjoystick.math.Vector;


public class JoyStick extends View implements View.OnTouchListener {

    private Paint inputCirclePaint, backgroundCirclePaint;
    private Vector inputPosition;
    private float inputRadius;
    private boolean isTouching;

    public JoyStick(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        setClickable(true);
        setOnTouchListener(this);

        inputCirclePaint = new Paint();
        backgroundCirclePaint = new Paint();
        inputPosition = new Vector(0, 0);
        inputRadius = 60;

        backgroundCirclePaint.setColor(ContextCompat.getColor(getContext(), R.color.cardview_dark_background));
        inputCirclePaint.setColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
    }

    public float mag(){
        return inputPosition.mag() / getBackgroundRadius();
    }

    public float getInputX(){
        return inputPosition.getX() / getBackgroundRadius();
    }

    public float getInputY(){
        return -inputPosition.getY() / getBackgroundRadius();
    }

    private float getCenterX(){
        return getMeasuredWidth() * 0.5f;
    }

    private float getCenterY(){
        return getMeasuredHeight() * 0.5f;
    }

    private float getBackgroundRadius(){
        return Math.min(getCenterX(), getCenterY());
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {

        switch (motionEvent.getAction()){
            case MotionEvent.ACTION_MOVE :
                //update circle position
                isTouching = true;
                inputPosition.setX(motionEvent.getX() - getCenterX());
                inputPosition.setY(motionEvent.getY() - getCenterY());

                if (inputPosition.sqrtMag() > getBackgroundRadius() * getBackgroundRadius()){
                    inputPosition.setMag(getBackgroundRadius());
                }
                break;
            case MotionEvent.ACTION_UP:
                //reset circle position
                inputPosition.setY(0);
                inputPosition.setX(0);
                isTouching = false;
                break;
        }

        invalidate();
        return true;
    }

    public boolean isTouching(){
        return isTouching;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        //draw great circle
        canvas.drawCircle(getCenterX(), getCenterY(), getBackgroundRadius(), backgroundCirclePaint);
        //draw little circle
        canvas.drawCircle(inputPosition.getX() + getCenterX(), inputPosition.getY() + getCenterY(), inputRadius, inputCirclePaint);
    }


}
