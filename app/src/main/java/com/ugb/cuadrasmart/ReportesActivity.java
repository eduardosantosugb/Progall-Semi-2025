package com.ugb.cuadrasmart;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceGray; // Para el color de fondo de la celda de encabezado
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.HorizontalAlignment;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ReportesActivity extends AppCompatActivity {

    private static final String TAG = "ReportesActivity";

    // UI Elements
    private EditText etFiltroFecha;
    private Spinner spinnerFiltroCajero;
    private Button btnFiltrar, btnGenerarPDF;
    private BarChart barChart;
    private Toolbar toolbarReportes;

    // Componentes
    private DatabaseHelper dbHelper;
    private SharedPreferences prefs;
    private String currentStore = "";

    // Listas para el gráfico
    private ArrayList<String> cashierLabels = new ArrayList<>();
    private List<BarEntry> entries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reportes);

        toolbarReportes = findViewById(R.id.toolbarReportes);
        setSupportActionBar(toolbarReportes);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.reports);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        etFiltroFecha = findViewById(R.id.etFiltroFecha);
        spinnerFiltroCajero = findViewById(R.id.spinnerFiltroCajero);
        btnFiltrar = findViewById(R.id.btnFiltrar);
        btnGenerarPDF = findViewById(R.id.btnGenerarPDF);
        barChart = findViewById(R.id.barChart);

        dbHelper = new DatabaseHelper(this);
        prefs = getSharedPreferences("CuadraSmartPrefs", MODE_PRIVATE);

        currentStore = prefs.getString("selected_store", "");
        if (TextUtils.isEmpty(currentStore)) {
            Toast.makeText(this, "Error: Tienda no seleccionada.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        configureBarChart();
        populateCajeroSpinner();
        loadReportData(null, null);

        etFiltroFecha.setOnClickListener(v -> showDatePicker());

        btnFiltrar.setOnClickListener(v -> {
            String fechaFiltro = etFiltroFecha.getText().toString().trim();
            String cajeroFiltro = spinnerFiltroCajero.getSelectedItemPosition() > 0 ?
                    spinnerFiltroCajero.getSelectedItem().toString() : null;
            loadReportData(fechaFiltro, cajeroFiltro);
        });

        btnGenerarPDF.setOnClickListener(v -> generatePDF());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void configureBarChart() {
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        barChart.getDescription().setEnabled(false);
        barChart.setMaxVisibleValueCount(50);
        barChart.setPinchZoom(false);
        barChart.setDrawGridBackground(false);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(cashierLabels));

        ValueFormatter currencyFormatter = new ValueFormatter() {
            private final DecimalFormat mFormat = new DecimalFormat("'$'###,##0.00");
            @Override
            public String getFormattedValue(float value) {
                return mFormat.format(value);
            }
        };

        barChart.getAxisLeft().setValueFormatter(currencyFormatter);
        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setExtraBottomOffset(10f);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (DatePicker view, int year, int month, int dayOfMonth) -> {
            String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            etFiltroFecha.setText(dateStr);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void populateCajeroSpinner() {
        ArrayList<String> cajeroNames = new ArrayList<>();
        cajeroNames.add("Todos");
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            String query = "SELECT DISTINCT " + DatabaseContract.TurnoEntry.COLUMN_CAJERO +
                    " FROM " + DatabaseContract.TurnoEntry.TABLE_NAME +
                    " WHERE " + DatabaseContract.TurnoEntry.COLUMN_TIENDA + " = ?" +
                    " ORDER BY " + DatabaseContract.TurnoEntry.COLUMN_CAJERO + " ASC";
            cursor = db.rawQuery(query, new String[]{currentStore});
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String name = safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_CAJERO);
                    if (!TextUtils.isEmpty(name)) {
                        cajeroNames.add(name);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al poblar spinner de cajeros", e);
            Toast.makeText(this, "Error cargando cajeros", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null) cursor.close();
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cajeroNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFiltroCajero.setAdapter(adapter);
    }

    private void loadReportData(String fechaFiltro, String cajeroFiltro) {
        cashierLabels.clear();
        entries.clear();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = dbHelper.getReadableDatabase();
            StringBuilder queryBuilder = new StringBuilder();
            ArrayList<String> argsList = new ArrayList<>();

            queryBuilder.append("SELECT ").append(DatabaseContract.TurnoEntry.COLUMN_CAJERO)
                    .append(", SUM(").append(DatabaseContract.TurnoEntry.COLUMN_DISCREPANCIA).append(") as total_discrepancia ")
                    .append("FROM ").append(DatabaseContract.TurnoEntry.TABLE_NAME)
                    .append(" WHERE ").append(DatabaseContract.TurnoEntry.COLUMN_TIENDA).append(" = ?");
            argsList.add(currentStore);

            if (!TextUtils.isEmpty(fechaFiltro)) {
                queryBuilder.append(" AND ").append(DatabaseContract.TurnoEntry.COLUMN_FECHA).append(" = ?");
                argsList.add(fechaFiltro);
            }
            if (!TextUtils.isEmpty(cajeroFiltro) && !cajeroFiltro.equals("Todos")) {
                queryBuilder.append(" AND ").append(DatabaseContract.TurnoEntry.COLUMN_CAJERO).append(" = ?");
                argsList.add(cajeroFiltro);
            }
            queryBuilder.append(" GROUP BY ").append(DatabaseContract.TurnoEntry.COLUMN_CAJERO)
                    .append(" ORDER BY total_discrepancia DESC");

            String[] selectionArgs = argsList.toArray(new String[0]);
            cursor = db.rawQuery(queryBuilder.toString(), selectionArgs);

            if (cursor != null && cursor.moveToFirst()) {
                int index = 0;
                do {
                    String cashier = safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_CAJERO);
                    float totalDiscrepancy = (float) safeGetDouble(cursor, "total_discrepancia");
                    entries.add(new BarEntry(index, totalDiscrepancy));
                    cashierLabels.add(cashier);
                    index++;
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al cargar datos del reporte", e);
            Toast.makeText(this, "Error al cargar reporte: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (!entries.isEmpty()) {
            BarDataSet dataSet = new BarDataSet(entries, "Discrepancia por Cajero");
            dataSet.setColor(ContextCompat.getColor(this, R.color.colorPrimary));
            dataSet.setValueTextColor(ContextCompat.getColor(this, R.color.textColorPrimary));
            dataSet.setValueTextSize(10f);
            dataSet.setValueFormatter(new ValueFormatter() {
                private final DecimalFormat mFormat = new DecimalFormat("'$'###,##0.00");
                @Override
                public String getFormattedValue(float value) {
                    return mFormat.format(value);
                }
            });

            BarData barData = new BarData(dataSet);
            barData.setBarWidth(0.8f);

            barChart.setData(barData);
            barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(cashierLabels));
            barChart.getXAxis().setLabelCount(cashierLabels.size(), false);

            barChart.notifyDataSetChanged();
            barChart.invalidate();
            barChart.animateY(1200);
            Log.d(TAG, "Gráfico actualizado con " + entries.size() + " entradas.");
        } else {
            barChart.clear();
            barChart.invalidate();
            Toast.makeText(this, "No hay datos para mostrar en el reporte.", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Gráfico limpiado, no hay datos.");
        }
    }

    private void generatePDF() {
        if (entries.isEmpty() || cashierLabels.isEmpty()) {
            Toast.makeText(this, "No hay datos para generar el PDF del reporte.", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "Reporte_Discrepancias_" + currentStore.replace(" ", "_") + "_" + System.currentTimeMillis() + ".pdf";
        Uri pdfUri = null;
        OutputStream outStream = null;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + File.separator + "CuadraSmart");
                pdfUri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (pdfUri == null) throw new IOException("Error al crear URI del PDF con MediaStore");
                outStream = getContentResolver().openOutputStream(pdfUri);
            } else {
                File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File appFolder = new File(downloadsFolder, "CuadraSmart");
                if (!appFolder.exists() && !appFolder.mkdirs()) throw new IOException("No se pudo crear carpeta CuadraSmart");
                File pdfFile = new File(appFolder, fileName);
                outStream = new FileOutputStream(pdfFile);
            }

            if (outStream == null) throw new IOException("No se pudo obtener OutputStream para el PDF");

            writePdfContent(outStream);
            Toast.makeText(this, "PDF guardado en Descargas/CuadraSmart", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e(TAG, "Error al generar PDF de reporte", e);
            Toast.makeText(this, "Error al generar PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && pdfUri != null) {
                try { getContentResolver().delete(pdfUri, null, null); } catch (Exception ignored) {}
            }
        } finally {
            try { if (outStream != null) outStream.close(); } catch (IOException ignored) {}
        }
    }

    private void writePdfContent(OutputStream outStream) throws Exception {
        PdfWriter writer = new PdfWriter(outStream);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);
        DecimalFormat currencyFormat = new DecimalFormat("'$'###,##0.00");

        document.add(new Paragraph("Reporte de Discrepancias - " + currentStore)
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15));

        Bitmap chartBitmap = getBitmapFromView(barChart);
        if (chartBitmap != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            chartBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] bitmapData = stream.toByteArray();
            Image chartImage = new Image(ImageDataFactory.create(bitmapData));

            // --- CORRECCIÓN APLICADA AQUÍ ---
            float availableWidth = pdfDoc.getDefaultPageSize().getWidth() - document.getLeftMargin() - document.getRightMargin();
            chartImage.scaleToFit(availableWidth, Float.MAX_VALUE); // Escalar para ajustar al ancho

            document.add(chartImage.setHorizontalAlignment(HorizontalAlignment.CENTER).setMarginBottom(10));
            Log.d(TAG, "Imagen del gráfico añadida al PDF, escalada para ajustarse.");
        } else {
            Log.e(TAG, "No se pudo capturar el gráfico como Bitmap.");
            document.add(new Paragraph("[Error al generar imagen del gráfico]").setFontColor(com.itextpdf.kernel.colors.ColorConstants.RED).setFontSize(10));
        }

        if (!cashierLabels.isEmpty()) {
            document.add(new Paragraph("Datos del Reporte")
                    .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                    .setFontSize(12).setMarginTop(10).setMarginBottom(5));

            float[] columnWidths = {200, 150};
            Table table = new Table(UnitValue.createPercentArray(columnWidths)).useAllAvailableWidth();
            table.setHorizontalAlignment(HorizontalAlignment.CENTER);
            table.setMarginBottom(10);

            table.addHeaderCell(createHeaderCell("Cajero"));
            table.addHeaderCell(createHeaderCell("Total Discrepancia"));

            for (int i = 0; i < cashierLabels.size(); i++) {
                table.addCell(createCell(cashierLabels.get(i)));
                table.addCell(createCell(currencyFormat.format(entries.get(i).getY())).setTextAlignment(TextAlignment.RIGHT));
            }
            document.add(table);
        }
        document.close();
    }

    public static Bitmap getBitmapFromView(View view) {
        if (view.getWidth() <= 0 || view.getHeight() <= 0) {
            view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
            if (view.getWidth() <= 0 || view.getHeight() <= 0) {
                Log.e(TAG, "La vista sigue sin dimensiones después de medir/layout. No se puede capturar Bitmap.");
                return null;
            }
        }
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null) {
            bgDrawable.draw(canvas);
        } else {
            canvas.drawColor(Color.WHITE);
        }
        view.draw(canvas);
        return returnedBitmap;
    }

    private Cell createHeaderCell(String text) {
        try {
            return new Cell().add(new Paragraph(text)
                            .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                            .setFontSize(10)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(new DeviceGray(0.85f))
                    .setPadding(5);
        } catch (IOException e) {
            Log.e(TAG, "Error creando fuente para celda de encabezado", e);
            return new Cell().add(new Paragraph(text));
        }
    }

    private Cell createCell(String text) {
        try {
            return new Cell().add(new Paragraph(text != null ? text : "")
                    .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                    .setFontSize(9)).setPadding(4);
        } catch (IOException e) {
            Log.e(TAG, "Error creando fuente para celda de contenido", e);
            return new Cell().add(new Paragraph(text != null ? text : ""));
        }
    }

    private String safeGetString(Cursor cursor, String columnName) {
        if (cursor == null || cursor.isClosed()) return "";
        try {
            int index = cursor.getColumnIndexOrThrow(columnName);
            return cursor.isNull(index) ? "" : cursor.getString(index);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Columna no encontrada en cursor: " + columnName, e);
            return "";
        }
    }
    private double safeGetDouble(Cursor cursor, String columnName) {
        if (cursor == null || cursor.isClosed()) return 0.0;
        try {
            int index = cursor.getColumnIndexOrThrow(columnName);
            return cursor.isNull(index) ? 0.0 : cursor.getDouble(index);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Columna no encontrada en cursor: " + columnName, e);
            return 0.0;
        }
    }
}