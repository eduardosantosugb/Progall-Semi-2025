package com.ugb.cuadrasmart;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;

public class RegistroTurnoActivity extends AppCompatActivity {

    private EditText etFecha, etHoraInicio, etHoraCierre, etNumeroCaja;
    private Spinner spinnerCajeros;
    private EditText etBilletes, etMonedas, etCheques, etVentasEsperadas;
    private Button btnCalcularDiscrepancia, btnAdjuntarFoto, btnGuardarRegistro;
    private LinearLayout llJustificacion;
    private EditText etComentario;

    private DatabaseHelper dbHelper;
    private Uri evidenciaUri = null;
    private double discrepanciaCalculada = 0.0;

    public static final String PREFS_NAME = "CuadraSmartPrefs";
    public static final String KEY_SELECTED_TIENDA = "selected_tienda";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        // Inicialización de vistas
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
        btnAdjuntarFoto = findViewById(R.id.btnAdjuntarFoto);
        btnGuardarRegistro = findViewById(R.id.btnGuardarRegistro);
        llJustificacion = findViewById(R.id.llJustificacion);
        etComentario = findViewById(R.id.etComentario);

        dbHelper = new DatabaseHelper(this);

        // Configurar DatePicker para la fecha
        etFecha.setFocusable(false);
        etFecha.setClickable(true);
        etFecha.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar cal = Calendar.getInstance();
                new DatePickerDialog(RegistroTurnoActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker dp, int year, int month, int dayOfMonth) {
                        String fechaStr = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year);
                        etFecha.setText(fechaStr);
                    }
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        // Configurar TimePicker para la hora de inicio
        etHoraInicio.setFocusable(false);
        etHoraInicio.setClickable(true);
        etHoraInicio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar cal = Calendar.getInstance();
                new TimePickerDialog(RegistroTurnoActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker tp, int hourOfDay, int minute) {
                        String amPm = (hourOfDay >= 12) ? "PM" : "AM";
                        int hour12 = (hourOfDay == 0 || hourOfDay == 12) ? 12 : hourOfDay % 12;
                        String timeStr = String.format("%02d:%02d %s", hour12, minute, amPm);
                        etHoraInicio.setText(timeStr);
                    }
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show();
            }
        });

        // Configurar TimePicker para la hora de cierre
        etHoraCierre.setFocusable(false);
        etHoraCierre.setClickable(true);
        etHoraCierre.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar cal = Calendar.getInstance();
                new TimePickerDialog(RegistroTurnoActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker tp, int hourOfDay, int minute) {
                        String amPm = (hourOfDay >= 12) ? "PM" : "AM";
                        int hour12 = (hourOfDay == 0 || hourOfDay == 12) ? 12 : hourOfDay % 12;
                        String timeStr = String.format("%02d:%02d %s", hour12, minute, amPm);
                        etHoraCierre.setText(timeStr);
                    }
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show();
            }
        });

        // Cargar Spinner de Cajeros (esto requiere que ya tengas cajeros registrados en la BD)
        cargarSpinnerCajeros();

        // Botón para Calcular Discrepancia: Solo se consideran billetes y monedas
        btnCalcularDiscrepancia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                double billetes = parseDouble(etBilletes.getText().toString());
                double monedas = parseDouble(etMonedas.getText().toString());
                double ventasEsperadas = parseDouble(etVentasEsperadas.getText().toString());
                // Cheques no se cuentan para cuadratura, se ignoran en el cálculo
                double totalIngresado = billetes + monedas;
                discrepanciaCalculada = totalIngresado - ventasEsperadas;
                Toast.makeText(RegistroTurnoActivity.this, "Discrepancia: " + discrepanciaCalculada, Toast.LENGTH_SHORT).show();

                // Mostrar el panel de justificación si la discrepancia supera 1 dólar
                if (Math.abs(discrepanciaCalculada) > 1) {
                    llJustificacion.setVisibility(View.VISIBLE);
                } else {
                    llJustificacion.setVisibility(View.GONE);
                }
            }
        });

        // Botón para adjuntar foto (la funcionalidad puede ser implementada para elegir de galería o tomar foto)
        btnAdjuntarFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Aquí se implementaría la lógica para adjuntar foto
                Toast.makeText(RegistroTurnoActivity.this, "Funcionalidad de adjuntar foto", Toast.LENGTH_SHORT).show();
            }
        });

        // Botón para Guardar Registro
        btnGuardarRegistro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Validar campos obligatorios
                if (TextUtils.isEmpty(etFecha.getText()) ||
                        TextUtils.isEmpty(etHoraInicio.getText()) ||
                        TextUtils.isEmpty(etHoraCierre.getText()) ||
                        TextUtils.isEmpty(etNumeroCaja.getText()) ||
                        TextUtils.isEmpty(etBilletes.getText()) ||
                        TextUtils.isEmpty(etMonedas.getText()) ||
                        TextUtils.isEmpty(etVentasEsperadas.getText())) {
                    Toast.makeText(RegistroTurnoActivity.this, "Complete todos los campos obligatorios", Toast.LENGTH_SHORT).show();
                    return;
                }

                String fecha = etFecha.getText().toString().trim();
                String horaInicio = etHoraInicio.getText().toString().trim();
                String horaCierre = etHoraCierre.getText().toString().trim();
                int numeroCaja = Integer.parseInt(etNumeroCaja.getText().toString().trim());
                // Seleccionar cajero del Spinner
                String cajero = spinnerCajeros.getSelectedItem().toString();
                double billetes = parseDouble(etBilletes.getText().toString());
                double monedas = parseDouble(etMonedas.getText().toString());
                double cheques = parseDouble(etCheques.getText().toString());
                double ventasEsperadas = parseDouble(etVentasEsperadas.getText().toString());
                String justificacion = "";
                if (llJustificacion.getVisibility() == View.VISIBLE) {
                    justificacion = etComentario.getText().toString().trim();
                    if (TextUtils.isEmpty(justificacion)) {
                        Toast.makeText(RegistroTurnoActivity.this, "Ingrese una justificación", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    justificacion = "no hubo justificación";
                }

                // Obtener la tienda global seleccionada
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                String tienda = prefs.getString(KEY_SELECTED_TIENDA, "Sin Tienda");

                // Insertar el registro en la base de datos
                boolean insertado = dbHelper.insertarRegistro(fecha, horaInicio, horaCierre, numeroCaja, cajero,
                        billetes, monedas, cheques, ventasEsperadas, discrepanciaCalculada, justificacion,
                        (evidenciaUri != null ? evidenciaUri.toString() : ""), tienda);
                if (insertado) {
                    Toast.makeText(RegistroTurnoActivity.this, "Registro guardado exitosamente", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(RegistroTurnoActivity.this, "Error al guardar el registro", Toast.LENGTH_SHORT).show();
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
        // Aquí puedes cargar los datos de cajeros desde la base de datos.
        // Para este ejemplo, usaremos datos ficticios.
        String[] cajerosFicticios = {"cajero1@example.com", "cajero2@example.com", "cajero3@example.com"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, cajerosFicticios);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCajeros.setAdapter(adapter);
    }
}
