package net.jaredwhitney.bluetoothclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ControlsReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getStringExtra("action");
        Log.w("notiControl", "received '" + action + "'");
        if (action.equals("volume up"))
        {
            MyService.writer.println("volume up");
            MyService.writer.flush();
        }
        else if (action.equals("volume down"))
        {
            MyService.writer.println("volume down");
            MyService.writer.flush();
        }
    }
}
