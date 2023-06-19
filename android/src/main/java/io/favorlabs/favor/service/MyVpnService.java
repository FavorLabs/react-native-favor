package io.favorlabs.favor.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;
import android.system.OsConstants;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.facebook.react.bridge.Promise;

import org.asynchttpclient.ws.WebSocket;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;

import io.favorlabs.favor.R;
import io.favorlabs.favor.model.Const;
import io.favorlabs.favor.model.Global;
import io.favorlabs.favor.model.LocalIp;
import io.favorlabs.favor.thread.NotifyThread;
import io.favorlabs.favor.thread.VpnThread;

public class MyVpnService extends VpnService {
  private static StateListener stateListener;
  private static IpService ipService;
  private VpnBinder vpnBinder = null;
  private final IntentFilter filter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
  private final BroadcastReceiver airplaneModeOnReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
        if (Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0) {
          Log.i(Const.DEFAULT_TAG, "VPN airplane_mode_on");
          vpnBinder.disconnect();
        }
      }
    }
  };

  @Override
  public void onCreate() {
    Log.i(Const.DEFAULT_TAG, "VPN onCreate");
    registerReceiver(airplaneModeOnReceiver, filter);
    vpnBinder = new VpnBinder();
  }

  @Override
  public IBinder onBind(Intent intent) {
    Log.i(Const.DEFAULT_TAG, "VPN onBind " + intent.toString());
    return vpnBinder;
  }

  public void onRevoke() {
    Log.i(Const.DEFAULT_TAG, "VPN onRevoke");
  }

  @Override
  public void onDestroy() {
    Log.i(Const.DEFAULT_TAG, "VPN onDestroy");
    unregisterReceiver(airplaneModeOnReceiver);
    vpnBinder.disconnect();
  }

  public void createNotification() {
    NotificationChannel channel = new NotificationChannel(Const.NOTIFICATION_CHANNEL_ID, Const.NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE);
    channel.setDescription("Provides information about the VPN connection state and serves as permanent notification to keep the VPN service running in the background.");
    channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
    channel.setShowBadge(true);
    NotificationManager notificationManager = getSystemService(NotificationManager.class);
    notificationManager.createNotificationChannel(channel);
    NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channel.getId());
    notificationBuilder.setSmallIcon(R.drawable.autofill_inline_suggestion_chip_background)
      .setPriority(NotificationCompat.PRIORITY_MIN)
      .setOngoing(true)
      .setShowWhen(false)
      .setOnlyAlertOnce(true);
    startForeground(Const.NOTIFICATION_ID, notificationBuilder.build());
    NotifyThread notifyThread = new NotifyThread(notificationManager, notificationBuilder, this, ipService);
    notifyThread.start();
  }

  public interface StateListener {
    void updateState(String up, String down, String total);

    void updateStatus();
  }

  public synchronized static void addStateListener(StateListener sl) {
    stateListener = sl;
  }

  public synchronized void updateStateString(String up, String down, String total) {
    if (stateListener != null) {
      stateListener.updateState(up, down, total);
    }
  }

  public synchronized void updateStatus() {
    if (stateListener != null) {
      stateListener.updateStatus();
    }
  }

  public class VpnBinder extends Binder {
    private WebSocket webSocket;
    private FileInputStream in;
    private FileOutputStream out;
    private ParcelFileDescriptor tun;

    public void connect(Promise promise) {
      Log.d(Const.DEFAULT_TAG, "VPN start...");
      ipService = new IpService();
      String err = ipService.addObserveGroup();
      if (!Objects.equals(err, "OK")) {
        promise.reject(Const.DEFAULT_TAG, "VPN Observe Group err:" + err);
        return;
      }
      if (!ipService.health()) {
        promise.reject(Const.DEFAULT_TAG, "VPN connect test failed");
        return;
      }
      // pick ip
      LocalIp localIP = ipService.pickIp();
      if (localIP == null) {
        promise.reject(Const.DEFAULT_TAG, "VPN pick ipv4 failed");
        return;
      }
      Global.LOCAL_IP = localIP.getLocalIp();
      //pick ipv6
      LocalIp localIPv6 = ipService.pickIpv6();
      if (localIPv6 == null) {
        ipService.deleteIp(Global.LOCAL_IP);
        promise.reject(Const.DEFAULT_TAG, "VPN pick ipv6 failed");
        return;
      }
      Global.LOCAL_IPv6 = localIPv6.getLocalIp();

      try {
        // create tun
        tun = createTunnel(localIP, localIPv6);
        if (tun == null) {
          ipService.deleteIp(Global.LOCAL_IP);
          promise.reject(Const.DEFAULT_TAG, "VPN create tun failed");
          return;
        }
        in = new FileInputStream(tun.getFileDescriptor());
        out = new FileOutputStream(tun.getFileDescriptor());
        // create ws client
        webSocket = ipService.connectWebSocket(out);
        if (webSocket == null || !webSocket.isOpen()) {
          ipService.deleteIp(Global.LOCAL_IP);
          Log.i(Const.DEFAULT_TAG, "VPN webSocket is not open");
          in.close();
          out.close();
          promise.reject(Const.DEFAULT_TAG, "VPN webSocket is not open");
        } else {
          Log.i(Const.DEFAULT_TAG, "VPN webSocket is open");
          Global.RUNNING = true;
          VpnThread vpnThread = new VpnThread(in, webSocket, this);
          vpnThread.start();
          createNotification();
          Log.i(Const.DEFAULT_TAG, "VPN started");
        }
      } catch (Exception e) {
        ipService.deleteIp(Global.LOCAL_IP);
        promise.reject(Const.DEFAULT_TAG, "VPN start failed:" + e.toString());
      }
    }

    public void disconnect() {
      if (!Global.RUNNING) {
        return;
      }
      Log.d(Const.DEFAULT_TAG, "VPN stop...");
      Global.RUNNING = false;
      ipService.deleteIp(Global.LOCAL_IP);
      closeTun();
      updateStatus();
      stopForeground(true);
      Log.i(Const.DEFAULT_TAG, "VPN stopped");
    }

    public void closeTun() {
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
    }
  }

  public ParcelFileDescriptor createTunnel(LocalIp localIP, LocalIp localIPv6) throws PackageManager.NameNotFoundException {
    VpnService.Builder builder = new Builder();
    builder.setMtu(Const.MTU)
      .addAddress(localIP.getLocalIp(), localIP.getLocalPrefixLength())
      .addAddress(localIPv6.getLocalIp(), localIPv6.getLocalPrefixLength())
      .addRoute(Const.DEFAULT_ROUTE, 0)
      .addRoute(Const.DEFAULT_ROUTEv6, 0)
      .addDnsServer(Const.DEFAULT_DNS)
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
    } else {
      for (String packageName : Global.vpn_whitelist) {
        builder.addAllowedApplication(packageName);
      }
    }
    Log.i(Const.DEFAULT_TAG, "VPN whitelist apps:" + Global.vpn_whitelist);
    Log.i(Const.DEFAULT_TAG, "VPN blacklist apps:" + Global.vpn_blacklist);
    return builder.establish();
  }

}
