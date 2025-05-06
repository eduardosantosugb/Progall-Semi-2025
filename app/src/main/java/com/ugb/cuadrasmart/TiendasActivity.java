package com.ugb.cuadrasmart;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor; // Necesario para Cursor
import android.database.sqlite.SQLiteDatabase; // Necesario para SQLiteDatabase
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log; // Para Logs
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.Toast;

import androidx.annotation.NonNull; // Para @NonNull
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Para Toolbar
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList; // Usar ArrayList en lugar de String[]
import java.util.List; // Para la lista de permisos

public class TiendasActivity extends AppCompatActivity {

    private static final String TAG = "TiendasActivity"; // TAG para logs
    private static final int PERMISSION_REQUEST_CODE = 100;

    private GridView gridStores;
    // private final String[] tiendas = { ... }; // <--- ELIMINAR ESTA LÍNEA
    private ArrayList<String> nombresDeTiendas; // Usar ArrayList para tiendas dinámicas
    private ArrayAdapter<String> adapter; // Referencia al adaptador para actualizarlo
    private DatabaseHelper dbHelper; // Para acceder a la base de datos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tiendas);
        Log.d(TAG, "onCreate: Iniciando actividad.");

        // --- Configurar Toolbar (Opcional, pero bueno para consistencia) ---
        Toolbar toolbar = findViewById(R.id.toolbarTiendas); // Asume que tienes este ID en activity_tiendas.xml
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(R.string.select_store); // Usar string resource
            }
        }

        checkAndRequestPermissions(); // Solicitar permisos

        // --- Inicializar componentes ---
        gridStores = findViewById(R.id.gridStores);
        dbHelper = new DatabaseHelper(this); // Inicializar DatabaseHelper
        nombresDeTiendas = new ArrayList<>(); // Inicializar la lista

        // --- Configurar Adaptador ---
        adapter = new ArrayAdapter<>(this,
                R.layout.item_grid_tienda, // <--- USA UN LAYOUT PERSONALIZADO PARA MEJOR APARIENCIA
                // android.R.layout.simple_list_item_1, // Layout simple por defecto
                R.id.tvNombreTiendaGrid, // ID del TextView dentro de item_grid_tienda.xml
                nombresDeTiendas);
        gridStores.setAdapter(adapter);

        // --- Listener para seleccionar tienda ---
        gridStores.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position < nombresDeTiendas.size()) { // Chequeo de seguridad
                    String selectedStore = nombresDeTiendas.get(position);
                    Log.d(TAG, "Tienda seleccionada: " + selectedStore);

                    SharedPreferences prefs = getSharedPreferences("CuadraSmartPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("selected_store", selectedStore);
                    editor.apply();

                    Intent intent = new Intent(TiendasActivity.this, DashboardActivity.class);
                    startActivity(intent);
                    finish(); // Terminar TiendasActivity
                } else {
                    Log.e(TAG, "Error: Posición de clic fuera de los límites de la lista de tiendas.");
                }
            }
        });

        // Cargar tiendas desde la base de datos
        // Se llama en onResume para que se actualice si volvemos de AdministrarTiendasActivity
        // loadTiendasFromDb(); // Puedes llamarlo aquí o solo en onResume
        Log.d(TAG, "onCreate: Configuración completa.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Cargando/Recargando tiendas desde la DB.");
        loadTiendasFromDb(); // Cargar/recargar tiendas cada vez que la actividad se vuelve visible
    }

    // Método para cargar tiendas desde la base de datos
    private void loadTiendasFromDb() {
        Log.d(TAG, "loadTiendasFromDb: Iniciando carga desde DB...");
        nombresDeTiendas.clear(); // Limpiar lista actual
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();
            // Usar el método getAllTiendas de DatabaseHelper si existe y es adecuado,
            // o hacer la consulta directamente aquí.
            cursor = db.query(DatabaseContract.TiendaEntry.TABLE_NAME,
                    new String[]{DatabaseContract.TiendaEntry.COLUMN_NOMBRE}, // Solo necesitamos el nombre
                    null, null, null, null,
                    DatabaseContract.TiendaEntry.COLUMN_NOMBRE + " ASC"); // Ordenar alfabéticamente

            if (cursor != null) {
                Log.d(TAG, "loadTiendasFromDb: Consulta ejecutada. Filas: " + cursor.getCount());
                if (cursor.moveToFirst()) {
                    int nombreColumnIndex = cursor.getColumnIndex(DatabaseContract.TiendaEntry.COLUMN_NOMBRE);
                    if (nombreColumnIndex != -1) { // Verificar si la columna existe
                        do {
                            String nombre = cursor.getString(nombreColumnIndex);
                            if (!TextUtils.isEmpty(nombre)) {
                                nombresDeTiendas.add(nombre);
                                Log.d(TAG, "loadTiendasFromDb: Tienda añadida: " + nombre);
                            }
                        } while (cursor.moveToNext());
                    } else {
                        Log.e(TAG, "loadTiendasFromDb: Columna '" + DatabaseContract.TiendaEntry.COLUMN_NOMBRE + "' no encontrada.");
                    }
                } else {
                    Log.d(TAG, "loadTiendasFromDb: No hay tiendas en la base de datos.");
                }
            } else {
                Log.e(TAG, "loadTiendasFromDb: Cursor es null después de la consulta.");
            }
        } catch (Exception e) {
            Log.e(TAG, "loadTiendasFromDb: Error al cargar tiendas desde DB", e);
            Toast.makeText(this, "Error al cargar lista de tiendas.", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // dbHelper.close(); // No cerrar aquí si el helper es compartido o usado en otros hilos.
        }

        adapter.notifyDataSetChanged(); // Notificar al adaptador que los datos cambiaron
        Log.d(TAG, "loadTiendasFromDb: Carga completa. Tiendas en lista: " + nombresDeTiendas.size());

        if (nombresDeTiendas.isEmpty()) {
            // Opcional: Mostrar un mensaje si no hay tiendas disponibles para seleccionar
            Toast.makeText(this, "No hay tiendas configuradas. Por favor, agregue tiendas desde la administración.", Toast.LENGTH_LONG).show();
        }
    }

    // --- Métodos de Permisos (sin cambios) ---
    private void checkAndRequestPermissions() {
        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                // ACCESS_MEDIA_LOCATION es para Android 10+ si necesitas acceso a metadatos de ubicación de fotos
                // Si tu targetSdk es 30+, READ_EXTERNAL_STORAGE podría no ser suficiente para todo.
                // Para Android 13+ (API 33), necesitas permisos más granulares como READ_MEDIA_IMAGES.
                // Por ahora, mantenemos los que tenías.
                Manifest.permission.ACCESS_MEDIA_LOCATION
        };

        ArrayList<String> listPermissionsNeeded = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(permission);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            Log.d(TAG, "Solicitando permisos: " + listPermissionsNeeded.toString());
            ActivityCompat.requestPermissions(this,
                    listPermissionsNeeded.toArray(new String[0]), // Convertir ArrayList a Array
                    PERMISSION_REQUEST_CODE);
        } else {
            Log.d(TAG, "Todos los permisos ya están concedidos.");
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
                    Log.w(TAG, "Permiso denegado: " + permissions[i]);
                } else {
                    Log.d(TAG, "Permiso concedido: " + permissions[i]);
                }
            }
            if (!allGranted) {
                Toast.makeText(this, "Algunos permisos son necesarios para el funcionamiento completo.", Toast.LENGTH_LONG).show();
            }
        }
    }
}