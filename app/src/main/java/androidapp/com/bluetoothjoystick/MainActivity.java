package androidapp.com.bluetoothjoystick;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private boolean running = false;
    private BluetoothAdapter adapter;
    private String myUUID = "00001101-0000-1000-8000-00805F9B34FB";
    private JoyStick stick;
    private BluetoothDevice device;
    private BluetoothSocket socket;
    private TextView textView;
    private float lastX = Float.NaN, lastY = Float.NaN;
    private float maxSpeed = 255;
    private boolean isSending = false;

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView

                Log.i("device", String.valueOf(device));


                if (device.getName().equals("HC-06")) {
                    MainActivity activity = (MainActivity) context;
                    activity.device = device;
                    activity.startSend();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        stick = findViewById(R.id.joyStick);
        textView = findViewById(R.id.textView);

        adapter = BluetoothAdapter.getDefaultAdapter();

        textView.setText("Finding device...");

        if(!adapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }else {
            device = getDevice();
            if (device != null){
                startSend();
            }
            else {
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
                adapter.startDiscovery();
            }
        }
    }


    private BluetoothDevice getDevice(){
        Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {

                if (device.getName().equals("HC-06")){
                    return device;
                }
            }
        }


        return null;
    }


    private void sendToDriver(DataOutputStream outputStream) throws IOException {
        if (!stick.isTouching() && isSending){
            lastX = 0;
            lastY = 0;
            outputStream.writeShort(0);
            outputStream.writeShort(0);
            outputStream.writeShort(0);
            outputStream.writeShort(0);
            isSending = false;
        }else if (stick.getInputX() + stick.getInputY() != 0){

            isSending = true;
            lastX = stick.getInputX();
            lastY = stick.getInputY();
            float angle = (float) Math.atan2(lastY, lastX);

            if(angle > 0) {
                if(angle < 5.0f/12 * Math.PI){
                    //up right
                    outputStream.writeShort((short) (stick.mag() * maxSpeed));
                    outputStream.writeShort((short) ((1 - lastX) * lastY * maxSpeed));
                }else if (angle > 7.0f/12 * Math.PI){
                    //up left
                    outputStream.writeShort((short) ((1 + lastX) * lastY * maxSpeed));
                    outputStream.writeShort((short) (stick.mag() * maxSpeed));
                }else {
                    //forward
                    outputStream.writeShort((short) (stick.mag() * maxSpeed));
                    outputStream.writeShort((short) (stick.mag() * maxSpeed));
                }
            }else {
                if(angle < -5.0f/12 * Math.PI){
                    //down left
                    outputStream.writeShort((short) ((1 + lastX) * lastY * maxSpeed));
                    outputStream.writeShort((short) (-stick.mag() * maxSpeed));

                }else if (angle > -7.0f/12 * Math.PI){
                    //down right
                    outputStream.writeShort((short) (-stick.mag() * maxSpeed));
                    outputStream.writeShort((short) ((1 - lastX) * lastY * maxSpeed));

                }else {
                    //down
                    outputStream.writeShort((short) (-stick.mag() * maxSpeed));
                    outputStream.writeShort((short) (-stick.mag() * maxSpeed));
                }
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }


    private void startSend(){

        textView.setText("Connecting.");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    socket = device.createRfcommSocketToServiceRecord(UUID.fromString(myUUID));

                    socket.connect();
                    adapter.cancelDiscovery();
                    DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView.setText("Device connected.");
                        }
                    });

                    running = true;




                    while (running){

                        sendToDriver(outputStream);

                        try {
                            Thread.sleep(1000/12);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    socket.close();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    private void sendData(short val, OutputStream stream){

        try {
            stream.write(val >> 8 & 0xFF);
            stream.flush();
            stream.write(val & 0xFF);
            stream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1){
            if (resultCode == RESULT_OK){
                device = getDevice();
                if (device != null) startSend();
                else {
                    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
                    adapter.startDiscovery();
                }
            }
        }

    }

    @Override
    protected void onStop() {
        running = false;
        super.onStop();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }
}
