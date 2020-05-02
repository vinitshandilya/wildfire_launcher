package com.dragonfire.wildfirelauncher;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class MyNotificationListenerService extends NotificationListenerService {
    static NotificationInterface notificationInterface;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Notification mNotification=sbn.getNotification();
        if (mNotification!=null){
            Bundle extras = mNotification.extras;
            extras.putString("PACKAGE_NAME", sbn.getPackageName());
            extras.putString("TICKER_TEXT", sbn.getNotification().tickerText + "");
            notificationInterface.onNotificationAdded(extras);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Notification mNotification=sbn.getNotification();
        if (mNotification!=null){
            Bundle extras = mNotification.extras;
            extras.putString("PACKAGE_NAME", sbn.getPackageName());
            extras.putString("TICKER_TEXT", sbn.getNotification().tickerText + "");
            notificationInterface.onNotificationRemoved(extras);
        }
    }

    public void setListener (NotificationInterface notificationInterface) {
        MyNotificationListenerService.notificationInterface = notificationInterface;
    }
}
