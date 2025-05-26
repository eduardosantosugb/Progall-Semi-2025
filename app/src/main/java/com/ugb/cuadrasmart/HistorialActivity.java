package com.ugb.cuadrasmart;

import android.app.DatePickerDialog;
import android.content.ContentValues; // No parece usarse aquí, se puede quitar
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem; // Import para MenuItem
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Import para Toolbar
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue; // Asegúrate que este import esté si usas UnitValue para la tabla PDF

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class HistorialActivity extends AppCompatActivity {

    private static final String TAG = "HistorialActivity";

    // UI Elements
    private EditText etFiltroFecha;
    private Spinner spinnerFiltroCajero;
    private RecyclerView rvHistorial;
    private Button btnFiltrar, btnGenerarPDF;
    private Toolbar toolbarHistorial; // Para la Toolbar

    // Componentes
    private DatabaseHelper dbHelper;
    private ArrayList<Turno> turnoList;
    private TurnoAdapter turnoAdapter;
    private SharedPreferences prefs;
    private String currentStore = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial);

        // Configuración de la Toolbar
        toolbarHistorial = findViewById(R.id.toolbarHistorial); // Asegúrate que este ID exista en activity_historial.xml
        setSupportActionBar(toolbarHistorial);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.history); // Usar string resource
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // --- Inicialización de vistas ---
        etFiltroFecha = findViewById(R.id.etFiltroFecha);
        spinnerFiltroCajero = findViewById(R.id.spinnerFiltroCajero);
        rvHistorial = findViewById(R.id.rvHistorial);
        btnFiltrar = findViewById(R.id.btnFiltrar);
        btnGenerarPDF = findViewById(R.id.btnGenerarPDF);

        dbHelper = new DatabaseHelper(this);
        prefs = getSharedPreferences("CuadraSmartPrefs", MODE_PRIVATE);
        turnoList = new ArrayList<>();

        currentStore = prefs.getString("selected_store", "");
        if (TextUtils.isEmpty(currentStore)) {
            Toast.makeText(this, "Error: Tienda no seleccionada.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // --- Configuración UI ---
        rvHistorial.setLayoutManager(new LinearLayoutManager(this));
        turnoAdapter = new TurnoAdapter(turnoList);
        rvHistorial.setAdapter(turnoAdapter);

        etFiltroFecha.setOnClickListener(v -> showDatePicker());
        populateCajeroSpinner();
        loadTurnos(null, null); // Carga inicial

        // --- Listeners ---
        btnFiltrar.setOnClickListener(v -> {
            String fechaFiltro = etFiltroFecha.getText().toString().trim();
            String cajeroFiltro = spinnerFiltroCajero.getSelectedItemPosition() > 0 ?
                    spinnerFiltroCajero.getSelectedItem().toString() : null;
            loadTurnos(fechaFiltro, cajeroFiltro);
        });

        btnGenerarPDF.setOnClickListener(v -> generatePDF());
    }

    // Manejar el clic en el botón de atrás de la Toolbar
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
                    // Usar safeGetString para evitar crashes si una columna no existe
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
            if (cursor != null) {
                cursor.close();
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, cajeroNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFiltroCajero.setAdapter(adapter);
    }

    private void loadTurnos(String fechaFiltro, String cajeroFiltro) {
        turnoList.clear();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            StringBuilder selectionBuilder = new StringBuilder();
            ArrayList<String> argsList = new ArrayList<>();

            selectionBuilder.append(DatabaseContract.TurnoEntry.COLUMN_TIENDA).append("=?");
            argsList.add(currentStore);

            if (!TextUtils.isEmpty(fechaFiltro)) {
                selectionBuilder.append(" AND ").append(DatabaseContract.TurnoEntry.COLUMN_FECHA).append("=?");
                argsList.add(fechaFiltro);
            }
            if (!TextUtils.isEmpty(cajeroFiltro) && !cajeroFiltro.equals("Todos")) {
                selectionBuilder.append(" AND ").append(DatabaseContract.TurnoEntry.COLUMN_CAJERO).append("=?");
                argsList.add(cajeroFiltro);
            }

            String selection = selectionBuilder.toString();
            String[] selectionArgs = argsList.toArray(new String[0]);
            String orderBy = DatabaseContract.TurnoEntry.COLUMN_FECHA + " DESC, " + DatabaseContract.TurnoEntry.COLUMN_HORA_INICIO + " DESC";

            cursor = db.query(DatabaseContract.TurnoEntry.TABLE_NAME, null, selection, selectionArgs, null, null, orderBy);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Turno turno = new Turno(
                            safeGetInt(cursor, DatabaseContract.TurnoEntry._ID),
                            safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_FECHA),
                            safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_HORA_INICIO),
                            safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_HORA_CIERRE),
                            safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_NUMERO_CAJA),
                            safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_CAJERO),
                            String.format(Locale.getDefault(), "%.2f", safeGetDouble(cursor, DatabaseContract.TurnoEntry.COLUMN_BILLETES)),
                            String.format(Locale.getDefault(), "%.2f", safeGetDouble(cursor, DatabaseContract.TurnoEntry.COLUMN_MONEDAS)),
                            String.format(Locale.getDefault(), "%.2f", safeGetDouble(cursor, DatabaseContract.TurnoEntry.COLUMN_CHEQUES)),
                            String.format(Locale.getDefault(), "%.2f", safeGetDouble(cursor, DatabaseContract.TurnoEntry.COLUMN_VENTAS_ESPERADAS)),
                            String.format(Locale.getDefault(), "%.2f", safeGetDouble(cursor, DatabaseContract.TurnoEntry.COLUMN_DISCREPANCIA)),
                            safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_JUSTIFICACION),
                            safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_EVIDENCIA),
                            safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_TIENDA)
                    );
                    turnoList.add(turno);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al cargar turnos", e);
            Toast.makeText(this, "Error al cargar historial: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        turnoAdapter.notifyDataSetChanged();
    }

    private void generatePDF() {
        if (turnoList == null || turnoList.isEmpty()) {
            Toast.makeText(this, "No hay datos para generar el PDF.", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "Historial_Turnos_" + currentStore.replace(" ", "_") + "_" + System.currentTimeMillis() + ".pdf";
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
            Log.e(TAG, "Error al generar PDF", e);
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

        document.add(new Paragraph("Historial de Turnos - " + currentStore)
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                .setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15));

        // Tabla 1: Resumen de Turnos
        document.add(new Paragraph("Resumen de Turnos").setFontSize(12).setBold().setMarginBottom(5));
        float[] colWidths1 = {60, 50, 50, 40, 70, 50, 50, 60}; // Ajustado para cajero más ancho
        Table table1 = new Table(UnitValue.createPercentArray(colWidths1)).useAllAvailableWidth();

        table1.addHeaderCell(createHeaderCell("Fecha"));
        table1.addHeaderCell(createHeaderCell("Inicio"));
        table1.addHeaderCell(createHeaderCell("Cierre"));
        table1.addHeaderCell(createHeaderCell("Caja"));
        table1.addHeaderCell(createHeaderCell("Cajero"));
        table1.addHeaderCell(createHeaderCell("Billetes"));
        table1.addHeaderCell(createHeaderCell("Monedas"));
        table1.addHeaderCell(createHeaderCell("Discrep."));

        for (Turno t : turnoList) {
            table1.addCell(createCell(t.fecha));
            table1.addCell(createCell(t.horaInicio));
            table1.addCell(createCell(t.horaCierre));
            table1.addCell(createCell(t.numeroCaja));
            table1.addCell(createCell(t.cajero));
            table1.addCell(createCell(t.billetes));
            table1.addCell(createCell(t.monedas));
            table1.addCell(createCell(t.discrepancia));
        }
        document.add(table1);

        // Tabla 2: Justificaciones
        ArrayList<Turno> turnosConJustificacion = new ArrayList<>();
        for(Turno t : turnoList) {
            if(!TextUtils.isEmpty(t.justificacion)) {
                turnosConJustificacion.add(t);
            }
        }

        if (!turnosConJustificacion.isEmpty()) {
            document.add(new Paragraph("\nJustificaciones Registradas")
                    .setFontSize(12).setBold().setMarginTop(10).setMarginBottom(5));
            float[] colWidths2 = {60, 70, 270};
            Table table2 = new Table(UnitValue.createPercentArray(colWidths2)).useAllAvailableWidth();

            table2.addHeaderCell(createHeaderCell("Fecha"));
            table2.addHeaderCell(createHeaderCell("Cajero"));
            table2.addHeaderCell(createHeaderCell("Justificación"));

            for (Turno t : turnosConJustificacion) {
                table2.addCell(createCell(t.fecha));
                table2.addCell(createCell(t.cajero));
                table2.addCell(createCell(t.justificacion));
            }
            document.add(table2);
        }
        document.close();
    }

    private Cell createHeaderCell(String text) {
        try {
            return new Cell().add(new Paragraph(text)
                            .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                            .setFontSize(9)
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(new DeviceGray(0.75f)) // Usar DeviceGray
                    .setPadding(4); // Ajustar padding
        } catch (IOException e) {
            Log.e(TAG, "Error creando fuente para celda de encabezado", e);
            return new Cell().add(new Paragraph(text));
        }
    }

    private Cell createCell(String text) {
        try {
            return new Cell().add(new Paragraph(text != null ? text : "")
                            .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                            .setFontSize(8)) // Tamaño reducido
                    .setPadding(3); // Ajustar padding
        } catch (IOException e) {
            Log.e(TAG, "Error creando fuente para celda de contenido", e);
            return new Cell().add(new Paragraph(text != null ? text : ""));
        }
    }

    // Modelo Turno (sin cambios, pero necesario para que compile)
    public static class Turno {
        public int id;
        public String fecha;
        public String horaInicio;
        public String horaCierre;
        public String numeroCaja;
        public String cajero;
        public String billetes;
        public String monedas;
        public String cheques;
        public String ventasEsperadas;
        public String discrepancia;
        public String justificacion;
        public String evidencia;
        public String tienda;

        public Turno(int id, String fecha, String horaInicio, String horaCierre, String numeroCaja, String cajero,
                     String billetes, String monedas, String cheques, String ventasEsperadas, String discrepancia,
                     String justificacion, String evidencia, String tienda) {
            this.id = id;
            this.fecha = fecha;
            this.horaInicio = horaInicio;
            this.horaCierre = horaCierre;
            this.numeroCaja = numeroCaja;
            this.cajero = cajero;
            this.billetes = billetes;
            this.monedas = monedas;
            this.cheques = cheques;
            this.ventasEsperadas = ventasEsperadas;
            this.discrepancia = discrepancia;
            this.justificacion = justificacion;
            this.evidencia = evidencia;
            this.tienda = tienda;
        }
    }

    // Adaptador TurnoAdapter (sin cambios, pero necesario para que compile)
    public class TurnoAdapter extends RecyclerView.Adapter<TurnoAdapter.TurnoViewHolder> {
        private final ArrayList<Turno> adapterTurnoList;
        public TurnoAdapter(ArrayList<Turno> turnoList) {
            this.adapterTurnoList = turnoList;
        }
        @NonNull @Override
        public TurnoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new TurnoViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull TurnoViewHolder holder, int position) {
            Turno t = adapterTurnoList.get(position);
            String line1 = t.fecha + " | Caja: " + t.numeroCaja + " | Cajero: " + t.cajero;
            String line2 = "Inicio: " + t.horaInicio + " Cierre: " + t.horaCierre + " | Disc: $" + t.discrepancia;
            holder.text1.setText(line1);
            holder.text2.setText(line2);
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(HistorialActivity.this, EditRegistroActivity.class);
                intent.putExtra("turnoId", t.id);
                startActivity(intent);
            });
        }
        @Override public int getItemCount() { return adapterTurnoList.size(); }
        public class TurnoViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            public TurnoViewHolder(@NonNull View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }

    // Métodos auxiliares safeGet...
    private String safeGetString(Cursor cursor, String columnName) {
        if (cursor == null || cursor.isClosed()) return "";
        try {
            int index = cursor.getColumnIndexOrThrow(columnName);
            return cursor.isNull(index) ? "" : cursor.getString(index);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Columna '" + columnName + "' no encontrada.", e); return "";
        }
    }
    private int safeGetInt(Cursor cursor, String columnName) {
        if (cursor == null || cursor.isClosed()) return 0;
        try {
            int index = cursor.getColumnIndexOrThrow(columnName);
            return cursor.isNull(index) ? 0 : cursor.getInt(index);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Columna '" + columnName + "' no encontrada.", e); return 0;
        }
    }
    private double safeGetDouble(Cursor cursor, String columnName) {
        if (cursor == null || cursor.isClosed()) return 0.0;
        try {
            int index = cursor.getColumnIndexOrThrow(columnName);
            return cursor.isNull(index) ? 0.0 : cursor.getDouble(index);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Columna '" + columnName + "' no encontrada.", e); return 0.0;
        }
    }
}