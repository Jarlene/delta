package org.byteam.delta;


/**
 * Listener that lives around the lifecycle of patching.
 * <p>
 * Created by chenenyu on 16/9/14.
 */
public interface PatchListener {

    void patchResult(boolean result, String message);

}
