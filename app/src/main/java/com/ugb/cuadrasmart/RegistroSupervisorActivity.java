package com.ugb.cuadrasmart;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.ugb.cuadrasmart.R;


public class RegistroSupervisorActivity extends AppCompatActivity {

    private EditText etNombreSupervisor, etCorreoSupervisor, etPasswordSupervisor, etCodigoCreacion;
    private Button btnRegistrarSupervisor;
    private DatabaseHelper dbHelper;

    // Código de creación requerido para registrar supervisores (puede configurarse o ser más dinámico)
    private static final String CREATION_CODE = "admin123";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro_supervisor);

        etNombreSupervisor = findViewById(R.id.etNombreSupervisor);
        etCorreoSupervisor = findViewById(R.id.etCorreoSupervisor);
        etPasswordSupervisor = findViewById(R.id.etPasswordSupervisor);
        etCodigoCreacion = findViewById(R.id.etCodigoCreacion);
        btnRegistrarSupervisor = findViewById(R.id.btnRegistrarSupervisor);

        dbHelper = new DatabaseHelper(this);

        btnRegistrarSupervisor.setOnClickListener(view -> {
            String nombre = etNombreSupervisor.getText().toString().trim();
            String correo = etCorreoSupervisor.getText().toString().trim();
            String passwordSupervisor = etPasswordSupervisor.getText().toString().trim();
            String codigoCreacion = etCodigoCreacion.getText().toString().trim();

            if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(correo) ||
                    TextUtils.isEmpty(passwordSupervisor) || TextUtils.isEmpty(codigoCreacion)) {
                Toast.makeText(RegistroSupervisorActivity.this, "Complete todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!codigoCreacion.equals(CREATION_CODE)) {
                Toast.makeText(RegistroSupervisorActivity.this, "Código de creación incorrecto", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean inserted = dbHelper.insertSupervisor(nombre, correo, passwordSupervisor);
            if (inserted) {
                Toast.makeText(RegistroSupervisorActivity.this, "Supervisor registrado exitosamente", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(RegistroSupervisorActivity.this, "Error al registrar supervisor", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
