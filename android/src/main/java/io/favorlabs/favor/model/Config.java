package io.favorlabs.favor.model;


import java.io.Serializable;

public class Config implements Serializable {
  private String serverAddress;
  private int serverPort;
  private String path;
  private String dns;
  private String key;
  private String proto;
  private String bypassApps;

  public Config(String serverAddress, String path, String dns, String key) {
    this.serverAddress = serverAddress;
    this.serverPort = Global.vpn_port.intValue();
    this.path = path;
    this.dns = dns;
    this.key = key;
    this.proto = Global.enable_tls ? "wss" : "ws";
  }

  public String getServerAddress() {
    return serverAddress;
  }

  public void setServerAddress(String serverAddress) {
    this.serverAddress = serverAddress;
  }

  public int getServerPort() {
    return serverPort;
  }

  public void setServerPort(int serverPort) {
    this.serverPort = serverPort;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getDns() {
    return dns;
  }

  public void setDns(String dns) {
    this.dns = dns;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getProto() {
    return proto;
  }

  public void setProto(String proto) {
    this.proto = proto;
  }

  public String getBypassApps() {
    return bypassApps;
  }

  public void setBypassApps(String bypassApps) {
    this.bypassApps = bypassApps;
  }

  @Override
  public String toString() {
    return "Config{" +
      "serverAddress='" + serverAddress + '\'' +
      ", serverPort=" + serverPort +
      ", path='" + path + '\'' +
      ", dns='" + dns + '\'' +
      ", key='" + key + '\'' +
      ", proto='" + proto + '\'' +
      ", bypassApps='" + bypassApps + '\'' +
      '}';
  }
}
