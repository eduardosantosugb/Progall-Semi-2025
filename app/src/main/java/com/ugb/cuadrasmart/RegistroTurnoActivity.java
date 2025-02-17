package com.ugb.cuadrasmart;

import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class RegistroTurnoActivity extends AppCompatActivity {

    private EditText etFecha, etHoraInicio, etHoraCierre, etNumeroCaja, etBilletes, etMonedas, etCheques, etVentasEsperadas, etComentario;
    private Spinner spinnerCajeros;
    private Button btnCalcularDiscrepancia, btnAdjuntarFoto, btnGuardarRegistro;
    private LinearLayout llJustificacion;
    private RadioGroup rgJustificacion;
    private RadioButton rbJustificar, rbNoJustificar;

    private DatabaseHelper dbHelper;

    // Variable para almacenar la discrepancia calculada
    private double discrepanciaCalculada = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro_turno);

        // Referenciar elementos del layout
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

        // Cargar el Spinner de cajeros usando el método actualizado
        cargarSpinnerCajeros();

        // Calcular Discrepancia al pulsar el botón
        btnCalcularDiscrepancia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Obtener los valores numéricos ingresados
                double billetes = parseDouble(etBilletes.getText().toString());
                double monedas = parseDouble(etMonedas.getText().toString());
                double cheques = parseDouble(etCheques.getText().toString());
                double ventasEsperadas = parseDouble(etVentasEsperadas.getText().toString());

                double totalIngresado = billetes + monedas + cheques;
                discrepanciaCalculada = totalIngresado - ventasEsperadas;

                Toast.makeText(RegistroTurnoActivity.this, "Discrepancia: " + discrepanciaCalculada, Toast.LENGTH_SHORT).show();

                // Si la discrepancia es mayor a 1 (o menor a -1), se muestra el panel de justificación
                if (Math.abs(discrepanciaCalculada) > 1) {
                    llJustificacion.setVisibility(View.VISIBLE);
                } else {
                    llJustificacion.setVisibility(View.GONE);
                }
            }
        });

        // Botón para "Adjuntar Foto" (Funcionalidad pendiente)
        btnAdjuntarFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(RegistroTurnoActivity.this, "Funcionalidad de adjuntar foto (pendiente)", Toast.LENGTH_SHORT).show();
            }
        });

        // Guardar el registro al pulsar el botón "Guardar Registro"
        btnGuardarRegistro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Validar campos obligatorios
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

                // Recoger valores de los campos
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

                // Validar justificación si la discrepancia es mayor a 1
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

                // La evidencia (foto) se deja vacía o se podría asignar una ruta temporal
                String evidencia = "";

                // Insertar registro en la base de datos
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

    // Método auxiliar para convertir a double, devuelve 0.0 en caso de error
    private double parseDouble(String num) {
        try {
            return Double.parseDouble(num);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // Método para cargar los nombres de cajeros en el Spinner utilizando getColumnIndexOrThrow
    private void cargarSpinnerCajeros() {
        List<String> listaCajeros = new ArrayList<>();

        Cursor cursor = dbHelper.obtenerCajeros();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                // Usamos getColumnIndexOrThrow para asegurarnos de obtener el índice correcto
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