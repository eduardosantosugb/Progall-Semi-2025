package com.ugb.cuadrasmart;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar; // Opcional, para progreso de audio
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit; // Para formatear duración

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "ChatAdapter";
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private ArrayList<ChatPrivadoActivity.ChatMessage> messagesList;
    private String currentUserId;
    private String recipientName;

    // Para manejar un solo MediaPlayer activo a la vez
    private static MediaPlayer activeMediaPlayer;
    private static ImageButton activePlayButton;
    private static int currentlyPlayingPosition = -1;
    private static Handler progressHandler = new Handler();
    private static Runnable progressRunnable;


    public ChatAdapter(ArrayList<ChatPrivadoActivity.ChatMessage> messagesList, String currentUserId, String recipientName) {
        this.messagesList = messagesList;
        this.currentUserId = currentUserId;
        this.recipientName = recipientName;
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
            return new SentMessageViewHolder(view);
        } else { // VIEW_TYPE_RECEIVED
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatPrivadoActivity.ChatMessage message = messagesList.get(position);
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

    // Método para detener cualquier reproducción de audio activa
    public static void stopAnyActiveAudio() {
        if (activeMediaPlayer != null) {
            if (activeMediaPlayer.isPlaying()) {
                activeMediaPlayer.stop();
            }
            activeMediaPlayer.release();
            activeMediaPlayer = null;
            if (activePlayButton != null) {
                activePlayButton.setImageResource(R.drawable.ic_baseline_play_arrow_24);
            }
            if (progressHandler != null && progressRunnable != null) {
                progressHandler.removeCallbacks(progressRunnable);
            }
            activePlayButton = null;
            currentlyPlayingPosition = -1;
            Log.d(TAG, "Audio activo detenido y liberado.");
        }
    }


    // --- ViewHolders ---

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageContent, tvMessageTimestamp, tvAudioDurationSent;
        ImageView ivMessageImageSent;
        LinearLayout layoutAudioPlayerSent;
        ImageButton btnPlayAudioSent;
        // SeekBar seekBarAudioSent; // Opcional

        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageContent = itemView.findViewById(R.id.tvMessageContent);
            tvMessageTimestamp = itemView.findViewById(R.id.tvMessageTimestamp);
            ivMessageImageSent = itemView.findViewById(R.id.ivMessageImageSent);
            layoutAudioPlayerSent = itemView.findViewById(R.id.layoutAudioPlayerSent);
            btnPlayAudioSent = itemView.findViewById(R.id.btnPlayAudioSent);
            tvAudioDurationSent = itemView.findViewById(R.id.tvAudioDurationSent);
            // seekBarAudioSent = itemView.findViewById(R.id.seekBarAudioSent); // Si añades un SeekBar
        }

        void bind(final ChatPrivadoActivity.ChatMessage message, final int position) {
            tvMessageTimestamp.setText(message.getFormattedTimestamp());
            resetViewState(); // Resetear vistas antes de configurar

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
                    // tvAudioDurationSent.setText("00:00"); // Placeholder, se actualizará al cargar
                    updatePlayButtonState(btnPlayAudioSent, position); // Actualizar ícono play/pause
                    btnPlayAudioSent.setOnClickListener(v -> handleAudioPlay(message.getContent(), btnPlayAudioSent, tvAudioDurationSent, null, itemView.getContext(), position));
                }
            } else {
                tvMessageContent.setText(message.getContent() != null ? message.getContent() : "[Mensaje no válido]");
                tvMessageContent.setVisibility(View.VISIBLE);
            }
        }

        private void resetViewState() {
            tvMessageContent.setVisibility(View.GONE);
            if (ivMessageImageSent != null) ivMessageImageSent.setVisibility(View.GONE);
            if (layoutAudioPlayerSent != null) layoutAudioPlayerSent.setVisibility(View.GONE);
            if (btnPlayAudioSent != null) btnPlayAudioSent.setOnClickListener(null);
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvSenderName, tvMessageContent, tvMessageTimestamp, tvAudioDurationReceived;
        ImageView ivMessageImageReceived;
        LinearLayout layoutAudioPlayerReceived;
        ImageButton btnPlayAudioReceived;
        // SeekBar seekBarAudioReceived; // Opcional

        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSenderName = itemView.findViewById(R.id.tvMessageSenderName);
            tvMessageContent = itemView.findViewById(R.id.tvMessageContentReceived);
            tvMessageTimestamp = itemView.findViewById(R.id.tvMessageTimestampReceived);
            ivMessageImageReceived = itemView.findViewById(R.id.ivMessageImageReceived);
            layoutAudioPlayerReceived = itemView.findViewById(R.id.layoutAudioPlayerReceived);
            btnPlayAudioReceived = itemView.findViewById(R.id.btnPlayAudioReceived);
            tvAudioDurationReceived = itemView.findViewById(R.id.tvAudioDurationReceived);
            // seekBarAudioReceived = itemView.findViewById(R.id.seekBarAudioReceived);
        }

        void bind(final ChatPrivadoActivity.ChatMessage message, String senderDisplayName, final int position) {
            if (tvSenderName != null) tvSenderName.setText(senderDisplayName);
            tvMessageTimestamp.setText(message.getFormattedTimestamp());
            resetViewState();

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
                    updatePlayButtonState(btnPlayAudioReceived, position);
                    btnPlayAudioReceived.setOnClickListener(v -> handleAudioPlay(message.getContent(), btnPlayAudioReceived, tvAudioDurationReceived, null, itemView.getContext(), position));
                }
            } else {
                tvMessageContent.setText(message.getContent() != null ? message.getContent() : "[Mensaje no válido]");
                tvMessageContent.setVisibility(View.VISIBLE);
            }
        }
        private void resetViewState() {
            tvMessageContent.setVisibility(View.GONE);
            if (ivMessageImageReceived != null) ivMessageImageReceived.setVisibility(View.GONE);
            if (layoutAudioPlayerReceived != null) layoutAudioPlayerReceived.setVisibility(View.GONE);
            if (btnPlayAudioReceived != null) btnPlayAudioReceived.setOnClickListener(null);
        }
    }

    // --- Métodos Comunes para ViewHolders ---
    private static void openFullScreenImage(String base64Image, Context context) {
        Intent intent = new Intent(context, FullScreenImageActivity.class);
        intent.putExtra(FullScreenImageActivity.EXTRA_IMAGE_BASE64, base64Image);
        context.startActivity(intent);
    }

    private static void updatePlayButtonState(ImageButton playButton, int position) {
        if (position == currentlyPlayingPosition && activeMediaPlayer != null && activeMediaPlayer.isPlaying()) {
            playButton.setImageResource(R.drawable.ic_baseline_pause_24);
        } else {
            playButton.setImageResource(R.drawable.ic_baseline_play_arrow_24);
        }
    }

    private static void handleAudioPlay(String base64Audio, ImageButton playButton, TextView durationView, @Nullable SeekBar seekBar, Context context, int position) {
        if (currentlyPlayingPosition == position && activeMediaPlayer != null && activeMediaPlayer.isPlaying()) {
            // Pausar el audio actual
            activeMediaPlayer.pause();
            playButton.setImageResource(R.drawable.ic_baseline_play_arrow_24);
            if (progressHandler != null && progressRunnable != null) {
                progressHandler.removeCallbacks(progressRunnable);
            }
            Log.d(TAG, "Audio pausado en posición: " + position);
        } else if (currentlyPlayingPosition == position && activeMediaPlayer != null && !activeMediaPlayer.isPlaying()) {
            // Reanudar el audio pausado
            activeMediaPlayer.start();
            playButton.setImageResource(R.drawable.ic_baseline_pause_24);
            updateProgressBar(durationView, seekBar); // Reanudar actualización de progreso
            Log.d(TAG, "Audio reanudado en posición: " + position);
        } else {
            // Detener cualquier audio anterior
            stopAnyActiveAudio();
            currentlyPlayingPosition = position;
            activePlayButton = playButton;

            try {
                byte[] decodedString = Base64.decode(base64Audio, Base64.DEFAULT);
                File tempAudioFile = File.createTempFile("playback_audio_", ".3gp", context.getCacheDir());
                FileOutputStream fos = new FileOutputStream(tempAudioFile);
                fos.write(decodedString);
                fos.close();

                activeMediaPlayer = new MediaPlayer();
                activeMediaPlayer.setDataSource(tempAudioFile.getAbsolutePath());
                activeMediaPlayer.setOnPreparedListener(mp -> {
                    mp.start();
                    playButton.setImageResource(R.drawable.ic_baseline_pause_24);
                    if (durationView != null) {
                        durationView.setText(formatDuration(mp.getDuration()));
                    }
                    if (seekBar != null) {
                        seekBar.setMax(mp.getDuration());
                    }
                    updateProgressBar(durationView, seekBar);
                    Log.d(TAG, "Reproduciendo audio. Posición: " + position + ", Duración: " + mp.getDuration());
                });
                activeMediaPlayer.setOnCompletionListener(mp -> {
                    stopAnyActiveAudio(); // Llama al método de limpieza general
                    if (tempAudioFile.exists()) tempAudioFile.delete();
                    Log.d(TAG, "Reproducción completada. Posición: " + position);
                });
                activeMediaPlayer.setOnErrorListener((mp, what, extra) -> {
                    Log.e(TAG, "MediaPlayer Error: what=" + what + ", extra=" + extra + ", Posición: " + position);
                    stopAnyActiveAudio();
                    if (tempAudioFile.exists()) tempAudioFile.delete();
                    Toast.makeText(context, "Error al reproducir audio.", Toast.LENGTH_SHORT).show();
                    return true;
                });
                activeMediaPlayer.prepareAsync();

            } catch (IOException | IllegalArgumentException e) {
                Log.e(TAG, "Error preparando audio Base64: ", e);
                Toast.makeText(context, "Error al preparar audio.", Toast.LENGTH_SHORT).show();
                stopAnyActiveAudio(); // Limpiar en caso de error de preparación
            }
        }
    }

    private static void updateProgressBar(TextView durationView, @Nullable SeekBar seekBar) {
        if (activeMediaPlayer != null && activeMediaPlayer.isPlaying()) {
            if (seekBar != null) {
                seekBar.setProgress(activeMediaPlayer.getCurrentPosition());
            }
            // Actualizar también el textview de duración/tiempo actual si se desea
            // durationView.setText(formatDuration(activeMediaPlayer.getCurrentPosition()) + " / " + formatDuration(activeMediaPlayer.getDuration()));

            progressRunnable = () -> updateProgressBar(durationView, seekBar);
            progressHandler.postDelayed(progressRunnable, 500); // Actualizar cada 500ms
        }
    }

    private static String formatDuration(long millis) {
        if (millis < 0) millis = 0;
        return String.format(Locale.US, "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }
}