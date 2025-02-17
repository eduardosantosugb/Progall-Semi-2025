package com.ugb.cuadrasmart;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class HistorialActivity extends AppCompatActivity {

    private ListView lvHistorial;
    private DatabaseHelper dbHelper;
    private ArrayAdapter<String> adapter;
    private List<String> registrosList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial);

        // Referenciar el ListView
        lvHistorial = findViewById(R.id.lvHistorial);
        dbHelper = new DatabaseHelper(this);
        registrosList = new ArrayList<>();

        // Cargar registros desde la base de datos
        cargarRegistros();

        // Crear un ArrayAdapter para mostrar el resumen de cada registro
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, registrosList);
        lvHistorial.setAdapter(adapter);
    }

    // Método para cargar los registros desde la base de datos
    private void cargarRegistros() {
        Cursor cursor = dbHelper.obtenerRegistros();
        if (cursor != null && cursor.moveToFirst()) {
            // Opcional: Imprimir en el log las columnas disponibles para depuración
            String[] columnNames = cursor.getColumnNames();
            for (String name : columnNames) {
                Log.d("HistorialActivity", "Columna: " + name);
            }
            do {
                // Usar getColumnIndexOrThrow para asegurarnos de obtener el índice correcto
                String fecha = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_FECHA));
                String cajero = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_CAJERO));
                double discrepancia = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_DISCREPANCIA));
                // Crear un resumen simple del registro
                String resumen = "Fecha: " + fecha + "\nCajero: " + cajero + "\nDiscrepancia: " + discrepancia;
                registrosList.add(resumen);
            } while (cursor.moveToNext());
            cursor.close();
        }
    }
}