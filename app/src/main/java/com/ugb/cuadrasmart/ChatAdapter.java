package com.ugb.cuadrasmart;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface; // Para texto editado
import android.media.MediaPlayer;
import android.os.Handler;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "ChatAdapter";
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private ArrayList<ChatPrivadoActivity.ChatMessage> messagesList;
    private String currentUserId;
    private String recipientName;
    private MessageInteractionListener interactionListener; // Interfaz para comunicar acciones a la Activity

    private static MediaPlayer activeMediaPlayer;
    private static ImageButton activePlayButton;
    private static TextView activeDurationView;
    private static SeekBar activeSeekBar; // Opcional
    private static int currentlyPlayingPosition = -1;
    private static Handler progressHandler = new Handler();
    private static Runnable progressRunnable;

    public interface MessageInteractionListener {
        void onDeleteMessageForEveryone(String messageId);
        void onDeleteMessageForMe(String messageId);
        void onEditMessage(ChatPrivadoActivity.ChatMessage message);
    }

    public ChatAdapter(ArrayList<ChatPrivadoActivity.ChatMessage> messagesList, String currentUserId, String recipientName, MessageInteractionListener listener) {
        this.messagesList = messagesList;
        this.currentUserId = currentUserId;
        this.recipientName = recipientName;
        this.interactionListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        ChatPrivadoActivity.ChatMessage message = messagesList.get(position);
        if (message.getSenderId() != null && message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_SENT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message_sent, parent, false);
            return new SentMessageViewHolder(view, interactionListener, currentUserId);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message_received, parent, false);
            return new ReceivedMessageViewHolder(view, interactionListener, currentUserId);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatPrivadoActivity.ChatMessage message = messagesList.get(position);
        // Ocultar el mensaje si está borrado para el usuario actual
        if (message.isDeletedForCurrentUser(currentUserId)) {
            holder.itemView.setVisibility(View.GONE);
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0)); // Colapsar vista
            return;
        } else {
            holder.itemView.setVisibility(View.VISIBLE);
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        if (holder.getItemViewType() == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).bind(message, position);
        } else {
            ((ReceivedMessageViewHolder) holder).bind(message, recipientName, position);
        }
    }

    @Override
    public int getItemCount() {
        return messagesList.size();
    }

    public static void stopAnyActiveAudio() {
        if (activeMediaPlayer != null) {
            try {
                if (activeMediaPlayer.isPlaying()) activeMediaPlayer.stop();
                activeMediaPlayer.release();
            } catch (Exception e) { Log.e(TAG, "Error liberando activeMediaPlayer", e); }
            activeMediaPlayer = null;
        }
        if (activePlayButton != null) activePlayButton.setImageResource(R.drawable.ic_baseline_play_arrow_24);
        if (progressHandler != null && progressRunnable != null) progressHandler.removeCallbacks(progressRunnable);
        activePlayButton = null;
        activeDurationView = null;
        activeSeekBar = null;
        currentlyPlayingPosition = -1;
    }

    // --- ViewHolders ---
    static class BaseMessageViewHolder extends RecyclerView.ViewHolder {
        MessageInteractionListener interactionListener;
        String currentUserId;

        public BaseMessageViewHolder(@NonNull View itemView, MessageInteractionListener listener, String currentUserId) {
            super(itemView);
            this.interactionListener = listener;
            this.currentUserId = currentUserId;
        }

        protected void setupLongClickListener(final ChatPrivadoActivity.ChatMessage message) {
            itemView.setOnLongClickListener(v -> {
                if (message.getDocumentId() == null) return false;

                ArrayList<String> optionsList = new ArrayList<>();
                optionsList.add("Borrar para mí");

                if (message.getSenderId() != null && message.getSenderId().equals(currentUserId)) {
                    optionsList.add("Borrar para todos");
                    if ("text".equals(message.getMessageType())) { // Solo permitir editar mensajes de texto
                        optionsList.add("Editar mensaje");
                    }
                }

                String[] options = optionsList.toArray(new String[0]);

                new AlertDialog.Builder(itemView.getContext())
                        .setItems(options, (dialog, which) -> {
                            String selectedOption = options[which];
                            switch (selectedOption) {
                                case "Borrar para mí":
                                    interactionListener.onDeleteMessageForMe(message.getDocumentId());
                                    break;
                                case "Borrar para todos":
                                    interactionListener.onDeleteMessageForEveryone(message.getDocumentId());
                                    break;
                                case "Editar mensaje":
                                    interactionListener.onEditMessage(message);
                                    break;
                            }
                        })
                        .show();
                return true;
            });
        }
    }


    static class SentMessageViewHolder extends BaseMessageViewHolder {
        TextView tvMessageContent, tvMessageTimestamp, tvAudioDurationSent, tvEditedSent;
        ImageView ivMessageImageSent;
        LinearLayout layoutAudioPlayerSent;
        ImageButton btnPlayAudioSent;

        public SentMessageViewHolder(@NonNull View itemView, MessageInteractionListener listener, String currentUserId) {
            super(itemView, listener, currentUserId);
            tvMessageContent = itemView.findViewById(R.id.tvMessageContent);
            tvMessageTimestamp = itemView.findViewById(R.id.tvMessageTimestamp);
            ivMessageImageSent = itemView.findViewById(R.id.ivMessageImageSent);
            layoutAudioPlayerSent = itemView.findViewById(R.id.layoutAudioPlayerSent);
            btnPlayAudioSent = itemView.findViewById(R.id.btnPlayAudioSent);
            tvAudioDurationSent = itemView.findViewById(R.id.tvAudioDurationSent);
            tvEditedSent = itemView.findViewById(R.id.tvEditedSent); // Necesitas añadir este TextView en item_chat_message_sent.xml
        }

        void bind(final ChatPrivadoActivity.ChatMessage message, final int position) {
            resetViewState();
            String formattedTime = message.getFormattedTimestamp();
            if (message.isEdited() && tvEditedSent != null) {
                tvEditedSent.setVisibility(View.VISIBLE);
                tvMessageTimestamp.setText(" (editado) " + formattedTime);
            } else {
                if (tvEditedSent != null) tvEditedSent.setVisibility(View.GONE);
                tvMessageTimestamp.setText(formattedTime);
            }


            if ("text".equals(message.getMessageType())) {
                tvMessageContent.setText(message.getContent());
                tvMessageContent.setVisibility(View.VISIBLE);
            } else if ("image_base64".equals(message.getMessageType()) && message.getContent() != null) {
                if (ivMessageImageSent != null) {
                    ivMessageImageSent.setVisibility(View.VISIBLE);
                    try {
                        byte[] decodedString = Base64.decode(message.getContent(), Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        ivMessageImageSent.setImageBitmap(decodedByte);
                        ivMessageImageSent.setOnClickListener(v -> openFullScreenImage(message.getContent(), itemView.getContext()));
                    } catch (Exception e) {
                        Log.e(TAG, "Error decodificando imagen Base64 (enviada): ", e);
                        ivMessageImageSent.setImageResource(R.drawable.ic_baseline_broken_image_24);
                    }
                }
            } else if ("audio_base64".equals(message.getMessageType()) && message.getContent() != null) {
                if (layoutAudioPlayerSent != null && btnPlayAudioSent != null && tvAudioDurationSent != null) {
                    layoutAudioPlayerSent.setVisibility(View.VISIBLE);
                    updatePlayButtonState(btnPlayAudioSent, position, tvAudioDurationSent);
                    btnPlayAudioSent.setOnClickListener(v -> handleAudioPlay(message.getContent(), btnPlayAudioSent, tvAudioDurationSent, null, itemView.getContext(), position));
                }
            } else {
                tvMessageContent.setText(message.getContent() != null ? message.getContent() : "[Mensaje no válido]");
                tvMessageContent.setVisibility(View.VISIBLE);
            }
            setupLongClickListener(message);
        }

        private void resetViewState() {
            tvMessageContent.setVisibility(View.GONE);
            if (ivMessageImageSent != null) ivMessageImageSent.setVisibility(View.GONE);
            if (layoutAudioPlayerSent != null) layoutAudioPlayerSent.setVisibility(View.GONE);
            if (btnPlayAudioSent != null) btnPlayAudioSent.setOnClickListener(null);
            if (tvEditedSent != null) tvEditedSent.setVisibility(View.GONE);
        }
    }

    static class ReceivedMessageViewHolder extends BaseMessageViewHolder {
        TextView tvSenderName, tvMessageContent, tvMessageTimestamp, tvAudioDurationReceived, tvEditedReceived;
        ImageView ivMessageImageReceived;
        LinearLayout layoutAudioPlayerReceived;
        ImageButton btnPlayAudioReceived;

        public ReceivedMessageViewHolder(@NonNull View itemView, MessageInteractionListener listener, String currentUserId) {
            super(itemView, listener, currentUserId);
            tvSenderName = itemView.findViewById(R.id.tvMessageSenderName);
            tvMessageContent = itemView.findViewById(R.id.tvMessageContentReceived);
            tvMessageTimestamp = itemView.findViewById(R.id.tvMessageTimestampReceived);
            ivMessageImageReceived = itemView.findViewById(R.id.ivMessageImageReceived);
            layoutAudioPlayerReceived = itemView.findViewById(R.id.layoutAudioPlayerReceived);
            btnPlayAudioReceived = itemView.findViewById(R.id.btnPlayAudioReceived);
            tvAudioDurationReceived = itemView.findViewById(R.id.tvAudioDurationReceived);
            tvEditedReceived = itemView.findViewById(R.id.tvEditedReceived); // Necesitas añadir este TextView en item_chat_message_received.xml
        }

        void bind(final ChatPrivadoActivity.ChatMessage message, String senderDisplayName, final int position) {
            if (tvSenderName != null) tvSenderName.setText(senderDisplayName);
            resetViewState();

            String formattedTime = message.getFormattedTimestamp();
            if (message.isEdited() && tvEditedReceived != null) {
                tvEditedReceived.setVisibility(View.VISIBLE);
                tvMessageTimestamp.setText(" (editado) " + formattedTime);
            } else {
                if (tvEditedReceived != null) tvEditedReceived.setVisibility(View.GONE);
                tvMessageTimestamp.setText(formattedTime);
            }

            if ("text".equals(message.getMessageType())) {
                tvMessageContent.setText(message.getContent());
                tvMessageContent.setVisibility(View.VISIBLE);
            } else if ("image_base64".equals(message.getMessageType()) && message.getContent() != null) {
                if (ivMessageImageReceived != null) {
                    ivMessageImageReceived.setVisibility(View.VISIBLE);
                    try {
                        byte[] decodedString = Base64.decode(message.getContent(), Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        ivMessageImageReceived.setImageBitmap(decodedByte);
                        ivMessageImageReceived.setOnClickListener(v -> openFullScreenImage(message.getContent(), itemView.getContext()));
                    } catch (Exception e) {
                        Log.e(TAG, "Error decodificando imagen Base64 (recibida): ", e);
                        ivMessageImageReceived.setImageResource(R.drawable.ic_baseline_broken_image_24);
                    }
                }
            } else if ("audio_base64".equals(message.getMessageType()) && message.getContent() != null) {
                if (layoutAudioPlayerReceived != null && btnPlayAudioReceived != null && tvAudioDurationReceived != null) {
                    layoutAudioPlayerReceived.setVisibility(View.VISIBLE);
                    updatePlayButtonState(btnPlayAudioReceived, position, tvAudioDurationReceived);
                    btnPlayAudioReceived.setOnClickListener(v -> handleAudioPlay(message.getContent(), btnPlayAudioReceived, tvAudioDurationReceived, null, itemView.getContext(), position));
                }
            } else {
                tvMessageContent.setText(message.getContent() != null ? message.getContent() : "[Mensaje no válido]");
                tvMessageContent.setVisibility(View.VISIBLE);
            }
            setupLongClickListener(message);
        }
        private void resetViewState() {
            tvMessageContent.setVisibility(View.GONE);
            if (ivMessageImageReceived != null) ivMessageImageReceived.setVisibility(View.GONE);
            if (layoutAudioPlayerReceived != null) layoutAudioPlayerReceived.setVisibility(View.GONE);
            if (btnPlayAudioReceived != null) btnPlayAudioReceived.setOnClickListener(null);
            if (tvEditedReceived != null) tvEditedReceived.setVisibility(View.GONE);
        }
    }

    private static void openFullScreenImage(String base64Image, Context context) {
        Intent intent = new Intent(context, FullScreenImageActivity.class);
        intent.putExtra(FullScreenImageActivity.EXTRA_IMAGE_BASE64, base64Image);
        context.startActivity(intent);
    }

    private static void updatePlayButtonState(ImageButton playButton, int position, TextView durationView) {
        if (position == currentlyPlayingPosition && activeMediaPlayer != null && activeMediaPlayer.isPlaying()) {
            playButton.setImageResource(R.drawable.ic_baseline_pause_24);
        } else {
            playButton.setImageResource(R.drawable.ic_baseline_play_arrow_24);
            // Si no está reproduciendo este, y tenemos duración del media player anterior, mostrarla
            if (activeMediaPlayer != null && durationView != null && currentlyPlayingPosition == position) { // o solo si mp no es null
                // No hacer nada aquí, se actualiza al preparar
            } else if (durationView != null){
                // durationView.setText("00:00"); // O dejarlo como estaba
            }
        }
    }

    private static void handleAudioPlay(String base64Audio, ImageButton playButton, TextView durationView, @Nullable SeekBar seekBar, Context context, int position) {
        if (currentlyPlayingPosition == position && activeMediaPlayer != null && activeMediaPlayer.isPlaying()) {
            activeMediaPlayer.pause();
            playButton.setImageResource(R.drawable.ic_baseline_play_arrow_24);
            if (progressHandler != null && progressRunnable != null) progressHandler.removeCallbacks(progressRunnable);
        } else if (currentlyPlayingPosition == position && activeMediaPlayer != null && !activeMediaPlayer.isPlaying()) {
            activeMediaPlayer.start();
            playButton.setImageResource(R.drawable.ic_baseline_pause_24);
            updateAudioProgress(durationView, seekBar);
        } else {
            stopAnyActiveAudio();
            currentlyPlayingPosition = position;
            activePlayButton = playButton;
            activeDurationView = durationView;
            activeSeekBar = seekBar;

            try {
                byte[] decodedString = Base64.decode(base64Audio, Base64.DEFAULT);
                File tempAudioFile = File.createTempFile("chat_audio_playback_", ".3gp", context.getCacheDir());
                FileOutputStream fos = new FileOutputStream(tempAudioFile);
                fos.write(decodedString);
                fos.close();

                activeMediaPlayer = new MediaPlayer();
                activeMediaPlayer.setDataSource(tempAudioFile.getAbsolutePath());
                activeMediaPlayer.setOnPreparedListener(mp -> {
                    mp.start();
                    playButton.setImageResource(R.drawable.ic_baseline_pause_24);
                    if (durationView != null) durationView.setText(formatDuration(mp.getDuration()));
                    if (seekBar != null) seekBar.setMax(mp.getDuration());
                    updateAudioProgress(durationView, seekBar);
                });
                activeMediaPlayer.setOnCompletionListener(mp -> {
                    if (tempAudioFile.exists()) tempAudioFile.delete();
                    stopAnyActiveAudio(); // Esto reseteará el botón y el estado
                });
                activeMediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.e(TAG, "MediaPlayer Error: what=" + what + ", extra=" + extra);
                    if (tempAudioFile.exists()) tempAudioFile.delete();
                    stopAnyActiveAudio();
                    Toast.makeText(context, "Error al reproducir audio.", Toast.LENGTH_SHORT).show();
                    return true;
                });
                activeMediaPlayer.prepareAsync();
            } catch (IOException | IllegalArgumentException e) {
                Log.e(TAG, "Error preparando audio Base64: ", e);
                Toast.makeText(context, "Error al preparar audio.", Toast.LENGTH_SHORT).show();
                stopAnyActiveAudio();
            }
        }
    }

    private static void updateAudioProgress(TextView durationView, @Nullable SeekBar seekBar) {
        if (activeMediaPlayer != null && activeMediaPlayer.isPlaying()) {
            if (seekBar != null) seekBar.setProgress(activeMediaPlayer.getCurrentPosition());
            // Actualizar el TextView de duración para mostrar tiempo actual / total
            // if (durationView != null) {
            //    durationView.setText(String.format("%s / %s",
            //        formatDuration(activeMediaPlayer.getCurrentPosition()),
            //        formatDuration(activeMediaPlayer.getDuration())));
            // }
            progressRunnable = () -> updateAudioProgress(durationView, seekBar);
            progressHandler.postDelayed(progressRunnable, 500);
        }
    }

    private static String formatDuration(long millis) {
        if (millis < 0) millis = 0; // Evitar duraciones negativas si algo sale mal
        return String.format(Locale.US, "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }
}