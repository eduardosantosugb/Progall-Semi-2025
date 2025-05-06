package com.ugb.cuadrasmart;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.ugb.cuadrasmart.R;


public class RegistroCajeroActivity extends AppCompatActivity {

    private EditText etNombreCajero, etCorreoCajero, etPasswordCajero, etSupervisorPassword;
    private Button btnRegistrarCajero;
    private DatabaseHelper dbHelper;

    // Contraseña del supervisor para autorizar el registro de cajeros (puede configurarse o leerse de forma segura)
    private static final String SUPERVISOR_PASSWORD = "supervisor123";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro_cajero);

        etNombreCajero = findViewById(R.id.etNombreCajero);
        etCorreoCajero = findViewById(R.id.etCorreoCajero);
        etPasswordCajero = findViewById(R.id.etPasswordCajero);
        etSupervisorPassword = findViewById(R.id.etSupervisorPassword);
        btnRegistrarCajero = findViewById(R.id.btnRegistrarCajero);

        dbHelper = new DatabaseHelper(this);

        btnRegistrarCajero.setOnClickListener(view -> {
            String nombre = etNombreCajero.getText().toString().trim();
            String correo = etCorreoCajero.getText().toString().trim();
            String passwordCajero = etPasswordCajero.getText().toString().trim();
            String supervisorPass = etSupervisorPassword.getText().toString().trim();

            if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(correo) ||
                    TextUtils.isEmpty(passwordCajero) || TextUtils.isEmpty(supervisorPass)) {
                Toast.makeText(RegistroCajeroActivity.this, "Complete todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!supervisorPass.equals(SUPERVISOR_PASSWORD)) {
                Toast.makeText(RegistroCajeroActivity.this, "Contraseña de supervisor incorrecta", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean inserted = dbHelper.insertCajero(nombre, correo, passwordCajero);
            if (inserted) {
                Toast.makeText(RegistroCajeroActivity.this, "Cajero registrado exitosamente", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(RegistroCajeroActivity.this, "Error al registrar cajero", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
