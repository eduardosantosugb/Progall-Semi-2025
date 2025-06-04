package com.ugb.cuadrasmart;

import android.app.DatePickerDialog;
import android.content.ContentValues; // No se usa, se puede quitar si no hay más usos
import android.content.DialogInterface;
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
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog; // Asegurar importación
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
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
import com.itextpdf.layout.property.UnitValue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class HistorialActivity extends AppCompatActivity {

    private static final String TAG = "HistorialActivity";

    private EditText etFiltroFecha;
    private Spinner spinnerFiltroCajero;
    private RecyclerView rvHistorial;
    private Button btnFiltrar, btnGenerarPDF;
    private Toolbar toolbarHistorial;

    private DatabaseHelper dbHelper;
    private ArrayList<Turno> turnoList;
    private TurnoAdapter turnoAdapter;
    private SharedPreferences prefs;
    private String currentStore = "";
    private String currentUserRole; // Para el rol del usuario

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial);
        Log.d(TAG, "onCreate: Iniciando HistorialActivity.");

        toolbarHistorial = findViewById(R.id.toolbarHistorial);
        setSupportActionBar(toolbarHistorial);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.history);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        etFiltroFecha = findViewById(R.id.etFiltroFecha);
        spinnerFiltroCajero = findViewById(R.id.spinnerFiltroCajero);
        rvHistorial = findViewById(R.id.rvHistorial);
        btnFiltrar = findViewById(R.id.btnFiltrar);
        btnGenerarPDF = findViewById(R.id.btnGenerarPDF);

        dbHelper = new DatabaseHelper(this);
        prefs = getSharedPreferences("CuadraSmartPrefs", MODE_PRIVATE);
        turnoList = new ArrayList<>();

        // Obtener rol y tienda
        currentUserRole = prefs.getString("override_role", "cajero"); // Usar override_role
        currentStore = prefs.getString("selected_store", "");

        Log.d(TAG, "onCreate: Rol Vista Actual=" + currentUserRole + ", Tienda Actual=" + currentStore);

        if (TextUtils.isEmpty(currentStore)) {
            Toast.makeText(this, "Error: Tienda no seleccionada.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        rvHistorial.setLayoutManager(new LinearLayoutManager(this));
        turnoAdapter = new TurnoAdapter(turnoList);
        rvHistorial.setAdapter(turnoAdapter);

        etFiltroFecha.setOnClickListener(v -> showDatePicker());
        populateCajeroSpinner();
        loadTurnos(null, null);

        btnFiltrar.setOnClickListener(v -> {
            String fechaFiltro = etFiltroFecha.getText().toString().trim();
            String cajeroFiltro = spinnerFiltroCajero.getSelectedItemPosition() > 0 ?
                    spinnerFiltroCajero.getSelectedItem().toString() : null;
            loadTurnos(fechaFiltro, cajeroFiltro);
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

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String dateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
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
                    if (!TextUtils.isEmpty(name)) cajeroNames.add(name);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) { Log.e(TAG, "Error poblando spinner cajeros", e);
        } finally { if (cursor != null) cursor.close(); }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cajeroNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFiltroCajero.setAdapter(adapter);
    }

    private void loadTurnos(String fechaFiltro, String cajeroFiltro) {
        turnoList.clear();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            StringBuilder selectionBuilder = new StringBuilder(DatabaseContract.TurnoEntry.COLUMN_TIENDA + "=?");
            ArrayList<String> argsList = new ArrayList<>();
            argsList.add(currentStore);

            if (!TextUtils.isEmpty(fechaFiltro)) {
                selectionBuilder.append(" AND ").append(DatabaseContract.TurnoEntry.COLUMN_FECHA).append("=?");
                argsList.add(fechaFiltro);
            }
            if (!TextUtils.isEmpty(cajeroFiltro) && !cajeroFiltro.equals("Todos")) {
                selectionBuilder.append(" AND ").append(DatabaseContract.TurnoEntry.COLUMN_CAJERO).append("=?");
                argsList.add(cajeroFiltro);
            }
            String orderBy = DatabaseContract.TurnoEntry.COLUMN_FECHA + " DESC, " + DatabaseContract.TurnoEntry.COLUMN_HORA_INICIO + " DESC";
            cursor = db.query(DatabaseContract.TurnoEntry.TABLE_NAME, null, selectionBuilder.toString(), argsList.toArray(new String[0]), null, null, orderBy);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    turnoList.add(new Turno(
                            safeGetInt(cursor, DatabaseContract.TurnoEntry._ID),
                            safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_FECHA),
                            safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_HORA_INICIO),
                            safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_HORA_CIERRE),
                            safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_NUMERO_CAJA),
                            safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_CAJERO),
                            String.format(Locale.US, "%.2f", safeGetDouble(cursor, DatabaseContract.TurnoEntry.COLUMN_BILLETES)),
                            String.format(Locale.US, "%.2f", safeGetDouble(cursor, DatabaseContract.TurnoEntry.COLUMN_MONEDAS)),
                            String.format(Locale.US, "%.2f", safeGetDouble(cursor, DatabaseContract.TurnoEntry.COLUMN_CHEQUES)),
                            String.format(Locale.US, "%.2f", safeGetDouble(cursor, DatabaseContract.TurnoEntry.COLUMN_VENTAS_ESPERADAS)),
                            String.format(Locale.US, "%.2f", safeGetDouble(cursor, DatabaseContract.TurnoEntry.COLUMN_DISCREPANCIA)),
                            safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_JUSTIFICACION),
                            safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_EVIDENCIA),
                            safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_TIENDA)
                    ));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) { Log.e(TAG, "Error cargando turnos", e);
        } finally { if (cursor != null) cursor.close(); }
        turnoAdapter.notifyDataSetChanged();
        if (turnoList.isEmpty()) Toast.makeText(this, "No hay registros para los filtros.", Toast.LENGTH_SHORT).show();
    }

    // --- Lógica de Eliminación ---
    private void attemptDeleteTurno(final Turno turnoParaEliminar) {
        if (!"supervisor".equals(currentUserRole)) {
            Toast.makeText(this, "Solo supervisores pueden eliminar.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (turnoParaEliminar == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Eliminar Registro")
                .setMessage("¿Eliminar permanentemente el registro del cajero '"
                        + turnoParaEliminar.cajero + "' del día '" + turnoParaEliminar.fecha + "'?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Sí, Continuar", (dialog, which) -> showSecondDeleteConfirmation(turnoParaEliminar))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showSecondDeleteConfirmation(final Turno turnoParaEliminar) {
        new AlertDialog.Builder(this)
                .setTitle("¡CONFIRMACIÓN FINAL!")
                .setMessage("Esta acción es irreversible. ¿Está seguro?")
                .setPositiveButton("SÍ, ELIMINAR", (dialog, which) -> deleteTurnoFromDb(turnoParaEliminar.id))
                .setNegativeButton("No, Cancelar", null)
                .show();
    }

    private void deleteTurnoFromDb(int turnoId) {
        if (dbHelper.deleteTurnoById(turnoId)) {
            Toast.makeText(this, "Registro eliminado.", Toast.LENGTH_SHORT).show();
            String fechaFiltro = etFiltroFecha.getText().toString().trim();
            String cajeroFiltro = spinnerFiltroCajero.getSelectedItemPosition() > 0 ? spinnerFiltroCajero.getSelectedItem().toString() : null;
            loadTurnos(fechaFiltro, cajeroFiltro); // Recargar lista
        } else {
            Toast.makeText(this, "Error al eliminar.", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Métodos de PDF (sin cambios) ---
    private void generatePDF() { /* ... tu código ... */
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
    private void writePdfContent(OutputStream outStream) throws Exception { /* ... tu código ... */
        PdfWriter writer = new PdfWriter(outStream);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);
        document.add(new Paragraph("Historial de Turnos - " + currentStore)
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)).setFontSize(16)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(15));
        document.add(new Paragraph("Resumen de Turnos").setFontSize(12).setBold().setMarginBottom(5));
        float[] colWidths1 = {60, 50, 50, 40, 70, 50, 50, 60};
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
        ArrayList<Turno> turnosConJustificacion = new ArrayList<>();
        for(Turno t : turnoList) if(!TextUtils.isEmpty(t.justificacion)) turnosConJustificacion.add(t);
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
    private Cell createHeaderCell(String text) throws IOException { /* ... tu código ... */
        return new Cell().add(new Paragraph(text)
                        .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)).setFontSize(9)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(DeviceGray.GRAY).setPadding(4);
    }
    private Cell createCell(String text) throws IOException { /* ... tu código ... */
        return new Cell().add(new Paragraph(text != null ? text : "")
                        .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA)).setFontSize(8))
                .setPadding(3);
    }

    // --- Clase Turno (Modelo de Datos) ---
    public static class Turno {
        public int id;
        public String fecha, horaInicio, horaCierre, numeroCaja, cajero, billetes, monedas, cheques, ventasEsperadas, discrepancia, justificacion, evidencia, tienda;
        public Turno(int id, String f, String hi, String hc, String nc, String caj, String b, String m, String ch, String ve, String disc, String just, String evid, String ti) {
            this.id=id; this.fecha=f; this.horaInicio=hi; this.horaCierre=hc; this.numeroCaja=nc; this.cajero=caj; this.billetes=b; this.monedas=m; this.cheques=ch; this.ventasEsperadas=ve; this.discrepancia=disc; this.justificacion=just; this.evidencia=evid; this.tienda=ti;
        }
    }

    // --- Adaptador para el RecyclerView del Historial ---
    public class TurnoAdapter extends RecyclerView.Adapter<TurnoAdapter.TurnoViewHolder> {
        private ArrayList<Turno> adapterTurnoList;

        public TurnoAdapter(ArrayList<Turno> turnoList) {
            this.adapterTurnoList = turnoList;
        }

        @NonNull @Override
        public TurnoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_turno_historial, parent, false);
            return new TurnoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TurnoViewHolder holder, int position) {
            final Turno t = adapterTurnoList.get(position); // Hacer t final para el listener

            holder.tvHistorialLinea1.setText(t.fecha + " | Caja: " + t.numeroCaja + " | Cajero: " + t.cajero);
            holder.tvHistorialLinea2.setText("Inicio: " + t.horaInicio + " Cierre: " + t.horaCierre + " | Disc: $" + t.discrepancia);

            if (!TextUtils.isEmpty(t.justificacion)) {
                holder.tvHistorialJustificacion.setText("Justificación: " + t.justificacion);
                holder.tvHistorialJustificacion.setVisibility(View.VISIBLE);
            } else {
                holder.tvHistorialJustificacion.setVisibility(View.GONE);
            }

            double discrepanciaValor = 0.0;
            try {
                String discStr = t.discrepancia.replace("$", "").replace(",", ".");
                discrepanciaValor = Double.parseDouble(discStr);
            } catch (NumberFormatException e) { /* Log ya en otro lado */ }

            if (Math.abs(discrepanciaValor) <= 1.00) {
                holder.llTurnoHistorialBackground.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.historial_cuadratura_ok_bg));
            } else {
                holder.llTurnoHistorialBackground.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.historial_cuadratura_error_bg));
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(HistorialActivity.this, EditRegistroActivity.class);
                intent.putExtra("turnoId", t.id);
                startActivity(intent);
            });

            // OnLongClickListener para eliminar, solo para supervisores
            if ("supervisor".equals(currentUserRole)) {
                holder.itemView.setOnLongClickListener(v -> {
                    attemptDeleteTurno(t); // Llamar al método de la actividad
                    return true; // Consumir el evento de long click
                });
            } else {
                holder.itemView.setOnLongClickListener(null); // Deshabilitar long click si no es supervisor
            }
        }

        @Override public int getItemCount() { return adapterTurnoList.size(); }

        public class TurnoViewHolder extends RecyclerView.ViewHolder {
            TextView tvHistorialLinea1, tvHistorialLinea2, tvHistorialJustificacion;
            LinearLayout llTurnoHistorialBackground;
            public TurnoViewHolder(@NonNull View itemView) {
                super(itemView);
                tvHistorialLinea1 = itemView.findViewById(R.id.tvHistorialLinea1);
                tvHistorialLinea2 = itemView.findViewById(R.id.tvHistorialLinea2);
                tvHistorialJustificacion = itemView.findViewById(R.id.tvHistorialJustificacion);
                llTurnoHistorialBackground = itemView.findViewById(R.id.llTurnoHistorialBackground);
            }
        }
    }

    // --- Métodos auxiliares safeGet... ---
    private String safeGetString(Cursor cursor, String columnName) {
        if (cursor == null || cursor.isClosed()) return "";
        try { int index = cursor.getColumnIndexOrThrow(columnName); return cursor.isNull(index) ? "" : cursor.getString(index); }
        catch (IllegalArgumentException e) { Log.w(TAG, "Columna no encontrada: " + columnName, e); return ""; }
    }
    private int safeGetInt(Cursor cursor, String columnName) {
        if (cursor == null || cursor.isClosed()) return 0;
        try { int index = cursor.getColumnIndexOrThrow(columnName); return cursor.isNull(index) ? 0 : cursor.getInt(index); }
        catch (IllegalArgumentException e) { Log.w(TAG, "Columna no encontrada: " + columnName, e); return 0; }
    }
    private double safeGetDouble(Cursor cursor, String columnName) {
        if (cursor == null || cursor.isClosed()) return 0.0;
        try { int index = cursor.getColumnIndexOrThrow(columnName); return cursor.isNull(index) ? 0.0 : cursor.getDouble(index); }
        catch (IllegalArgumentException e) { Log.w(TAG, "Columna no encontrada: " + columnName, e); return 0.0; }
    }
}