package com.ugb.cuadrasmart;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap; // Necesario para Bitmap
import android.graphics.Canvas; // Necesario para Canvas
import android.graphics.Color; // Necesario para Color
import android.graphics.drawable.Drawable; // Necesario para Drawable
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View; // Necesario para View en getBitmapFromView
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory; // Necesario para imagen PDF
import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image; // Necesario para imagen PDF
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.HorizontalAlignment; // Necesario para alinear tabla/imagen
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue; // Necesario para escalar imagen/tabla

import java.io.ByteArrayOutputStream; // Necesario para convertir Bitmap a bytes
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

    // Componentes
    private DatabaseHelper dbHelper;
    private SharedPreferences prefs;
    private String currentStore = "";

    // Listas para el gráfico
    private ArrayList<String> cashierLabels = new ArrayList<>();
    private List<BarEntry> entries = new ArrayList<>(); // Usar List<BarEntry>

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reportes);

        // --- Inicialización ---
        etFiltroFecha = findViewById(R.id.etFiltroFecha);
        spinnerFiltroCajero = findViewById(R.id.spinnerFiltroCajero);
        btnFiltrar = findViewById(R.id.btnFiltrar);
        btnGenerarPDF = findViewById(R.id.btnGenerarPDF);
        barChart = findViewById(R.id.barChart);

        dbHelper = new DatabaseHelper(this);
        prefs = getSharedPreferences("CuadraSmartPrefs", MODE_PRIVATE);

        // Obtener tienda actual
        currentStore = prefs.getString("selected_store", "");
        if (TextUtils.isEmpty(currentStore)) {
            Toast.makeText(this, "Error: Tienda no seleccionada. Volviendo a selección.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // --- Configuración UI y carga inicial ---
        configureBarChart();
        populateCajeroSpinner(); // Llena spinner con cajeros de la tienda actual
        loadReportData(null, null); // Carga datos para la tienda actual

        // --- Listeners ---
        etFiltroFecha.setOnClickListener(v -> showDatePicker());
        btnFiltrar.setOnClickListener(v -> {
            String fechaFiltro = etFiltroFecha.getText().toString().trim();
            String cajeroFiltro = spinnerFiltroCajero.getSelectedItemPosition() > 0 ?
                    spinnerFiltroCajero.getSelectedItem().toString() : null;
            loadReportData(fechaFiltro, cajeroFiltro);
        });
        btnGenerarPDF.setOnClickListener(v -> generatePDF());
    }

    // Configuración inicial del gráfico de barras
    private void configureBarChart() {
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        barChart.getDescription().setEnabled(false); // Sin descripción
        barChart.setMaxVisibleValueCount(50); // Límite de barras visibles
        barChart.setPinchZoom(false); // Deshabilitar zoom con dos dedos
        barChart.setDrawGridBackground(false);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f); // Intervalo mínimo 1
        // xAxis.setLabelCount(7); // Dejar que la librería calcule o ajustar dinámicamente
        xAxis.setValueFormatter(new IndexAxisValueFormatter(cashierLabels)); // Usar nombres de cajero

        // Formateador para el eje Y (valores de discrepancia)
        ValueFormatter currencyFormatter = new ValueFormatter() {
            private final DecimalFormat mFormat = new DecimalFormat("'$'###,##0.00");
            @Override
            public String getFormattedValue(float value) {
                return mFormat.format(value);
            }
        };

        barChart.getAxisLeft().setValueFormatter(currencyFormatter); // Eje izquierdo con formato moneda
        // barChart.getAxisLeft().setAxisMinimum(0f); // Comentado para permitir valores negativos
        barChart.getAxisRight().setEnabled(false); // Deshabilitar eje derecho

        barChart.getLegend().setEnabled(false); // Ocultar leyenda si solo hay una serie
        barChart.setExtraBottomOffset(10f); // Más espacio abajo para etiquetas X
    }


    // Muestra el selector de fecha
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (DatePicker view, int year, int month, int dayOfMonth) -> {
            String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            etFiltroFecha.setText(dateStr);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    // Llena el Spinner con cajeros que tienen registros EN LA TIENDA ACTUAL
    private void populateCajeroSpinner() {
        ArrayList<String> cajeroNames = new ArrayList<>();
        cajeroNames.add("Todos");
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            String query = "SELECT DISTINCT " + DatabaseContract.TurnoEntry.COLUMN_CAJERO +
                    " FROM " + DatabaseContract.TurnoEntry.TABLE_NAME +
                    " WHERE " + DatabaseContract.TurnoEntry.COLUMN_TIENDA + " = ?";
            cursor = db.rawQuery(query, new String[]{currentStore});
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String name = cursor.getString(0);
                    if (!TextUtils.isEmpty(name)) {
                        cajeroNames.add(name);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al poblar spinner de cajeros", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cajeroNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFiltroCajero.setAdapter(adapter);
    }

    /**
     * Carga los datos del reporte (discrepancia agrupada por cajero)
     * filtrando por tienda y opcionalmente por fecha/cajero.
     */
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
            argsList.add(currentStore); // Filtro obligatorio por tienda

            if (!TextUtils.isEmpty(fechaFiltro)) {
                queryBuilder.append(" AND ").append(DatabaseContract.TurnoEntry.COLUMN_FECHA).append(" = ?");
                argsList.add(fechaFiltro);
            }
            if (!TextUtils.isEmpty(cajeroFiltro) && !cajeroFiltro.equals("Todos")) {
                queryBuilder.append(" AND ").append(DatabaseContract.TurnoEntry.COLUMN_CAJERO).append(" = ?");
                argsList.add(cajeroFiltro);
            }
            queryBuilder.append(" GROUP BY ").append(DatabaseContract.TurnoEntry.COLUMN_CAJERO)
                    .append(" ORDER BY total_discrepancia DESC"); // Ordenar por discrepancia

            String[] selectionArgs = argsList.toArray(new String[0]);

            // Log para depuración
            Log.d(TAG, "Tienda actual: " + currentStore);
            Log.d(TAG, "Query: " + queryBuilder.toString());
            Log.d(TAG, "Args: " + argsList.toString());

            cursor = db.rawQuery(queryBuilder.toString(), selectionArgs);

            if (cursor != null) {
                Log.d(TAG, "Número de filas devueltas: " + cursor.getCount());
                int index = 0;
                if (cursor.moveToFirst()) {
                    do {
                        String cashier = safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_CAJERO);
                        float totalDiscrepancy = (float) safeGetDouble(cursor, "total_discrepancia");
                        Log.d(TAG, "Cajero: " + cashier + ", Discrepancia: " + totalDiscrepancy);

                        entries.add(new BarEntry(index, totalDiscrepancy));
                        cashierLabels.add(cashier);
                        index++;
                    } while (cursor.moveToNext());
                }
            } else {
                Log.e(TAG, "El cursor es null después de la consulta.");
            }

            Log.d(TAG, "Tamaño de entries: " + entries.size());
            Log.d(TAG, "Tamaño de cashierLabels: " + cashierLabels.size());

        } catch (Exception e) {
            Log.e(TAG, "Error al cargar datos del reporte", e);
            Toast.makeText(this, "Error al cargar reporte: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // --- Actualizar el gráfico ---
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
            // Ajustar dinámicamente el número de etiquetas visibles en el eje X
            int labelCount = cashierLabels.size();
            barChart.getXAxis().setLabelCount(labelCount);
            // Forzar redibujo de etiquetas si son demasiadas
            // barChart.getXAxis().setGranularityEnabled(true);
            // barChart.getXAxis().setGranularity(1f);

            barChart.notifyDataSetChanged(); // Notificar al gráfico sobre los cambios
            barChart.invalidate(); // Redibujar el gráfico
            barChart.animateY(1200);
            Log.d(TAG, "Gráfico actualizado con " + entries.size() + " entradas.");
        } else {
            barChart.clear(); // Limpiar gráfico si no hay datos
            barChart.invalidate();
            Log.d(TAG, "Gráfico limpiado porque no hay datos.");
        }
    }


    /**
     * Genera un archivo PDF con el reporte de discrepancias (gráfico y tabla).
     */
    private void generatePDF() {
        // Verificar si hay datos para el gráfico (necesarios para el PDF)
        if (entries.isEmpty() || cashierLabels.isEmpty()) {
            Toast.makeText(this, "No hay datos para generar el PDF del reporte.", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "Reporte_" + currentStore.replace(" ", "_") + "_" + System.currentTimeMillis() + ".pdf";
        Uri pdfUri = null;
        OutputStream outStream = null;

        try {
            // Lógica de creación de archivo/URI (igual que en HistorialActivity)
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
                if (!appFolder.exists() && !appFolder.mkdirs()) throw new IOException("No se pudo crear carpeta");
                File pdfFile = new File(appFolder, fileName);
                outStream = new FileOutputStream(pdfFile);
            }

            if (outStream == null) throw new IOException("No se pudo obtener OutputStream");

            writePdfContent(outStream); // Escribir contenido (gráfico y tabla)
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

    /**
     * Escribe el contenido del PDF del reporte usando iText 7 (Gráfico + Tabla).
     */
    private void writePdfContent(OutputStream outStream) throws Exception {
        PdfWriter writer = new PdfWriter(outStream);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);
        DecimalFormat currencyFormat = new DecimalFormat("'$'###,##0.00");

        // --- Título ---
        document.add(new Paragraph("Reporte de Discrepancias - " + currentStore)
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15));

        // --- Capturar el Gráfico como Imagen ---
        Bitmap chartBitmap = getBitmapFromView(barChart);

        if (chartBitmap != null) {
            Log.d(TAG, "Bitmap del gráfico capturado exitosamente.");
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            chartBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] bitmapData = stream.toByteArray();
            Image chartImage = new Image(ImageDataFactory.create(bitmapData));

            // Escalar imagen para que quepa
            float documentWidth = pdfDoc.getDefaultPageSize().getWidth() - document.getLeftMargin() - document.getRightMargin();
            // Evitar división por cero si la imagen no tiene ancho
            if (chartImage.getImageWidth() > 0) {
                float scaler = Math.min(1f, documentWidth / chartImage.getImageWidth()); // No escalar más grande que 100%
                chartImage.scale(scaler, scaler);
                chartImage.setWidth(UnitValue.createPercentValue(95)); // Ajustar al 95% del ancho
                document.add(chartImage.setHorizontalAlignment(HorizontalAlignment.CENTER).setMarginBottom(10));
                Log.d(TAG, "Imagen del gráfico añadida al PDF.");
            } else {
                Log.e(TAG, "La imagen del gráfico capturado tiene ancho 0. Omitiendo.");
                document.add(new Paragraph("[Error: Imagen de gráfico inválida]").setFontColor(com.itextpdf.kernel.colors.ColorConstants.RED).setFontSize(10));
            }
        } else {
            Log.e(TAG, "No se pudo capturar el gráfico como Bitmap. Se omitirá en el PDF.");
            document.add(new Paragraph("[Error al generar imagen del gráfico]").setFontColor(com.itextpdf.kernel.colors.ColorConstants.RED).setFontSize(10));
        }

        // --- Añadir la Tabla de Datos ---
        if (!cashierLabels.isEmpty()) {
            document.add(new Paragraph("Datos del Reporte").setFontSize(12).setBold().setMarginTop(10).setMarginBottom(5));

            float[] columnWidths = {200, 150};
            Table table = new Table(columnWidths);
            table.setWidth(UnitValue.createPercentValue(80));
            table.setHorizontalAlignment(HorizontalAlignment.CENTER);
            table.setMarginBottom(10);

            table.addHeaderCell(createHeaderCell("Cajero"));
            table.addHeaderCell(createHeaderCell("Total Discrepancia"));

            for (int i = 0; i < cashierLabels.size(); i++) {
                table.addCell(createCell(cashierLabels.get(i)));
                table.addCell(createCell(currencyFormat.format(entries.get(i).getY())).setTextAlignment(TextAlignment.RIGHT));
            }
            document.add(table);
            Log.d(TAG, "Tabla de datos añadida al PDF.");
        } else {
            Log.w(TAG, "No hay datos para añadir la tabla al PDF.");
        }

        document.close();
        Log.i(TAG, "Documento PDF cerrado.");
    }

    // --- Helper Methods ---

    /**
     * Crea un Bitmap a partir del contenido actual de una View.
     */
    public static Bitmap getBitmapFromView(View view) {
        if (view.getWidth() <= 0 || view.getHeight() <= 0) {
            // Intentar forzar medición y layout antes de capturar
            view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());

            if (view.getWidth() <= 0 || view.getHeight() <= 0) {
                Log.e(TAG, "La vista sigue sin dimensiones después de medir/layout. No se puede capturar Bitmap.");
                return null;
            }
            Log.d(TAG,"Vista medida - Ancho: " + view.getWidth() + ", Alto: " + view.getHeight());
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

    // Helper para crear celdas de encabezado (igual que en HistorialActivity)
    private Cell createHeaderCell(String text) {
        try {
            // --- CORRECCIÓN AQUÍ para LIGHT_GRAY ---
            return new Cell().add(new Paragraph(text)
                            .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                            .setFontSize(10)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(new DeviceGray(0.85f)) // Gris más claro
                    .setPadding(5);
        } catch (IOException e) {
            Log.e(TAG, "Error creando fuente para celda de encabezado", e);
            return new Cell().add(new Paragraph(text));
        }
    }

    // Helper para crear celdas de contenido (igual que en HistorialActivity)
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

    // Métodos auxiliares safeGet... (igual que en HistorialActivity)
    private String safeGetString(Cursor cursor, String columnName) {
        try {
            // Usar getColumnIndexOrThrow es más seguro en caso de que la columna NO EXISTA
            int index = cursor.getColumnIndexOrThrow(columnName);
            return cursor.isNull(index) ? "" : cursor.getString(index);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Columna no encontrada en cursor: " + columnName);
            return "";
        }
    }
    private double safeGetDouble(Cursor cursor, String columnName) {
        try {
            int index = cursor.getColumnIndexOrThrow(columnName);
            return cursor.isNull(index) ? 0.0 : cursor.getDouble(index);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Columna no encontrada en cursor: " + columnName);
            return 0.0;
        }
    }
}