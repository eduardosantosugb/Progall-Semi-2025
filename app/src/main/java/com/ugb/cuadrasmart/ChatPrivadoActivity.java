package com.ugb.cuadrasmart;

import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

import com.ugb.cuadrasmart.R;


public class ChatPrivadoActivity extends AppCompatActivity {

    private RecyclerView rvChat;
    private EditText etMensajeChat;
    private Button btnEnviarChat;
    private ImageButton btnAdjuntarChat;
    private DatabaseHelper dbHelper;
    private ChatAdapter chatAdapter;
    private ArrayList<ChatMessage> chatMessages;
    // Estos identificadores pueden ser dinámicos en una implementación real
    private String currentUser = "supervisor1@example.com";
    private String chatRecipient = "supervisor2@example.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_privado);

        rvChat = findViewById(R.id.rvChat);
        etMensajeChat = findViewById(R.id.etMensajeChat);
        btnEnviarChat = findViewById(R.id.btnEnviarChat);
        btnAdjuntarChat = findViewById(R.id.btnAdjuntarChat);

        dbHelper = new DatabaseHelper(this);
        chatMessages = new ArrayList<>();

        rvChat.setLayoutManager(new LinearLayoutManager(this));
        chatAdapter = new ChatAdapter(chatMessages);
        rvChat.setAdapter(chatAdapter);

        loadChatMessages();

        btnEnviarChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = etMensajeChat.getText().toString().trim();
                if (!TextUtils.isEmpty(message)) {
                    sendMessage(message);
                    etMensajeChat.setText("");
                }
            }
        });

        btnAdjuntarChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Implementa la lógica para adjuntar archivos (imágenes, audio, etc.)
            }
        });
    }

    private void loadChatMessages() {
        chatMessages.clear();
        Cursor cursor = dbHelper.getChatMessages(currentUser, chatRecipient);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String sender = safeGetString(cursor, DatabaseContract.ChatMessageEntry.COLUMN_SENDER);
                String receiver = safeGetString(cursor, DatabaseContract.ChatMessageEntry.COLUMN_RECEIVER);
                String content = safeGetString(cursor, DatabaseContract.ChatMessageEntry.COLUMN_CONTENT);
                String timestamp = safeGetString(cursor, DatabaseContract.ChatMessageEntry.COLUMN_TIMESTAMP);
                String messageType = safeGetString(cursor, DatabaseContract.ChatMessageEntry.COLUMN_MESSAGE_TYPE);
                String uri = safeGetString(cursor, DatabaseContract.ChatMessageEntry.COLUMN_URI);
                String status = safeGetString(cursor, DatabaseContract.ChatMessageEntry.COLUMN_STATUS);

                ChatMessage chatMessage = new ChatMessage(sender, receiver, content, messageType, timestamp, uri, status);
                chatMessages.add(chatMessage);
            } while (cursor.moveToNext());
            cursor.close();
        }
        chatAdapter.notifyDataSetChanged();
        if (chatMessages.size() > 0) {
            rvChat.scrollToPosition(chatMessages.size() - 1);
        }
    }

    private void sendMessage(String content) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        // Se asume que el tipo es "text" y el estado inicial es "sent"
        boolean inserted = dbHelper.insertChatMessage(currentUser, chatRecipient, content, "text", timestamp, "", "sent");
        if (inserted) {
            ChatMessage newMessage = new ChatMessage(currentUser, chatRecipient, content, "text", timestamp, "", "sent");
            chatMessages.add(newMessage);
            chatAdapter.notifyItemInserted(chatMessages.size() - 1);
            rvChat.scrollToPosition(chatMessages.size() - 1);
        }
    }

    // Modelo para mensajes de chat
    public static class ChatMessage {
        public String sender;
        public String receiver;
        public String content;
        public String messageType;
        public String timestamp;
        public String uri;
        public String status;

        public ChatMessage(String sender, String receiver, String content, String messageType, String timestamp, String uri, String status) {
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
    public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

        private ArrayList<ChatMessage> messages;

        public ChatAdapter(ArrayList<ChatMessage> messages) {
            this.messages = messages;
        }

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            ChatMessage message = messages.get(position);
            holder.text1.setText(message.sender + ": " + message.content);
            holder.text2.setText("Enviado: " + message.timestamp);
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        public class ChatViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            public ChatViewHolder(@NonNull View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }

    // Método auxiliar para obtener valores de forma segura
    private String safeGetString(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndex(columnName);
        if (index >= 0 && !cursor.isNull(index)) {
            return cursor.getString(index);
        }
        return "";
    }
}
