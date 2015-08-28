package ru.twosphere.metrica.src;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Александр on 27.08.2015.
 */
public class BootBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d("TwosphereMetrics", "BootBroadcastReceiver BOOT_COMPLETED");
            context.startService(new Intent(context, LocationService.class));
        }
    }
}
