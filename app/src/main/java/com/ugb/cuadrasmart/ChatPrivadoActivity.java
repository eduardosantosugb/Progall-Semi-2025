package com.ugb.cuadrasmart;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
// import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ChatPrivadoActivity extends AppCompatActivity {

    private static final String TAG = "ChatPrivadoActivity";

    private RecyclerView rvChat;
    private EditText etMensajeChat;
    private Button btnEnviarChat;
    private ImageButton btnAdjuntarChat;
    private Toolbar toolbarChat;
    // private ProgressBar pbUpload;

    private FirebaseAuth mAuth;
    private FirebaseFirestore dbFirestore;
    private ListenerRegistration messagesListener;

    private ChatAdapter chatAdapter;
    private ArrayList<ChatMessage> chatMessagesList;

    private String currentUserUid;
    private String chatRecipientUid;
    private String chatRecipientName;
    private String chatId;

    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int REQUEST_IMAGE_PICK = 102;
    private Uri currentPhotoUri;
    private static final int IMAGE_MAX_SIDE_PX = 800;
    private static final int IMAGE_JPEG_QUALITY = 60;

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 202;
    // private static final int REQUEST_WRITE_STORAGE_PERMISSION = 201; // <--- ELIMINADA o comentada si no se usa aquí

    private MediaRecorder mediaRecorder;
    private File audioFile;
    private boolean isRecording = false;
    private Handler recordingHandler = new Handler();
    private Runnable stopRecordingRunnable;
    private AlertDialog recordingProgressDialog;
    private TextView tvRecordingDialogTimer;
    private long recordingStartTimeMillis;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_privado);
        Log.d(TAG, "onCreate: Iniciando.");

        mAuth = FirebaseAuth.getInstance();
        dbFirestore = FirebaseFirestore.getInstance();

        FirebaseUser firebaseCurrentUser = mAuth.getCurrentUser();
        if (firebaseCurrentUser == null) {
            handleUnauthenticatedUser();
            return;
        }
        currentUserUid = firebaseCurrentUser.getUid();
        Log.d(TAG, "onCreate: currentUserUid: " + currentUserUid);

        chatRecipientUid = getIntent().getStringExtra("recipient_uid");
        chatRecipientName = getIntent().getStringExtra("recipient_name");

        if (TextUtils.isEmpty(chatRecipientUid) || TextUtils.isEmpty(chatRecipientName)) {
            handleInvalidRecipient();
            return;
        }
        Log.d(TAG, "onCreate: chatRecipientUid: " + chatRecipientUid + ", name: " + chatRecipientName);

        generateChatId();
        setupToolbar();
        setupUIViews();
        setupRecyclerView();

        Log.d(TAG, "onCreate: Config UI completa. Llamando a loadChatMessagesFromFirestore.");
        loadChatMessagesFromFirestore();
        setupSendButtonListener();
        setupAttachButtonListener();
    }

    private void handleUnauthenticatedUser() {
        Toast.makeText(this, "Error: Usuario no autenticado.", Toast.LENGTH_LONG).show();
        Log.e(TAG, "Usuario actual de Firebase es null. Finalizando.");
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handleInvalidRecipient() {
        Toast.makeText(this, "Error: Destinatario no especificado.", Toast.LENGTH_LONG).show();
        Log.e(TAG, "recipient_uid o recipient_name es null. Finalizando.");
        finish();
    }

    private void generateChatId() {
        if (currentUserUid.compareTo(chatRecipientUid) > 0) {
            chatId = currentUserUid + "_" + chatRecipientUid;
        } else {
            chatId = chatRecipientUid + "_" + currentUserUid;
        }
        Log.i(TAG, "Chat ID generado: " + chatId);
    }

    private void setupToolbar() {
        toolbarChat = findViewById(R.id.toolbarChat);
        setSupportActionBar(toolbarChat);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle(chatRecipientName);
        }
    }

    private void setupUIViews() {
        rvChat = findViewById(R.id.rvChat);
        etMensajeChat = findViewById(R.id.etMensajeChat);
        btnEnviarChat = findViewById(R.id.btnEnviarChat);
        btnAdjuntarChat = findViewById(R.id.btnAdjuntarChat);
    }

    private void setupRecyclerView() {
        chatMessagesList = new ArrayList<>();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvChat.setLayoutManager(layoutManager);
        chatAdapter = new ChatAdapter(chatMessagesList, currentUserUid, chatRecipientName);
        rvChat.setAdapter(chatAdapter);
    }

    private void setupSendButtonListener() {
        btnEnviarChat.setOnClickListener(view -> {
            String messageContent = etMensajeChat.getText().toString().trim();
            if (!TextUtils.isEmpty(messageContent)) {
                sendMessageToFirestore(messageContent, "text", null);
                etMensajeChat.setText("");
            } else {
                Toast.makeText(ChatPrivadoActivity.this, "El mensaje no puede estar vacío", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupAttachButtonListener() {
        btnAdjuntarChat.setOnClickListener(view -> showAttachmentDialog());
    }

    private void showAttachmentDialog() {
        String[] options = {"Enviar Imagen", "Grabar Audio (máx 30s)"};
        new AlertDialog.Builder(this)
                .setTitle("Adjuntar")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showImageSourceDialog();
                    } else if (which == 1) {
                        checkAudioPermissionAndStartRecordingSetup();
                    }
                })
                .show();
    }

    private void showImageSourceDialog() {
        String[] imageOptions = {"Tomar foto", "Seleccionar de la galería"};
        new AlertDialog.Builder(this)
                .setTitle("Seleccionar Fuente de Imagen")
                .setItems(imageOptions, (dialog, which) -> {
                    if (which == 0) dispatchTakePictureIntent();
                    else dispatchPickPictureIntent();
                })
                .show();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Error creando archivo de imagen", ex);
                Toast.makeText(this, "Error al preparar la cámara", Toast.LENGTH_SHORT).show();
                return;
            }
            if (photoFile != null) {
                currentPhotoUri = FileProvider.getUriForFile(this, "com.ugb.cuadrasmart.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        } else { Toast.makeText(this, "No se encontró app de cámara.", Toast.LENGTH_SHORT).show(); }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void dispatchPickPictureIntent() {
        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickPhotoIntent.setType("image/*");
        startActivityForResult(pickPhotoIntent, REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                if (currentPhotoUri != null) processAndSendImage(currentPhotoUri);
            } else if (requestCode == REQUEST_IMAGE_PICK && data != null && data.getData() != null) {
                processAndSendImage(data.getData());
            }
        }
    }

    private void processAndSendImage(Uri imageUri) {
        Toast.makeText(this, "Procesando imagen...", Toast.LENGTH_SHORT).show();
        try {
            Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            Bitmap resizedBitmap = getResizedBitmap(originalBitmap, IMAGE_MAX_SIDE_PX);
            String base64Image = bitmapToBase64(resizedBitmap, IMAGE_JPEG_QUALITY);

            if (originalBitmap != null && !originalBitmap.isRecycled()) originalBitmap.recycle();
            if (resizedBitmap != null && !resizedBitmap.isRecycled() && resizedBitmap != originalBitmap) resizedBitmap.recycle();

            if (base64Image != null) {
                sendMessageToFirestore(base64Image, "image_base64", null);
                Toast.makeText(ChatPrivadoActivity.this, "Imagen enviada.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error al procesar la imagen (Base64 null).", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error al convertir URI a Bitmap o Base64", e);
            Toast.makeText(this, "Error al procesar imagen.", Toast.LENGTH_LONG).show();
        }
    }

    public static Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= maxSize && height <= maxSize) return image;
        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) { width = maxSize; height = (int) (width / bitmapRatio);
        } else { height = maxSize; width = (int) (height * bitmapRatio); }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    private String bitmapToBase64(Bitmap bitmap, int quality) {
        if (bitmap == null) return null;
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            byte[] byteArray = baos.toByteArray();
            if (byteArray.length > 750000) {
                Log.w(TAG, "Imagen Base64 demasiado grande: " + byteArray.length + " bytes");
                Toast.makeText(this, "La imagen es demasiado grande.", Toast.LENGTH_LONG).show();
                return null;
            }
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) { Log.e(TAG, "Error en bitmapToBase64", e); return null;
        } finally { if (baos != null) try { baos.close(); } catch (IOException ignored) {} }
    }

    private void checkAudioPermissionAndStartRecordingSetup() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            showRecordingDialog();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // La constante REQUEST_WRITE_STORAGE_PERMISSION no está definida en esta clase,
        // ya que esa lógica de permiso está en FullScreenImageActivity.
        // if (requestCode == REQUEST_WRITE_STORAGE_PERMISSION) {
        //     // ...
        // } else
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showRecordingDialog();
            } else {
                Toast.makeText(this, "Permiso de grabación necesario.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private File createAudioFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String audioFileName = "AUDIO_" + timeStamp + "_" + currentUserUid;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC); // Usar DIRECTORY_MUSIC o DIRECTORY_RECORDINGS
        if (storageDir == null) { // Fallback si getExternalFilesDir devuelve null
            storageDir = getFilesDir(); // Directorio interno
        }
        if (!storageDir.exists()){
            if(!storageDir.mkdirs()){
                Log.e(TAG, "No se pudo crear el directorio de audio.");
                throw new IOException("No se pudo crear el directorio de audio.");
            }
        }
        return File.createTempFile(audioFileName, ".3gp", storageDir);
    }

    private void showRecordingDialog() {
        if (isRecording) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_record_audio, null);
        builder.setView(dialogView).setTitle("Grabando Audio...").setCancelable(false);

        final Button btnControl = dialogView.findViewById(R.id.btnRecordAudioControl);
        tvRecordingDialogTimer = dialogView.findViewById(R.id.tvRecordingTimer);
        tvRecordingDialogTimer.setText("00 / 30 s");

        btnControl.setText("Detener Grabación");
        btnControl.setOnClickListener(v -> {
            if (isRecording) stopActualRecording(true);
        });

        recordingProgressDialog = builder.create();
        recordingProgressDialog.setOnShowListener(dialogInterface -> startActualRecording());
        recordingProgressDialog.show();
    }

    private void startActualRecording() {
        try {
            audioFile = createAudioFile();
        } catch (IOException e) {
            Log.e(TAG, "startActualRecording: Error creando archivo de audio", e);
            Toast.makeText(this, "Error preparando grabación.", Toast.LENGTH_SHORT).show();
            if (recordingProgressDialog != null && recordingProgressDialog.isShowing()) recordingProgressDialog.dismiss();
            return;
        }

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(audioFile.getAbsolutePath());

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            recordingStartTimeMillis = System.currentTimeMillis();
            Log.i(TAG, "Grabación de audio iniciada: " + audioFile.getAbsolutePath());
            updateRecordingTimerUI();

            stopRecordingRunnable = () -> {
                if (isRecording) {
                    Log.d(TAG, "Límite de tiempo de grabación alcanzado.");
                    stopActualRecording(false);
                }
            };
            recordingHandler.postDelayed(stopRecordingRunnable, 30000);
        } catch (IOException | IllegalStateException e) {
            Log.e(TAG, "startActualRecording: MediaRecorder prepare()/start() falló", e);
            Toast.makeText(this, "Error al iniciar grabación.", Toast.LENGTH_LONG).show();
            releaseMediaRecorder();
            if (recordingProgressDialog != null && recordingProgressDialog.isShowing()) recordingProgressDialog.dismiss();
            isRecording = false;
        }
    }

    private void updateRecordingTimerUI() {
        if (isRecording && tvRecordingDialogTimer != null) {
            long elapsedMillis = System.currentTimeMillis() - recordingStartTimeMillis;
            long seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMillis);
            tvRecordingDialogTimer.setText(String.format(Locale.US, "%02d / 30 s", seconds));
            if (seconds < 30) recordingHandler.postDelayed(this::updateRecordingTimerUI, 500);
        }
    }

    private void stopActualRecording(boolean userInitiated) {
        if (!isRecording) return;
        Log.d(TAG, "stopActualRecording. Usuario lo inició: " + userInitiated);
        isRecording = false;
        if (stopRecordingRunnable != null) recordingHandler.removeCallbacks(stopRecordingRunnable);

        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (RuntimeException e) {
                Log.w(TAG, "Excepción en mediaRecorder.stop(): " + e.getMessage());
                if (audioFile != null && audioFile.exists() && audioFile.length() == 0) {
                    audioFile.delete(); audioFile = null;
                }
            }
            releaseMediaRecorder();
        }

        if (recordingProgressDialog != null && recordingProgressDialog.isShowing()) {
            recordingProgressDialog.dismiss();
        }

        if (audioFile != null && audioFile.exists() && audioFile.length() > 500) { // Umbral pequeño para audio válido
            Log.i(TAG, "Grabación finalizada. Archivo: " + audioFile.getAbsolutePath() + ", Tamaño: " + audioFile.length());
            processAndSendAudio(Uri.fromFile(audioFile));
        } else {
            Log.w(TAG, "Archivo de audio no válido o muy corto post-grabación.");
            if (audioFile != null && audioFile.exists()) audioFile.delete();
            if (!userInitiated) Toast.makeText(this, "No se grabó audio suficiente.", Toast.LENGTH_SHORT).show();
        }
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            try { if (isRecording) mediaRecorder.stop(); } catch (Exception ignored) {} // Intenta detener por si acaso
            try { mediaRecorder.reset(); mediaRecorder.release(); } catch (Exception ignored) {}
            mediaRecorder = null;
            isRecording = false;
            Log.d(TAG, "MediaRecorder liberado.");
        }
    }

    private void processAndSendAudio(Uri audioUri) {
        if (audioUri == null || audioFile == null || !audioFile.exists()) {
            Toast.makeText(this, "Error al procesar audio (archivo no encontrado).", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "processAndSendAudio: audioUri o audioFile nulo o no existe.");
            return;
        }
        Toast.makeText(this, "Procesando audio...", Toast.LENGTH_SHORT).show();
        try {
            byte[] audioBytes = fileToByteArray(audioFile);
            if (audioBytes == null) {
                Toast.makeText(this, "Error al leer archivo de audio (bytes nulos).", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "processAndSendAudio: audioBytes es null después de fileToByteArray.");
                return;
            }
            String base64Audio = Base64.encodeToString(audioBytes, Base64.DEFAULT);
            Log.d(TAG, "Audio Base64 (longitud original bytes: " + audioBytes.length + ", longitud string: " + base64Audio.length() + ")");

            if (base64Audio.length() > 950000) { // Límite conservador
                Log.w(TAG, "Audio Base64 demasiado grande para Firestore: " + base64Audio.length());
                Toast.makeText(this, "El audio grabado es demasiado largo para enviar.", Toast.LENGTH_LONG).show();
                return;
            }
            sendMessageToFirestore(base64Audio, "audio_base64", null);
            Toast.makeText(this, "Audio enviado.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "Error convirtiendo audio a bytes o I/O general", e);
            Toast.makeText(this, "Error al procesar audio.", Toast.LENGTH_SHORT).show();
        } finally {
            if (audioFile != null && audioFile.exists()) {
                if (audioFile.delete()) {
                    Log.d(TAG, "Archivo de audio temporal eliminado: " + audioFile.getAbsolutePath());
                } else {
                    Log.w(TAG, "No se pudo eliminar el archivo de audio temporal: " + audioFile.getAbsolutePath());
                }
            }
            audioFile = null;
        }
    }

    private byte[] fileToByteArray(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024 * 4];
        try {
            for (int readNum; (readNum = fis.read(buf)) != -1;) bos.write(buf, 0, readNum);
        } finally {
            fis.close();
            bos.close();
        }
        return bos.toByteArray();
    }

    private void loadChatMessagesFromFirestore() {
        if (chatId == null) { Log.e(TAG, "loadChatMessages: chatId es null."); return; }
        Log.d(TAG, "Configurando listener para chatId: " + chatId);
        messagesListener = dbFirestore.collection("chats").document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) { Log.e(TAG, "Error escuchando mensajes.", e); return; }
                    if (snapshots == null) { Log.w(TAG, "Snapshots de mensajes es null."); return; }

                    boolean newMessages = false;
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            try {
                                ChatMessage msg = dc.getDocument().toObject(ChatMessage.class);
                                chatMessagesList.add(msg); newMessages = true;
                            } catch (Exception ex) { Log.e(TAG, "Error convirtiendo doc: " + dc.getDocument().getId(), ex); }
                        }
                    }
                    if (newMessages) {
                        Collections.sort(chatMessagesList, Comparator.comparing(ChatMessage::getTimestamp, Comparator.nullsLast(com.google.firebase.Timestamp::compareTo)));
                        chatAdapter.notifyDataSetChanged();
                        scrollToBottom();
                    }
                });
    }

    private void sendMessageToFirestore(String contentOrBase64, String messageType, @Nullable String mediaUrl) {
        if (chatId == null || currentUserUid == null || chatRecipientUid == null) {
            Log.e(TAG, "sendMessage: Info crítica faltante."); return;
        }
        if (("text".equals(messageType) && TextUtils.isEmpty(contentOrBase64)) && mediaUrl == null && !"image_base64".equals(messageType) && !"audio_base64".equals(messageType)) {
            Log.w(TAG, "sendMessage: Contenido vacío para texto y sin media."); return;
        }

        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUserUid);
        message.put("receiverId", chatRecipientUid);
        message.put("messageType", messageType);
        message.put("timestamp", FieldValue.serverTimestamp());

        if ("image_base64".equals(messageType) || "audio_base64".equals(messageType)) {
            message.put("content", contentOrBase64);
        } else { message.put("content", contentOrBase64); }

        dbFirestore.collection("chats").document(chatId).collection("messages").add(message)
                .addOnSuccessListener(docRef -> Log.i(TAG, "Mensaje enviado ID: " + docRef.getId()))
                .addOnFailureListener(err -> Log.e(TAG, "Error enviando mensaje", err));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (messagesListener != null) messagesListener.remove();
        releaseMediaRecorder(); // Liberar si estaba grabando
        ChatAdapter.stopAnyActiveAudio();
        Log.i(TAG, "onStop: Listener y MediaRecorder liberados.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListener != null) messagesListener.remove();
        releaseMediaRecorder();
        ChatAdapter.stopAnyActiveAudio();
        if (recordingHandler != null) { // Limpiar todos los callbacks del handler
            recordingHandler.removeCallbacksAndMessages(null);
        }
    }

    private void scrollToBottom() {
        if (chatAdapter != null && chatAdapter.getItemCount() > 0) {
            rvChat.post(() -> rvChat.smoothScrollToPosition(chatAdapter.getItemCount() - 1));
        }
    }

    public static class ChatMessage {
        private String senderId, receiverId, content, messageType, status;
        private com.google.firebase.Timestamp timestamp;

        public ChatMessage() {}

        public String getSenderId() { return senderId; }
        public void setSenderId(String senderId) { this.senderId = senderId; }
        public String getReceiverId() { return receiverId; }
        public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getMessageType() { return messageType; }
        public void setMessageType(String messageType) { this.messageType = messageType; }
        public com.google.firebase.Timestamp getTimestamp() { return timestamp; }
        public void setTimestamp(com.google.firebase.Timestamp timestamp) { this.timestamp = timestamp; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getContentShort() {
            if (("image_base64".equals(messageType) || "audio_base64".equals(messageType)) && content != null && content.length() > 50) {
                return messageType + "[len=" + content.length() + "]";
            }
            return content;
        }

        public String getFormattedTimestamp() {
            if (timestamp == null) return "";
            try {
                Calendar cal = Calendar.getInstance(Locale.getDefault());
                cal.setTimeInMillis(timestamp.toDate().getTime());
                return DateFormat.format("hh:mm a", cal).toString();
            } catch (Exception e) { Log.e(TAG, "Error formateando timestamp", e); return ""; }
        }
    }
}