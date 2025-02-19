package com.ugb.cuadrasmart;

import android.app.DatePickerDialog;
import android.database.Cursor;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HistorialActivity extends AppCompatActivity {

    private EditText etFechaHistorial;
    private Spinner spinnerHistorialCajeros;
    private Button btnAplicarHistorialFiltro, btnGenerarHistorialPDF;
    private ListView lvHistorial;
    private DatabaseHelper dbHelper;
    private HistorialAdapter adapter;
    private List<Registro> registrosList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial);

        etFechaHistorial = findViewById(R.id.etFechaHistorial);
        spinnerHistorialCajeros = findViewById(R.id.spinnerHistorialCajeros);
        btnAplicarHistorialFiltro = findViewById(R.id.btnAplicarHistorialFiltro);
        btnGenerarHistorialPDF = findViewById(R.id.btnGenerarHistorialPDF);
        lvHistorial = findViewById(R.id.lvHistorial);
        dbHelper = new DatabaseHelper(this);
        registrosList = new ArrayList<>();

        // Configurar DatePicker para el filtro de fecha
        configurarPickerFecha(etFechaHistorial);

        // Cargar Spinner de cajeros
        cargarSpinnerHistorialCajeros();

        // Configurar botón para aplicar el filtro
        btnAplicarHistorialFiltro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cargarRegistrosFiltrados();
            }
        });

        // Configurar botón para generar PDF del historial
        btnGenerarHistorialPDF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generarPdfHistorial();
            }
        });
    }

    // Configurar DatePicker para el campo de fecha
    private void configurarPickerFecha(final EditText editText) {
        editText.setFocusable(false);
        editText.setClickable(true);
        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar cal = Calendar.getInstance();
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH);
                int day = cal.get(Calendar.DAY_OF_MONTH);
                DatePickerDialog dpd = new DatePickerDialog(HistorialActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker datePicker, int selectedYear, int selectedMonth, int selectedDay) {
                                String dateStr = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                                editText.setText(dateStr);
                            }
                        }, year, month, day);
                dpd.show();
            }
        });
    }

    // Cargar Spinner de cajeros para el historial
    private void cargarSpinnerHistorialCajeros() {
        List<String> listaCajeros = new ArrayList<>();
        Cursor cursor = dbHelper.obtenerCajeros();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int index = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CAJ_NOMBRE);
                listaCajeros.add(cursor.getString(index));
            } while (cursor.moveToNext());
            cursor.close();
        }
        if (listaCajeros.isEmpty()) {
            listaCajeros.add("Todos");
        } else {
            listaCajeros.add(0, "Todos");
        }
        ArrayAdapter<String> adapterSpinner = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, listaCajeros);
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHistorialCajeros.setAdapter(adapterSpinner);
    }

    // Cargar registros filtrados de la base de datos en el ListView
    private void cargarRegistrosFiltrados() {
        String fechaFiltro = etFechaHistorial.getText().toString().trim();
        String cajeroFiltro = spinnerHistorialCajeros.getSelectedItem().toString();

        if (TextUtils.isEmpty(fechaFiltro)) {
            Toast.makeText(this, "Por favor selecciona una fecha", Toast.LENGTH_SHORT).show();
            return;
        }

        // Consulta: suponemos que la columna "fecha" en la tabla está en formato dd/mm/yyyy
        String query;
        String[] args;
        if (cajeroFiltro.equals("Todos")) {
            query = "SELECT * FROM " + DatabaseHelper.TABLE_REGISTROS + " WHERE fecha = ?";
            args = new String[]{fechaFiltro};
        } else {
            query = "SELECT * FROM " + DatabaseHelper.TABLE_REGISTROS + " WHERE fecha = ? AND " +
                    DatabaseHelper.COLUMN_REG_CAJERO + " = ?";
            args = new String[]{fechaFiltro, cajeroFiltro};
        }
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(query, args);

        registrosList.clear();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_ID));
                String fecha = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_FECHA));
                String horaInicio = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_HORA_INICIO));
                String horaCierre = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_HORA_CIERRE));
                int numeroCaja = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_NUMERO_CAJA));
                String cajero = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_CAJERO));
                double billetes = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_BILLETES));
                double monedas = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_MONEDAS));
                double cheques = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_CHEQUES));
                double discrepancia = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_DISCREPANCIA));
                String justificacion = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_JUSTIFICACION));
                String evidencia = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_EVIDENCIA));

                registrosList.add(new Registro(id, fecha, horaInicio, horaCierre, numeroCaja, cajero,
                        billetes, monedas, cheques, discrepancia, justificacion, evidencia));
            } while (cursor.moveToNext());
            cursor.close();
        }

        if (registrosList.isEmpty()) {
            Toast.makeText(this, "No se encontraron registros para la fecha seleccionada", Toast.LENGTH_SHORT).show();
        }

        adapter = new HistorialAdapter(this, registrosList);
        lvHistorial.setAdapter(adapter);
    }

    // Método para generar un PDF a partir del historial mostrado
    private void generarPdfHistorial() {
        if (registrosList.isEmpty()) {
            Toast.makeText(this, "No hay datos para generar PDF", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument pdfDocument = new PdfDocument();
        // Definimos una página de tamaño A4 (ajusta según necesidad)
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        android.graphics.Canvas canvas = page.getCanvas();
        int yPosition = 40;
        final int lineHeight = 20;

        // Escribimos un título
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColor(android.graphics.Color.BLACK);
        paint.setTextSize(16);
        canvas.drawText("Historial de Registros", 40, yPosition, paint);
        yPosition += lineHeight * 2;

        // Iterar sobre cada registro y escribir sus datos
        for (Registro registro : registrosList) {
            String line = "Fecha: " + registro.getFecha() +
                    ", Entrada: " + registro.getHoraInicio() +
                    ", Salida: " + registro.getHoraCierre() +
                    ", Caja: " + registro.getNumeroCaja() +
                    ", Cajero: " + registro.getCajero();
            canvas.drawText(line, 40, yPosition, paint);
            yPosition += lineHeight;
            line = "Discrepancia: " + registro.getDiscrepancia() +
                    " (" + registro.getEstado() + ")";
            canvas.drawText(line, 40, yPosition, paint);
            yPosition += lineHeight;
            if (registro.getJustificacion() != null && !registro.getJustificacion().isEmpty()
                    && !registro.getJustificacion().equalsIgnoreCase("no hubo justificación")) {
                canvas.drawText("Justificación: " + registro.getJustificacion(), 40, yPosition, paint);
                yPosition += lineHeight;
            }
            if (registro.getEvidencia() != null && !registro.getEvidencia().isEmpty()) {
                canvas.drawText("Foto: " + registro.getEvidencia(), 40, yPosition, paint);
                yPosition += lineHeight;
            }
            yPosition += lineHeight; // espacio entre registros
            // Si se supera el alto de la página, se debería agregar una nueva página (simplificado aquí)
        }

        pdfDocument.finishPage(page);

        try {
            File pdfDir = getExternalFilesDir(null);
            if (pdfDir == null) {
                Toast.makeText(this, "No se pudo acceder al almacenamiento", Toast.LENGTH_SHORT).show();
                return;
            }
            File pdfFile = new File(pdfDir, "historial_registros.pdf");
            FileOutputStream fos = new FileOutputStream(pdfFile);
            pdfDocument.writeTo(fos);
            fos.close();
            Toast.makeText(this, "PDF generado en: " + pdfFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al generar PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        pdfDocument.close();
    }
}