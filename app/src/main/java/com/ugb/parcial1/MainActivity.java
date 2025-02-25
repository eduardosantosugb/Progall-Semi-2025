package com.ugb.parcial1;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private TabHost tabHost;
    private EditText txtConsumo, txtCantidad;
    private TextView lblResultadoAgua, lblResultadoArea;
    private Spinner spnDeArea, spnAArea;
    private final Map<String, Double> conversionRates = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configuración de TabHost
        tabHost = findViewById(android.R.id.tabhost);
        tabHost.setup();
        agregarPestana("Pago Agua", R.id.tab1);
        agregarPestana("Conversor de Área", R.id.tab2);

        // Inicialización de componentes
        inicializarPestanaAgua();
        inicializarPestanaArea();
    }

    private void agregarPestana(String nombre, int id) {
        TabHost.TabSpec spec = tabHost.newTabSpec(nombre);
        spec.setContent(id);
        spec.setIndicator(nombre);
        tabHost.addTab(spec);
    }

    private void inicializarPestanaAgua() {
        txtConsumo = findViewById(R.id.txtConsumo);
        lblResultadoAgua = findViewById(R.id.lblResultadoAgua);
        Button btnCalcular = findViewById(R.id.btnCalcularAgua);

        btnCalcular.setOnClickListener(v -> calcularPagoAgua());
    }

    private void calcularPagoAgua() {
        String input = txtConsumo.getText().toString();
        if (input.isEmpty()) {
            Toast.makeText(this, "Ingrese un valor válido", Toast.LENGTH_SHORT).show();
            return;
        }
        int consumo = Integer.parseInt(input);
        double total = 6; // Cuota fija

        if (consumo > 18) {
            int exceso = consumo - 18;
            if (consumo <= 28) {
                total += exceso * 0.45;
            } else {
                total += (10 * 0.45) + ((consumo - 28) * 0.65);
            }
        }
        lblResultadoAgua.setText("Total a pagar: $" + total);
    }

    private void inicializarPestanaArea() {
        txtCantidad = findViewById(R.id.txtCantidadArea);
        spnDeArea = findViewById(R.id.spnDeArea);
        spnAArea = findViewById(R.id.spnAArea);
        lblResultadoArea = findViewById(R.id.lblResultadoArea);
        Button btnConvertir = findViewById(R.id.btnConvertirArea);

        // Definir conversiones
        String[] unidades = {"Pie Cuadrado", "Vara Cuadrada", "Yarda Cuadrada", "Metro Cuadrado", "Tareas", "Manzana", "Hectárea"};
        double[] valores = {1, 1.43, 0.111, 0.0929, 0.00246, 0.000140, 0.00001};
        for (int i = 0; i < unidades.length; i++) {
            for (int j = 0; j < unidades.length; j++) {
                conversionRates.put(unidades[i] + "-" + unidades[j], valores[j] / valores[i]);
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, unidades);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnDeArea.setAdapter(adapter);
        spnAArea.setAdapter(adapter);

        btnConvertir.setOnClickListener(v -> calcularConversionArea());
    }

    private void calcularConversionArea() {
        String cantidadStr = txtCantidad.getText().toString();
        if (cantidadStr.isEmpty()) {
            Toast.makeText(this, "Ingrese un valor válido", Toast.LENGTH_SHORT).show();
            return;
        }
        double cantidad = Double.parseDouble(cantidadStr);
        String de = spnDeArea.getSelectedItem().toString();
        String a = spnAArea.getSelectedItem().toString();

        double resultado = cantidad * conversionRates.get(de + "-" + a);
        lblResultadoArea.setText("Resultado: " + resultado + " " + a);
    }
}
