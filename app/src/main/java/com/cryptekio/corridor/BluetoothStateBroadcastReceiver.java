package com.cryptekio.corridor;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BluetoothStateBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "BTStateReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    Log.d(TAG,"state_off");

                    //setButtonText("Bluetooth off");
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    //setButtonText("Turning Bluetooth off...");
                    break;
                case BluetoothAdapter.STATE_ON:
                    Log.d(TAG,"state_on");
                    if(Build.VERSION.SDK_INT >25){
                        context.startForegroundService(new Intent(context, BTScannerService.class));
                    }else{
                        context.startService(new Intent(context, BTScannerService.class));
                    }
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    break;
            }
        }
    }
}
