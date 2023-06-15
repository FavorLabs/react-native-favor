package io.favorlabs.favor.thread;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.system.OsConstants;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.asynchttpclient.ws.WebSocket;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import io.favorlabs.favor.model.Config;
import io.favorlabs.favor.model.Const;
import io.favorlabs.favor.model.Global;
import io.favorlabs.favor.model.LocalIp;
import io.favorlabs.favor.model.Stats;
import io.favorlabs.favor.service.IpService;
import io.favorlabs.favor.service.MyVpnService;
import io.favorlabs.favor.ws.MyWebSocketClient;

public class VpnThread extends BaseThread {
    private static final String TAG = "VPNThread";
    private final Config config;
    private FileInputStream in = null;
    private FileOutputStream out = null;
    private ParcelFileDescriptor tun = null;
    private WebSocket webSocket = null;

    public VpnThread(Config config, MyVpnService vpnService, IpService ipService, NotificationManager notificationManager, NotificationCompat.Builder notificationBuilder) {
        this.config = config;
        this.vpnService = vpnService;
        this.ipService = ipService;
        this.notificationManager = notificationManager;
        this.notificationBuilder = notificationBuilder;
    }

    @Override
    public void run() {
        try {
            Log.i(TAG, "start");
            if (!ipService.addObserveGroup()) {
              vpnService.stopVpn();
              return;
            }
            while (!ipService.health()){
                TimeUnit.SECONDS.sleep(2);
                if (!Global.RUNNING) {
                    return;
                }
            }
            // pick ip
            LocalIp localIP = ipService.pickIp();
            if (localIP == null) {
                vpnService.stopVpn();
                return;
            }
            Global.LOCAL_IP = localIP.getLocalIp();
            //pick ipv6
            LocalIp localIPv6 = ipService.pickIpv6();
            if (localIPv6 == null) {
                vpnService.stopVpn();
                return;
            }
            Global.LOCAL_IPv6 = localIPv6.getLocalIp();
            // create tun
            tun = createTunnel(config, localIP, localIPv6);
            if (tun == null) {
                vpnService.stopVpn();
                return;
            }
            in = new FileInputStream(tun.getFileDescriptor());
            out = new FileOutputStream(tun.getFileDescriptor());
            // create ws client
            @SuppressLint("DefaultLocale") String uri = String.format("%s://%s:%d%s", config.getProto(), config.getServerAddress(), config.getServerPort(), config.getPath());
            webSocket = MyWebSocketClient.connectWebSocket(uri, config.getKey(), config, out);
            if (webSocket == null || !webSocket.isOpen()) {
                Log.i(TAG, "webSocket is not open");
                vpnService.stopVpn();
                closeTun();
                return;
            }else {
              Log.i(TAG, "webSocket is open");
              Global.CONNECTED = true;
            }
            // start notify threads
            NotifyThread notifyThread = new NotifyThread(notificationManager, notificationBuilder, vpnService, ipService);
            notifyThread.start();
            // forward data
            byte[] buf = new byte[Const.BUFFER_SIZE];
            while (Global.RUNNING) {
                try {
                    int ln = in.read(buf);
                    if (ln <= 0) {
                        continue;
                    }
                    if (webSocket != null && webSocket.isOpen()) {
                        Global.CONNECTED = true;
                        byte[] data = Arrays.copyOfRange(buf, 0, ln);
                        webSocket.sendBinaryFrame(data);
                        Stats.UPLOAD_BYTES.addAndGet(ln);
                    } else {
                      Global.CONNECTED = false;
                      Log.i(TAG, "ws client is disconnected");
                      break;
//                        webSocket = MyWebSocketClient.connectWebSocket(uri, config.getKey(), config, out);
//                        TimeUnit.MILLISECONDS.sleep(200);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "error on WsThread:" + e.toString());
                }
            }
            Log.i(TAG, "stop");
        } catch (Exception e) {
            Log.e(TAG, "error on WsThread:" + e.toString());
        } finally {
            ipService.deleteIp(Global.LOCAL_IP);
            ipService.delObserveGroup();
            closeTun();
        }
    }

    private ParcelFileDescriptor createTunnel(Config config, LocalIp localIP, LocalIp localIPv6) throws PackageManager.NameNotFoundException {
        if (config == null || localIP == null || localIPv6 == null) {
            return null;
        }
        VpnService.Builder builder = vpnService.new Builder();
        builder.setMtu(Const.MTU)
                .addAddress(localIP.getLocalIp(), localIP.getLocalPrefixLength())
                .addAddress(localIPv6.getLocalIp(), localIPv6.getLocalPrefixLength())
                .addRoute(Const.DEFAULT_ROUTE, 0)
                .addRoute(Const.DEFAULT_ROUTEv6, 0)
                .addDnsServer(config.getDns())
                .setSession(Const.APP_NAME)
                .setConfigureIntent(null)
                .allowFamily(OsConstants.AF_INET)
                .allowFamily(OsConstants.AF_INET6)
                .setBlocking(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false);
        }
        if (Global.vpn_whitelist.isEmpty()) {
          for (String packageName : Global.vpn_blacklist) {
            builder.addDisallowedApplication(packageName);
          }
        }else {
          for (String packageName : Global.vpn_whitelist) {
            builder.addAllowedApplication(packageName);
          }
        }
        Log.i(TAG, "whitelist apps:" + Global.vpn_whitelist);
        Log.i(TAG, "blacklist apps:" + Global.vpn_blacklist);
        return builder.establish();
    }

    private void closeTun() {
        if (webSocket != null && webSocket.isOpen()) {
            webSocket.sendCloseFrame();
        }
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (tun != null) {
            try {
                tun.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Global.RUNNING = false;
    }
}
