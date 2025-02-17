package com.ugb.cuadrasmart;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class AdministrarCajerosActivity extends AppCompatActivity {

    private ListView lvCajeros;
    private Button btnAgregarCajero;
    private DatabaseHelper dbHelper;
    private ArrayAdapter<Cajero> adapter;
    private List<Cajero> cajeroList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_administrar_cajeros);

        lvCajeros = findViewById(R.id.lvCajeros);
        btnAgregarCajero = findViewById(R.id.btnAgregarCajero);
        dbHelper = new DatabaseHelper(this);
        cajeroList = new ArrayList<>();

        cargarCajeros();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, cajeroList);
        lvCajeros.setAdapter(adapter);

        // Botón para agregar un nuevo cajero
        btnAgregarCajero.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarDialogoAgregarCajero();
            }
        });

        // Long click en un elemento de la lista para eliminar el cajero
        lvCajeros.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Cajero cajero = cajeroList.get(position);
                confirmarEliminacion(cajero);
                return true;
            }
        });
    }

    // Método para cargar los cajeros desde la base de datos
    private void cargarCajeros() {
        cajeroList.clear();
        Cursor cursor = dbHelper.obtenerCajeros();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CAJ_ID));
                String nombre = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CAJ_NOMBRE));
                cajeroList.add(new Cajero(id, nombre));
            } while (cursor.moveToNext());
            cursor.close();
        }
    }

    // Muestra un diálogo para agregar un nuevo cajero
    private void mostrarDialogoAgregarCajero() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Agregar Cajero");

        final EditText input = new EditText(this);
        input.setHint("Nombre del Cajero");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Agregar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String nombre = input.getText().toString().trim();
                if (nombre.isEmpty() || nombre.length() < 3) {
                    Toast.makeText(AdministrarCajerosActivity.this, "Ingresa un nombre válido (mínimo 3 caracteres)", Toast.LENGTH_SHORT).show();
                } else {
                    boolean agregado = dbHelper.agregarCajero(nombre);
                    if (agregado) {
                        Toast.makeText(AdministrarCajerosActivity.this, "Cajero agregado", Toast.LENGTH_SHORT).show();
                        cargarCajeros();
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(AdministrarCajerosActivity.this, "Error al agregar cajero", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    // Método para confirmar la eliminación de un cajero con doble confirmación
    private void confirmarEliminacion(final Cajero cajero) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirmación");
        builder.setMessage("¿Está seguro de eliminar a " + cajero.getNombre() + "?");
        builder.setPositiveButton("Sí", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Segunda confirmación
                new AlertDialog.Builder(AdministrarCajerosActivity.this)
                        .setTitle("Confirmación Final")
                        .setMessage("Esta acción es irreversible. ¿Desea eliminar definitivamente a " + cajero.getNombre() + "?")
                        .setPositiveButton("Sí, eliminar", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog2, int which2) {
                                boolean eliminado = dbHelper.eliminarCajero(cajero.getId());
                                if (eliminado) {
                                    Toast.makeText(AdministrarCajerosActivity.this, "Cajero eliminado", Toast.LENGTH_SHORT).show();
                                    cargarCajeros();
                                    adapter.notifyDataSetChanged();
                                } else {
                                    Toast.makeText(AdministrarCajerosActivity.this, "Error al eliminar cajero", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }
}