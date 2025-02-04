package com.ugb.miprimeraaplicacion;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText txtNum1, txtNum2;
    private TextView lblRespuesta;
    private RadioButton optSuma, optResta, optMultiplicacion, optDivision, optExponente, optPorcentaje, optRaiz, optFactorial, optModulo, optMayor;
    private Button btnCalcular;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtNum1 = findViewById(R.id.txtNum1);
        txtNum2 = findViewById(R.id.txtNum2);
        lblRespuesta = findViewById(R.id.lblRespuesta);
        optSuma = findViewById(R.id.optSuma);
        optResta = findViewById(R.id.optResta);
        optMultiplicacion = findViewById(R.id.optMultiplicacion);
        optDivision = findViewById(R.id.optDivision);
        optExponente = findViewById(R.id.optExponente);
        optPorcentaje = findViewById(R.id.optPorcentaje);
        optRaiz = findViewById(R.id.optRaiz);
        optFactorial = findViewById(R.id.optFactorial);
        optModulo = findViewById(R.id.optModulo);
        optMayor = findViewById(R.id.optMayor);
        btnCalcular = findViewById(R.id.btnCalcular);

        btnCalcular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calcular();
            }
        });
    }

    private void calcular() {
        double num1 = Double.parseDouble(txtNum1.getText().toString());
        double num2 = Double.parseDouble(txtNum2.getText().toString());
        double resultado = 0;

        if (optSuma.isChecked()) {
            resultado = num1 + num2;
        } else if (optResta.isChecked()) {
            resultado = num1 - num2;
        } else if (optMultiplicacion.isChecked()) {
            resultado = num1 * num2;
        } else if (optDivision.isChecked()) {
            if (num2 != 0) {
                resultado = num1 / num2;
            } else {
                lblRespuesta.setText("Error: División por cero");
                return;
            }
        } else if (optExponente.isChecked()) {
            resultado = Math.pow(num1, num2);
        } else if (optPorcentaje.isChecked()) {
            resultado = (num1 * num2) / 100;
        } else if (optRaiz.isChecked()) {
            resultado = Math.pow(num1, 1.0 / num2);
        } else if (optFactorial.isChecked()) {
            resultado = factorial((int) num1);
        } else if (optModulo.isChecked()) {
            resultado = num1 % num2;
        } else if (optMayor.isChecked()) {
            resultado = Math.max(num1, num2);
        }

        lblRespuesta.setText("Respuesta: " + resultado);
    }

    private int factorial(int n) {
        if (n == 0 || n == 1) {
            return 1;
        } else {
            return n * factorial(n - 1);
        }
    }
}