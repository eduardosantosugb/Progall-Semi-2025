package com.ugb.cuadrasmart;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences; // Importar SharedPreferences
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private EditText etUsername, etPassword;
    private Button btnLogin;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Asegúrate de que en activity_login.xml existan estos IDs
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        dbHelper = new DatabaseHelper(this);

        // Verificar si existen usuarios en la base de datos; si no, se muestra el diálogo para crear cuenta
        checkIfUsersExist();

        btnLogin.setOnClickListener(view -> {
            String username = etUsername.getText().toString().trim().toLowerCase(); // Guardar email en minúsculas para consistencia
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                Toast.makeText(LoginActivity.this, "Ingrese usuario y contraseña", Toast.LENGTH_SHORT).show();
                return;
            }
            authenticateUser(username, password);
        });
    }

    /**
     * Verifica si la tabla de usuarios está vacía.
     * Si no hay usuarios, se muestra automáticamente el diálogo para crear una cuenta de supervisor.
     */
    private void checkIfUsersExist() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.query(
                    DatabaseContract.UserEntry.TABLE_NAME,
                    new String[]{DatabaseContract.UserEntry._ID}, // Solo necesitamos saber si hay filas
                    null, null, null, null, null, "1" // Limitar a 1 para eficiencia
            );
            if (cursor != null && cursor.getCount() == 0) {
                Log.d(TAG, "No existen usuarios registrados. Mostrando diálogo para crear cuenta de supervisor.");
                showCreateInitialSupervisorAccountDialog();
            }
        } catch (Exception e) {
            Log.e(TAG, "checkIfUsersExist: Error verificando usuarios", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            // No cierres db aquí si dbHelper lo maneja globalmente o es usado en otro hilo.
        }
    }

    /**
     * Muestra un diálogo para crear la primera cuenta de supervisor si la base de datos está vacía.
     */
    private void showCreateInitialSupervisorAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Crear Cuenta de Administrador")
                .setMessage("No hay usuarios registrados. ¿Desea crear la cuenta principal de supervisor ahora?")
                .setPositiveButton("Sí, Crear", (dialog, which) -> {
                    Intent intent = new Intent(LoginActivity.this, RegistroSupervisorActivity.class);
                    // Podrías pasar una bandera para indicar que es la creación inicial si necesitas
                    // alguna lógica especial en RegistroSupervisorActivity (ej. no pedir código de creación la primera vez)
                    // intent.putExtra("IS_INITIAL_SETUP", true);
                    startActivity(intent);
                })
                .setNegativeButton("Más tarde", (dialog, which) -> {
                    // Opcional: Informar al usuario que no podrá usar la app sin una cuenta
                    Toast.makeText(LoginActivity.this, "Se requiere una cuenta para usar la aplicación.", Toast.LENGTH_LONG).show();
                })
                .setCancelable(false) // Para forzar una decisión
                .show();
    }


    /**
     * Intenta autenticar al usuario mediante la consulta en la tabla de usuarios.
     * Si se encuentra, guarda su email y rol (si es supervisor) y redirige a TiendasActivity.
     * De lo contrario, muestra el diálogo para crear cuenta.
     */
    private void authenticateUser(String username, String password) {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.query(
                    DatabaseContract.UserEntry.TABLE_NAME,
                    new String[]{DatabaseContract.UserEntry._ID, DatabaseContract.UserEntry.COLUMN_ROLE}, // Obtener también el rol
                    DatabaseContract.UserEntry.COLUMN_EMAIL + "=? AND " +
                            DatabaseContract.UserEntry.COLUMN_PASSWORD + "=?",
                    new String[]{username, password},
                    null, null, null, "1"
            );

            if (cursor != null && cursor.moveToFirst()) {
                Log.d(TAG, "Usuario '" + username + "' encontrado.");

                // Obtener el rol del usuario
                String userRole = safeGetString(cursor, DatabaseContract.UserEntry.COLUMN_ROLE);

                // Guardar el email del usuario que inició sesión y su rol original
                SharedPreferences prefs = getSharedPreferences("CuadraSmartPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("logged_user_email", username); // 'username' es el email
                editor.putString("logged_user_role", userRole);  // Rol original

                // El "override_role" inicial será el rol real del usuario.
                // Si es supervisor, podrá cambiarlo. Si es cajero, siempre será cajero.
                editor.putString("override_role", userRole);

                editor.apply();
                Log.d(TAG, "Email '" + username + "' y rol '" + userRole + "' guardados en SharedPreferences.");

                cursor.close(); // Cerrar cursor aquí después de usarlo

                Intent intent = new Intent(LoginActivity.this, TiendasActivity.class);
                startActivity(intent);
                finish();

            } else {
                Log.w(TAG, "Usuario '" + username + "' no encontrado o contraseña incorrecta.");
                if (cursor != null) {
                    cursor.close(); // Cerrar cursor si no se encontró el usuario
                }
                showCreateAccountDialog(); // Ofrecer crear cuenta si el login falla
            }
        } catch (Exception e) {
            Log.e(TAG, "authenticateUser: Error durante la autenticación para " + username, e);
            Toast.makeText(this, "Error de autenticación. Intente de nuevo.", Toast.LENGTH_SHORT).show();
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Muestra un diálogo que ofrece crear una nueva cuenta de supervisor cuando el usuario no se encuentra.
     */
    private void showCreateAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Usuario no encontrado")
                .setMessage("El usuario no existe o la contraseña es incorrecta. ¿Desea crear una nueva cuenta de supervisor?")
                .setPositiveButton("Sí, Crear Supervisor", (dialog, which) -> {
                    Intent intent = new Intent(LoginActivity.this, RegistroSupervisorActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Método auxiliar para obtener de forma segura un valor String desde un Cursor.
     */
    private String safeGetString(Cursor cursor, String columnName) {
        if (cursor == null || cursor.isClosed()) {
            Log.w(TAG, "safeGetString: Cursor es null o está cerrado.");
            return "";
        }
        try {
            int index = cursor.getColumnIndexOrThrow(columnName);
            return cursor.isNull(index) ? "" : cursor.getString(index);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "safeGetString: Columna no encontrada en cursor: " + columnName, e);
            return "";
        }
    }
}