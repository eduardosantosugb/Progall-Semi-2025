package com.ugb.cuadrasmart;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public class RegistroSupervisorActivity extends AppCompatActivity {

    private static final String TAG = "RegSupervisorActivity";

    private EditText etNombreSupervisor, etCorreoSupervisor, etPasswordSupervisor, etCodigoCreacion;
    private Button btnRegistrarSupervisor;
    private DatabaseHelper dbHelper;
    private Toolbar toolbarRegistroSupervisor;

    private FirebaseAuth mAuth;
    private FirebaseFirestore dbFirestore;

    private static final String CREATION_CODE_VALIDATION = "admin123";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro_supervisor);

        toolbarRegistroSupervisor = findViewById(R.id.toolbarRegistroSupervisor);
        setSupportActionBar(toolbarRegistroSupervisor);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Registrar Supervisor");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        etNombreSupervisor = findViewById(R.id.etNombreSupervisor);
        etCorreoSupervisor = findViewById(R.id.etCorreoSupervisor);
        etPasswordSupervisor = findViewById(R.id.etPasswordSupervisor);
        etCodigoCreacion = findViewById(R.id.etCodigoCreacion);
        btnRegistrarSupervisor = findViewById(R.id.btnRegistrarSupervisor);

        dbHelper = new DatabaseHelper(this);
        mAuth = FirebaseAuth.getInstance();
        dbFirestore = FirebaseFirestore.getInstance();

        btnRegistrarSupervisor.setOnClickListener(view -> {
            Log.d(TAG, "Botón Registrar Supervisor presionado.");
            registrarNuevoSupervisor();
        });
    }

    private void registrarNuevoSupervisor() {
        String nombre = etNombreSupervisor.getText().toString().trim();
        String correo = etCorreoSupervisor.getText().toString().trim().toLowerCase();
        String passwordSupervisor = etPasswordSupervisor.getText().toString().trim();
        String codigoCreacionInput = etCodigoCreacion.getText().toString().trim();

        Log.d(TAG, "Iniciando proceso de registro para: " + correo);

        if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(correo) ||
                TextUtils.isEmpty(passwordSupervisor) || TextUtils.isEmpty(codigoCreacionInput)) {
            Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Registro fallido: Campos incompletos.");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            Toast.makeText(this, "Ingrese un correo electrónico válido", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Registro fallido: Correo inválido: " + correo);
            return;
        }

        if (passwordSupervisor.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Registro fallido: Contraseña muy corta.");
            return;
        }

        if (!codigoCreacionInput.equals(CREATION_CODE_VALIDATION)) {
            Toast.makeText(this, "Código de creación incorrecto", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Registro fallido: Código de creación incorrecto.");
            return;
        }

        btnRegistrarSupervisor.setEnabled(false);
        Log.d(TAG, "Botón de registro deshabilitado.");

        // Pre-validación SQLite (opcional, pero la mantenemos)
        // if (dbHelper.checkIfUserExists(correo)) {
        //     Toast.makeText(this, "Este correo electrónico ya está registrado localmente.", Toast.LENGTH_LONG).show();
        //     btnRegistrarSupervisor.setEnabled(true);
        //     Log.w(TAG, "Registro fallido: Correo ya existe en SQLite (pre-check): " + correo);
        //     return;
        // }

        Log.d(TAG, "Intentando crear usuario en Firebase Auth para: " + correo);
        mAuth.createUserWithEmailAndPassword(correo, passwordSupervisor)
                .addOnCompleteListener(this, taskAuth -> {
                    if (taskAuth.isSuccessful()) {
                        Log.i(TAG, "ÉXITO al crear usuario en Firebase Auth: " + correo);
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            saveSupervisorDetailsToFirestore(firebaseUser.getUid(), nombre, correo, passwordSupervisor, firebaseUser);
                        } else {
                            Log.e(TAG, "FALLO CRÍTICO: FirebaseUser es null después de un registro exitoso en Auth para: " + correo);
                            Toast.makeText(this, "Error inesperado al obtener usuario de Firebase.", Toast.LENGTH_LONG).show();
                            btnRegistrarSupervisor.setEnabled(true);
                        }
                    } else {
                        Log.e(TAG, "FALLO al crear usuario en Firebase Auth para: " + correo, taskAuth.getException());
                        String errorMessage = "Error al registrar en Firebase: ";
                        if (taskAuth.getException() instanceof FirebaseAuthUserCollisionException) {
                            errorMessage = "Este correo electrónico ya está registrado en Firebase.";
                        } else if (taskAuth.getException() != null) {
                            errorMessage += taskAuth.getException().getMessage();
                        } else {
                            errorMessage = "Error desconocido al registrar en Firebase.";
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                        btnRegistrarSupervisor.setEnabled(true);
                    }
                });
    }

    private void saveSupervisorDetailsToFirestore(String userId, String name, String email, String passwordForSQLite, FirebaseUser firebaseUserForRevert) {
        Map<String, Object> supervisor = new HashMap<>();
        supervisor.put("name", name);
        supervisor.put("email", email);
        supervisor.put("role", "supervisor");
        supervisor.put("createdAt", FieldValue.serverTimestamp());

        Log.d(TAG, "Intentando guardar en Firestore para UID: " + userId + ", Email: " + email);
        dbFirestore.collection("users").document(userId)
                .set(supervisor)
                .addOnSuccessListener(aVoid -> {
                    Log.i(TAG, "ÉXITO al guardar detalles del supervisor en Firestore. UID: " + userId);

                    // ----- INICIO DE PRUEBA DE AISLAMIENTO -----
                    // Toast.makeText(RegistroSupervisorActivity.this, "PRUEBA: Firestore OK. UID: " + userId, Toast.LENGTH_LONG).show();
                    // Log.d(TAG, "PRUEBA: Antes de llamar a finish() después de Firestore OK.");
                    // finish();
                    // if (true) return; // Para detener la ejecución aquí temporalmente y probar el Toast/finish
                    // ----- FIN DE PRUEBA DE AISLAMIENTO -----


                    Log.d(TAG, "Intentando insertar en SQLite para email: " + email);
                    boolean insertedInSQLite = dbHelper.insertSupervisor(name, email, passwordForSQLite);

                    if (insertedInSQLite) {
                        Log.i(TAG, "ÉXITO TOTAL: Supervisor '" + name + "' registrado en Auth, Firestore y SQLite. Email: " + email);
                        // Asegurarse que el Toast se muestre en el hilo UI si hay dudas
                        runOnUiThread(() -> Toast.makeText(RegistroSupervisorActivity.this, "Supervisor '" + name + "' registrado exitosamente!", Toast.LENGTH_LONG).show());
                        Log.d(TAG, "Cerrando RegistroSupervisorActivity...");
                        finish();
                    } else {
                        Log.e(TAG, "FALLO al insertar supervisor en SQLite después de Firestore. Email: " + email + ", UID: " + userId);
                        // Asegurarse que el Toast se muestre en el hilo UI
                        runOnUiThread(() -> Toast.makeText(RegistroSupervisorActivity.this, "Error en DB local. Contacte soporte. Reversión Firebase iniciada.", Toast.LENGTH_LONG).show());
                        revertFirebaseRegistration(firebaseUserForRevert, userId);
                        btnRegistrarSupervisor.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "FALLO al guardar detalles del supervisor en Firestore. UID: " + userId + ", Email: " + email, e);
                    // Asegurarse que el Toast se muestre en el hilo UI
                    runOnUiThread(() -> Toast.makeText(RegistroSupervisorActivity.this, "Error guardando en Firestore: " + e.getMessage() + ". Reversión Firebase Auth iniciada.", Toast.LENGTH_LONG).show());
                    revertFirebaseRegistration(firebaseUserForRevert, null);
                    btnRegistrarSupervisor.setEnabled(true);
                });
    }

    private void revertFirebaseRegistration(FirebaseUser firebaseUser, String firestoreUserIdToDelete) {
        Log.w(TAG, "Iniciando reversión de registro de Firebase...");
        if (firebaseUser != null) {
            String emailForLog = firebaseUser.getEmail();
            firebaseUser.delete().addOnCompleteListener(deleteAuthTask -> {
                if (deleteAuthTask.isSuccessful()) {
                    Log.i(TAG, "Usuario de Firebase Auth eliminado (reversión) para: " + emailForLog);
                } else {
                    Log.e(TAG, "Error al eliminar usuario de Firebase Auth (reversión) para: " + emailForLog, deleteAuthTask.getException());
                }
            });
        }
        if (firestoreUserIdToDelete != null) {
            dbFirestore.collection("users").document(firestoreUserIdToDelete).delete()
                    .addOnSuccessListener(aVoid -> Log.i(TAG, "Documento de Firestore eliminado (reversión) para UID: " + firestoreUserIdToDelete))
                    .addOnFailureListener(e -> Log.e(TAG, "Error eliminando documento de Firestore (reversión). UID: " + firestoreUserIdToDelete, e));
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}