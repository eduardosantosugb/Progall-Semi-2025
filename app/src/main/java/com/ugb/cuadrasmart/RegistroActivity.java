package com.ugb.cuadrasmart;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class RegistroActivity extends AppCompatActivity {

    private EditText etRegistroEmail, etRegistroPassword, etConfirmarPassword, etClaveSupervisor;
    private Button btnRegistrar;
    private DatabaseHelper dbHelper;
    // Clave de supervisor para la versión académica
    private static final String CLAVE_SUPERVISOR = "SUPERV1234";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        // Referenciar elementos del layout
        etRegistroEmail = findViewById(R.id.etRegistroEmail);
        etRegistroPassword = findViewById(R.id.etRegistroPassword);
        etConfirmarPassword = findViewById(R.id.etConfirmarPassword);
        etClaveSupervisor = findViewById(R.id.etClaveSupervisor);
        btnRegistrar = findViewById(R.id.btnRegistrar);

        // Inicializar DatabaseHelper
        dbHelper = new DatabaseHelper(this);

        btnRegistrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Obtener datos ingresados
                String email = etRegistroEmail.getText().toString().trim();
                String password = etRegistroPassword.getText().toString();
                String confirmarPassword = etConfirmarPassword.getText().toString();
                String claveSupervisor = etClaveSupervisor.getText().toString().trim();

                // Validaciones
                if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    etRegistroEmail.setError("Ingresa un email válido");
                    etRegistroEmail.requestFocus();
                    return;
                }
                if (password.isEmpty()) {
                    etRegistroPassword.setError("Ingresa una contraseña");
                    etRegistroPassword.requestFocus();
                    return;
                }
                if (!password.equals(confirmarPassword)) {
                    etConfirmarPassword.setError("Las contraseñas no coinciden");
                    etConfirmarPassword.requestFocus();
                    return;
                }
                if (claveSupervisor.isEmpty()) {
                    etClaveSupervisor.setError("Ingresa la clave de supervisor");
                    etClaveSupervisor.requestFocus();
                    return;
                }
                if (!claveSupervisor.equals(CLAVE_SUPERVISOR)) {
                    etClaveSupervisor.setError("Clave de supervisor incorrecta");
                    etClaveSupervisor.requestFocus();
                    return;
                }

                // Intentar registrar al supervisor en la base de datos
                boolean registroExitoso = dbHelper.registrarSupervisor(email, password);
                if (registroExitoso) {
                    Toast.makeText(RegistroActivity.this, "Cuenta creada exitosamente", Toast.LENGTH_SHORT).show();
                    finish(); // Cierra la actividad y vuelve a la pantalla de login
                } else {
                    Toast.makeText(RegistroActivity.this, "Error al crear la cuenta. Verifica si el email ya existe.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}