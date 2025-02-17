package com.ugb.cuadrasmart;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;
import android.content.DialogInterface;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RegistroTurnoActivity extends AppCompatActivity {

    private EditText etFecha, etHoraInicio, etHoraCierre, etNumeroCaja;
    private EditText etBilletes, etMonedas, etCheques, etVentasEsperadas, etComentario;
    private Spinner spinnerCajeros;
    private Button btnCalcularDiscrepancia, btnAdjuntarFoto, btnGuardarRegistro;
    private LinearLayout llJustificacion;
    private RadioGroup rgJustificacion;
    private RadioButton rbJustificar, rbNoJustificar;

    private DatabaseHelper dbHelper;
    private double discrepanciaCalculada = 0.0;

    // Request codes para imagen
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAPTURE_IMAGE_REQUEST = 2;
    private Uri evidenciaUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro_turno);

        // Referenciar vistas
        etFecha = findViewById(R.id.etFecha);
        etHoraInicio = findViewById(R.id.etHoraInicio);
        etHoraCierre = findViewById(R.id.etHoraCierre);
        etNumeroCaja = findViewById(R.id.etNumeroCaja);
        spinnerCajeros = findViewById(R.id.spinnerCajeros);
        etBilletes = findViewById(R.id.etBilletes);
        etMonedas = findViewById(R.id.etMonedas);
        etCheques = findViewById(R.id.etCheques);
        etVentasEsperadas = findViewById(R.id.etVentasEsperadas);
        btnCalcularDiscrepancia = findViewById(R.id.btnCalcularDiscrepancia);
        llJustificacion = findViewById(R.id.llJustificacion);
        rgJustificacion = findViewById(R.id.rgJustificacion);
        rbJustificar = findViewById(R.id.rbJustificar);
        rbNoJustificar = findViewById(R.id.rbNoJustificar);
        etComentario = findViewById(R.id.etComentario);
        btnAdjuntarFoto = findViewById(R.id.btnAdjuntarFoto);
        btnGuardarRegistro = findViewById(R.id.btnGuardarRegistro);

        dbHelper = new DatabaseHelper(this);

        // Evitar que los EditText de fecha y hora abran el teclado manualmente
        etFecha.setFocusable(false);
        etHoraInicio.setFocusable(false);
        etHoraCierre.setFocusable(false);

        // Configurar DatePicker para la fecha
        etFecha.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar calendar = Calendar.getInstance();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                DatePickerDialog datePickerDialog = new DatePickerDialog(RegistroTurnoActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
                                String selectedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                                etFecha.setText(selectedDate);
                            }
                        }, year, month, day);
                datePickerDialog.show();
            }
        });

        // Configurar TimePicker para la hora de inicio
        etHoraInicio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);
                TimePickerDialog timePickerDialog = new TimePickerDialog(RegistroTurnoActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                String amPm = (hourOfDay >= 12) ? "PM" : "AM";
                                int hour12 = (hourOfDay == 0 || hourOfDay == 12) ? 12 : hourOfDay % 12;
                                String selectedTime = String.format("%02d:%02d %s", hour12, minute, amPm);
                                etHoraInicio.setText(selectedTime);
                            }
                        }, hour, minute, false);
                timePickerDialog.show();
            }
        });

        // Configurar TimePicker para la hora de cierre
        etHoraCierre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);
                TimePickerDialog timePickerDialog = new TimePickerDialog(RegistroTurnoActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                String amPm = (hourOfDay >= 12) ? "PM" : "AM";
                                int hour12 = (hourOfDay == 0 || hourOfDay == 12) ? 12 : hourOfDay % 12;
                                String selectedTime = String.format("%02d:%02d %s", hour12, minute, amPm);
                                etHoraCierre.setText(selectedTime);
                            }
                        }, hour, minute, false);
                timePickerDialog.show();
            }
        });

        // Cargar Spinner de cajeros
        cargarSpinnerCajeros();

        // Calcular discrepancia
        btnCalcularDiscrepancia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double billetes = parseDouble(etBilletes.getText().toString());
                double monedas = parseDouble(etMonedas.getText().toString());
                double cheques = parseDouble(etCheques.getText().toString());
                double ventasEsperadas = parseDouble(etVentasEsperadas.getText().toString());
                double totalIngresado = billetes + monedas + cheques;
                discrepanciaCalculada = totalIngresado - ventasEsperadas;
                Toast.makeText(RegistroTurnoActivity.this, "Discrepancia: " + discrepanciaCalculada, Toast.LENGTH_SHORT).show();
                if (Math.abs(discrepanciaCalculada) > 1) {
                    llJustificacion.setVisibility(View.VISIBLE);
                } else {
                    llJustificacion.setVisibility(View.GONE);
                }
            }
        });

        // Mostrar diálogo para elegir entre galería y cámara
        btnAdjuntarFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] opciones = {"Seleccionar de Galería", "Tomar Foto"};
                new AlertDialog.Builder(RegistroTurnoActivity.this)
                        .setTitle("Adjuntar Foto")
                        .setItems(opciones, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    // Seleccionar de Galería
                                    Intent intent = new Intent();
                                    intent.setType("image/*");
                                    intent.setAction(Intent.ACTION_GET_CONTENT);
                                    startActivityForResult(Intent.createChooser(intent, "Selecciona una imagen"), PICK_IMAGE_REQUEST);
                                } else if (which == 1) {
                                    // Tomar Foto
                                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                    startActivityForResult(intent, CAPTURE_IMAGE_REQUEST);
                                }
                            }
                        })
                        .show();
            }
        });

        // Guardar Registro
        btnGuardarRegistro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (TextUtils.isEmpty(etFecha.getText()) ||
                        TextUtils.isEmpty(etHoraInicio.getText()) ||
                        TextUtils.isEmpty(etHoraCierre.getText()) ||
                        TextUtils.isEmpty(etNumeroCaja.getText()) ||
                        TextUtils.isEmpty(etBilletes.getText()) ||
                        TextUtils.isEmpty(etMonedas.getText()) ||
                        TextUtils.isEmpty(etCheques.getText()) ||
                        TextUtils.isEmpty(etVentasEsperadas.getText())) {

                    Toast.makeText(RegistroTurnoActivity.this, "Por favor completa todos los campos obligatorios.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String fecha = etFecha.getText().toString().trim();
                String horaInicio = etHoraInicio.getText().toString().trim();
                String horaCierre = etHoraCierre.getText().toString().trim();
                int numeroCaja = Integer.parseInt(etNumeroCaja.getText().toString().trim());
                String cajero = spinnerCajeros.getSelectedItem().toString();
                double billetes = parseDouble(etBilletes.getText().toString());
                double monedas = parseDouble(etMonedas.getText().toString());
                double cheques = parseDouble(etCheques.getText().toString());
                double ventasEsperadas = parseDouble(etVentasEsperadas.getText().toString());
                double discrepancia = discrepanciaCalculada;

                String justificacion = "";
                if (Math.abs(discrepancia) > 1) {
                    int selectedId = rgJustificacion.getCheckedRadioButtonId();
                    if (selectedId == -1) {
                        Toast.makeText(RegistroTurnoActivity.this, "Selecciona si deseas justificar o no la discrepancia.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (selectedId == rbJustificar.getId()) {
                        justificacion = etComentario.getText().toString().trim();
                        if (justificacion.isEmpty()) {
                            Toast.makeText(RegistroTurnoActivity.this, "Ingresa un comentario para justificar.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } else {
                        justificacion = "no hubo justificación";
                    }
                }

                String evidencia = "";
                if (evidenciaUri != null) {
                    evidencia = evidenciaUri.toString();
                }

                boolean insertado = dbHelper.insertarRegistro(fecha, horaInicio, horaCierre, numeroCaja, cajero,
                        billetes, monedas, cheques, discrepancia, justificacion, evidencia);
                if (insertado) {
                    Toast.makeText(RegistroTurnoActivity.this, "Registro guardado exitosamente.", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(RegistroTurnoActivity.this, "Error al guardar el registro.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            evidenciaUri = data.getData();
            Toast.makeText(this, "Imagen seleccionada de la galería", Toast.LENGTH_SHORT).show();
        } else if (requestCode == CAPTURE_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            // Obtener el thumbnail como Bitmap
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            if (bitmap != null) {
                // Guardar el bitmap en un archivo temporal y obtener su URI
                evidenciaUri = guardarImagenTemporal(bitmap);
                Toast.makeText(this, "Imagen capturada", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Método auxiliar para guardar un Bitmap en un archivo temporal y retornar su URI
    private Uri guardarImagenTemporal(Bitmap bitmap) {
        try {
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs(); // crear directorio si no existe
            File file = new File(cachePath, "evidencia.png");
            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.flush();
            stream.close();
            // Retornar el URI del archivo guardado
            return Uri.fromFile(file);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Método auxiliar para convertir cadena a double
    private double parseDouble(String num) {
        try {
            return Double.parseDouble(num);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // Método para cargar el Spinner de cajeros
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
            listaCajeros.add("No hay cajeros registrados");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, listaCajeros);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCajeros.setAdapter(adapter);
    }
}