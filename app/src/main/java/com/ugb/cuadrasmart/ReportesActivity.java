package com.ugb.cuadrasmart;




import android.app.DatePickerDialog;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ReportesActivity extends AppCompatActivity {

    private EditText etFechaInicio, etFechaFin;
    private Spinner spinnerCajeros;
    private Button btnAplicarFiltro, btnGenerarPDF;
    private BarChart barChart;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reportes);

        etFechaInicio = findViewById(R.id.etFechaInicio);
        etFechaFin = findViewById(R.id.etFechaFin);
        spinnerCajeros = findViewById(R.id.spinnerCajeros);
        btnAplicarFiltro = findViewById(R.id.btnAplicarFiltro);
        btnGenerarPDF = findViewById(R.id.btnGenerarPDF);
        barChart = findViewById(R.id.barChart);
        dbHelper = new DatabaseHelper(this);

        // Configurar DatePicker para ambos EditText de fecha
        configurarPickerFecha(etFechaInicio);
        configurarPickerFecha(etFechaFin);

        // Cargar los cajeros en el Spinner
        cargarSpinnerCajeros();

        // Botón para aplicar el filtro y cargar datos en el gráfico
        btnAplicarFiltro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadFilteredData();
            }
        });

        // Botón para generar el PDF a partir del gráfico
        btnGenerarPDF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                generarPdfDesdeGrafica();
            }
        });
    }

    // Método para asignar un DatePickerDialog a un EditText
    private void configurarPickerFecha(final EditText editText) {
        editText.setFocusable(false);
        editText.setClickable(true);
        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar cal = Calendar.getInstance();
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH);
                int day = cal.get(Calendar.DAY_OF_MONTH);
                DatePickerDialog dpd = new DatePickerDialog(ReportesActivity.this,
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

    // Cargar los nombres de cajeros en el Spinner (incluye opción "Todos")
    private void cargarSpinnerCajeros() {
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
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, listaCajeros);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCajeros.setAdapter(adapter);
    }

    // Método para cargar datos filtrados en el gráfico
    private void loadFilteredData() {
        String fechaInicio = etFechaInicio.getText().toString().trim();
        String fechaFin = etFechaFin.getText().toString().trim();
        String cajeroSeleccionado = spinnerCajeros.getSelectedItem().toString();

        if (TextUtils.isEmpty(fechaInicio) || TextUtils.isEmpty(fechaFin)) {
            Toast.makeText(this, "Por favor selecciona ambas fechas", Toast.LENGTH_SHORT).show();
            return;
        }

        // Realizar la consulta en la base de datos.
        // Nota: Esta consulta asume que la columna "fecha" se almacena como cadena en formato dd/mm/yyyy.
        String query;
        String[] args;
        if (cajeroSeleccionado.equals("Todos")) {
            query = "SELECT * FROM " + DatabaseHelper.TABLE_REGISTROS + " WHERE fecha BETWEEN ? AND ?";
            args = new String[]{fechaInicio, fechaFin};
        } else {
            query = "SELECT * FROM " + DatabaseHelper.TABLE_REGISTROS + " WHERE fecha BETWEEN ? AND ? AND " +
                    DatabaseHelper.COLUMN_REG_CAJERO + " = ?";
            args = new String[]{fechaInicio, fechaFin, cajeroSeleccionado};
        }
        Cursor cursor = dbHelper.getReadableDatabase().rawQuery(query, args);

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String fecha = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_FECHA));
                double discrepancia = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_DISCREPANCIA));
                entries.add(new BarEntry(index, (float) discrepancia));
                labels.add(fecha);
                index++;
            } while (cursor.moveToNext());
            cursor.close();
        }

        if (entries.isEmpty()) {
            Toast.makeText(this, "No se encontraron registros en el periodo seleccionado", Toast.LENGTH_SHORT).show();
            barChart.clear();
            barChart.invalidate();
            return;
        }

        setupBarChart();
        BarDataSet dataSet = new BarDataSet(entries, "Discrepancia");
        dataSet.setColor(getResources().getColor(R.color.purple_500));
        dataSet.setValueTextSize(12f);
        BarData data = new BarData(dataSet);
        data.setBarWidth(0.9f);
        barChart.setData(data);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.invalidate();
    }

    // Configurar el BarChart
    private void setupBarChart() {
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setPinchZoom(false);
        barChart.setMaxVisibleValueCount(50);
        XAxis xAxis = barChart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
    }

    // Método para generar un PDF a partir del gráfico
    private void generarPdfDesdeGrafica() {
        Bitmap bitmap = barChart.getChartBitmap();
        if (bitmap == null) {
            Toast.makeText(this, "No se pudo obtener la imagen de la gráfica", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        canvas.drawBitmap(bitmap, 0, 0, null);
        pdfDocument.finishPage(page);

        try {
            File pdfDir = getExternalFilesDir(null);
            if (pdfDir == null) {
                Toast.makeText(this, "No se pudo acceder al almacenamiento", Toast.LENGTH_SHORT).show();
                return;
            }
            File pdfFile = new File(pdfDir, "reporte_filtrado.pdf");
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