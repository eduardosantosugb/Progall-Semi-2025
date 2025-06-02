package com.ugb.cuadrasmart;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    public static final String CHANNEL_ID = "CuadraSmartChatChannel";

    /**
     * Llamado cuando se recibe un mensaje.
     * @param remoteMessage Objeto que representa el mensaje recibido de FCM.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        // Este método se llama cuando la app está en primer plano.
        // Si la app está en segundo plano, FCM maneja la notificación automáticamente
        // si el mensaje tiene un payload "notification".
        // Si solo tiene un payload "data", onMessageReceived se llama también en segundo plano.

        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Verificar si el mensaje contiene un payload de datos.
        // Usaremos el payload de datos para enviar información personalizada para la notificación.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            Map<String, String> data = remoteMessage.getData();
            String title = data.get("title");
            String body = data.get("body");
            String chatId = data.get("chatId"); // ID del chat para abrir
            String recipientUid = data.get("senderId"); // Quien envió el mensaje (será el recipient en la app)
            String recipientName = data.get("senderName"); // Nombre de quien envió

            // Aquí podrías verificar si el usuario ya está en la pantalla de chat activa
            // para ese chatId y decidir no mostrar la notificación.
            // Por ahora, siempre la mostraremos si la app está en primer plano.

            sendNotification(title, body, chatId, recipientUid, recipientName);
        }

        // Verificar si el mensaje contiene un payload de notificación.
        // (Esto es manejado por el sistema si la app está en segundo plano/cerrada)
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            // Si quieres manejar esto incluso en primer plano, puedes hacerlo aquí.
            // Pero usualmente el payload de datos es más flexible.
        }
    }

    /**
     * Llamado si el token de instancia de FCM se actualiza.
     * Este puede ser el caso en la primera ejecución de la app donde se genera el token,
     * o cuando el usuario reinstala la app, borra datos, o el token expira.
     * @param token El nuevo token.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        sendRegistrationToServer(token);
    }

    /**
     * Persiste el token FCM en Firestore para el usuario actual.
     * @param token El nuevo token FCM.
     */
    private void sendRegistrationToServer(String token) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            String userId = firebaseUser.getUid();
            FirebaseFirestore.getInstance().collection("users").document(userId)
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid -> Log.i(TAG, "FCM Token actualizado en Firestore para UID: " + userId))
                    .addOnFailureListener(e -> Log.w(TAG, "Error actualizando FCM Token en Firestore para UID: " + userId, e));
        } else {
            Log.w(TAG, "No hay usuario logueado, no se puede actualizar FCM token en servidor.");
        }
    }

    /**
     * Crea y muestra una notificación simple.
     * @param title Título del mensaje.
     * @param messageBody Cuerpo del mensaje.
     * @param chatId ID del chat para abrir al tocar la notificación.
     * @param recipientUid UID del otro participante del chat (quien envió el mensaje).
     * @param recipientName Nombre del otro participante del chat.
     */
    private void sendNotification(String title, String messageBody, String chatId, String recipientUid, String recipientName) {
        Intent intent;
        // Si tenemos la info del chat, abrimos ChatPrivadoActivity
        if (chatId != null && recipientUid != null && recipientName != null) {
            intent = new Intent(this, ChatPrivadoActivity.class);
            intent.putExtra("chatId", chatId); // Pasar chatId para posible uso futuro
            intent.putExtra("recipient_uid", recipientUid);
            intent.putExtra("recipient_name", recipientName);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Para que no se apilen actividades
        } else {
            // Fallback: abrir LoginActivity o la actividad principal
            intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.icon_app) // Usa tu ícono de app
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH); // Para notificaciones heads-up

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Desde Android Oreo (API 26), se requiere un canal de notificación.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Notificaciones de Chat CuadraSmart",
                    NotificationManager.IMPORTANCE_HIGH); // IMPORTANCE_HIGH para heads-up
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0 /* ID de la notificación. Usa uno único si quieres mostrar múltiples */,
                notificationBuilder.build());
        Log.d(TAG, "Notificación enviada: " + title + " - " + messageBody);
    }
}