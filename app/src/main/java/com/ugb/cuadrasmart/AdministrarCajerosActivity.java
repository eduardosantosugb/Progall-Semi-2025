package com.ugb.cuadrasmart;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;

public class AdministrarCajerosActivity extends AppCompatActivity {

    private RecyclerView rvCajeros;
    private FloatingActionButton fabAddCajero;
    private ArrayList<Cajero> cajeroList;
    private CajeroAdapter cajeroAdapter;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_administrar_cajeros);

        rvCajeros = findViewById(R.id.rvCajeros);
        fabAddCajero = findViewById(R.id.fabAddCajero);
        dbHelper = new DatabaseHelper(this);
        cajeroList = new ArrayList<>();

        rvCajeros.setLayoutManager(new LinearLayoutManager(this));
        cajeroAdapter = new CajeroAdapter(cajeroList);
        rvCajeros.setAdapter(cajeroAdapter);

        loadCajeros();

        fabAddCajero.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddCajeroDialog();
            }
        });
    }

    // Carga la lista de cajeros (usuarios cuyo rol es "cajero") desde la base de datos
    private void loadCajeros() {
        cajeroList.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = DatabaseContract.UserEntry.COLUMN_ROLE + "=?";
        String[] selectionArgs = {"cajero"};
        Cursor cursor = db.query(DatabaseContract.UserEntry.TABLE_NAME,
                null,
                selection,
                selectionArgs,
                null,
                null,
                null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = safeGetInt(cursor, DatabaseContract.UserEntry._ID);
                String name = safeGetString(cursor, DatabaseContract.UserEntry.COLUMN_NAME);
                String email = safeGetString(cursor, DatabaseContract.UserEntry.COLUMN_EMAIL);
                cajeroList.add(new Cajero(id, name, email));
            } while (cursor.moveToNext());
            cursor.close();
        }
        cajeroAdapter.notifyDataSetChanged();
    }

    // Muestra un diálogo para agregar un nuevo cajero
    private void showAddCajeroDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Agregar Cajero");
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_agregar_cajero, null);
        final EditText etNombre = viewInflated.findViewById(R.id.etNombreCajero);
        final EditText etCorreo = viewInflated.findViewById(R.id.etCorreoCajero);
        final EditText etPassword = viewInflated.findViewById(R.id.etPasswordCajero);
        builder.setView(viewInflated);
        builder.setPositiveButton("Agregar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String nombre = etNombre.getText().toString().trim();
                String correo = etCorreo.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(correo) || TextUtils.isEmpty(password)) {
                    Toast.makeText(AdministrarCajerosActivity.this, "Todos los campos son requeridos", Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean inserted = dbHelper.insertCajero(nombre, correo, password);
                if (inserted) {
                    Toast.makeText(AdministrarCajerosActivity.this, "Cajero agregado", Toast.LENGTH_SHORT).show();
                    loadCajeros();
                } else {
                    Toast.makeText(AdministrarCajerosActivity.this, "Error al agregar cajero", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // Modelo para representar un cajero
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

    // Adaptador para el RecyclerView
    public class CajeroAdapter extends RecyclerView.Adapter<CajeroAdapter.CajeroViewHolder> {

        private ArrayList<Cajero> cajeroList;

        public CajeroAdapter(ArrayList<Cajero> list) {
            this.cajeroList = list;
        }

        @NonNull
        @Override
        public CajeroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cajero, parent, false);
            return new CajeroViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CajeroViewHolder holder, int position) {
            final Cajero cajero = cajeroList.get(position);
            holder.tvCajeroName.setText(cajero.name);
            holder.tvCajeroEmail.setText(cajero.email);
            holder.btnDeleteCajero.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmDeleteCajero(cajero);
                }
            });
        }

        @Override
        public int getItemCount() {
            return cajeroList.size();
        }

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
                .setMessage("¿Está seguro de eliminar al cajero: " + cajero.name + "?")
                .setPositiveButton("Eliminar", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new AlertDialog.Builder(AdministrarCajerosActivity.this)
                                .setTitle("Confirmar Eliminación")
                                .setMessage("Esta acción es irreversible. ¿Desea continuar?")
                                .setPositiveButton("Sí", (dialogInterface, i) -> deleteCajero(cajero))
                                .setNegativeButton("No", null)
                                .show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deleteCajero(Cajero cajero) {
        boolean success = dbHelper.deleteCajero(cajero.id);
        if (success) {
            Toast.makeText(this, "Cajero eliminado", Toast.LENGTH_SHORT).show();
            loadCajeros();
        } else {
            Toast.makeText(this, "Error al eliminar cajero", Toast.LENGTH_SHORT).show();
        }
    }

    // Métodos auxiliares para obtener datos de forma segura desde el Cursor
    private int safeGetInt(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndex(columnName);
        return index >= 0 ? cursor.getInt(index) : 0;
    }

    private String safeGetString(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndex(columnName);
        return index >= 0 ? cursor.getString(index) : "";
    }
}
