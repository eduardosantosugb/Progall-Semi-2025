package com.ugb.cuadrasmart;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log; // Import Log
import android.view.LayoutInflater;
import android.view.MenuItem; // Import MenuItem
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull; // Import NonNull
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Import Toolbar
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;

public class AdministrarTiendasActivity extends AppCompatActivity {

    private static final String TAG = "AdminTiendasActivity";

    private RecyclerView rvTiendas;
    private TiendaAdapter tiendaAdapter;
    private ArrayList<Tienda> tiendaList;
    private DatabaseHelper dbHelper;
    private FloatingActionButton btnAgregarTienda;
    private Toolbar toolbar; // Añadir referencia al Toolbar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_administrar_tiendas);
        Log.d(TAG, "onCreate: Iniciando actividad.");

        // --- Inicialización de Vistas y Objetos ---
        rvTiendas = findViewById(R.id.rvTiendas);
        btnAgregarTienda = findViewById(R.id.btnAgregarTienda);
        toolbar = findViewById(R.id.toolbarAdministrarTiendas); // Encontrar el Toolbar
        dbHelper = new DatabaseHelper(this);
        tiendaList = new ArrayList<>();
        Log.d(TAG, "onCreate: Vistas y helpers inicializados.");

        // --- Configuración del Toolbar ---
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Administrar Tiendas");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Mostrar botón atrás
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                Log.d(TAG, "onCreate: Toolbar configurado con botón Atrás.");
            } else {
                Log.w(TAG, "onCreate: getSupportActionBar() es null.");
            }
        } else {
            Log.w(TAG, "onCreate: Toolbar (toolbarAdministrarTiendas) no encontrado.");
        }

        // --- Validación del RecyclerView ---
        if (rvTiendas == null) {
            Log.e(TAG, "onCreate: ¡ERROR! RecyclerView (rvTiendas) no encontrado.");
            Toast.makeText(this, "Error crítico: RecyclerView no encontrado", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // --- Configuración del RecyclerView ---
        rvTiendas.setLayoutManager(new LinearLayoutManager(this));
        tiendaAdapter = new TiendaAdapter(tiendaList);
        rvTiendas.setAdapter(tiendaAdapter);
        Log.d(TAG, "onCreate: RecyclerView configurado.");

        // --- Carga inicial de datos ---
        loadTiendas();

        // --- Listener para el botón de agregar ---
        btnAgregarTienda.setOnClickListener(view -> {
            Log.d(TAG, "Botón Agregar Tienda presionado.");
            mostrarDialogoAgregarTienda();
        });
    }

    // --- Manejo del Botón Atrás/Up ---
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Log.d(TAG, "onOptionsItemSelected: Botón Atrás/Up presionado.");
            finish(); // Cierra esta actividad
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    // --- Resto de métodos (loadTiendas, mostrarDialogoAgregar/Editar, agregar/actualizar/eliminarTienda, etc.) ---
    // --- Usar la versión con logs detallados que te proporcioné anteriormente ---
    // --- Copia y pega aquí todos los métodos desde loadTiendas() hasta safeGetString() ---
    // --- de la versión anterior con logs.                                        ---

    // Carga las tiendas desde la base de datos (con logs)
    private void loadTiendas() {
        Log.d(TAG, "loadTiendas: Iniciando carga de tiendas...");
        tiendaList.clear();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        int tiendasCargadas = 0;

        try {
            db = dbHelper.getReadableDatabase();
            Log.d(TAG, "loadTiendas: Obtenida base de datos legible.");
            cursor = db.query(DatabaseContract.TiendaEntry.TABLE_NAME,
                    null, null, null, null, null, DatabaseContract.TiendaEntry.COLUMN_NOMBRE + " ASC");

            if (cursor != null) {
                Log.d(TAG, "loadTiendas: Consulta ejecutada. Número de filas: " + cursor.getCount());
                if (cursor.moveToFirst()) {
                    Log.d(TAG, "loadTiendas: Iterando sobre el cursor...");
                    do {
                        int id = safeGetInt(cursor, DatabaseContract.TiendaEntry._ID);
                        String nombre = safeGetString(cursor, DatabaseContract.TiendaEntry.COLUMN_NOMBRE);
                        Log.d(TAG, "loadTiendas: Leyendo fila -> ID: " + id + ", Nombre: '" + nombre + "'");

                        if (id > 0 && !TextUtils.isEmpty(nombre)) {
                            tiendaList.add(new Tienda(id, nombre));
                            tiendasCargadas++;
                            Log.d(TAG, "loadTiendas: Tienda válida añadida a la lista. Tamaño actual: " + tiendaList.size());
                        } else {
                            Log.w(TAG, "loadTiendas: Tienda inválida omitida (ID: " + id + ", Nombre: '" + nombre + "')");
                        }
                    } while (cursor.moveToNext());
                } else {
                    Log.d(TAG, "loadTiendas: El cursor está vacío.");
                }
            } else {
                Log.e(TAG, "loadTiendas: ¡ERROR! El cursor es null después de la consulta.");
            }
        } catch (Exception e) {
            Log.e(TAG, "loadTiendas: Excepción al cargar tiendas: " + e.getMessage(), e);
            Toast.makeText(this, "Error al cargar tiendas: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            if (cursor != null) {
                cursor.close();
                Log.d(TAG, "loadTiendas: Cursor cerrado.");
            }
        }

        Log.d(TAG, "loadTiendas: Carga finalizada. Total tiendas en lista: " + tiendaList.size());
        // ¡IMPORTANTE! Asegurarse que el adaptador se notifica DESPUÉS de que la lista se actualiza.
        if (tiendaAdapter != null) {
            tiendaAdapter.notifyDataSetChanged();
            Log.d(TAG, "loadTiendas: notifyDataSetChanged() llamado.");
        } else {
            Log.e(TAG, "loadTiendas: ¡ERROR! tiendaAdapter es null al intentar notificar.");
        }

        if (tiendasCargadas == 0) {
            Log.d(TAG, "loadTiendas: No se cargó ninguna tienda válida.");
        }
    }

    // Muestra el diálogo para agregar una nueva tienda (con logs)
    private void mostrarDialogoAgregarTienda() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Agregar Nueva Tienda");
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_agregar_tienda, (ViewGroup) findViewById(android.R.id.content), false);
        final EditText etNombreTienda = viewInflated.findViewById(R.id.etNombreTienda);

        builder.setView(viewInflated);
        builder.setPositiveButton("Agregar", (dialog, which) -> {
            String nombre = etNombreTienda.getText().toString().trim();
            if (!TextUtils.isEmpty(nombre)) {
                Log.d(TAG, "Dialogo Agregar: Intentando agregar tienda '" + nombre + "'");
                agregarTienda(nombre);
            } else {
                Toast.makeText(AdministrarTiendasActivity.this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
        Log.d(TAG, "Dialogo Agregar Tienda mostrado.");
    }

    // Agrega la tienda en la base de datos y recarga la lista (con logs)
    private void agregarTienda(String nombre) {
        boolean inserted = dbHelper.insertTienda(nombre); // El helper ya tiene logs
        if (inserted) {
            Toast.makeText(this, "Tienda '" + nombre + "' agregada", Toast.LENGTH_SHORT).show();
            loadTiendas(); // Recargar la lista
        } else {
            Toast.makeText(this, "Error al agregar tienda '" + nombre + "'. ¿Ya existe?", Toast.LENGTH_LONG).show();
        }
    }

    // Muestra el diálogo para editar el nombre de una tienda existente (con logs)
    private void mostrarDialogoEditarTienda(final Tienda tienda) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Editar Nombre de Tienda");
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_editar_tienda, (ViewGroup) findViewById(android.R.id.content), false);
        final EditText etNuevoNombreTienda = viewInflated.findViewById(R.id.etNuevoNombreTienda);

        etNuevoNombreTienda.setText(tienda.nombre);

        builder.setView(viewInflated);
        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String nuevoNombre = etNuevoNombreTienda.getText().toString().trim();
            if (!TextUtils.isEmpty(nuevoNombre)) {
                if (!nuevoNombre.equals(tienda.nombre)) {
                    Log.d(TAG, "Dialogo Editar: Intentando actualizar tienda ID " + tienda.id + " de '" + tienda.nombre + "' a '" + nuevoNombre + "'");
                    actualizarTienda(tienda.id, nuevoNombre);
                } else {
                    Log.d(TAG, "Dialogo Editar: El nombre no cambió.");
                    dialog.cancel();
                }
            } else {
                Toast.makeText(AdministrarTiendasActivity.this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
        Log.d(TAG, "Dialogo Editar Tienda mostrado para ID " + tienda.id);
    }

    // Actualiza el nombre de la tienda en la base de datos (con logs)
    private void actualizarTienda(int id, String nuevoNombre) {
        boolean success = dbHelper.updateTienda(id, nuevoNombre); // El helper ya tiene logs
        if (success) {
            Toast.makeText(this, "Tienda actualizada a '" + nuevoNombre + "'", Toast.LENGTH_SHORT).show();
            loadTiendas(); // Recargar la lista
        } else {
            Toast.makeText(this, "Error al actualizar tienda. ¿Nombre duplicado?", Toast.LENGTH_LONG).show();
        }
    }

    // Confirma la eliminación de una tienda con doble confirmación (con logs)
    private void confirmarEliminarTienda(final Tienda tienda) {
        Log.d(TAG, "confirmarEliminarTienda: Mostrando diálogo para tienda ID " + tienda.id + " ('" + tienda.nombre + "')");
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Tienda")
                .setMessage("¿Está seguro de eliminar la tienda '" + tienda.nombre + "'? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    Log.d(TAG, "confirmarEliminarTienda: Primera confirmación OK.");
                    new AlertDialog.Builder(AdministrarTiendasActivity.this)
                            .setTitle("Confirmar Eliminación")
                            .setMessage("¡ADVERTENCIA! Se eliminará la tienda '" + tienda.nombre + "'. ¿Desea continuar?")
                            .setPositiveButton("Sí, Eliminar", (dialogInterface, i) -> {
                                Log.d(TAG, "confirmarEliminarTienda: Segunda confirmación OK.");
                                eliminarTienda(tienda);
                            })
                            .setNegativeButton("No", (d, i) -> Log.d(TAG, "confirmarEliminarTienda: Segunda confirmación cancelada."))
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                })
                .setNegativeButton("Cancelar", (d, i) -> Log.d(TAG, "confirmarEliminarTienda: Primera confirmación cancelada."))
                .show();
    }

    // Elimina la tienda y recarga la lista (con logs)
    private void eliminarTienda(Tienda tienda) {
        boolean deleted = dbHelper.deleteTienda(tienda.id); // El helper ya tiene logs
        if (deleted) {
            Toast.makeText(this, "Tienda '" + tienda.nombre + "' eliminada", Toast.LENGTH_SHORT).show();
            loadTiendas(); // Recargar la lista
        } else {
            Toast.makeText(this, "Error al eliminar tienda '" + tienda.nombre + "'", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Modelo de datos para Tienda ---
    public static class Tienda {
        public int id;
        public String nombre;
        public Tienda(int id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }
    }

    // --- Adaptador para el RecyclerView de tiendas (con logs) ---
    public class TiendaAdapter extends RecyclerView.Adapter<TiendaAdapter.TiendaViewHolder> {
        private ArrayList<Tienda> adapterTiendaList;

        public TiendaAdapter(ArrayList<Tienda> list) {
            this.adapterTiendaList = (list != null) ? list : new ArrayList<>();
            Log.d(TAG, "TiendaAdapter: Constructor llamado con lista tamaño " + this.adapterTiendaList.size());
        }

        @NonNull @Override
        public TiendaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Log.d(TAG, "TiendaAdapter: onCreateViewHolder");
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tienda, parent, false);
            return new TiendaViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TiendaViewHolder holder, int position) {
            if (adapterTiendaList == null || position >= adapterTiendaList.size()) {
                Log.e(TAG, "TiendaAdapter: onBindViewHolder - Índice fuera de límites o lista nula. Pos: " + position + ", Tamaño: " + (adapterTiendaList == null ? "null" : adapterTiendaList.size()));
                return; // Evitar crash
            }
            final Tienda tiendaActual = adapterTiendaList.get(position);
            Log.d(TAG, "TiendaAdapter: onBindViewHolder - Pos: " + position + ", Tienda: '" + tiendaActual.nombre + "'");
            holder.tvTiendaNombre.setText(tiendaActual.nombre);
            holder.btnEliminarTienda.setOnClickListener(v -> confirmarEliminarTienda(tiendaActual));
            holder.btnEditarTienda.setOnClickListener(v -> mostrarDialogoEditarTienda(tiendaActual));
        }

        @Override
        public int getItemCount() {
            int count = (adapterTiendaList == null) ? 0 : adapterTiendaList.size();
            Log.d(TAG, "TiendaAdapter: getItemCount -> " + count);
            return count;
        }

        public class TiendaViewHolder extends RecyclerView.ViewHolder {
            TextView tvTiendaNombre;
            ImageButton btnEditarTienda;
            ImageButton btnEliminarTienda;
            public TiendaViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTiendaNombre = itemView.findViewById(R.id.tvTiendaNombre);
                btnEditarTienda = itemView.findViewById(R.id.btnEditarTienda);
                btnEliminarTienda = itemView.findViewById(R.id.btnEliminarTienda);
                if (tvTiendaNombre == null || btnEditarTienda == null || btnEliminarTienda == null) {
                    Log.e(TAG, "ViewHolder: Uno o más elementos no encontrados en item_tienda.xml!");
                }
            }
        }
    }

    // --- Métodos auxiliares safeGet... (sin cambios) ---
    private int safeGetInt(Cursor cursor, String columnName) {
        try {
            int index = cursor.getColumnIndexOrThrow(columnName);
            return cursor.getInt(index);
        } catch (Exception e) {
            Log.w(TAG, "safeGetInt: Error leyendo INT: " + columnName, e);
            return 0;
        }
    }
    private String safeGetString(Cursor cursor, String columnName) {
        try {
            int index = cursor.getColumnIndexOrThrow(columnName);
            return cursor.isNull(index) ? "" : cursor.getString(index);
        } catch (Exception e) {
            Log.w(TAG, "safeGetString: Error leyendo STRING: " + columnName, e);
            return "";
        }
    }
}