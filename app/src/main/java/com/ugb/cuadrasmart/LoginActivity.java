package com.ugb.cuadrasmart;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private CheckBox chbMostrarPassword;
    private Button btnLogin;
    private TextView tvCrearCuenta;
    private DatabaseHelper dbHelper;

    // Parámetro para la validación de la contraseña (por ejemplo, mínimo 6 caracteres)
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inicialización de vistas
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        chbMostrarPassword = findViewById(R.id.chbMostrarPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvCrearCuenta = findViewById(R.id.tvCrearCuenta);

        // Inicializar el DatabaseHelper
        dbHelper = new DatabaseHelper(this);

        // Configurar el CheckBox para mostrar/ocultar la contraseña
        chbMostrarPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        // Configurar el botón de Login
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString();

                // Validar que el correo tenga un formato correcto
                if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    etEmail.setError("Ingresa un correo válido");
                    etEmail.requestFocus();
                    return;
                }
                // Validar que la contraseña no esté vacía y tenga al menos MIN_PASSWORD_LENGTH caracteres
                if (password.isEmpty()) {
                    etPassword.setError("Ingresa la contraseña");
                    etPassword.requestFocus();
                    return;
                }
                if (password.length() < MIN_PASSWORD_LENGTH) {
                    etPassword.setError("La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres");
                    etPassword.requestFocus();
                    return;
                }

                Log.d(TAG, "Validación exitosa, email: " + email);
                // Autenticación en la base de datos
                boolean autenticado = dbHelper.autenticarSupervisor(email, password);
                if (autenticado) {
                    Toast.makeText(LoginActivity.this, "Ingreso exitoso", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Configurar el enlace para crear cuenta
        tvCrearCuenta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegistroActivity.class));
            }
        });
    }
}