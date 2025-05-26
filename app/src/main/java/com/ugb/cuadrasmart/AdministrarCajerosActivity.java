package com.ugb.cuadrasmart;

import android.app.AlertDialog;
// import android.content.ContentValues; // No se usa directamente aquí
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log; // Para Logs
import android.view.LayoutInflater;
import android.view.MenuItem; // Import para MenuItem
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Import para Toolbar
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton; // Import para FAB
import java.util.ArrayList;

public class AdministrarCajerosActivity extends AppCompatActivity {

    private static final String TAG = "AdminCajerosActivity"; // TAG para logs

    private RecyclerView rvCajeros;
    private FloatingActionButton fabAddCajero;
    private ArrayList<Cajero> cajeroList;
    private CajeroAdapter cajeroAdapter;
    private DatabaseHelper dbHelper;
    private Toolbar toolbarAdministrarCajeros; // Para la Toolbar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_administrar_cajeros); // Asegúrate que este es el layout correcto

        // Configuración de la Toolbar
        toolbarAdministrarCajeros = findViewById(R.id.toolbarAdministrarCajeros); // ID del XML
        setSupportActionBar(toolbarAdministrarCajeros);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.manage_cashiers); // Usar string resource
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Inicialización de vistas y componentes
        rvCajeros = findViewById(R.id.rvCajeros);
        fabAddCajero = findViewById(R.id.fabAddCajero);
        dbHelper = new DatabaseHelper(this);
        cajeroList = new ArrayList<>();

        // Configuración del RecyclerView
        rvCajeros.setLayoutManager(new LinearLayoutManager(this));
        cajeroAdapter = new CajeroAdapter(cajeroList);
        rvCajeros.setAdapter(cajeroAdapter);

        // Cargar cajeros
        loadCajeros();

        // Listener para el FAB
        fabAddCajero.setOnClickListener(view -> {
            Log.d(TAG, "FAB para agregar cajero presionado.");
            showAddCajeroDialog();
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

    // Carga la lista de cajeros (usuarios cuyo rol es "cajero") desde la base de datos
    private void loadCajeros() {
        cajeroList.clear();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            String selection = DatabaseContract.UserEntry.COLUMN_ROLE + "=?";
            String[] selectionArgs = {"cajero"};
            // Ordenar por nombre para una mejor visualización
            String orderBy = DatabaseContract.UserEntry.COLUMN_NAME + " ASC";

            cursor = db.query(DatabaseContract.UserEntry.TABLE_NAME,
                    null, // Proyection: null para todas las columnas
                    selection,
                    selectionArgs,
                    null, // groupBy
                    null, // having
                    orderBy); // orderBy

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int id = safeGetInt(cursor, DatabaseContract.UserEntry._ID);
                    String name = safeGetString(cursor, DatabaseContract.UserEntry.COLUMN_NAME);
                    String email = safeGetString(cursor, DatabaseContract.UserEntry.COLUMN_EMAIL);
                    cajeroList.add(new Cajero(id, name, email));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al cargar cajeros de la base de datos", e);
            Toast.makeText(this, "Error al cargar lista de cajeros", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // No cerrar db aquí si dbHelper lo maneja globalmente
        }
        cajeroAdapter.notifyDataSetChanged();
        Log.d(TAG, "Cajeros cargados: " + cajeroList.size());
    }

    // Muestra un diálogo para agregar un nuevo cajero
    private void showAddCajeroDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Agregar Nuevo Cajero"); // Título más descriptivo

        // Inflar el layout personalizado para el diálogo
        // Asumo que dialog_agregar_cajero.xml tiene EditTexts con estos IDs
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_agregar_cajero, (ViewGroup) findViewById(android.R.id.content), false);
        final EditText etNombre = viewInflated.findViewById(R.id.etNombreCajero);
        final EditText etCorreo = viewInflated.findViewById(R.id.etCorreoCajero);
        final EditText etPassword = viewInflated.findViewById(R.id.etPasswordCajero);

        builder.setView(viewInflated);
        builder.setPositiveButton("Agregar", (dialog, which) -> {
            String nombre = etNombre.getText().toString().trim();
            String correo = etCorreo.getText().toString().trim().toLowerCase(); // Guardar en minúsculas
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(correo) || TextUtils.isEmpty(password)) {
                Toast.makeText(AdministrarCajerosActivity.this, "Todos los campos son requeridos", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                Toast.makeText(AdministrarCajerosActivity.this, "Formato de correo inválido", Toast.LENGTH_SHORT).show();
                return;
            }
            if (dbHelper.checkIfUserExists(correo)) { // Reutilizar el método de DatabaseHelper
                Toast.makeText(AdministrarCajerosActivity.this, "El correo electrónico ya está registrado.", Toast.LENGTH_LONG).show();
                return;
            }


            boolean inserted = dbHelper.insertCajero(nombre, correo, password);
            if (inserted) {
                Toast.makeText(AdministrarCajerosActivity.this, "Cajero '" + nombre + "' agregado", Toast.LENGTH_SHORT).show();
                loadCajeros(); // Recargar la lista
            } else {
                Toast.makeText(AdministrarCajerosActivity.this, "Error al agregar cajero. El correo podría ya existir.", Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // Modelo para representar un cajero (sin cambios)
    public static class Cajero {
        public int id;
        public String name;
        public String email;

        public Cajero(int id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }
    }

    // Adaptador para el RecyclerView (sin cambios, pero se incluye para completitud)
    public class CajeroAdapter extends RecyclerView.Adapter<CajeroAdapter.CajeroViewHolder> {
        private ArrayList<Cajero> adapterCajeroList; // Renombrado para evitar confusión con la variable de la Activity
        public CajeroAdapter(ArrayList<Cajero> list) {
            this.adapterCajeroList = list;
        }
        @NonNull @Override
        public CajeroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cajero, parent, false);
            return new CajeroViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull CajeroViewHolder holder, int position) {
            final Cajero cajero = adapterCajeroList.get(position);
            holder.tvCajeroName.setText(cajero.name);
            holder.tvCajeroEmail.setText(cajero.email);
            holder.btnDeleteCajero.setOnClickListener(v -> {
                Log.d(TAG, "Botón eliminar presionado para cajero: " + cajero.name);
                confirmDeleteCajero(cajero);
            });
        }
        @Override public int getItemCount() { return adapterCajeroList.size(); }
        public class CajeroViewHolder extends RecyclerView.ViewHolder {
            TextView tvCajeroName, tvCajeroEmail;
            ImageButton btnDeleteCajero;
            public CajeroViewHolder(@NonNull View itemView) {
                super(itemView);
                tvCajeroName = itemView.findViewById(R.id.tvCajeroName);
                tvCajeroEmail = itemView.findViewById(R.id.tvCajeroEmail);
                btnDeleteCajero = itemView.findViewById(R.id.btnDeleteCajero);
            }
        }
    }

    // Confirma la eliminación del cajero mediante doble confirmación
    private void confirmDeleteCajero(Cajero cajero) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar Eliminación")
                .setMessage("¿Está seguro de eliminar al cajero: " + cajero.name + "? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    // Segunda confirmación (opcional pero buena práctica para delete)
                    new AlertDialog.Builder(AdministrarCajerosActivity.this)
                            .setTitle("¡Advertencia!")
                            .setMessage("La eliminación es permanente. ¿Desea continuar eliminando a " + cajero.name + "?")
                            .setPositiveButton("Sí, Eliminar", (dialogInterface, i) -> deleteCajeroFromDb(cajero))
                            .setNegativeButton("No", null)
                            .setIcon(android.R.drawable.ic_dialog_alert) // Icono de advertencia
                            .show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // Método para eliminar realmente el cajero de la DB
    private void deleteCajeroFromDb(Cajero cajero) {
        boolean success = dbHelper.deleteCajero(cajero.id); // Asume que deleteCajero solo necesita el ID
        if (success) {
            Toast.makeText(this, "Cajero '" + cajero.name + "' eliminado", Toast.LENGTH_SHORT).show();
            loadCajeros(); // Recargar la lista para reflejar el cambio
        } else {
            Toast.makeText(this, "Error al eliminar cajero '" + cajero.name + "'", Toast.LENGTH_SHORT).show();
        }
    }

    // Métodos auxiliares para obtener datos de forma segura desde el Cursor
    private int safeGetInt(Cursor cursor, String columnName) {
        if (cursor == null || cursor.isClosed()) return 0;
        try {
            int index = cursor.getColumnIndexOrThrow(columnName);
            return cursor.isNull(index) ? 0 : cursor.getInt(index);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Columna '" + columnName + "' no encontrada en safeGetInt.", e); return 0;
        }
    }

    private String safeGetString(Cursor cursor, String columnName) {
        if (cursor == null || cursor.isClosed()) return "";
        try {
            int index = cursor.getColumnIndexOrThrow(columnName);
            return cursor.isNull(index) ? "" : cursor.getString(index);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Columna '" + columnName + "' no encontrada en safeGetString.", e); return "";
        }
    }
}