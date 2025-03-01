package com.ugb.cuadrasmart;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
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

    public static final String PREFS_NAME = "CuadraSmartPrefs";
    public static final String KEY_SELECTED_TIENDA = "selected_tienda";

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

        configurarPickerFecha(etFechaHistorial);
        cargarSpinnerHistorialCajeros();

        btnAplicarHistorialFiltro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cargarRegistrosFiltrados();
            }
        });

        btnGenerarHistorialPDF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generarPdfHistorial();
            }
        });
    }

    // Configura un DatePickerDialog para el EditText de fecha
    private void configurarPickerFecha(final EditText editText) {
        editText.setFocusable(false);
        editText.setClickable(true);
        editText.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Calendar cal = Calendar.getInstance();
                new DatePickerDialog(HistorialActivity.this, new DatePickerDialog.OnDateSetListener(){
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        String fechaStr = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year);
                        editText.setText(fechaStr);
                    }
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
    }

    // Cargar el Spinner con una lista de cajeros (puedes obtenerlos desde la BD o usar datos ficticios)
    private void cargarSpinnerHistorialCajeros() {
        List<String> listaCajeros = new ArrayList<>();
        // Ejemplo: obtenemos cajeros de la base de datos
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(
                "SELECT " + DatabaseHelper.COLUMN_USR_EMAIL + " FROM " + DatabaseHelper.TABLE_USUARIOS + " WHERE " + DatabaseHelper.COLUMN_USR_ROL + " = ?",
                new String[]{"cajero"});
        if (cursor != null && cursor.moveToFirst()){
            do {
                listaCajeros.add(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USR_EMAIL)));
            } while(cursor.moveToNext());
            cursor.close();
        }
        if (listaCajeros.isEmpty()){
            listaCajeros.add("Todos");
        } else {
            listaCajeros.add(0, "Todos");
        }
        ArrayAdapter<String> adapterSpinner = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, listaCajeros);
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHistorialCajeros.setAdapter(adapterSpinner);
    }

    // Cargar registros filtrados en base a la fecha y cajero seleccionados
    private void cargarRegistrosFiltrados() {
        String fechaFiltro = etFechaHistorial.getText().toString().trim();
        String cajeroFiltro = spinnerHistorialCajeros.getSelectedItem().toString();

        if (TextUtils.isEmpty(fechaFiltro)) {
            Toast.makeText(this, getString(R.string.historial_error_select_fecha), Toast.LENGTH_SHORT).show();
            return;
        }

        // Consulta: suponemos que la columna "fecha" se almacena en formato dd/mm/yyyy
        String query;
        String[] args;
        if (cajeroFiltro.equals("Todos")) {
            query = "SELECT * FROM " + DatabaseHelper.TABLE_REGISTROS + " WHERE " + DatabaseHelper.COLUMN_REG_FECHA + " = ?";
            args = new String[]{fechaFiltro};
        } else {
            query = "SELECT * FROM " + DatabaseHelper.TABLE_REGISTROS + " WHERE " + DatabaseHelper.COLUMN_REG_FECHA + " = ? AND " + DatabaseHelper.COLUMN_REG_CAJERO + " = ?";
            args = new String[]{fechaFiltro, cajeroFiltro};
        }
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(query, args);
        registrosList.clear();
        if (cursor != null && cursor.moveToFirst()){
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
                double ventasEsperadas = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_VENTAS_ESPERADAS));
                double discrepancia = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_DISCREPANCIA));
                String justificacion = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_JUSTIFICACION));
                String evidencia = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_EVIDENCE));
                String tienda = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_TIENDA));

                registrosList.add(new Registro(id, fecha, horaInicio, horaCierre, numeroCaja, cajero,
                        billetes, monedas, cheques, ventasEsperadas, discrepancia, justificacion, evidencia, tienda));

            } while(cursor.moveToNext());
            cursor.close();
        }
        if (registrosList.isEmpty()){
            Toast.makeText(this, getString(R.string.historial_no_records), Toast.LENGTH_SHORT).show();
        }
        adapter = new HistorialAdapter(this, registrosList);
        lvHistorial.setAdapter(adapter);
    }

    // Genera un PDF con los registros del historial (este ejemplo es básico; se puede mejorar para formatear el contenido)
    private void generarPdfHistorial() {
        if (registrosList.isEmpty()){
            Toast.makeText(this, "No hay datos para generar PDF", Toast.LENGTH_SHORT).show();
            return;
        }
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        android.graphics.Canvas canvas = page.getCanvas();
        int yPosition = 40;
        int lineHeight = 20;
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setColor(android.graphics.Color.BLACK);
        paint.setTextSize(16);
        canvas.drawText("Historial de Registros", 40, yPosition, paint);
        yPosition += lineHeight * 2;

        for (Registro registro : registrosList) {
            String linea1 = "Tienda: " + registro.getTienda() + " | Fecha: " + registro.getFecha();
            String linea2 = "Entrada: " + registro.getHoraInicio() + " - Salida: " + registro.getHoraCierre();
            String linea3 = "Caja: " + registro.getNumeroCaja() + " | Cajero: " + registro.getCajero();
            String linea4 = "Discrepancia: " + registro.getDiscrepancia() + " (" + registro.getEstado() + ")";
            canvas.drawText(linea1, 40, yPosition, paint);
            yPosition += lineHeight;
            canvas.drawText(linea2, 40, yPosition, paint);
            yPosition += lineHeight;
            canvas.drawText(linea3, 40, yPosition, paint);
            yPosition += lineHeight;
            canvas.drawText(linea4, 40, yPosition, paint);
            yPosition += lineHeight;
            if (!TextUtils.isEmpty(registro.getJustificacion()) && !registro.getJustificacion().equalsIgnoreCase("no hubo justificación")) {
                canvas.drawText("Justificación: " + registro.getJustificacion(), 40, yPosition, paint);
                yPosition += lineHeight;
            }
            yPosition += lineHeight; // Espacio extra entre registros
            // Nota: Si la altura excede la página, habría que agregar nuevas páginas
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
