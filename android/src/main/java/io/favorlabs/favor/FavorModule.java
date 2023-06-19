package io.favorlabs.favor;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.VpnService;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.Objects;

import io.favorlabs.favor.model.Global;
import io.favorlabs.favor.service.MyVpnService;
import mobile.Mobile;
import mobile.Options;

@ReactModule(name = FavorModule.NAME)
public class FavorModule extends ReactContextBaseJavaModule implements MyVpnService.StateListener {
  public static final String NAME = "Favor";
  private final ReactApplicationContext reactContext;
  private MyVpnService.VpnBinder vpnBinder;
  private static final int START_VPN_PROFILE = 70;
  private Promise vpnPromise;
  private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
      if (requestCode == START_VPN_PROFILE) {
        if (vpnPromise != null) {
          if (resultCode == RESULT_OK) {
            Log.i(NAME, "onActivityResult: Prepare VPN ok");
            vpnBinder.connect(vpnPromise);
          } else {
            vpnPromise.reject(NAME, "onActivityResult: Prepare VPN failed");
            vpnPromise = null;
          }
        }
      }
    }
  };

  private ServiceConnection mConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
      vpnBinder = (MyVpnService.VpnBinder) service;
      Log.d(NAME, "VPN onServiceConnected");
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
      Log.d(NAME, "VPN onServiceDisconnected");
    }
  };

  private void prepareVpn(final Promise promise) {
    Log.d(NAME,"VPN prepareVpn");
    Activity currentActivity = getCurrentActivity();

    if (currentActivity == null) {
      promise.reject(NAME, "Activity doesn't exist");
      return;
    }

    vpnPromise = promise;
    Intent intent = VpnService.prepare(currentActivity);

    if (intent != null) {
      currentActivity.startActivityForResult(intent, START_VPN_PROFILE);
      return;
    }

    vpnBinder.connect(promise);
  }

  public FavorModule(ReactApplicationContext context) {
    super(context);
    reactContext = context;
    reactContext.addActivityEventListener(mActivityEventListener);
    MyVpnService.addStateListener(this);

    Intent intent = new Intent(context, MyVpnService.class);
    context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
  }

  void sendEvent(@Nullable WritableMap params) {
    reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("stateChanged", params);
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  @ReactMethod
  public void version(Promise promise) {
    promise.resolve(Mobile.version());
  }

  @ReactMethod
  public void start(ReadableMap params, Promise promise) {
    Options options = new Options();

    // api setting
    options.setApiPort(params.getInt("api-port"));
    options.setDebugAPIPort(params.getInt("debug-api-port"));
    options.setWebsocketPort(params.getInt("ws-port"));
    options.setEnableDebugAPI(params.getBoolean("debug-api-enable"));

    // proxy setting
    options.setProxyEnable(params.getBoolean("proxy-enable"));
    options.setProxyGroupName(params.getString("proxy-group"));
    options.setProxyPort(params.getInt("proxy-port"));

    // vpn setting
    options.setVpnEnable(params.getBoolean("vpn-enable"));
    options.setVpnPort(params.getInt("vpn-port"));

    // group setting
    options.setGroup(Objects.requireNonNull(params.getArray("groups")).toString());

    // p2p setup
    options.setNetworkID(params.getInt("network-id"));
    options.setP2PPort(params.getInt("p2p-port"));
    options.setWelcomeMessage(params.getString("welcome-message"));

    // kademlia
    options.setBinMaxPeers(params.getInt("bin-max-peers"));
    options.setLightMaxPeers(params.getInt("light-max-peers"));

    // cache size
    options.setCacheCapacity(params.getInt("cache-capacity"));

    // node bootstrap
    options.setBootNodes(params.getString("boot-nodes"));
    options.setEnableDevNode(params.getBoolean("dev-mode"));
    options.setEnableFullNode(params.getBoolean("full-node"));

    // chain setting
    options.setChainEndpoint(params.getString("chain-endpoint"));
    options.setOracleContract(params.getString("oracle-contract-addr"));

    // traffic stat
    options.setEnableFlowStat(params.getBoolean("traffic"));
    options.setFlowContract(params.getString("traffic-contract-addr"));

    // security
    options.setPassword(params.getString("password"));
    options.setDataPath(reactContext.getExternalFilesDir(null).getPath());

    // misc
    options.setVerbosity(params.getString("verbosity"));
    options.setEnableTLS(params.getBoolean("enable-tls"));

    Global.enable_tls = options.getEnableTLS();
    Global.enable_vpn = options.getVpnEnable();
    Global.vpn_port = options.getVpnPort();

    try {
      Global.NODE = Mobile.newNode(options);
    } catch (Exception e) {
      Log.e(NAME, "Start node failed:" + e);
      promise.reject(NAME, "Start node failed:" + e);
      return;
    }
    promise.resolve(null);
    Log.i(NAME, "start node successful");
  }

  @ReactMethod
  public void stop(Promise promise) {
    try {
      if (Global.NODE != null) {
        stopVPN();
        Global.NODE.stop();
        Global.NODE = null;
        Log.i(NAME, "stop node successful");
      } else {
        Log.w(NAME, "stop node, node not running");
      }
    } catch (Exception e) {
      Log.e(NAME, "Stop node failed:" + e);
      promise.reject(NAME, "Stop node failed:" + e);
      return;
    }
    promise.resolve(null);
  }

  @ReactMethod
  public void startVPN(ReadableMap options, Promise promise) {
    if (Global.NODE == null) {
      promise.reject(NAME, "Please start FavorX node first");
      return;
    }
    if (Global.RUNNING) {
      Log.d(NAME, "vpn already running");
      return;
    }
    ReadableArray nodes = options.getArray("nodes");
    if (nodes == null) {
      promise.reject(NAME, "The nodes of the vpn group are empty");
      return;
    }
    for (int i = 0; i < nodes.size(); ++i) {
      Global.vpn_group_nods.add(nodes.getString(i));
    }

    ReadableArray whitelist = options.getArray("whitelist");
    ReadableArray blacklist = options.getArray("blacklist");
    if (whitelist == null && blacklist == null) {
      promise.reject(NAME, "whitelist and blacklist are empty");
      return;
    }
    if (whitelist != null && blacklist != null) {
      promise.reject(NAME, "whitelist, blacklist and only one of them can be used at the same time");
      return;
    }
    if (whitelist != null) {
      for (int i = 0; i < whitelist.size(); ++i) {
        Global.vpn_whitelist.add(whitelist.getString(i));
      }
    }
    if (blacklist != null) {
      for (int i = 0; i < blacklist.size(); ++i) {
        Global.vpn_blacklist.add(blacklist.getString(i));
      }
    }

    Global.vpn_group = options.getString("group");

    prepareVpn(promise);
  }

  @ReactMethod
  public void stopVPN() {
    if (!Global.RUNNING) {
      Log.d(NAME, "VPN not running");
      return;
    }
    vpnBinder.disconnect();
  }

  @Override
  public void updateState(String up, String down, String total) {
    WritableMap params = Arguments.createMap();
    params.putString("up", up);
    params.putString("down", down);
    params.putString("total", total);
    params.putBoolean("running", Global.RUNNING);
    sendEvent(params);
  }
  @Override
  public void updateStatus() {
    WritableMap params = Arguments.createMap();
    params.putString("up", "");
    params.putString("down", "");
    params.putString("total", "");
    params.putBoolean("running", Global.RUNNING);
    sendEvent(params);
  }

}
