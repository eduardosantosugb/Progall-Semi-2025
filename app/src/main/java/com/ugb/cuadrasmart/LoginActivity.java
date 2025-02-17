package com.ugb.cuadrasmart;


import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private CheckBox chbMostrarPassword;
    private Button btnLogin;
    private TextView tvCrearCuenta;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Referenciar elementos del layout
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        chbMostrarPassword = findViewById(R.id.chbMostrarPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvCrearCuenta = findViewById(R.id.tvCrearCuenta);

        // Configurar el checkbox para mostrar/ocultar la contraseña
        chbMostrarPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Mostrar la contraseña en texto plano
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                // Volver a enmascarar la contraseña
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            // Colocar el cursor al final del texto
            etPassword.setSelection(etPassword.getText().length());
        });

        // Configurar el botón de login
        btnLogin.setOnClickListener(view -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();

            // Aquí se agregará la lógica de autenticación (por ahora, solo un ejemplo básico)
            if (!email.isEmpty() && !password.isEmpty()) {
                // Suponiendo que la autenticación es exitosa, se inicia MainActivity
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                // Aquí podrías mostrar un mensaje de error (usando Toast, Snackbar, etc.)
            }
        });

        // Configurar el enlace "Crear Cuenta"
        tvCrearCuenta.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, RegistroActivity.class);
            startActivity(intent);
        });
    }
}