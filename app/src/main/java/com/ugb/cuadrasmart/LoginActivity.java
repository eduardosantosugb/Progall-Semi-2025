package com.ugb.cuadrasmart;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private CheckBox cbShowPassword;
    private Button btnLogin;
    private DatabaseHelper dbHelper;

    public static final String PREFS_NAME = "CuadraSmartPrefs";
    public static final String KEY_SELECTED_TIENDA = "selected_tienda";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Inicializar vistas
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        cbShowPassword = findViewById(R.id.cbShowPassword);
        btnLogin = findViewById(R.id.btnLogin);

        dbHelper = new DatabaseHelper(this);

        // Configuración para mostrar/ocultar contraseña
        cbShowPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                // Validar el correo
                if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    etEmail.setError("Ingrese un correo válido");
                    etEmail.requestFocus();
                    return;
                }
                // Validar la contraseña (mínimo 6 caracteres)
                if (password.isEmpty() || password.length() < 6) {
                    etPassword.setError("Ingrese una contraseña válida (mínimo 6 caracteres)");
                    etPassword.requestFocus();
                    return;
                }

                // Autenticación: se asume que authenticateUser devuelve el rol ("cajero" o "supervisor") o null en caso de error
                String role = dbHelper.authenticateUser(email, password);
                if (role == null) {
                    Toast.makeText(LoginActivity.this, getString(R.string.login_error_invalid_credentials), Toast.LENGTH_SHORT).show();
                    return;
                }

                // Autenticación exitosa: verificamos si ya se ha seleccionado una tienda
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                String selectedTienda = prefs.getString(KEY_SELECTED_TIENDA, null);

                Intent intent;
                if (selectedTienda == null || selectedTienda.isEmpty()) {
                    // No hay tienda seleccionada: ir a TiendasActivity para que el usuario seleccione la tienda
                    intent = new Intent(LoginActivity.this, TiendasActivity.class);
                } else {
                    // La tienda ya está seleccionada: ir al menú principal
                    intent = new Intent(LoginActivity.this, MainActivity.class);
                }
                // Opcional: pasar el rol a la siguiente actividad si es necesario
                intent.putExtra("USER_ROLE", role);
                startActivity(intent);
                finish();
            }
        });
    }
}
