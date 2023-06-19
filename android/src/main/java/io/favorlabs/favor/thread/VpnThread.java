package io.favorlabs.favor.thread;

import android.util.Log;

import org.asynchttpclient.ws.WebSocket;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import io.favorlabs.favor.model.Const;
import io.favorlabs.favor.model.Global;
import io.favorlabs.favor.model.Stats;
import io.favorlabs.favor.service.MyVpnService;

public class VpnThread extends BaseThread {
  private static final String TAG = "VPNThread";
  private final FileInputStream in;
  private final WebSocket webSocket;
  private final MyVpnService.VpnBinder vpnBinder;

  public VpnThread(FileInputStream in, WebSocket webSocket, MyVpnService.VpnBinder vpnBinder) {
    this.in = in;
    this.webSocket = webSocket;
    this.vpnBinder = vpnBinder;
  }

  @Override
  public void run() {
    Log.i(TAG, "start");
    // forward data
    byte[] buf = new byte[Const.BUFFER_SIZE];
    while (Global.RUNNING) {
      int ln = 0;
      try {
        ln = in.read(buf);
        if (ln <= 0) {
          continue;
        }
      } catch (IOException e) {
        Log.i(TAG, "in.read err:" + e.toString());
        break;
      }
      if (webSocket != null && webSocket.isOpen()) {
        byte[] data = Arrays.copyOfRange(buf, 0, ln);
        webSocket.sendBinaryFrame(data);
        Stats.UPLOAD_BYTES.addAndGet(ln);
      } else {
        Log.i(TAG, "ws client is disconnected");
        break;
      }
    }
    Log.i(TAG, "stop");
    vpnBinder.disconnect();
  }
}
