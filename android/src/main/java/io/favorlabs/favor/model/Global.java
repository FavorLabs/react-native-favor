package io.favorlabs.favor.model;

import java.util.ArrayList;
import java.util.List;

import mobile.Node;

public class Global {
    public static volatile boolean RUNNING;
    public static volatile String LOCAL_IP;
    public static volatile String LOCAL_IPv6;

    // node
    public static volatile Node NODE;
    public static volatile boolean enable_tls;
    public static volatile boolean enable_vpn;

    // vpn
    public static volatile String vpn_group;
    public static volatile List<String> vpn_group_nods = new ArrayList<>();
    public static volatile List<String> vpn_whitelist = new ArrayList<>();
    public static volatile List<String> vpn_blacklist = new ArrayList<>();
    public static volatile Long vpn_port;
}
