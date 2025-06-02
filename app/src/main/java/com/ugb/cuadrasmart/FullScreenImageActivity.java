package com.ugb.cuadrasmart;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class FullScreenImageActivity extends AppCompatActivity {

    private static final String TAG = "FullScreenImageAct";
    public static final String EXTRA_IMAGE_BASE64 = "image_base64_string";
    private static final int REQUEST_WRITE_STORAGE_PERMISSION = 201;

    private PhotoView ivFullScreenImage;
    private FloatingActionButton fabSaveImage;
    private String imageBase64String;
    private Bitmap currentBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        ivFullScreenImage = findViewById(R.id.ivFullScreenImage);
        fabSaveImage = findViewById(R.id.fabSaveImage);
        Toolbar toolbar = findViewById(R.id.toolbarFullScreenImage);

        setSupportActionBar(toolbar);
        // Habilitar el botón de "atrás" en la toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true); // Para que el ícono sea visible
            // Usar el navigationIcon como botón de cierre si se prefiere
            // toolbar.setNavigationIcon(R.drawable.ic_baseline_close_24);
        }
        // toolbar.setNavigationOnClickListener(v -> onBackPressed()); // Reemplazado por onOptionsItemSelected

        imageBase64String = getIntent().getStringExtra(EXTRA_IMAGE_BASE64);

        if (imageBase64String != null && !imageBase64String.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(imageBase64String, Base64.DEFAULT);
                currentBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                if (currentBitmap != null) {
                    ivFullScreenImage.setImageBitmap(currentBitmap);
                } else {
                    throw new IllegalArgumentException("Bitmap decodificado es nulo");
                }
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Error decodificando imagen Base64 para pantalla completa", e);
                Toast.makeText(this, "Error al cargar la imagen.", Toast.LENGTH_SHORT).show();
                ivFullScreenImage.setImageResource(R.drawable.ic_baseline_broken_image_24);
                fabSaveImage.setVisibility(View.GONE);
            }
        } else {
            Toast.makeText(this, "No se proporcionó imagen.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No se recibió imageBase64String en el Intent.");
            ivFullScreenImage.setImageResource(R.drawable.ic_baseline_broken_image_24);
            fabSaveImage.setVisibility(View.GONE);
        }

        fabSaveImage.setOnClickListener(v -> {
            if (currentBitmap != null) {
                checkStoragePermissionAndSaveImage();
            } else {
                Toast.makeText(this, "No hay imagen para guardar.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkStoragePermissionAndSaveImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveImageToGalleryScoped(currentBitmap); // Usar Scoped Storage para Q+
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                saveImageToGalleryLegacy(currentBitmap); // Método legado para pre-Q
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_STORAGE_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permiso de escritura concedido.");
                if (currentBitmap != null) {
                    saveImageToGalleryLegacy(currentBitmap); // Usar método legado después de obtener permiso
                }
            } else {
                Log.w(TAG, "Permiso de escritura denegado.");
                Toast.makeText(this, "Permiso de almacenamiento necesario para guardar la imagen.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Método para Android Q (API 29) y superior usando MediaStore y Scoped Storage
    private void saveImageToGalleryScoped(Bitmap bitmap) {
        if (bitmap == null) {
            handleSaveError("Bitmap nulo.");
            return;
        }

        String imageFileName = "CuadraSmart_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".jpg";
        ContentValues values = new ContentValues(); // Declarar aquí para que esté en el alcance del finally
        values.put(MediaStore.Images.Media.DISPLAY_NAME, imageFileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "CuadraSmart");
        values.put(MediaStore.Images.Media.IS_PENDING, 1);

        Uri imageUri = null;
        OutputStream fos = null;

        try {
            imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (imageUri == null) {
                throw new IOException("Failed to create new MediaStore record.");
            }
            fos = getContentResolver().openOutputStream(imageUri);
            if (fos == null) {
                throw new IOException("Failed to get output stream.");
            }
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos); // Calidad alta
            Log.i(TAG, "Imagen guardada (Scoped): " + imageUri.toString());
            Toast.makeText(this, "Imagen guardada en Galería/CuadraSmart", Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            handleSaveError(e.getMessage());
            if (imageUri != null) { // Si se creó el URI pero falló la escritura, intentar borrar la entrada pendiente
                getContentResolver().delete(imageUri, null, null);
            }
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error cerrando OutputStream (Scoped)", e);
                }
            }
            // Quitar el estado pendiente solo si el imageUri se creó y la operación fue exitosa o falló después de crear el URI
            if (imageUri != null) {
                values.clear(); // Limpiar 'values' antes de reutilizarla para el update
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                getContentResolver().update(imageUri, values, null, null);
            }
        }
    }

    // Método para versiones anteriores a Android Q (API 29)
    private void saveImageToGalleryLegacy(Bitmap bitmap) {
        if (bitmap == null) {
            handleSaveError("Bitmap nulo.");
            return;
        }

        String imageFileName = "CuadraSmart_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".jpg";
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CuadraSmart");

        if (!directory.exists() && !directory.mkdirs()) {
            handleSaveError("No se pudo crear directorio: " + directory.getAbsolutePath());
            return;
        }

        File file = new File(directory, imageFileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
            Log.i(TAG, "Imagen guardada (Legacy): " + file.getAbsolutePath());

            // Notificar a la galería sobre la nueva imagen
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(file);
            mediaScanIntent.setData(contentUri);
            this.sendBroadcast(mediaScanIntent);

            Toast.makeText(this, "Imagen guardada en Galería/CuadraSmart", Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            handleSaveError(e.getMessage());
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error cerrando FileOutputStream (Legacy)", e);
                }
            }
        }
    }

    private void handleSaveError(String errorMessage) {
        Log.e(TAG, "Error al guardar imagen: " + errorMessage);
        Toast.makeText(this, "Error al guardar imagen.", Toast.LENGTH_LONG).show();
    }


    // Manejar el botón de atrás de la Toolbar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // O finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}