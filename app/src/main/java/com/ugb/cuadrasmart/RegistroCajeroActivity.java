package com.ugb.cuadrasmart;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem; // Import para MenuItem
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull; // Import para @NonNull
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Import para Toolbar

// No es necesario importar R directamente si está en el mismo paquete
// import com.ugb.cuadrasmart.R;


public class RegistroCajeroActivity extends AppCompatActivity {

    private EditText etNombreCajero, etCorreoCajero, etPasswordCajero, etSupervisorPassword;
    private Button btnRegistrarCajero;
    private DatabaseHelper dbHelper;
    private Toolbar toolbarRegistroCajero; // Para la Toolbar

    // Contraseña del supervisor para autorizar el registro de cajeros (puede configurarse o leerse de forma segura)
    // Considera mover esto a un lugar más seguro o hacerlo configurable si es una app real.
    private static final String SUPERVISOR_PASSWORD_VALIDATION = "supervisor123"; // Cambiado el nombre para evitar confusión

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro_cajero);

        // Configuración de la Toolbar
        toolbarRegistroCajero = findViewById(R.id.toolbarRegistroCajero);
        setSupportActionBar(toolbarRegistroCajero);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.register_cashier); // Usar string resource
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Encontrar vistas del layout
        // Los IDs dentro de los TextInputLayout son los de los TextInputEditText
        etNombreCajero = findViewById(R.id.etNombreCajero);
        etCorreoCajero = findViewById(R.id.etCorreoCajero);
        etPasswordCajero = findViewById(R.id.etPasswordCajero);
        etSupervisorPassword = findViewById(R.id.etSupervisorPassword);
        btnRegistrarCajero = findViewById(R.id.btnRegistrarCajero);

        dbHelper = new DatabaseHelper(this);

        btnRegistrarCajero.setOnClickListener(view -> {
            registrarNuevoCajero();
        });
    }

    private void registrarNuevoCajero() {
        String nombre = etNombreCajero.getText().toString().trim();
        String correo = etCorreoCajero.getText().toString().trim().toLowerCase(); // Guardar en minúsculas
        String passwordCajero = etPasswordCajero.getText().toString().trim();
        String supervisorPassInput = etSupervisorPassword.getText().toString().trim();

        if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(correo) ||
                TextUtils.isEmpty(passwordCajero) || TextUtils.isEmpty(supervisorPassInput)) {
            Toast.makeText(RegistroCajeroActivity.this, "Complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validación simple de formato de correo (puedes hacerla más robusta)
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            Toast.makeText(RegistroCajeroActivity.this, "Ingrese un correo electrónico válido", Toast.LENGTH_SHORT).show();
            // Podrías poner el foco en el campo de correo o usar setError en el TextInputLayout
            // TextInputLayout tilCorreo = findViewById(R.id.tilCorreoCajero);
            // tilCorreo.setError("Correo inválido");
            return;
        }
        // else {
        //     TextInputLayout tilCorreo = findViewById(R.id.tilCorreoCajero);
        //     tilCorreo.setError(null); // Limpiar error si es válido
        // }


        if (!supervisorPassInput.equals(SUPERVISOR_PASSWORD_VALIDATION)) {
            Toast.makeText(RegistroCajeroActivity.this, "Contraseña de supervisor incorrecta", Toast.LENGTH_SHORT).show();
            // TextInputLayout tilSupervisorPassword = findViewById(R.id.tilSupervisorPassword);
            // tilSupervisorPassword.setError("Contraseña incorrecta");
            return;
        }
        // else {
        //     TextInputLayout tilSupervisorPassword = findViewById(R.id.tilSupervisorPassword);
        //     tilSupervisorPassword.setError(null); // Limpiar error
        // }


        // Verificar si el cajero (email) ya existe
        if (dbHelper.checkIfUserExists(correo)) {
            Toast.makeText(RegistroCajeroActivity.this, "Este correo electrónico ya está registrado.", Toast.LENGTH_LONG).show();
            // TextInputLayout tilCorreo = findViewById(R.id.tilCorreoCajero);
            // tilCorreo.setError("Correo ya registrado");
            return;
        }


        boolean inserted = dbHelper.insertCajero(nombre, correo, passwordCajero);
        if (inserted) {
            Toast.makeText(RegistroCajeroActivity.this, "Cajero '" + nombre + "' registrado exitosamente", Toast.LENGTH_SHORT).show();
            // Opcional: limpiar campos o cerrar actividad
            // etNombreCajero.setText("");
            // etCorreoCajero.setText("");
            // etPasswordCajero.setText("");
            // etSupervisorPassword.setText("");
            finish(); // Cierra la actividad después de un registro exitoso
        } else {
            Toast.makeText(RegistroCajeroActivity.this, "Error al registrar cajero. Verifique los datos o el correo podría ya existir.", Toast.LENGTH_LONG).show();
        }
    }

    // Método para manejar el clic en el botón de "Atrás" de la Toolbar
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // Cierra esta actividad y vuelve a la anterior en el stack
            return true;
        }
        return super.onOptionsItemSelected(item);
    }



}