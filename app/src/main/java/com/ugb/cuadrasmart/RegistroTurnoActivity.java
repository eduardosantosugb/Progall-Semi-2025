package com.ugb.cuadrasmart;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
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
import androidx.appcompat.app.AppCompatActivity;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro_turno);

        // Referenciar vistas del layout
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

        // Evitar que el teclado se abra manualmente en fecha y hora
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
                // Formato 12 horas: último parámetro false
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

        // Cargar Spinner de cajeros (asegúrate de que el ID es "spinnerCajeros")
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

        // Funcionalidad para Adjuntar Foto (pendiente)
        btnAdjuntarFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(RegistroTurnoActivity.this, "Funcionalidad de adjuntar foto (pendiente)", Toast.LENGTH_SHORT).show();
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