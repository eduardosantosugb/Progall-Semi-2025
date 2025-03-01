package com.ugb.cuadrasmart;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
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

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;

public class EditRegistroActivity extends AppCompatActivity {

    private EditText etEditFecha, etEditHoraInicio, etEditHoraCierre, etEditNumeroCaja;
    private Spinner spinnerEditCajeros;
    private EditText etEditBilletes, etEditMonedas, etEditCheques, etEditVentasEsperadas;
    private Button btnEditCalcularDiscrepancia, btnEditAdjuntarFoto, btnGuardarCambios;
    private EditText etEditComentario;
    private LinearLayout llEditJustificacion;

    private DatabaseHelper dbHelper;
    private int registroId;
    private double discrepanciaCalculada = 0.0;

    public static final String PREFS_NAME = "CuadraSmartPrefs";
    public static final String KEY_SELECTED_TIENDA = "selected_tienda";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_registro);

        // Obtener referencias a los componentes del layout
        etEditFecha = findViewById(R.id.etEditFecha);
        etEditHoraInicio = findViewById(R.id.etEditHoraInicio);
        etEditHoraCierre = findViewById(R.id.etEditHoraCierre);
        etEditNumeroCaja = findViewById(R.id.etEditNumeroCaja);
        spinnerEditCajeros = findViewById(R.id.spinnerEditCajeros);
        etEditBilletes = findViewById(R.id.etEditBilletes);
        etEditMonedas = findViewById(R.id.etEditMonedas);
        etEditCheques = findViewById(R.id.etEditCheques);
        etEditVentasEsperadas = findViewById(R.id.etEditVentasEsperadas);
        btnEditCalcularDiscrepancia = findViewById(R.id.btnEditCalcularDiscrepancia);
        btnEditAdjuntarFoto = findViewById(R.id.btnEditAdjuntarFoto);
        btnGuardarCambios = findViewById(R.id.btnGuardarCambios);
        etEditComentario = findViewById(R.id.etEditComentario);
        llEditJustificacion = findViewById(R.id.llEditJustificacion);

        dbHelper = new DatabaseHelper(this);

        // Configurar DatePicker y TimePicker para los campos
        configurarPickerFecha(etEditFecha);
        configurarPickerHora(etEditHoraInicio);
        configurarPickerHora(etEditHoraCierre);

        // Cargar el Spinner de cajeros
        cargarSpinnerEditCajeros();

        // Obtener el registroId enviado desde la actividad anterior
        registroId = getIntent().getIntExtra("REGISTRO_ID", -1);
        if (registroId == -1) {
            Toast.makeText(this, "Registro no encontrado", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            cargarDatosRegistro(registroId);
        }

        // Botón para calcular la discrepancia (solo se consideran billetes y monedas)
        btnEditCalcularDiscrepancia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double billetes = parseDouble(etEditBilletes.getText().toString());
                double monedas = parseDouble(etEditMonedas.getText().toString());
                double ventasEsperadas = parseDouble(etEditVentasEsperadas.getText().toString());
                double totalIngresado = billetes + monedas; // Cheques se ignoran en el cálculo
                discrepanciaCalculada = discrepancias(totalIngresado, ventasEsperadas);
                Toast.makeText(EditRegistroActivity.this, "Discrepancia: " + discrepanciaCalculada, Toast.LENGTH_SHORT).show();
                if (Math.abs(discrepanciaCalculada) > 1) {
                    llEditJustificacion.setVisibility(View.VISIBLE);
                } else {
                    llEditJustificacion.setVisibility(View.GONE);
                }
            }
        });

        // Botón para adjuntar foto (funcionalidad pendiente)
        btnEditAdjuntarFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(EditRegistroActivity.this, "Funcionalidad de adjuntar foto pendiente", Toast.LENGTH_SHORT).show();
            }
        });

        // Botón para guardar cambios
        btnGuardarCambios.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Validar campos obligatorios
                if (TextUtils.isEmpty(etEditFecha.getText()) ||
                        TextUtils.isEmpty(etEditHoraInicio.getText()) ||
                        TextUtils.isEmpty(etEditHoraCierre.getText()) ||
                        TextUtils.isEmpty(etEditNumeroCaja.getText()) ||
                        TextUtils.isEmpty(etEditBilletes.getText()) ||
                        TextUtils.isEmpty(etEditMonedas.getText()) ||
                        TextUtils.isEmpty(etEditVentasEsperadas.getText())) {
                    Toast.makeText(EditRegistroActivity.this, "Complete todos los campos obligatorios", Toast.LENGTH_SHORT).show();
                    return;
                }

                String fecha = etEditFecha.getText().toString().trim();
                String horaInicio = etEditHoraInicio.getText().toString().trim();
                String horaCierre = etEditHoraCierre.getText().toString().trim();
                int numeroCaja = Integer.parseInt(etEditNumeroCaja.getText().toString().trim());
                String cajero = spinnerEditCajeros.getSelectedItem().toString();
                double billetes = parseDouble(etEditBilletes.getText().toString());
                double monedas = parseDouble(etEditMonedas.getText().toString());
                double cheques = parseDouble(etEditCheques.getText().toString());
                double ventasEsperadas = parseDouble(etEditVentasEsperadas.getText().toString());
                String justificacion;
                if (llEditJustificacion.getVisibility() == View.VISIBLE) {
                    justificacion = etEditComentario.getText().toString().trim();
                    if (TextUtils.isEmpty(justificacion)) {
                        Toast.makeText(EditRegistroActivity.this, "Ingrese una justificación", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    justificacion = "no hubo justificación";
                }

                // En este ejemplo, no se maneja evidencia; se deja vacío
                String evidencia = "";
                // Recuperar la tienda global seleccionada
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                String tienda = prefs.getString(KEY_SELECTED_TIENDA, "Sin Tienda");

                boolean actualizado = dbHelper.actualizarRegistro(registroId, fecha, horaInicio, horaCierre, numeroCaja, cajero,
                        billetes, monedas, cheques, ventasEsperadas, discrepanciaCalculada, justificacion, evidencia, tienda);
                if (actualizado) {
                    Toast.makeText(EditRegistroActivity.this, "Registro actualizado exitosamente", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditRegistroActivity.this, "Error al actualizar el registro", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Método auxiliar para convertir String a double
    private double parseDouble(String num) {
        try {
            return Double.parseDouble(num);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // Método para calcular discrepancia (billetes + monedas - ventasEsperadas)
    private double discrepancias(double totalIngresado, double ventasEsperadas) {
        return totalIngresado - ventasEsperadas;
    }

    // Método para cargar el Spinner de cajeros para edición
    private void cargarSpinnerEditCajeros() {
        // Datos ficticios. Si tienes datos reales, reemplaza con una consulta a la BD.
        String[] cajerosFicticios = {"cajero1@example.com", "cajero2@example.com", "cajero3@example.com"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, cajerosFicticios);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEditCajeros.setAdapter(adapter);
    }

    // Configurar DatePickerDialog para el campo de fecha
    private void configurarPickerFecha(final EditText editText) {
        editText.setFocusable(false);
        editText.setClickable(true);
        editText.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Calendar cal = Calendar.getInstance();
                new DatePickerDialog(EditRegistroActivity.this, new DatePickerDialog.OnDateSetListener(){
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        String fechaStr = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year);
                        editText.setText(fechaStr);
                    }
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
    }

    // Configurar TimePickerDialog para el campo de hora
    private void configurarPickerHora(final EditText editText) {
        editText.setFocusable(false);
        editText.setClickable(true);
        editText.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Calendar cal = Calendar.getInstance();
                new TimePickerDialog(EditRegistroActivity.this, new TimePickerDialog.OnTimeSetListener(){
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        String amPm = (hourOfDay >= 12) ? "PM" : "AM";
                        int hour12 = (hourOfDay == 0 || hourOfDay == 12) ? 12 : hourOfDay % 12;
                        String timeStr = String.format("%02d:%02d %s", hour12, minute, amPm);
                        editText.setText(timeStr);
                    }
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show();
            }
        });
    }

    // Cargar los datos del registro para editarlos
    private void cargarDatosRegistro(int registroId) {
        Cursor cursor = dbHelper.getReadableDatabase().query(DatabaseHelper.TABLE_REGISTROS,
                null,
                DatabaseHelper.COLUMN_REG_ID + " = ?",
                new String[]{String.valueOf(registroId)},
                null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            etEditFecha.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_FECHA)));
            etEditHoraInicio.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_HORA_INICIO)));
            etEditHoraCierre.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_HORA_CIERRE)));
            etEditNumeroCaja.setText(String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_NUMERO_CAJA))));
            String cajero = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_CAJERO));
            setSpinnerSelection(spinnerEditCajeros, cajero);
            etEditBilletes.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_BILLETES))));
            etEditMonedas.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_MONEDAS))));
            etEditCheques.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_CHEQUES))));
            etEditVentasEsperadas.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_VENTAS_ESPERADAS))));
            discrepanciaCalculada = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_DISCREPANCIA));
            String justificacion = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_JUSTIFICACION));
            if (!TextUtils.isEmpty(justificacion) && !justificacion.equalsIgnoreCase("no hubo justificación")) {
                etEditComentario.setText(justificacion);
                llEditJustificacion.setVisibility(View.VISIBLE);
            } else {
                llEditJustificacion.setVisibility(View.GONE);
            }
            cursor.close();
        }
    }

    // Método para establecer la selección del spinner según un valor dado
    private void setSpinnerSelection(Spinner spinner, String value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }
}
