package androidapp.com.bluetoothjoystick.math;

public class Vector {
    private float x;
    private float y;

    public Vector(float x, float y) {
        this.x = x;
        this.y = y;
    }


    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float sqrtMag(){
        return x * x + y * y;
    }

    public float mag(){
        return (float) Math.sqrt(x * x + y * y);
    }

    public void normalize(){

        float mag = mag();
        x /= mag;
        y /= mag;
    }

    public void mult(float s){
        x *= s;
        y *= s;
    }

    public void setMag(float s){
        normalize();
        mult(s);
    }
}
