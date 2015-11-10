package com.arenacloud.broadcast.android.streaming;

import android.os.Build;

/**
 * Created by thomas on 7/10/14.
 */
public class Utilities {
    public static boolean supportsAdaptiveStreaming() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

}
