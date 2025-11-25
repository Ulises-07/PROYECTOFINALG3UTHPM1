package com.example.proyectofinalg3uthpm1;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class ServicioDeMensajeria extends FirebaseMessagingService {

    private static final String TAG = "ServicioMensajeria";
    private static final String CANAL_ID = "canal_notificaciones_fcm";


    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "Recibido de: " + remoteMessage.getFrom());

        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Cuerpo de la Notificación: " + remoteMessage.getNotification().getBody());
            mostrarNotificacion(remoteMessage.getNotification().getTitle(), remoteMessage.getNotification().getBody());
        }
    }


    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Nuevo token de FCM: " + token);

        enviarTokenAFirestore(token);
    }


    private void enviarTokenAFirestore(String token) {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        if (usuario != null) {
            FirebaseFirestore.getInstance()
                    .collection("Usuarios")
                    .document(usuario.getUid())
                    .update("tokenDispositivo", token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Token actualizado en Firestore."))
                    .addOnFailureListener(e -> Log.w(TAG, "Error al actualizar token.", e));
        }
    }

    private void mostrarNotificacion(String titulo, String cuerpo) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CANAL_ID)
                        .setSmallIcon(R.drawable.ic_profile_placeholder)
                        .setContentTitle(titulo)
                        .setContentText(cuerpo)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CANAL_ID,
                    "Notificaciones Generales",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0 /* ID de la notificación */, notificationBuilder.build());
    }
}