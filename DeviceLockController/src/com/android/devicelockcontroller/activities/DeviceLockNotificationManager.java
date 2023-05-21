/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.devicelockcontroller.activities;

import static com.google.common.base.Preconditions.checkNotNull;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.android.devicelockcontroller.R;
import com.android.devicelockcontroller.storage.SetupParametersClient;
import com.android.devicelockcontroller.util.LogUtil;
import com.android.devicelockcontroller.util.StringUtil;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * A utility class used to send notification.
 */
public final class DeviceLockNotificationManager {

    private static final String TAG = "DeviceLockNotificationManager";

    private static final String PROVISION_NOTIFICATION_CHANNEL_ID = "devicelock-provision";
    private static final String DEVICE_RESET_NOTIFICATION_TAG = "devicelock-device-reset";
    private static final int DEVICE_RESET_NOTIFICATION_ID = 0;

    /**
     * Send the device reset notification. The call is thread safe and can be called from any
     * thread.
     *
     * @param context the context where the notification will be sent out
     * @param days    the number of days the reset will happen
     */
    public static void sendDeviceResetNotification(Context context, int days) {
        // TODO: check/request permission first

        // re-creating the same notification channel is essentially no-op
        createNotificationChannel(context);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        Futures.addCallback(createDeviceResetNotification(context, days),
                new FutureCallback<>() {
                    @Override
                    public void onSuccess(Notification notification) {
                        LogUtil.d(TAG, "send device reset notification");
                        notificationManager.notify(DEVICE_RESET_NOTIFICATION_TAG,
                                DEVICE_RESET_NOTIFICATION_ID, notification);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        LogUtil.e(TAG, "Failed to create device reset notification", t);
                    }
                }, context.getMainExecutor());
    }

    private static ListenableFuture<Notification> createDeviceResetNotification(Context context,
            int days) {
        return Futures.transform(SetupParametersClient.getInstance().getKioskAppProviderName(),
                providerName ->
                        // TODO: update the icon
                        new NotificationCompat.Builder(context, PROVISION_NOTIFICATION_CHANNEL_ID)
                                .setSmallIcon(R.drawable.ic_action_lock)
                                .setContentTitle(StringUtil.getPluralString(context, days,
                                        R.string.device_reset_in_days_notification_title))
                                .setContentText(context.getString(
                                        R.string.device_reset_notification_content,
                                        providerName)).build(),
                context.getMainExecutor());
    }

    private static void createNotificationChannel(Context context) {
        NotificationManager notificationManager = context.getSystemService(
                NotificationManager.class);
        checkNotNull(notificationManager);
        NotificationChannel notificationChannel = new NotificationChannel(
                PROVISION_NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.provision_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(notificationChannel);
    }
}