package com.ugb.cuadrasmart;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class EditRegistroActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;

    // UI elements
    private EditText etFechaEdit, etHoraInicioEdit, etHoraCierreEdit, etInicioDescansoEdit, etFinDescansoEdit;
    private EditText etNumeroCajaEdit, etBilletesEdit, etMonedasEdit, etChequesEdit, etVentasEsperadasEdit, etDiscrepanciaEdit, etJustificacionEdit;
    private Spinner spinnerCajeroEdit;
    private LinearLayout llJustificacionEdit;
    private Button btnCalcularDiscrepanciaEdit, btnAdjuntarEvidenciaEdit, btnActualizarRegistro;
    private ImageView ivEvidenciaEdit; // ImageView para mostrar evidencia

    private DatabaseHelper dbHelper;
    private int turnoId;
    private String evidenceUri = "";
    private Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_registro);

        etFechaEdit = findViewById(R.id.etFechaEdit);
        etHoraInicioEdit = findViewById(R.id.etHoraInicioEdit);
        etHoraCierreEdit = findViewById(R.id.etHoraCierreEdit);
        etInicioDescansoEdit = findViewById(R.id.etInicioDescansoEdit);
        etFinDescansoEdit = findViewById(R.id.etFinDescansoEdit);
        etNumeroCajaEdit = findViewById(R.id.etNumeroCajaEdit);
        spinnerCajeroEdit = findViewById(R.id.spinnerCajeroEdit);
        etBilletesEdit = findViewById(R.id.etBilletesEdit);
        etMonedasEdit = findViewById(R.id.etMonedasEdit);
        etChequesEdit = findViewById(R.id.etChequesEdit);
        etVentasEsperadasEdit = findViewById(R.id.etVentasEsperadasEdit);
        etDiscrepanciaEdit = findViewById(R.id.etDiscrepanciaEdit);
        etJustificacionEdit = findViewById(R.id.etJustificacionEdit);
        llJustificacionEdit = findViewById(R.id.llJustificacionEdit);
        btnCalcularDiscrepanciaEdit = findViewById(R.id.btnCalcularDiscrepanciaEdit);
        btnAdjuntarEvidenciaEdit = findViewById(R.id.btnAdjuntarEvidenciaEdit);
        btnActualizarRegistro = findViewById(R.id.btnActualizarRegistro);
        ivEvidenciaEdit = findViewById(R.id.ivEvidenciaEdit);

        dbHelper = new DatabaseHelper(this);

        turnoId = getIntent().getIntExtra("turnoId", -1);
        if (turnoId == -1) {
            Toast.makeText(this, "ID de turno no proporcionado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        populateCajeroSpinner();
        loadTurnoData(turnoId);

        // Configurar selectores
        etFechaEdit.setOnClickListener(v -> showDatePicker(etFechaEdit));
        etHoraInicioEdit.setOnClickListener(v -> showTimePicker(etHoraInicioEdit));
        etHoraCierreEdit.setOnClickListener(v -> showTimePicker(etHoraCierreEdit));
        etInicioDescansoEdit.setOnClickListener(v -> showTimePicker(etInicioDescansoEdit));
        etFinDescansoEdit.setOnClickListener(v -> showTimePicker(etFinDescansoEdit));

        btnCalcularDiscrepanciaEdit.setOnClickListener(v -> calculateAndDisplayDiscrepancyEdit());
        btnAdjuntarEvidenciaEdit.setOnClickListener(v -> showEvidenceOptionsDialogEdit());
        btnActualizarRegistro.setOnClickListener(v -> updateTurno());
    }

    private void populateCajeroSpinner() {
        ArrayList<String> cajeroNames = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = DatabaseContract.UserEntry.COLUMN_ROLE + "=?";
        String[] selectionArgs = {"cajero"};
        Cursor cursor = db.query(DatabaseContract.UserEntry.TABLE_NAME, null, selection, selectionArgs, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int index = cursor.getColumnIndex(DatabaseContract.UserEntry.COLUMN_NAME);
                if (index >= 0) {
                    String name = cursor.getString(index);
                    cajeroNames.add(name);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cajeroNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCajeroEdit.setAdapter(adapter);
    }

    private void loadTurnoData(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = DatabaseContract.TurnoEntry._ID + "=?";
        String[] selectionArgs = {String.valueOf(id)};
        Cursor cursor = db.query(DatabaseContract.TurnoEntry.TABLE_NAME, null, selection, selectionArgs, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            etFechaEdit.setText(safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_FECHA));
            etHoraInicioEdit.setText(safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_HORA_INICIO));
            etHoraCierreEdit.setText(safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_HORA_CIERRE));
            etInicioDescansoEdit.setText(safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_INICIO_DESCANSO));
            etFinDescansoEdit.setText(safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_FIN_DESCANSO));
            etNumeroCajaEdit.setText(safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_NUMERO_CAJA));
            etBilletesEdit.setText(safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_BILLETES));
            etMonedasEdit.setText(safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_MONEDAS));
            etChequesEdit.setText(safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_CHEQUES));
            etVentasEsperadasEdit.setText(safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_VENTAS_ESPERADAS));
            etDiscrepanciaEdit.setText(safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_DISCREPANCIA));
            etJustificacionEdit.setText(safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_JUSTIFICACION));
            evidenceUri = safeGetString(cursor, DatabaseContract.TurnoEntry.COLUMN_EVIDENCIA);
            // Mostrar la imagen de evidencia si existe
            if (!TextUtils.isEmpty(evidenceUri)) {
                ivEvidenciaEdit.setImageURI(Uri.parse(evidenceUri));
            }
            cursor.close();

            double discrepancy = parseDouble(etDiscrepanciaEdit.getText().toString().trim());
            if (Math.abs(discrepancy) > 1.0) {
                llJustificacionEdit.setVisibility(LinearLayout.VISIBLE);
            } else {
                llJustificacionEdit.setVisibility(LinearLayout.GONE);
            }
        }
    }

    private void showDatePicker(final EditText editText) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (DatePicker view, int year, int month, int dayOfMonth) -> {
            String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            editText.setText(dateStr);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker(final EditText editText) {
        Calendar calendar = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
            editText.setText(timeStr);
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    private void calculateAndDisplayDiscrepancyEdit() {
        String billetesStr = etBilletesEdit.getText().toString().trim();
        String monedasStr = etMonedasEdit.getText().toString().trim();
        String ventasEsperadasStr = etVentasEsperadasEdit.getText().toString().trim();

        if (TextUtils.isEmpty(billetesStr) || TextUtils.isEmpty(monedasStr) || TextUtils.isEmpty(ventasEsperadasStr)) {
            Toast.makeText(this, "Ingrese billetes, monedas y ventas esperadas", Toast.LENGTH_SHORT).show();
            return;
        }

        double billetes = parseDouble(billetesStr);
        double monedas = parseDouble(monedasStr);
        double ventasEsperadas = parseDouble(ventasEsperadasStr);
        double discrepancy = (billetes + monedas) - ventasEsperadas;
        etDiscrepanciaEdit.setText(String.format(Locale.getDefault(), "%.2f", discrepancy));

        if (Math.abs(discrepancy) > 1.0) {
            llJustificacionEdit.setVisibility(LinearLayout.VISIBLE);
        } else {
            llJustificacionEdit.setVisibility(LinearLayout.GONE);
            etJustificacionEdit.setText("");
            evidenceUri = "";
            ivEvidenciaEdit.setImageURI(null);
        }
    }

    private void showEvidenceOptionsDialogEdit() {
        String[] options = {"Tomar foto", "Seleccionar de la galería"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("Adjuntar evidencia")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        dispatchTakePictureIntent();
                    } else if (which == 1) {
                        dispatchPickPictureIntent();
                    }
                })
                .show();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error al crear archivo de imagen", Toast.LENGTH_SHORT).show();
                return;
            }
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this, "com.ugb.cuadrasmart.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private void dispatchPickPictureIntent() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickIntent, REQUEST_IMAGE_PICK);
    }

    private File createImageFile() throws IOException {
        String imageFileName = "JPEG_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Calendar.getInstance().getTime()) + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                if (photoUri != null) {
                    evidenceUri = photoUri.toString();
                    ivEvidenciaEdit.setImageURI(photoUri);
                    Toast.makeText(this, "Imagen capturada", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                Uri selectedImage = data.getData();
                if (selectedImage != null) {
                    evidenceUri = selectedImage.toString();
                    ivEvidenciaEdit.setImageURI(selectedImage);
                    Toast.makeText(this, "Imagen seleccionada", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void updateTurno() {
        String fecha = etFechaEdit.getText().toString().trim();
        String horaInicio = etHoraInicioEdit.getText().toString().trim();
        String horaCierre = etHoraCierreEdit.getText().toString().trim();
        String inicioDescanso = etInicioDescansoEdit.getText().toString().trim();
        String finDescanso = etFinDescansoEdit.getText().toString().trim();
        String numeroCajaStr = etNumeroCajaEdit.getText().toString().trim();
        String cajero = spinnerCajeroEdit.getSelectedItem() != null ? spinnerCajeroEdit.getSelectedItem().toString() : "";
        String billetesStr = etBilletesEdit.getText().toString().trim();
        String monedasStr = etMonedasEdit.getText().toString().trim();
        String chequesStr = etChequesEdit.getText().toString().trim();
        String ventasEsperadasStr = etVentasEsperadasEdit.getText().toString().trim();
        String justificacion = etJustificacionEdit.getText().toString().trim();

        if (TextUtils.isEmpty(fecha) || TextUtils.isEmpty(horaInicio) || TextUtils.isEmpty(horaCierre)
                || TextUtils.isEmpty(numeroCajaStr) || TextUtils.isEmpty(cajero)
                || TextUtils.isEmpty(billetesStr) || TextUtils.isEmpty(monedasStr)
                || TextUtils.isEmpty(ventasEsperadasStr)) {
            Toast.makeText(this, "Complete los campos obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        int numeroCaja = Integer.parseInt(numeroCajaStr);
        double billetes = parseDouble(billetesStr);
        double monedas = parseDouble(monedasStr);
        double cheques = parseDouble(chequesStr);
        double ventasEsperadas = parseDouble(ventasEsperadasStr);
        double discrepancy = (billetes + monedas) - ventasEsperadas;
        etDiscrepanciaEdit.setText(String.format(Locale.getDefault(), "%.2f", discrepancy));

        if (Math.abs(discrepancy) > 1.0 && TextUtils.isEmpty(justificacion)) {
            Toast.makeText(this, "Ingrese justificación para discrepancia mayor a $1", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("CuadraSmartPrefs", MODE_PRIVATE);
        String tienda = prefs.getString("selected_store", "Tienda no especificada");

        boolean updated = dbHelper.updateTurno(
                turnoId,
                fecha,
                horaInicio,
                horaCierre,
                inicioDescanso,
                finDescanso,
                numeroCaja,
                cajero,
                billetes,
                monedas,
                cheques,
                ventasEsperadas,
                discrepancy,
                justificacion,
                evidenceUri,
                tienda
        );

        if (updated) {
            Toast.makeText(this, "Registro actualizado exitosamente", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Error al actualizar el registro", Toast.LENGTH_SHORT).show();
        }
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // Métodos auxiliares para obtener datos del Cursor de forma segura
    private String safeGetString(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndex(columnName);
        return index >= 0 ? cursor.getString(index) : "";
    }

    private int safeGetInt(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndex(columnName);
        return index >= 0 ? cursor.getInt(index) : 0;
    }

    private double safeGetDouble(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndex(columnName);
        return index >= 0 ? cursor.getDouble(index) : 0.0;
    }
}
