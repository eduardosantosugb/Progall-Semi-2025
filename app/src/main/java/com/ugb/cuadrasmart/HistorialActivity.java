package com.ugb.cuadrasmart;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore; // Necesario para MediaStore API >= Q
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView; // TextView usado en el ViewHolder
import android.widget.Toast;

import androidx.annotation.NonNull; // Necesario para @NonNull
import androidx.appcompat.app.AppCompatActivity;
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
import com.itextpdf.layout.property.TextAlignment; // Para alinear texto

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class HistorialActivity extends AppCompatActivity {

    private static final String TAG = "HistorialActivity"; // Etiqueta para Logs

    // UI Elements
    private EditText etFiltroFecha;
    private Spinner spinnerFiltroCajero;
    private RecyclerView rvHistorial;
    private Button btnFiltrar, btnGenerarPDF;

    // Componentes
    private DatabaseHelper dbHelper;
    private ArrayList<Turno> turnoList;
    private TurnoAdapter turnoAdapter;
    private SharedPreferences prefs;
    private String currentStore = ""; // Almacenar la tienda actual

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial);

        // --- Inicialización ---
        etFiltroFecha = findViewById(R.id.etFiltroFecha);
        spinnerFiltroCajero = findViewById(R.id.spinnerFiltroCajero);
        rvHistorial = findViewById(R.id.rvHistorial);
        btnFiltrar = findViewById(R.id.btnFiltrar);
        btnGenerarPDF = findViewById(R.id.btnGenerarPDF);

        dbHelper = new DatabaseHelper(this);
        prefs = getSharedPreferences("CuadraSmartPrefs", MODE_PRIVATE);
        turnoList = new ArrayList<>();

        // Obtener tienda seleccionada
        currentStore = prefs.getString("selected_store", "");
        if (TextUtils.isEmpty(currentStore)) {
            Toast.makeText(this, "Error: Tienda no seleccionada. Volviendo a selección.", Toast.LENGTH_LONG).show();
            // Considera redirigir si la tienda es crucial para esta pantalla
            // Intent intent = new Intent(this, TiendasActivity.class);
            // startActivity(intent);
            finish(); // Cierra esta actividad si no hay tienda
            return; // Detiene la ejecución de onCreate
        }

        // --- Configuración UI ---
        rvHistorial.setLayoutManager(new LinearLayoutManager(this));
        turnoAdapter = new TurnoAdapter(turnoList);
        rvHistorial.setAdapter(turnoAdapter);

        etFiltroFecha.setOnClickListener(v -> showDatePicker());
        populateCajeroSpinner(); // Llena el spinner con cajeros de la tienda actual
        loadTurnos(null, null); // Carga inicial (filtrada por tienda)

        // --- Listeners ---
        btnFiltrar.setOnClickListener(v -> {
            String fechaFiltro = etFiltroFecha.getText().toString().trim();
            String cajeroFiltro = spinnerFiltroCajero.getSelectedItemPosition() > 0 ? // Evita "Todos"
                    spinnerFiltroCajero.getSelectedItem().toString() : null;
            loadTurnos(fechaFiltro, cajeroFiltro);
        });

        btnGenerarPDF.setOnClickListener(v -> generatePDF());
    }

    // Muestra el selector de fecha
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (DatePicker view, int year, int month, int dayOfMonth) -> {
            String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            etFiltroFecha.setText(dateStr);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    // Llena el Spinner con los cajeros que tienen registros EN LA TIENDA ACTUAL
    private void populateCajeroSpinner() {
        ArrayList<String> cajeroNames = new ArrayList<>();
        cajeroNames.add("Todos"); // Opción por defecto
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

    // Carga los turnos filtrados (siempre por tienda, opcionalmente por fecha y cajero)
    private void loadTurnos(String fechaFiltro, String cajeroFiltro) {
        turnoList.clear();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();

            // Construcción dinámica de la selección y argumentos
            StringBuilder selectionBuilder = new StringBuilder();
            ArrayList<String> argsList = new ArrayList<>();

            // Filtro base obligatorio por tienda
            selectionBuilder.append(DatabaseContract.TurnoEntry.COLUMN_TIENDA).append("=?");
            argsList.add(currentStore);

            // Añadir filtros opcionales
            if (!TextUtils.isEmpty(fechaFiltro)) {
                selectionBuilder.append(" AND ").append(DatabaseContract.TurnoEntry.COLUMN_FECHA).append("=?");
                argsList.add(fechaFiltro);
            }
            // Añadir filtro cajero solo si no es null o "Todos"
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
                    // Usar safeGet... para evitar crashes si una columna no existe
                    Turno turno = new Turno(
                            safeGetInt(cursor, DatabaseContract.TurnoEntry._ID),
                            safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_FECHA),
                            safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_HORA_INICIO),
                            safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_HORA_CIERRE),
                            safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_NUMERO_CAJA), // Guardado como String ahora
                            safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_CAJERO),
                            String.format(Locale.getDefault(), "%.2f", safeGetDouble(cursor, DatabaseContract.TurnoEntry.COLUMN_BILLETES)), // Formatear a 2 decimales
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
            } else {
                Log.d(TAG, "No se encontraron turnos para los filtros aplicados.");
                // Opcional: Mostrar mensaje si no hay resultados
                // Toast.makeText(this, "No hay registros para mostrar", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al cargar turnos", e);
            Toast.makeText(this, "Error al cargar historial: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        turnoAdapter.notifyDataSetChanged(); // Actualizar RecyclerView
    }

    /**
     * Genera un archivo PDF con el historial filtrado.
     * Guarda el PDF en Descargas/CuadraSmart.
     */
    private void generatePDF() {
        // Verificar si hay datos para generar el PDF
        if (turnoList == null || turnoList.isEmpty()) {
            Toast.makeText(this, "No hay datos para generar el PDF.", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "Historial_" + currentStore.replace(" ", "_") + "_" + System.currentTimeMillis() + ".pdf";
        Uri pdfUri = null;
        OutputStream outStream = null;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + File.separator + "CuadraSmart");

                pdfUri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (pdfUri == null) {
                    throw new IOException("Error al crear URI del PDF con MediaStore");
                }
                outStream = getContentResolver().openOutputStream(pdfUri);

            } else {
                // Para versiones < Q (Requiere permiso WRITE_EXTERNAL_STORAGE en Manifest si targetSdk < 29)
                File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File appFolder = new File(downloadsFolder, "CuadraSmart");
                if (!appFolder.exists() && !appFolder.mkdirs()) {
                    throw new IOException("No se pudo crear la carpeta CuadraSmart en Descargas");
                }
                File pdfFile = new File(appFolder, fileName);
                pdfUri = Uri.fromFile(pdfFile); // Uri para referencia, no para abrir stream directamente
                outStream = new FileOutputStream(pdfFile);
            }

            if (outStream == null) {
                throw new IOException("No se pudo obtener el OutputStream para el PDF");
            }

            writePdfContent(outStream); // Escribir contenido
            Toast.makeText(this, "PDF guardado en Descargas/CuadraSmart", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e(TAG, "Error al generar PDF", e);
            Toast.makeText(this, "Error al generar PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            // Si hubo error y se creó un URI con MediaStore, intentar eliminarlo
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && pdfUri != null) {
                try {
                    getContentResolver().delete(pdfUri, null, null);
                } catch (Exception deleteEx) {
                    Log.e(TAG, "Error al eliminar URI de PDF fallido", deleteEx);
                }
            }
        } finally {
            try {
                if (outStream != null) {
                    outStream.close(); // Asegurarse de cerrar el stream
                }
            } catch (IOException e) {
                Log.e(TAG, "Error al cerrar OutputStream del PDF", e);
            }
        }
    }

    /**
     * Escribe el contenido del PDF usando iText 7.
     */
    private void writePdfContent(OutputStream outStream) throws Exception {
        PdfWriter writer = new PdfWriter(outStream);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Título con nombre de tienda
        document.add(new Paragraph("Historial de Turnos - " + currentStore)
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                .setFontSize(16) // Tamaño ajustado
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15));

        // --- Tabla 1: Datos Generales del Turno ---
        document.add(new Paragraph("Resumen de Turnos").setFontSize(12).setBold().setMarginBottom(5));
        // Ajustar anchos de columna según necesidad
        float[] colWidths1 = {60, 50, 50, 40, 60, 50, 50, 60}; // Reducido número de columnas
        Table table1 = new Table(colWidths1);
        table1.setWidth(com.itextpdf.layout.property.UnitValue.createPercentValue(100)); // Usar todo el ancho

        // Encabezados Tabla 1
        table1.addHeaderCell(createHeaderCell("Fecha"));
        table1.addHeaderCell(createHeaderCell("Inicio"));
        table1.addHeaderCell(createHeaderCell("Cierre"));
        table1.addHeaderCell(createHeaderCell("Caja"));
        table1.addHeaderCell(createHeaderCell("Cajero"));
        table1.addHeaderCell(createHeaderCell("Billetes"));
        table1.addHeaderCell(createHeaderCell("Monedas"));
        table1.addHeaderCell(createHeaderCell("Discrep.")); // Abreviado

        // Contenido Tabla 1
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

        // --- Tabla 2: Justificaciones ---
        // Crear tabla solo si hay justificaciones
        ArrayList<Turno> turnosConJustificacion = new ArrayList<>();
        for(Turno t : turnoList) {
            if(!TextUtils.isEmpty(t.justificacion)) {
                turnosConJustificacion.add(t);
            }
        }

        if (!turnosConJustificacion.isEmpty()) {
            document.add(new Paragraph("\nJustificaciones Registradas")
                    .setFontSize(12).setBold().setMarginTop(10).setMarginBottom(5));

            float[] colWidths2 = {60, 70, 270}; // Anchos para Fecha, Cajero, Justificación
            Table table2 = new Table(colWidths2);
            table2.setWidth(com.itextpdf.layout.property.UnitValue.createPercentValue(100)); // Usar todo el ancho

            // Encabezados Tabla 2
            table2.addHeaderCell(createHeaderCell("Fecha"));
            table2.addHeaderCell(createHeaderCell("Cajero"));
            table2.addHeaderCell(createHeaderCell("Justificación"));

            // Contenido Tabla 2
            for (Turno t : turnosConJustificacion) {
                table2.addCell(createCell(t.fecha));
                table2.addCell(createCell(t.cajero));
                table2.addCell(createCell(t.justificacion));
            }
            document.add(table2);
        }

        document.close(); // Finalizar el documento
    }

    // Helper para crear celdas de encabezado estilizadas
    private Cell createHeaderCell(String text) {
        try {
            return new Cell().add(new Paragraph(text)
                            .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                            .setFontSize(9) // Tamaño reducido para más columnas
                            .setTextAlignment(TextAlignment.CENTER))
                    .setBackgroundColor(new DeviceGray(0.75f));
        } catch (IOException e) {
            Log.e(TAG, "Error creando fuente para celda", e);
            return new Cell().add(new Paragraph(text)); // Fallback sin estilo
        }
    }

    // Helper para crear celdas de contenido estilizadas
    private Cell createCell(String text) {
        try {
            return new Cell().add(new Paragraph(text != null ? text : "") // Manejar nulos
                    .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA))
                    .setFontSize(8)); // Tamaño reducido
        } catch (IOException e) {
            Log.e(TAG, "Error creando fuente para celda", e);
            return new Cell().add(new Paragraph(text != null ? text : "")); // Fallback sin estilo
        }
    }

    // --- Modelo para representar un turno ---
    // (Asegúrate que coincida con el que usas en otras Activities si es el mismo)
    public static class Turno {
        public int id;
        public String fecha;
        public String horaInicio;
        public String horaCierre;
        public String numeroCaja; // Podría ser String o int
        public String cajero;
        public String billetes; // Guardar como String formateado
        public String monedas;
        public String cheques;
        public String ventasEsperadas;
        public String discrepancia;
        public String justificacion;
        public String evidencia; // URI como String
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

    // --- Adaptador para el RecyclerView ---
    public class TurnoAdapter extends RecyclerView.Adapter<TurnoAdapter.TurnoViewHolder> {

        private final ArrayList<Turno> adapterTurnoList;

        public TurnoAdapter(ArrayList<Turno> turnoList) {
            this.adapterTurnoList = turnoList;
        }

        @NonNull
        @Override
        public TurnoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Usar un layout más informativo, ej. simple_list_item_2 o uno personalizado
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new TurnoViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TurnoViewHolder holder, int position) {
            Turno t = adapterTurnoList.get(position);
            // Mostrar información relevante en las dos líneas
            String line1 = t.fecha + " | Caja: " + t.numeroCaja + " | Cajero: " + t.cajero;
            String line2 = "Inicio: " + t.horaInicio + " Cierre: " + t.horaCierre + " | Disc: $" + t.discrepancia; // + " | Tienda: " + t.tienda; (Ya se filtra por tienda)
            holder.text1.setText(line1);
            holder.text2.setText(line2);

            // Funcionalidad de editar al pulsar el item
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(HistorialActivity.this, EditRegistroActivity.class);
                intent.putExtra("turnoId", t.id); // Pasar el ID del turno a editar
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return adapterTurnoList.size();
        }

        // --- ViewHolder ---
        public class TurnoViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2; // Referencias a los TextViews del layout del item
            public TurnoViewHolder(@NonNull View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }

    // --- Métodos auxiliares para obtener datos del Cursor de forma segura ---
    private String safeGetString(Cursor cursor, String columnName) {
        try {
            int index = cursor.getColumnIndexOrThrow(columnName);
            if (cursor.isNull(index)) {
                return ""; // Devuelve vacío si es NULL en DB
            }
            return cursor.getString(index);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Columna no encontrada en cursor: " + columnName);
            return ""; // Columna no existe
        }
    }

    private int safeGetInt(Cursor cursor, String columnName) {
        try {
            int index = cursor.getColumnIndexOrThrow(columnName);
            if (cursor.isNull(index)) {
                return 0; // O -1 si prefieres indicar NULL explícitamente
            }
            return cursor.getInt(index);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Columna no encontrada en cursor: " + columnName);
            return 0;
        }
    }

    private double safeGetDouble(Cursor cursor, String columnName) {
        try {
            int index = cursor.getColumnIndexOrThrow(columnName);
            if (cursor.isNull(index)) {
                return 0.0;
            }
            return cursor.getDouble(index);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Columna no encontrada en cursor: " + columnName);
            return 0.0;
        }
    }
}