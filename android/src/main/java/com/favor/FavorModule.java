package io.favorlabs.favor;

import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.bridge.ReadableMap;

import java.util.Objects;

import mobile.Mobile;
import mobile.Node;
import mobile.Options;

@ReactModule(name = FavorModule.NAME)
public class FavorModule extends ReactContextBaseJavaModule {
  public static final String NAME = "Favor";
  private static volatile Node node = null;
  private final ReactApplicationContext reactContext;

  public FavorModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
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

    try {
      node = Mobile.newNode(options);
      promise.resolve(null);
    } catch (Exception e) {
      e.printStackTrace();
      Log.e(NAME, "start node " + e.toString());
      promise.reject(e);
    }
  }

  @ReactMethod
  public void stop(Promise promise) {
    try {
      if (node != null) {
          node.stop();
          node = null;
      }
      promise.resolve(null);
    } catch (Exception e) {
      Log.e(NAME, "stop node " + e.toString());
      promise.reject(e);
    }
  }
}
