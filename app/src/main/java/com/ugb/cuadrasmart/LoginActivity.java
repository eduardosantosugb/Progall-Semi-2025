package com.ugb.cuadrasmart;

import android.content.DialogInterface;
import android.content.Intent;
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
            String username = etUsername.getText().toString().trim();
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
     * Si no hay usuarios, se muestra automáticamente el diálogo para crear una cuenta.
     */
    private void checkIfUsersExist() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseContract.UserEntry.TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                null
        );
        if (cursor != null) {
            if (cursor.getCount() == 0) {
                Log.d(TAG, "No existen usuarios registrados.");
                showCreateAccountDialog();
            }
            cursor.close();
        }
    }

    /**
     * Intenta autenticar al usuario mediante la consulta en la tabla de usuarios.
     * Si se encuentra, redirige a TiendasActivity; de lo contrario, muestra el diálogo para crear cuenta.
     */
    private void authenticateUser(String username, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseContract.UserEntry.TABLE_NAME,
                null,
                DatabaseContract.UserEntry.COLUMN_EMAIL + "=? AND " +
                        DatabaseContract.UserEntry.COLUMN_PASSWORD + "=?",
                new String[]{username, password},
                null,
                null,
                null
        );
        if (cursor != null && cursor.moveToFirst()) {
            Log.d(TAG, "Usuario encontrado. Redirigiendo a TiendasActivity.");
            // Aquí se ignora el rol, se redirige a la pantalla de selección de tienda
            cursor.close();
            Intent intent = new Intent(LoginActivity.this, TiendasActivity.class);
            intent.putExtra("user_username", username);
            startActivity(intent);
            finish();
        } else {
            if (cursor != null) {
                cursor.close();
            }
            showCreateAccountDialog();
        }
    }

    /**
     * Muestra un diálogo que ofrece crear una nueva cuenta cuando el usuario no se encuentra.
     */
    private void showCreateAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Usuario no encontrado")
                .setMessage("El usuario no existe. ¿Desea crear una nueva cuenta?")
                .setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Redirige a la actividad de registro (por ejemplo, RegistroSupervisorActivity)
                        Intent intent = new Intent(LoginActivity.this, RegistroSupervisorActivity.class);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Método auxiliar para obtener de forma segura un valor String desde un Cursor.
     */
    private String safeGetString(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndex(columnName);
        if (index >= 0 && !cursor.isNull(index)) {
            return cursor.getString(index);
        }
        return "";
    }
}
