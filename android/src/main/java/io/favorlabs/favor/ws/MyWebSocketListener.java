package io.favorlabs.favor.ws;

import android.util.Log;

import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketListener;

import java.io.FileOutputStream;
import java.io.IOException;

import io.favorlabs.favor.model.Config;
import io.favorlabs.favor.model.Stats;

public class MyWebSocketListener implements WebSocketListener {
    private static final String TAG = "MyWebSocketListener";
    private Config config;
    private FileOutputStream out;

    public MyWebSocketListener() {

    }

    public MyWebSocketListener(Config config, FileOutputStream out) {
        this.config = config;
        this.out = out;
    }

    @Override
    public void onOpen(WebSocket websocket) {
        Log.i(TAG, "onOpen");
    }

    @Override
    public void onClose(WebSocket websocket, int code, String reason) {
        Log.i(TAG, "onClose code:" + code + " reason:" + reason);
    }

    @Override
    public void onError(Throwable t) {
        Log.e(TAG, "onError " + t.getMessage());
    }

    @Override
    public void onBinaryFrame(byte[] payload, boolean finalFragment, int rsv) {
        if (out == null || config == null || payload == null || payload.length == 0) {
            return;
        }
        try {
            out.write(payload);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        Stats.DOWNLOAD_BYTES.addAndGet(payload.length);
    }
}
