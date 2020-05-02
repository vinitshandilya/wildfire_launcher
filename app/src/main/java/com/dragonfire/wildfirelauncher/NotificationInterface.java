package com.dragonfire.wildfirelauncher;

import android.os.Bundle;

public interface NotificationInterface {
    void onNotificationAdded(Bundle bundle);
    void onNotificationRemoved(Bundle bundle);
}
