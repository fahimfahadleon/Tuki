package com.horoftech.tuki;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.RemoteMessage;

public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        getSharedPreferences("_", MODE_PRIVATE).edit().putString("token", token).apply();
    }
    public static String getToken(Context context) {
        return context.getSharedPreferences("_", MODE_PRIVATE).getString("token", "empty");
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {

        Log.e("message",message.getData().toString());
        showNotification("title", message.getData().toString());
    }

    private RemoteViews getCustomDesign(String title, String message) {
        RemoteViews remoteViews = new RemoteViews(getApplicationContext().getPackageName(), R.layout.notification);
        remoteViews.setTextViewText(R.id.title, title);
        remoteViews.setTextViewText(R.id.content, message);
        remoteViews.setImageViewResource(R.id.icon, R.drawable.heartpink);
        return remoteViews;
    }

    // Method to display the notifications
    public void showNotification(String title, String message) {

        Intent intent = new Intent(this, MainActivity.class);

        String channel_id = getResources().getString(R.string.default_notification_channel_id);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent
                = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT|PendingIntent.FLAG_MUTABLE);

        NotificationCompat.Builder builder
                = new NotificationCompat
                .Builder(getApplicationContext(),
                channel_id)
                .setSmallIcon(R.drawable.heartpink)
                .setAutoCancel(true)
                .setVibrate(new long[] { 1000, 1000, 1000,
                        1000, 1000 })
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent);


        builder = builder.setContent(
                getCustomDesign(title, message));

        NotificationManager notificationManager
                = (NotificationManager)getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel
                    = new NotificationChannel(
                    channel_id, "web_app",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(
                    notificationChannel);
        }

        notificationManager.notify(0, builder.build());
    }
}
