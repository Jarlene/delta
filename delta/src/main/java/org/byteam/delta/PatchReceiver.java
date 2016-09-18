package org.byteam.delta;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Receiver used to deliver patch result.
 * <p>
 * Created by chenenyu on 16/9/14.
 */
public class PatchReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Patch patch = intent.getParcelableExtra(PatchService.PATCH_FLAG);
        Delta.patchResult(context, patch);
        abortBroadcast();
    }
}
