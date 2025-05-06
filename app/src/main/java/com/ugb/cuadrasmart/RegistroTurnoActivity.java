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

public class RegistroTurnoActivity extends AppCompatActivity {

    private static final String TAG = "RegistroTurnoActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;

    // UI Elements
    private EditText etFecha, etHoraInicio, etHoraCierre, etInicioDescanso, etFinDescanso;
    private EditText etNumeroCaja, etBilletes, etMonedas, etCheques, etVentasEsperadas, etDiscrepancia, etJustificacion;
    private Spinner spinnerCajero;
    private LinearLayout llJustificacion;
    private Button btnGuardarRegistro, btnCalcularDiscrepancia, btnAdjuntarEvidencia;
    private ImageView ivEvidencia; // ImageView para mostrar la foto de evidencia

    // Database helper
    private DatabaseHelper dbHelper;

    // Variables para evidencia
    private String evidenceUri = "";
    private Uri photoUri; // Para almacenar la URI temporal de la foto

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        // Inicializar vistas
        etFecha = findViewById(R.id.etFecha);
        etHoraInicio = findViewById(R.id.etHoraInicio);
        etHoraCierre = findViewById(R.id.etHoraCierre);
        etInicioDescanso = findViewById(R.id.etInicioDescanso);
        etFinDescanso = findViewById(R.id.etFinDescanso);
        etNumeroCaja = findViewById(R.id.etNumeroCaja);
        etBilletes = findViewById(R.id.etBilletes);
        etMonedas = findViewById(R.id.etMonedas);
        etCheques = findViewById(R.id.etCheques);
        etVentasEsperadas = findViewById(R.id.etVentasEsperadas);
        etDiscrepancia = findViewById(R.id.etDiscrepancia);
        etJustificacion = findViewById(R.id.etJustificacion);
        spinnerCajero = findViewById(R.id.spinnerCajero);
        llJustificacion = findViewById(R.id.llJustificacion);
        btnGuardarRegistro = findViewById(R.id.btnGuardarRegistro);
        btnCalcularDiscrepancia = findViewById(R.id.btnCalcularDiscrepancia);
        btnAdjuntarEvidencia = findViewById(R.id.btnAdjuntarEvidencia);
        ivEvidencia = findViewById(R.id.ivEvidencia);

        dbHelper = new DatabaseHelper(this);

        populateCajeroSpinner();

        // Ocultar panel de justificación inicialmente
        llJustificacion.setVisibility(LinearLayout.GONE);

        // Configurar selectores de fecha y hora
        etFecha.setOnClickListener(v -> showDatePicker(etFecha));
        etHoraInicio.setOnClickListener(v -> showTimePicker(etHoraInicio));
        etHoraCierre.setOnClickListener(v -> showTimePicker(etHoraCierre));
        etInicioDescanso.setOnClickListener(v -> showTimePicker(etInicioDescanso));
        etFinDescanso.setOnClickListener(v -> showTimePicker(etFinDescanso));

        btnCalcularDiscrepancia.setOnClickListener(v -> calculateAndDisplayDiscrepancy());

        btnAdjuntarEvidencia.setOnClickListener(v -> showEvidenceOptionsDialog());

        btnGuardarRegistro.setOnClickListener(v -> saveTurno());
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
        spinnerCajero.setAdapter(adapter);
    }

    private void showDatePicker(final EditText editText) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
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

    private void calculateAndDisplayDiscrepancy() {
        String billetesStr = etBilletes.getText().toString().trim();
        String monedasStr = etMonedas.getText().toString().trim();
        String ventasEsperadasStr = etVentasEsperadas.getText().toString().trim();

        if (TextUtils.isEmpty(billetesStr) || TextUtils.isEmpty(monedasStr) || TextUtils.isEmpty(ventasEsperadasStr)) {
            Toast.makeText(this, "Ingrese billetes, monedas y ventas esperadas", Toast.LENGTH_SHORT).show();
            return;
        }
        double billetes = parseDouble(billetesStr);
        double monedas = parseDouble(monedasStr);
        double ventasEsperadas = parseDouble(ventasEsperadasStr);
        double discrepancy = (billetes + monedas) - ventasEsperadas;
        etDiscrepancia.setText(String.format(Locale.getDefault(), "%.2f", discrepancy));

        if (Math.abs(discrepancy) > 1.0) {
            llJustificacion.setVisibility(LinearLayout.VISIBLE);
        } else {
            llJustificacion.setVisibility(LinearLayout.GONE);
            etJustificacion.setText("");
            evidenceUri = "";
            ivEvidencia.setImageURI(null);
        }
    }

    private void showEvidenceOptionsDialog() {
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
                Toast.makeText(this, "Error al crear el archivo de imagen", Toast.LENGTH_SHORT).show();
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
        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhotoIntent, REQUEST_IMAGE_PICK);
    }

    private File createImageFile() throws IOException {
        String imageFileName = "JPEG_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Calendar.getInstance().getTime()) + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                if (photoUri != null) {
                    evidenceUri = photoUri.toString();
                    ivEvidencia.setImageURI(photoUri);
                    Toast.makeText(this, "Imagen capturada", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                Uri selectedImage = data.getData();
                if (selectedImage != null) {
                    evidenceUri = selectedImage.toString();
                    ivEvidencia.setImageURI(selectedImage);
                    Toast.makeText(this, "Imagen seleccionada", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void saveTurno() {
        String fecha = etFecha.getText().toString().trim();
        String horaInicio = etHoraInicio.getText().toString().trim();
        String horaCierre = etHoraCierre.getText().toString().trim();
        String inicioDescanso = etInicioDescanso.getText().toString().trim();
        String finDescanso = etFinDescanso.getText().toString().trim();
        String numeroCajaStr = etNumeroCaja.getText().toString().trim();
        String cajero = spinnerCajero.getSelectedItem() != null ? spinnerCajero.getSelectedItem().toString() : "";
        String billetesStr = etBilletes.getText().toString().trim();
        String monedasStr = etMonedas.getText().toString().trim();
        String chequesStr = etCheques.getText().toString().trim();
        String ventasEsperadasStr = etVentasEsperadas.getText().toString().trim();
        String justificacion = etJustificacion.getText().toString().trim();

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

        double calculatedDiscrepancy = (billetes + monedas) - ventasEsperadas;
        etDiscrepancia.setText(String.format(Locale.getDefault(), "%.2f", calculatedDiscrepancy));

        if (Math.abs(calculatedDiscrepancy) > 1.0 && TextUtils.isEmpty(justificacion)) {
            Toast.makeText(this, "Ingrese justificación para discrepancia mayor a $1", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("CuadraSmartPrefs", MODE_PRIVATE);
        String tienda = prefs.getString("selected_store", "Tienda no especificada");

        boolean inserted = dbHelper.insertTurno(
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
                calculatedDiscrepancy,
                justificacion,
                evidenceUri,
                tienda
        );

        if (inserted) {
            Toast.makeText(this, "Registro guardado exitosamente", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Error al guardar el registro", Toast.LENGTH_SHORT).show();
        }
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
