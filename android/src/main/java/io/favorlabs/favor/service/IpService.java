package io.favorlabs.favor.service;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.util.Log;

import org.asynchttpclient.ws.WebSocket;

import java.io.FileOutputStream;
import java.net.UnknownHostException;

import io.favorlabs.favor.model.Const;
import io.favorlabs.favor.model.Global;
import io.favorlabs.favor.model.LocalIp;
import io.favorlabs.favor.utils.Ipv6AddressUtil;
import io.favorlabs.favor.ws.MyWebSocketClient;

public class IpService {
  private static final String TAG = "VPNIPService";
  private final String serverIp;
  private final int serverPort;
  private final String key;
  private final boolean https;

  public IpService() {
    this.serverIp = Const.DEFAULT_SERVER_ADDRESS;
    this.serverPort = Global.vpn_port.intValue();
    this.key = Const.DEFAULT_KEY;
    this.https = Global.enable_tls;
  }

  public WebSocket connectWebSocket(FileOutputStream out) throws Exception {
    @SuppressLint("DefaultLocale") String api = String.format("%s://%s:%d%s", https ? "wss" : "ws", serverIp, serverPort, Const.DEFAULT_PATH);
    return MyWebSocketClient.connectWebSocket(api, key, out);
  }

  public String addObserveGroup() {
    StringBuilder nodes = new StringBuilder();
    for (String v : Global.vpn_group_nods) {
      if (nodes.toString().equals("")) {
        nodes = new StringBuilder(v);
      } else {
        nodes.append(",").append(v);
      }
    }
    @SuppressLint("DefaultLocale") String api = String.format("%s://%s:%d/observe/add/group?nodes=%s", https ? "https" : "http", serverIp, serverPort, nodes);
    String resp = MyWebSocketClient.httpGet(api, key);
    Log.i(TAG, String.format("get api:%s resp:%s", api, resp.toString()));
    return resp;
  }

  public void delObserveGroup() {
    @SuppressLint("DefaultLocale") String api = String.format("%s://%s:%d/observe/delete/group", https ? "https" : "http", serverIp, serverPort);
    String resp = MyWebSocketClient.httpGet(api, key);
    Log.i(TAG, String.format("get api:%s resp:%s", api, resp.toString()));
  }

  public boolean health() {
    @SuppressLint("DefaultLocale") String api = String.format("%s://%s:%d/test", https ? "https" : "http", serverIp, serverPort);
    String resp = MyWebSocketClient.httpGet(api, key);
    Log.i(TAG, String.format("get api:%s resp:%s", api, resp.toString()));
    return TextUtils.equals(resp, "OK");
  }

  public LocalIp pickIp() {
    @SuppressLint("DefaultLocale") String api = String.format("%s://%s:%d/register/pick/ip", https ? "https" : "http", serverIp, serverPort);
    String resp = MyWebSocketClient.httpGet(api, key);
    Log.i(TAG, String.format("get api:%s resp:%s", api, resp));
    if (TextUtils.isEmpty(resp)) {
      return null;
    }
    String[] ip = resp.split("/");
    if (ip.length == 2) {
      return new LocalIp(ip[0], Integer.parseInt(ip[1]));
    }
    return null;
  }

  public LocalIp pickIpv6() {
    @SuppressLint("DefaultLocale") String api = String.format("%s://%s:%d/register/prefix/ipv6", https ? "https" : "http", serverIp, serverPort);
    String resp = MyWebSocketClient.httpGet(api, key);
    Log.i(TAG, String.format("get api:%s resp:%s", api, resp));
    if (TextUtils.isEmpty(resp)) {
      return null;
    }
    String[] ip = resp.split("/");
    if (ip.length == 2) {
      String ipv6Net = ip[0];
      int prefixLength = Integer.parseInt(ip[1]);
      String randomIpv6Address = null;
      try {
        randomIpv6Address = Ipv6AddressUtil.randomString(ipv6Net, prefixLength);
      } catch (UnknownHostException e) {
        e.printStackTrace();
      }
      if (randomIpv6Address != null) {
        return new LocalIp(randomIpv6Address, prefixLength);
      }
    }
    return null;
  }

  public void keepAliveIp(String ip) {
    @SuppressLint("DefaultLocale") String api = String.format("%s://%s:%d/register/keepalive/ip?ip=%s", https ? "https" : "http", serverIp, serverPort, ip);
    String resp = MyWebSocketClient.httpGet(api, key);
    Log.i(TAG, String.format("get api:%s resp:%s", api, resp));
  }

  public void deleteIp(String ip) {
    @SuppressLint("DefaultLocale") String api = String.format("%s://%s:%d/register/delete/ip?ip=%s", https ? "https" : "http", serverIp, serverPort, ip);
    String resp = MyWebSocketClient.httpGet(api, key);
    Log.i(TAG, String.format("get api:%s resp:%s", api, resp));
    delObserveGroup();
  }
}
