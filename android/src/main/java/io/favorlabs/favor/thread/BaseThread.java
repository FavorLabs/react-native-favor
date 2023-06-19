package io.favorlabs.favor.thread;

import android.app.NotificationManager;

import androidx.core.app.NotificationCompat;

import io.favorlabs.favor.service.IpService;
import io.favorlabs.favor.service.MyVpnService;

public class BaseThread extends Thread {
    protected MyVpnService vpnService;
    protected IpService ipService;
}
