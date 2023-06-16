package io.favorlabs.favor.thread;

import android.app.NotificationManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.concurrent.TimeUnit;

import io.favorlabs.favor.model.Const;
import io.favorlabs.favor.model.Global;
import io.favorlabs.favor.model.Stats;
import io.favorlabs.favor.service.IpService;
import io.favorlabs.favor.service.MyVpnService;
import io.favorlabs.favor.utils.FormatUtil;

public class NotifyThread extends BaseThread {
    private static final String TAG = "VPNNotifyThread";
    private final NotificationManager notificationManager;
    private final NotificationCompat.Builder builder;

    public NotifyThread(NotificationManager notificationManager, NotificationCompat.Builder builder, MyVpnService vpnService, IpService ipService) {
        this.notificationManager = notificationManager;
        this.builder = builder;
        this.vpnService = vpnService;
        this.ipService = ipService;
    }

    @Override
    public void run() {
        Log.i(TAG, "start");
        int seconds = 0;
        while (Global.RUNNING) {
            try {
                TimeUnit.SECONDS.sleep(1);
                Stats.TOTAL_BYTES.addAndGet(Stats.DOWNLOAD_BYTES.get());

                String up = FormatUtil.formatByte(Stats.UPLOAD_BYTES.get());
                String down = FormatUtil.formatByte(Stats.DOWNLOAD_BYTES.get());
                String total = FormatUtil.formatByte(Stats.TOTAL_BYTES.get());

                String text = String.format("↓ %s ↑ %s", down, up);
                String summary = String.format("Data Usage: %s", total);
                builder.setStyle(new NotificationCompat.BigTextStyle().setSummaryText(summary).setBigContentTitle("").bigText(text));
                notificationManager.notify(Const.NOTIFICATION_ID, builder.build());

                vpnService.updateStateString(up, down, total);

                Stats.UPLOAD_BYTES.set(0);
                Stats.DOWNLOAD_BYTES.set(0);
                seconds++;
                if (seconds % 300 == 0) {
                    ipService.keepAliveIp(Global.LOCAL_IP);
                }
            } catch (InterruptedException e) {
                Log.i(TAG, "error:" + e.getMessage());
            }
        }
        resetData();
        notificationManager.cancel(Const.NOTIFICATION_ID);

        Log.i(TAG, "stop");
    }

    public void resetData() {
        Stats.UPLOAD_BYTES.set(0);
        Stats.DOWNLOAD_BYTES.set(0);
        Stats.TOTAL_BYTES.set(0);
        Log.i(Const.DEFAULT_TAG, "reset data");
    }
}
