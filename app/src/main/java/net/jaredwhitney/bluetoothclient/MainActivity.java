package net.jaredwhitney.bluetoothclient;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_ENABLE_BT = 1;
    BluetoothAdapter btadapter;
    Intent serviceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serviceIntent = new Intent(this, MyService.class);

        Button resyncButton = findViewById(R.id.button);
        resyncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(serviceIntent);
            }
        });

        startServer();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT)
        {
            if (resultCode == RESULT_OK)
            {
                startService(new Intent(this, MyService.class));
            }
        }
    }

    private void startServer()
    {
        btadapter = BluetoothAdapter.getDefaultAdapter();
        if (btadapter!=null)
        {
            if (!btadapter.isEnabled())
            {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
            Log.d("Main","Starting server");
            startService(serviceIntent);
        }
        else
            Toast.makeText(getApplicationContext(), "BT Unavailable", Toast.LENGTH_LONG).show();
    }

}
