package com.ugb.cuadrasmart;

import android.database.Cursor;
// Importar Toolbar
import androidx.appcompat.widget.Toolbar;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat; // Para formatear el timestamp
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem; // Para manejar el botón de atrás
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
// Quitar LinearLayout si no se usa directamente
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Calendar; // Para el timestamp
import java.util.Locale;  // Para el timestamp

public class ChatPrivadoActivity extends AppCompatActivity {

    private static final String TAG = "ChatPrivadoActivity";

    private RecyclerView rvChat;
    private EditText etMensajeChat;
    private Button btnEnviarChat;
    private ImageButton btnAdjuntarChat; // Mantener por si se implementa luego
    private Toolbar toolbarChat;

    private DatabaseHelper dbHelper;
    private ChatAdapter chatAdapter;
    private ArrayList<ChatMessage> chatMessages;

    // Estos identificadores pueden ser dinámicos en una implementación real
    // Por ahora, mantenemos la lógica original para estos
    private String currentUser = "supervisor1@example.com"; // CAMBIAR O HACER DINÁMICO
    private String chatRecipient = "supervisor2@example.com"; // CAMBIAR O HACER DINÁMICO

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_privado);

        toolbarChat = findViewById(R.id.toolbarChat);
        setSupportActionBar(toolbarChat);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Mostrar botón de atrás
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            // Podrías poner el nombre del destinatario en el título
            // getSupportActionBar().setTitle(chatRecipient);
        }


        rvChat = findViewById(R.id.rvChat);
        etMensajeChat = findViewById(R.id.etMensajeChat);
        btnEnviarChat = findViewById(R.id.btnEnviarChat);
        btnAdjuntarChat = findViewById(R.id.btnAdjuntarChat);

        dbHelper = new DatabaseHelper(this);
        chatMessages = new ArrayList<>();

        // Configurar RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Para que los mensajes nuevos aparezcan abajo y se haga scroll
        rvChat.setLayoutManager(layoutManager);

        chatAdapter = new ChatAdapter(chatMessages, currentUser); // Pasar currentUser al adaptador
        rvChat.setAdapter(chatAdapter);

        // Cargar mensajes existentes
        loadChatMessages();

        btnEnviarChat.setOnClickListener(view -> {
            String messageContent = etMensajeChat.getText().toString().trim();
            if (!TextUtils.isEmpty(messageContent)) {
                sendMessage(messageContent);
                etMensajeChat.setText(""); // Limpiar campo de texto
            } else {
                Toast.makeText(ChatPrivadoActivity.this, "El mensaje no puede estar vacío", Toast.LENGTH_SHORT).show();
            }
        });

        btnAdjuntarChat.setOnClickListener(view -> {
            Toast.makeText(ChatPrivadoActivity.this, "Funcionalidad de adjuntar no implementada aún.", Toast.LENGTH_SHORT).show();
            // Implementa la lógica para adjuntar archivos (imágenes, audio, etc.)
        });
    }

    // Manejar el clic en el botón de atrás de la Toolbar
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Cierra la actividad actual
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadChatMessages() {
        chatMessages.clear();
        // Asumiendo que dbHelper.getChatMessages devuelve los mensajes entre currentUser y chatRecipient
        Cursor cursor = dbHelper.getChatMessages(currentUser, chatRecipient);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    do {
                        // Asegúrate que los nombres de columna coincidan con DatabaseContract.ChatMessageEntry
                        String sender = safeGetString(cursor, DatabaseContract.ChatMessageEntry.COLUMN_SENDER);
                        String receiver = safeGetString(cursor, DatabaseContract.ChatMessageEntry.COLUMN_RECEIVER);
                        String content = safeGetString(cursor, DatabaseContract.ChatMessageEntry.COLUMN_CONTENT);
                        String timestampStr = safeGetString(cursor, DatabaseContract.ChatMessageEntry.COLUMN_TIMESTAMP);
                        String messageType = safeGetString(cursor, DatabaseContract.ChatMessageEntry.COLUMN_MESSAGE_TYPE, "text"); // Default a text
                        String uri = safeGetString(cursor, DatabaseContract.ChatMessageEntry.COLUMN_URI, "");
                        String status = safeGetString(cursor, DatabaseContract.ChatMessageEntry.COLUMN_STATUS, "sent"); // Default a sent

                        long timestamp = 0;
                        try {
                            timestamp = Long.parseLong(timestampStr);
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "Timestamp inválido en DB: " + timestampStr);
                            timestamp = System.currentTimeMillis(); // Fallback
                        }

                        ChatMessage chatMessage = new ChatMessage(sender, receiver, content, messageType, timestamp, uri, status);
                        chatMessages.add(chatMessage);
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error cargando mensajes del cursor", e);
            } finally {
                cursor.close();
            }
        }
        chatAdapter.notifyDataSetChanged();
        scrollToBottom();
        Log.d(TAG, "Mensajes cargados: " + chatMessages.size());
    }

    private void sendMessage(String content) {
        long timestamp = System.currentTimeMillis();
        // Se asume que el tipo es "text" y el estado inicial es "sent"
        boolean inserted = dbHelper.insertChatMessage(currentUser, chatRecipient, content, "text", String.valueOf(timestamp), "", "sent");

        if (inserted) {
            ChatMessage newMessage = new ChatMessage(currentUser, chatRecipient, content, "text", timestamp, "", "sent");
            chatMessages.add(newMessage);
            chatAdapter.notifyItemInserted(chatMessages.size() - 1);
            scrollToBottom();
        } else {
            Toast.makeText(this, "Error al enviar mensaje", Toast.LENGTH_SHORT).show();
        }
    }

    private void scrollToBottom() {
        if (chatAdapter.getItemCount() > 0) {
            rvChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1);
        }
    }

    // Modelo para mensajes de chat (actualizado para usar long para timestamp)
    public static class ChatMessage {
        public String sender;
        public String receiver;
        public String content;
        public String messageType; // "text", "image", "audio"
        public long timestamp;     // Usar long para el timestamp
        public String uri;         // Para URI de imagen/audio
        public String status;      // "sent", "delivered", "read"

        public ChatMessage(String sender, String receiver, String content, String messageType, long timestamp, String uri, String status) {
            this.sender = sender;
            this.receiver = receiver;
            this.content = content;
            this.messageType = messageType;
            this.timestamp = timestamp;
            this.uri = uri;
            this.status = status;
        }
    }

    // Adaptador para el RecyclerView del chat
    public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int VIEW_TYPE_SENT = 1;
        private static final int VIEW_TYPE_RECEIVED = 2; // Necesitarás un layout para esto también

        private ArrayList<ChatMessage> messages;
        private String currentUserId;

        public ChatAdapter(ArrayList<ChatMessage> messages, String currentUserId) {
            this.messages = messages;
            this.currentUserId = currentUserId;
        }

        @Override
        public int getItemViewType(int position) {
            ChatMessage message = messages.get(position);
            if (message.sender.equals(currentUserId)) {
                return VIEW_TYPE_SENT;
            } else {
                return VIEW_TYPE_RECEIVED;
                // Deberías tener un layout y ViewHolder para VIEW_TYPE_RECEIVED
                // Por ahora, podría reusar el de SENT o uno simple si no tienes el layout de recibido.
                // return VIEW_TYPE_SENT; // TEMPORALMENTE REUSANDO SENT HASTA TENER item_chat_message_received.xml
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == VIEW_TYPE_SENT) {
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message_sent, parent, false);
                return new SentMessageViewHolder(view);
            } else { // VIEW_TYPE_RECEIVED
                // TODO: Inflar R.layout.item_chat_message_received y crear ReceivedMessageViewHolder
                // Por ahora, como fallback, usamos el de enviado (esto se verá mal, pero evita crash)
                // Deberías crear item_chat_message_received.xml (similar a sent, pero alineado a la izquierda y con otro color de burbuja)
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message_sent, parent, false); // CAMBIAR A LAYOUT DE RECIBIDO
                Log.w(TAG, "onCreateViewHolder: Usando layout de mensaje enviado como fallback para mensaje recibido. Crear item_chat_message_received.xml");
                return new SentMessageViewHolder(view); // CAMBIAR A VIEWHOLDER DE RECIBIDO
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ChatMessage message = messages.get(position);
            if (holder.getItemViewType() == VIEW_TYPE_SENT) {
                ((SentMessageViewHolder) holder).bind(message);
            } else { // VIEW_TYPE_RECEIVED
                // TODO: castear a ReceivedMessageViewHolder y llamar a su método bind
                // ((ReceivedMessageViewHolder) holder).bind(message);
                ((SentMessageViewHolder) holder).bind(message); // TEMPORAL
            }
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        // ViewHolder para mensajes enviados
        public class SentMessageViewHolder extends RecyclerView.ViewHolder {
            TextView tvMessageContent, tvMessageTimestamp;

            public SentMessageViewHolder(@NonNull View itemView) {
                super(itemView);
                tvMessageContent = itemView.findViewById(R.id.tvMessageContent);
                tvMessageTimestamp = itemView.findViewById(R.id.tvMessageTimestamp);
            }

            void bind(ChatMessage message) {
                tvMessageContent.setText(message.content);
                tvMessageTimestamp.setText(formatTimestamp(message.timestamp));
            }
        }

        // TODO: Crear ViewHolder para mensajes recibidos (ReceivedMessageViewHolder)
        // public class ReceivedMessageViewHolder extends RecyclerView.ViewHolder { ... }

    }

    // Formatear el timestamp a una cadena legible (ej. "10:30 AM")
    private String formatTimestamp(long timestamp) {
        Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeInMillis(timestamp);
        // Puedes usar "hh:mm a" para formato 12h con AM/PM
        // o "HH:mm" para formato 24h
        return DateFormat.format("hh:mm a", cal).toString();
    }


    // Método auxiliar para obtener valores de forma segura
    private String safeGetString(Cursor cursor, String columnName) {
        return safeGetString(cursor, columnName, ""); // Llama a la versión con valor por defecto
    }

    private String safeGetString(Cursor cursor, String columnName, String defaultValue) {
        if (cursor == null || cursor.isClosed()) {
            Log.w(TAG, "safeGetString: Cursor es null o está cerrado para columna " + columnName);
            return defaultValue;
        }
        try {
            int index = cursor.getColumnIndexOrThrow(columnName);
            if (cursor.isNull(index)) {
                return defaultValue;
            }
            return cursor.getString(index);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "safeGetString: Columna no encontrada en cursor: " + columnName, e);
            return defaultValue;
        }
    }
}