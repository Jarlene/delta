package org.byteam.delta;

import android.app.IntentService;
import android.content.Intent;

import org.byteam.delta.patch.DeltaPatch;

/**
 * This service is started in a new process to execute patch.
 * <p>
 * Created by chenenyu on 16/9/14.
 */
public class PatchService extends IntentService {

    public static final String PATCH_FLAG = "patch";

    /**
     * A constructor is required, and must call the super IntentService(String)
     * constructor with a name for the worker thread.
     */
    public PatchService() {
        super("PatchService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Patch patch = intent.getParcelableExtra(PATCH_FLAG);
        Intent patchReceiver = new Intent("org.byteam.delta.patch-receiver");
        if (patch != null) {
            int patchResult = DeltaPatch.patch(patch.getOldPath(), patch.getNewPath(),
                    patch.getPatchPath());
            if (patchResult == 0) {
                patchReceiver.putExtra(PATCH_FLAG, patch);
            }
        }
        sendBroadcast(patchReceiver);
    }
}
