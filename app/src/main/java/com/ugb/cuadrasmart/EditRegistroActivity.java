package com.ugb.cuadrasmart;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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

public class EditRegistroActivity extends AppCompatActivity {

    private EditText etEditFecha, etEditHoraInicio, etEditHoraCierre, etEditNumeroCaja;
    private EditText etEditBilletes, etEditMonedas, etEditCheques, etEditVentasEsperadas, etEditComentario;
    private Spinner spinnerEditCajeros;
    private Button btnEditCalcularDiscrepancia, btnEditAdjuntarFoto, btnGuardarCambios;
    private LinearLayout llEditJustificacion;
    private RadioGroup rgEditJustificacion;
    private RadioButton rbEditJustificar, rbEditNoJustificar;

    private DatabaseHelper dbHelper;
    private int registroId; // ID del registro a editar
    private double discrepanciaCalculada = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_registro);

        // Inicializar vistas
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
        llEditJustificacion = findViewById(R.id.llEditJustificacion);
        rgEditJustificacion = findViewById(R.id.rgEditJustificacion);
        rbEditJustificar = findViewById(R.id.rbEditJustificar);
        rbEditNoJustificar = findViewById(R.id.rbEditNoJustificar);
        etEditComentario = findViewById(R.id.etEditComentario);
        btnEditAdjuntarFoto = findViewById(R.id.btnEditAdjuntarFoto);
        btnGuardarCambios = findViewById(R.id.btnGuardarCambios);

        dbHelper = new DatabaseHelper(this);

        // Obtener el ID del registro a editar desde el Intent
        registroId = getIntent().getIntExtra("REGISTRO_ID", -1);
        if (registroId == -1) {
            Toast.makeText(this, "Registro no válido", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Cargar datos del registro y llenar campos
        cargarDatosRegistro();

        // Cargar Spinner de cajeros
        cargarSpinnerCajeros();

        // Calcular discrepancia al pulsar el botón
        btnEditCalcularDiscrepancia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                double billetes = parseDouble(etEditBilletes.getText().toString());
                double monedas = parseDouble(etEditMonedas.getText().toString());
                double cheques = parseDouble(etEditCheques.getText().toString());
                double ventasEsperadas = parseDouble(etEditVentasEsperadas.getText().toString());

                double totalIngresado = billetes + monedas + cheques;
                discrepanciaCalculada = totalIngresado - ventasEsperadas;

                Toast.makeText(EditRegistroActivity.this, "Discrepancia: " + discrepanciaCalculada, Toast.LENGTH_SHORT).show();

                if (Math.abs(discrepanciaCalculada) > 1) {
                    llEditJustificacion.setVisibility(View.VISIBLE);
                } else {
                    llEditJustificacion.setVisibility(View.GONE);
                }
            }
        });

        // Funcionalidad pendiente para Adjuntar Foto
        btnEditAdjuntarFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(EditRegistroActivity.this, "Funcionalidad de adjuntar foto (pendiente)", Toast.LENGTH_SHORT).show();
            }
        });

        // Guardar cambios con confirmación
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
                        TextUtils.isEmpty(etEditCheques.getText()) ||
                        TextUtils.isEmpty(etEditVentasEsperadas.getText())) {

                    Toast.makeText(EditRegistroActivity.this, "Por favor completa todos los campos obligatorios.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Mostrar diálogo de confirmación antes de guardar cambios
                new AlertDialog.Builder(EditRegistroActivity.this)
                        .setTitle("Confirmación")
                        .setMessage("¿Está seguro que desea guardar los cambios?")
                        .setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                // Recoger datos de los campos
                                String fecha = etEditFecha.getText().toString().trim();
                                String horaInicio = etEditHoraInicio.getText().toString().trim();
                                String horaCierre = etEditHoraCierre.getText().toString().trim();
                                int numeroCaja = Integer.parseInt(etEditNumeroCaja.getText().toString().trim());
                                String cajero = spinnerEditCajeros.getSelectedItem().toString();
                                double billetes = parseDouble(etEditBilletes.getText().toString());
                                double monedas = parseDouble(etEditMonedas.getText().toString());
                                double cheques = parseDouble(etEditCheques.getText().toString());
                                double ventasEsperadas = parseDouble(etEditVentasEsperadas.getText().toString());
                                double discrepancia = discrepanciaCalculada;

                                String justificacion = "";
                                if (Math.abs(discrepancia) > 1) {
                                    int selectedId = rgEditJustificacion.getCheckedRadioButtonId();
                                    if (selectedId == -1) {
                                        Toast.makeText(EditRegistroActivity.this, "Selecciona si deseas justificar o no la discrepancia.", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    if (selectedId == rbEditJustificar.getId()) {
                                        justificacion = etEditComentario.getText().toString().trim();
                                        if (justificacion.isEmpty()) {
                                            Toast.makeText(EditRegistroActivity.this, "Ingresa un comentario para justificar.", Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                    } else {
                                        justificacion = "no hubo justificación";
                                    }
                                }

                                String evidencia = "";

                                boolean actualizado = dbHelper.actualizarRegistro(registroId, fecha, horaInicio, horaCierre, numeroCaja,
                                        cajero, billetes, monedas, cheques, discrepancia, justificacion, evidencia);

                                if (actualizado) {
                                    Toast.makeText(EditRegistroActivity.this, "Registro actualizado exitosamente.", Toast.LENGTH_SHORT).show();
                                    finish();
                                } else {
                                    Toast.makeText(EditRegistroActivity.this, "Error al actualizar el registro.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });
    }

    // Método para cargar los datos del registro a editar
    private void cargarDatosRegistro() {
        Cursor cursor = dbHelper.obtenerRegistros();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_ID));
                if (id == registroId) {
                    etEditFecha.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_FECHA)));
                    etEditHoraInicio.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_HORA_INICIO)));
                    etEditHoraCierre.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_HORA_CIERRE)));
                    etEditNumeroCaja.setText(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_NUMERO_CAJA)));
                    etEditBilletes.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_BILLETES))));
                    etEditMonedas.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_MONEDAS))));
                    etEditCheques.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_CHEQUES))));
                    // Para "Ventas Esperadas" se asume que en este caso se almacena la discrepancia; ajusta según tu lógica
                    etEditVentasEsperadas.setText(String.valueOf(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_DISCREPANCIA))));

                    String justificacion = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REG_JUSTIFICACION));
                    if (justificacion != null && !justificacion.equals("no hubo justificación") && !justificacion.isEmpty()) {
                        rbEditJustificar.setChecked(true);
                        etEditComentario.setText(justificacion);
                        llEditJustificacion.setVisibility(View.VISIBLE);
                    } else {
                        rbEditNoJustificar.setChecked(true);
                        llEditJustificacion.setVisibility(View.GONE);
                    }
                    break;
                }
            }
            cursor.close();
        }
    }

    // Método auxiliar para convertir a double
    private double parseDouble(String num) {
        try {
            return Double.parseDouble(num);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    // Método para cargar los nombres de cajeros en el Spinner
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
        spinnerEditCajeros.setAdapter(adapter);
    }
}