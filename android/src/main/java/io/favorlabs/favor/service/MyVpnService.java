package io.favorlabs.favor.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import io.favorlabs.favor.R;
import io.favorlabs.favor.model.Config;
import io.favorlabs.favor.model.Const;
import io.favorlabs.favor.model.Global;
import io.favorlabs.favor.thread.NotifyThread;
import io.favorlabs.favor.thread.VpnThread;

public class MyVpnService extends VpnService {
  private static StateListener stateListener;
  public static final String CONNECT_ACTION = "io.favorlabs.favor.service.CONNECT_ACTION";
  public static final String DISCONNECT_ACTION = "io.favorlabs.favor.service.DISCONNECT_ACTION";
  private Config config;
  private NotificationManager notificationManager;
  private NotificationCompat.Builder notificationBuilder;
  private IpService ipService;
  private final IntentFilter filter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
  private final BroadcastReceiver airplaneModeOnReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
        if (Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0) {
          stopVpn();
        }
      }
    }
  };

  @Override
  public void onCreate() {
    registerReceiver(airplaneModeOnReceiver, filter);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent == null) {
      return START_NOT_STICKY;
    }
    switch (intent.getAction()) {
      case CONNECT_ACTION:
        Log.i(Const.DEFAULT_TAG, "----cmd start");
        // init config
        initConfig();
        // create notification
        createNotification();
        // start VPN
        startVpn();
        return START_STICKY;
      case DISCONNECT_ACTION:
        Log.i(Const.DEFAULT_TAG, "----cmd stop");
        // stop VPN
        stopVpn();
        return START_NOT_STICKY;
      default:
        return START_NOT_STICKY;
    }
  }


  @Override
  public void onDestroy() {
    unregisterReceiver(airplaneModeOnReceiver);
    stopVpn();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  public void initConfig() {
    String server = Const.DEFAULT_SERVER_ADDRESS;
    String path = Const.DEFAULT_PATH;
    String dns = Const.DEFAULT_DNS;
    String key = Const.DEFAULT_KEY;
    this.config = new Config(server, path, dns, key);
    this.ipService = new IpService(config.getServerAddress(), config.getServerPort(), config.getKey());
    Log.i(Const.DEFAULT_TAG, config.toString());
  }

  public void createNotification() {
    NotificationChannel channel = new NotificationChannel(Const.NOTIFICATION_CHANNEL_ID, Const.NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE);
    channel.setDescription("Provides information about the VPN connection state and serves as permanent notification to keep the VPN service running in the background.");
    channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
    channel.setShowBadge(false);
    notificationManager = getSystemService(NotificationManager.class);
    notificationManager.createNotificationChannel(channel);
    notificationBuilder = new NotificationCompat.Builder(this, channel.getId());
    notificationBuilder.setSmallIcon(R.drawable.autofill_inline_suggestion_chip_background);
  }

  public void startVpn() {
    try {
      Global.RUNNING = true;
      this.startForeground(Const.NOTIFICATION_ID, notificationBuilder.build());

      VpnThread vpnThread = new VpnThread(config, this, ipService, notificationManager, notificationBuilder);
      vpnThread.start();

      // start notify threads
      NotifyThread notifyThread = new NotifyThread(notificationManager, notificationBuilder, this, ipService);
      notifyThread.start();

      Log.i(Const.DEFAULT_TAG, "VPN started");
    } catch (Exception e) {
      Global.RUNNING = false;
      Log.e(Const.DEFAULT_TAG, "error on startVPN:" + e.toString());
    }
  }

  public void stopVpn() {
    this.stopForeground(true);
    resetGlobalVar();
    updateStatus();
    Log.i(Const.DEFAULT_TAG, "VPN stopped");
  }

  private void resetGlobalVar() {
    Global.CONNECTED = false;
    Global.RUNNING = false;
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

}
