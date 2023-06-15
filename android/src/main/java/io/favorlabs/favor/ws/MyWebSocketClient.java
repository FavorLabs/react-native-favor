package io.favorlabs.favor.ws;

import static org.asynchttpclient.Dsl.asyncHttpClient;

import android.util.Log;

import org.asynchttpclient.Response;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import io.favorlabs.favor.model.Config;
import io.favorlabs.favor.model.Const;
import io.favorlabs.favor.model.Global;

public class MyWebSocketClient {
    public static WebSocket connectWebSocket(String url, String key) {
        WebSocket websocket = null;
        try {
            websocket = asyncHttpClient().prepareGet(url)
              .addHeader("key", key)
              .addHeader("group", Global.vpn_group)
              .setRequestTimeout(5000)
              .execute(new WebSocketUpgradeHandler.Builder()
                .addWebSocketListener(new MyWebSocketListener())
                .build())
              .get();
        } catch (ExecutionException | InterruptedException e) {
            Log.e(Const.DEFAULT_TAG, "websocket connect test:" + e.toString());
        }
        return websocket;
    }

    public static WebSocket connectWebSocket(String url, String key, Config config, FileOutputStream out) {
        WebSocket websocket = null;
        try {
            websocket = asyncHttpClient().prepareGet(url)
              .addHeader("key", key)
              .addHeader("group", Global.vpn_group)
              .setRequestTimeout(10000)
              .execute(new WebSocketUpgradeHandler.Builder()
                .addWebSocketListener(new MyWebSocketListener(config, out))
                .build())
              .get();
        } catch (ExecutionException | InterruptedException e) {
            Log.e(Const.DEFAULT_TAG, "websocket connect:" + e.toString());
        }
        return websocket;
    }

    public static String httpGet(String url, String key) {
        Future<Response> whenResponse = asyncHttpClient()
          .prepareGet(url)
          .addHeader("key", key)
          .addHeader("group", Global.vpn_group)
          .setRequestTimeout(25000).execute();
        try {
            return whenResponse.get().getResponseBody(StandardCharsets.UTF_8);
        } catch (ExecutionException | InterruptedException e) {
            Log.e(Const.DEFAULT_TAG, "http get:" + e.toString());
            return e.toString();
        }
    }

}
