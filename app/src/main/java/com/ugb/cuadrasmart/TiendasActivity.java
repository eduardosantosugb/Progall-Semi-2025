package com.ugb.cuadrasmart;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class TiendasActivity extends AppCompatActivity {

    private static final String TAG = "TiendasActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;

    private RecyclerView rvTiendasGrid;
    private TiendaCardAdapter adapter;
    private ArrayList<String> nombresDeTiendas;
    private DatabaseHelper dbHelper;
    private TextView tvNoTiendasDisponibles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tiendas);
        Log.d(TAG, "onCreate: Iniciando TiendasActivity.");

        Toolbar toolbar = findViewById(R.id.toolbarTiendas);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.select_store);
        }

        // Verificar y solicitar permisos primero
        checkAndRequestPermissions();

        rvTiendasGrid = findViewById(R.id.rvTiendasGrid);
        tvNoTiendasDisponibles = findViewById(R.id.tvNoTiendasDisponibles);
        dbHelper = new DatabaseHelper(this);
        nombresDeTiendas = new ArrayList<>();

        // Configurar RecyclerView
        // Usar 2 columnas para la cuadrícula. Puedes ajustar este número.
        // Para tabletas, podrías incluso hacerlo dinámico (e.g., 3 o 4 columnas).
        int numberOfColumns = 2;
        rvTiendasGrid.setLayoutManager(new GridLayoutManager(this, numberOfColumns));

        adapter = new TiendaCardAdapter(nombresDeTiendas, nombreTienda -> {
            Log.i(TAG, "Tienda seleccionada: " + nombreTienda);
            SharedPreferences prefs = getSharedPreferences("CuadraSmartPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("selected_store", nombreTienda);
            editor.apply();
            Log.d(TAG, "Tienda '" + nombreTienda + "' guardada en SharedPreferences.");

            Intent intent = new Intent(TiendasActivity.this, DashboardActivity.class);
            startActivity(intent);
            finish(); // Finalizar TiendasActivity para que no se pueda volver con el botón "Atrás"
        });
        rvTiendasGrid.setAdapter(adapter);

        Log.d(TAG, "onCreate: Configuración de RecyclerView completa.");
        // loadTiendasFromDb() se llamará en onResume
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Cargando/Recargando tiendas desde la DB.");
        loadTiendasFromDb(); // Cargar tiendas cada vez que la actividad se vuelve visible
    }

    private void loadTiendasFromDb() {
        Log.d(TAG, "loadTiendasFromDb: Iniciando carga...");
        nombresDeTiendas.clear(); // Limpiar lista actual antes de recargar
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.query(DatabaseContract.TiendaEntry.TABLE_NAME,
                    new String[]{DatabaseContract.TiendaEntry.COLUMN_NOMBRE},
                    null, null, null, null,
                    DatabaseContract.TiendaEntry.COLUMN_NOMBRE + " ASC"); // Ordenar alfabéticamente

            if (cursor != null) {
                Log.d(TAG, "loadTiendasFromDb: Cursor obtenido con " + cursor.getCount() + " filas.");
                if (cursor.moveToFirst()) {
                    int nombreColumnIndex = cursor.getColumnIndex(DatabaseContract.TiendaEntry.COLUMN_NOMBRE);
                    if (nombreColumnIndex != -1) {
                        do {
                            String nombre = cursor.getString(nombreColumnIndex);
                            if (!TextUtils.isEmpty(nombre)) {
                                nombresDeTiendas.add(nombre);
                                Log.d(TAG, "loadTiendasFromDb: Tienda añadida: " + nombre);
                            } else {
                                Log.w(TAG, "loadTiendasFromDb: Nombre de tienda vacío o nulo encontrado en DB.");
                            }
                        } while (cursor.moveToNext());
                    } else {
                        Log.e(TAG, "loadTiendasFromDb: Columna '" + DatabaseContract.TiendaEntry.COLUMN_NOMBRE + "' no encontrada en el cursor.");
                    }
                } else {
                    Log.d(TAG, "loadTiendasFromDb: El cursor está vacío, no hay tiendas.");
                }
            } else {
                Log.e(TAG, "loadTiendasFromDb: El cursor es null después de la consulta.");
            }
        } catch (Exception e) {
            Log.e(TAG, "loadTiendasFromDb: Error al cargar tiendas desde la base de datos", e);
            Toast.makeText(this, "Error al cargar la lista de tiendas.", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // No es necesario cerrar 'db' aquí si dbHelper maneja su ciclo de vida.
        }

        adapter.notifyDataSetChanged(); // Notificar al adaptador que los datos cambiaron
        Log.d(TAG, "loadTiendasFromDb: Carga completa. Tiendas en la lista: " + nombresDeTiendas.size());

        // Actualizar visibilidad de vistas
        if (nombresDeTiendas.isEmpty()) {
            tvNoTiendasDisponibles.setVisibility(View.VISIBLE);
            rvTiendasGrid.setVisibility(View.GONE);
            Log.d(TAG, "loadTiendasFromDb: No hay tiendas, mostrando mensaje.");
        } else {
            tvNoTiendasDisponibles.setVisibility(View.GONE);
            rvTiendasGrid.setVisibility(View.VISIBLE);
            Log.d(TAG, "loadTiendasFromDb: Mostrando lista de tiendas.");
        }
    }

    private void checkAndRequestPermissions() {
        String[] requiredPermissions = {
                Manifest.permission.CAMERA,
                // Para Android 10 (API 29) y superior, READ_EXTERNAL_STORAGE no da acceso amplio.
                // Para Android 13 (API 33) y superior, necesitas permisos más granulares como READ_MEDIA_IMAGES.
                // Por ahora, mantenemos READ_EXTERNAL_STORAGE para compatibilidad con versiones anteriores.
                // Si tu targetSDK es 33+, considera añadir READ_MEDIA_IMAGES, READ_MEDIA_AUDIO, READ_MEDIA_VIDEO.
                Manifest.permission.READ_EXTERNAL_STORAGE,
                // ACCESS_MEDIA_LOCATION es para acceder a metadatos de ubicación de fotos, si es necesario.
                // Manifest.permission.ACCESS_MEDIA_LOCATION
                Manifest.permission.RECORD_AUDIO // Lo necesitarás para el chat de audio
        };

        ArrayList<String> listPermissionsNeeded = new ArrayList<>();
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(permission);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            Log.d(TAG, "Solicitando permisos: " + listPermissionsNeeded.toString());
            ActivityCompat.requestPermissions(this,
                    listPermissionsNeeded.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        } else {
            Log.d(TAG, "Todos los permisos requeridos ya están concedidos.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    Log.w(TAG, "Permiso DENEGADO: " + permissions[i]);
                } else {
                    Log.i(TAG, "Permiso CONCEDIDO: " + permissions[i]);
                }
            }
            if (!allGranted) {
                // Podrías mostrar un diálogo explicando por qué los permisos son importantes
                // y ofrecer llevar al usuario a los ajustes de la app.
                Toast.makeText(this, "Algunos permisos son necesarios para el funcionamiento completo de la app.", Toast.LENGTH_LONG).show();
            }
        }
    }

    // --- Adaptador Interno para RecyclerView ---
    private static class TiendaCardAdapter extends RecyclerView.Adapter<TiendaCardAdapter.TiendaViewHolder> {
        private ArrayList<String> tiendasList; // Cambiado el nombre para claridad
        private OnTiendaClickListener clickListener; // Cambiado el nombre para claridad

        public interface OnTiendaClickListener {
            void onTiendaClicked(String nombreTienda);
        }

        TiendaCardAdapter(ArrayList<String> tiendas, OnTiendaClickListener listener) {
            this.tiendasList = tiendas;
            this.clickListener = listener;
        }

        @NonNull
        @Override
        public TiendaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tienda_card, parent, false);
            return new TiendaViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TiendaViewHolder holder, int position) {
            String nombreTienda = tiendasList.get(position);
            holder.tvNombreTienda.setText(nombreTienda);
            // Aquí podrías cambiar el ícono dinámicamente si tuvieras diferentes íconos por tienda
            // holder.ivTiendaIcon.setImageResource(R.drawable.algun_icono_especifico_tienda);

            holder.itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onTiendaClicked(nombreTienda);
                }
            });
        }

        @Override
        public int getItemCount() {
            return tiendasList.size();
        }

        static class TiendaViewHolder extends RecyclerView.ViewHolder {
            ImageView ivTiendaIcon;
            TextView tvNombreTienda;

            TiendaViewHolder(@NonNull View itemView) {
                super(itemView);
                ivTiendaIcon = itemView.findViewById(R.id.ivTiendaIcon);
                tvNombreTienda = itemView.findViewById(R.id.tvNombreTiendaCard); // ID del TextView en item_tienda_card.xml
            }
        }
    }
}