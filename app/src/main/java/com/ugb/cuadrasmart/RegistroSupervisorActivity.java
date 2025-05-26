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

// No es necesario importar R directamente
// import com.ugb.cuadrasmart.R;

public class RegistroSupervisorActivity extends AppCompatActivity {

    private EditText etNombreSupervisor, etCorreoSupervisor, etPasswordSupervisor, etCodigoCreacion;
    private Button btnRegistrarSupervisor;
    private DatabaseHelper dbHelper;
    private Toolbar toolbarRegistroSupervisor; // Para la Toolbar

    // Código de creación requerido para registrar supervisores
    // Considera mover esto a un lugar más seguro o hacerlo configurable.
    private static final String CREATION_CODE_VALIDATION = "admin123"; // Renombrado para claridad

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro_supervisor);

        // Configuración de la Toolbar
        toolbarRegistroSupervisor = findViewById(R.id.toolbarRegistroSupervisor);
        setSupportActionBar(toolbarRegistroSupervisor);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Registrar Supervisor"); // Puedes usar un string resource
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Encontrar vistas del layout
        // Los IDs dentro de los TextInputLayout son los de los TextInputEditText
        etNombreSupervisor = findViewById(R.id.etNombreSupervisor);
        etCorreoSupervisor = findViewById(R.id.etCorreoSupervisor);
        etPasswordSupervisor = findViewById(R.id.etPasswordSupervisor);
        etCodigoCreacion = findViewById(R.id.etCodigoCreacion);
        btnRegistrarSupervisor = findViewById(R.id.btnRegistrarSupervisor);

        dbHelper = new DatabaseHelper(this);

        btnRegistrarSupervisor.setOnClickListener(view -> {
            registrarNuevoSupervisor();
        });
    }

    private void registrarNuevoSupervisor() {
        String nombre = etNombreSupervisor.getText().toString().trim();
        String correo = etCorreoSupervisor.getText().toString().trim().toLowerCase(); // Guardar en minúsculas
        String passwordSupervisor = etPasswordSupervisor.getText().toString().trim();
        String codigoCreacionInput = etCodigoCreacion.getText().toString().trim();

        if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(correo) ||
                TextUtils.isEmpty(passwordSupervisor) || TextUtils.isEmpty(codigoCreacionInput)) {
            Toast.makeText(RegistroSupervisorActivity.this, "Complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validación simple de formato de correo
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            Toast.makeText(RegistroSupervisorActivity.this, "Ingrese un correo electrónico válido", Toast.LENGTH_SHORT).show();
            // Opcional: TextInputLayout tilCorreo = findViewById(R.id.tilCorreoSupervisor); tilCorreo.setError("Correo inválido");
            return;
        }
        // else { TextInputLayout tilCorreo = findViewById(R.id.tilCorreoSupervisor); tilCorreo.setError(null); }


        if (!codigoCreacionInput.equals(CREATION_CODE_VALIDATION)) {
            Toast.makeText(RegistroSupervisorActivity.this, "Código de creación incorrecto", Toast.LENGTH_SHORT).show();
            // Opcional: TextInputLayout tilCodigo = findViewById(R.id.tilCodigoCreacion); tilCodigo.setError("Código incorrecto");
            return;
        }
        // else { TextInputLayout tilCodigo = findViewById(R.id.tilCodigoCreacion); tilCodigo.setError(null); }

        // Verificar si el supervisor (email) ya existe
        if (dbHelper.checkIfUserExists(correo)) { // Usamos el método que ya está en DatabaseHelper
            Toast.makeText(RegistroSupervisorActivity.this, "Este correo electrónico ya está registrado.", Toast.LENGTH_LONG).show();
            // Opcional: TextInputLayout tilCorreo = findViewById(R.id.tilCorreoSupervisor); tilCorreo.setError("Correo ya registrado");
            return;
        }

        boolean inserted = dbHelper.insertSupervisor(nombre, correo, passwordSupervisor);
        if (inserted) {
            Toast.makeText(RegistroSupervisorActivity.this, "Supervisor '" + nombre + "' registrado exitosamente", Toast.LENGTH_SHORT).show();
            // Opcional: limpiar campos o cerrar actividad
            // etNombreSupervisor.setText("");
            // etCorreoSupervisor.setText("");
            // etPasswordSupervisor.setText("");
            // etCodigoCreacion.setText("");
            finish(); // Cierra la actividad después de un registro exitoso
        } else {
            Toast.makeText(RegistroSupervisorActivity.this, "Error al registrar supervisor. Verifique los datos o el correo podría ya existir.", Toast.LENGTH_LONG).show();
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