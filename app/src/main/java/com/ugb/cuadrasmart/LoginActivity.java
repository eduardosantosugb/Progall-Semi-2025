package com.ugb.cuadrasmart;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private EditText etUsername, etPassword;
    private Button btnLogin, btnGoToRegisterSupervisor;
    private TextView tvLoginTitle;
    private View tilUsernameLayout, tilPasswordLayout; // Para referenciar los TextInputLayout

    private DatabaseHelper dbHelper;
    private FirebaseAuth mAuth;
    private FirebaseFirestore dbFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Log.d(TAG, "onCreate: Iniciando LoginActivity.");

        mAuth = FirebaseAuth.getInstance();
        dbFirestore = FirebaseFirestore.getInstance();
        dbHelper = new DatabaseHelper(this);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoToRegisterSupervisor = findViewById(R.id.btnGoToRegisterSupervisor);
        tvLoginTitle = findViewById(R.id.tvLoginTitle);
        tilUsernameLayout = findViewById(R.id.tilUsername); // ID del TextInputLayout
        tilPasswordLayout = findViewById(R.id.tilPassword); // ID del TextInputLayout

        btnGoToRegisterSupervisor.setOnClickListener(view -> {
            Log.d(TAG, "Botón 'Crear Cuenta de Supervisor' presionado desde UI.");
            Intent intent = new Intent(LoginActivity.this, RegistroSupervisorActivity.class);
            startActivity(intent);
        });

        btnLogin.setOnClickListener(view -> {
            Log.d(TAG, "Botón 'Login' presionado.");
            String username = etUsername.getText().toString().trim().toLowerCase();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                Toast.makeText(LoginActivity.this, "Ingrese usuario y contraseña", Toast.LENGTH_SHORT).show();
                return;
            }
            authenticateWithFirebase(username, password);
        });

        // Lógica de decisión de UI
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Usuario Firebase detectado: " + currentUser.getEmail() + ". Intentando autologin...");
            hideLoginScreenElements(); // Ocultar UI de login mientras se verifica
            checkSelectedStoreAndProceed(currentUser);
        } else {
            Log.d(TAG, "No hay usuario Firebase activo.");
            if (!areUsersInSQLite()) {
                Log.d(TAG, "No hay usuarios en SQLite. Configurando UI para primer inicio.");
                // El diálogo es modal, el usuario debe interactuar con él.
                // La UI se configura después de la interacción o si el diálogo no se muestra.
                configureUiForFirstTime();
                showCreateInitialSupervisorAccountDialog(); // Mostrar diálogo DESPUÉS de configurar UI base
            } else {
                Log.d(TAG, "Hay usuarios en SQLite. Configurando UI para login normal.");
                configureUiForNormalLogin();
            }
        }
    }

    private void hideLoginScreenElements() {
        Log.d(TAG, "hideLoginScreenElements: Ocultando elementos de login.");
        if (tilUsernameLayout != null) tilUsernameLayout.setVisibility(View.GONE);
        if (etUsername != null) etUsername.setVisibility(View.GONE);
        if (tilPasswordLayout != null) tilPasswordLayout.setVisibility(View.GONE);
        if (etPassword != null) etPassword.setVisibility(View.GONE);
        if (btnLogin != null) btnLogin.setVisibility(View.GONE);
        if (btnGoToRegisterSupervisor != null) btnGoToRegisterSupervisor.setVisibility(View.GONE);
        // Podrías mostrar un ProgressBar aquí
    }

    private void configureUiForFirstTime() {
        Log.d(TAG, "configureUiForFirstTime: Configurando UI para primer uso.");
        if (tilUsernameLayout != null) tilUsernameLayout.setVisibility(View.GONE);
        if (etUsername != null) etUsername.setVisibility(View.GONE);
        if (tilPasswordLayout != null) tilPasswordLayout.setVisibility(View.GONE);
        if (etPassword != null) etPassword.setVisibility(View.GONE);
        if (btnLogin != null) btnLogin.setVisibility(View.GONE);

        if (tvLoginTitle != null) tvLoginTitle.setText("Bienvenido a CuadraSmart");
        if (btnGoToRegisterSupervisor != null) {
            btnGoToRegisterSupervisor.setVisibility(View.VISIBLE);
            btnGoToRegisterSupervisor.setEnabled(true);
        }
    }

    private void configureUiForNormalLogin() {
        Log.d(TAG, "configureUiForNormalLogin: Configurando UI para login estándar.");
        if (tilUsernameLayout != null) tilUsernameLayout.setVisibility(View.VISIBLE);
        if (etUsername != null) etUsername.setVisibility(View.VISIBLE);
        if (tilPasswordLayout != null) tilPasswordLayout.setVisibility(View.VISIBLE);
        if (etPassword != null) etPassword.setVisibility(View.VISIBLE);
        if (btnLogin != null) {
            btnLogin.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(true);
        }

        if (tvLoginTitle != null) tvLoginTitle.setText(getString(R.string.login_title));
        if (btnGoToRegisterSupervisor != null) {
            // Se mostrará si el login falla, así que por defecto oculto
            btnGoToRegisterSupervisor.setVisibility(View.GONE);
        }
    }

    private boolean areUsersInSQLite() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.query(
                    DatabaseContract.UserEntry.TABLE_NAME,
                    new String[]{DatabaseContract.UserEntry._ID},
                    null, null, null, null, null, "1"
            );
            boolean hasUsers = (cursor != null && cursor.getCount() > 0);
            Log.d(TAG, "areUsersInSQLite: " + (hasUsers ? "SÍ hay usuarios." : "NO hay usuarios."));
            return hasUsers;
        } catch (Exception e) {
            Log.e(TAG, "areUsersInSQLite: Error verificando usuarios", e);
            return true; // Asumir que hay usuarios en caso de error para evitar bucles.
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void checkSelectedStoreAndProceed(FirebaseUser firebaseUser) {
        SharedPreferences prefs = getSharedPreferences("CuadraSmartPrefs", MODE_PRIVATE);
        String selectedStore = prefs.getString("selected_store", null);

        // No necesitamos verificar firebaseUser == null aquí porque ya lo hicimos antes de llamar
        Log.d(TAG, "checkSelectedStoreAndProceed para: " + firebaseUser.getEmail());
        fetchUserRoleFromFirestore(firebaseUser.getUid(), firebaseUser.getEmail(), selectedStore, true);
    }

    private void showCreateInitialSupervisorAccountDialog() {
        // Asegurarse que el diálogo no se muestre múltiples veces si la actividad se recrea
        if (isFinishing() || isDestroyed()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Configuración Inicial Requerida")
                .setMessage("Parece que es la primera vez que usas la aplicación o no hay cuentas de administrador.\n\n¿Deseas crear la cuenta principal de supervisor ahora?")
                .setPositiveButton("Sí, Crear Ahora", (dialog, which) -> {
                    Log.d(TAG, "Diálogo inicial: Usuario eligió 'Sí, Crear Ahora'");
                    Intent intent = new Intent(LoginActivity.this, RegistroSupervisorActivity.class);
                    startActivity(intent);
                    // La UI ya está configurada por configureUiForFirstTime, el usuario interactuará allí.
                })
                .setNegativeButton("Salir de la App", (dialog, which) -> {
                    Log.d(TAG, "Diálogo inicial: Usuario eligió 'Salir de la App'");
                    Toast.makeText(LoginActivity.this, "Se requiere una cuenta para usar la aplicación.", Toast.LENGTH_LONG).show();
                    finishAffinity();
                })
                .setCancelable(false)
                .show();
    }

    private void authenticateWithFirebase(String email, String password) {
        btnLogin.setEnabled(false);
        if (btnGoToRegisterSupervisor != null) btnGoToRegisterSupervisor.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            fetchUserRoleFromFirestore(firebaseUser.getUid(), email, null, false);
                        } else {
                            Log.e(TAG, "signInWithEmail:success pero FirebaseUser es null");
                            Toast.makeText(LoginActivity.this, "Error al obtener usuario de Firebase.", Toast.LENGTH_SHORT).show();
                            enableLoginButtons(true);
                        }
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Autenticación fallida. Verifica tus credenciales.", Toast.LENGTH_LONG).show();
                        enableLoginButtons(true); // Habilitar y mostrar botón de registro
                    }
                });
    }

    private void fetchUserRoleFromFirestore(String userId, String email, String preSelectedStore, boolean isAutoLogin) {
        Log.d(TAG, "fetchUserRoleFromFirestore para UID: " + userId + ", Email: " + email + ", AutoLogin: " + isAutoLogin + ", TiendaPrevia: " + preSelectedStore);
        dbFirestore.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            String userRole = document.getString("role");
                            if (userRole == null) {
                                userRole = "cajero"; // Fallback
                                Log.w(TAG, "Rol no encontrado en Firestore para UID: " + userId + ". Usando 'cajero'.");
                            }
                            Log.d(TAG, "Rol obtenido de Firestore: " + userRole);

                            saveUserDetailsToPrefs(email, userRole);
                            updateFCMToken(userId);

                            if (preSelectedStore != null) {
                                Log.d(TAG, "Navegando a Dashboard (tienda preseleccionada: " + preSelectedStore + ")");
                                navigateTo(DashboardActivity.class);
                            } else {
                                Log.d(TAG, "Navegando a TiendasActivity (no hay tienda preseleccionada)");
                                navigateTo(TiendasActivity.class);
                            }
                        } else {
                            Log.w(TAG, "No se encontró documento de usuario en Firestore para UID: " + userId);
                            Toast.makeText(LoginActivity.this, "Datos de usuario no encontrados. Contacte soporte.", Toast.LENGTH_LONG).show();
                            if (isAutoLogin) {
                                Log.d(TAG, "Logout forzado porque los datos del usuario (autoLogin) no se encontraron en Firestore.");
                                logoutFirebaseUser();
                            } else {
                                enableLoginButtons(true);
                            }
                        }
                    } else {
                        Log.e(TAG, "Error al obtener datos del usuario desde Firestore: ", task.getException());
                        Toast.makeText(LoginActivity.this, "Error al cargar perfil. Intente de nuevo.", Toast.LENGTH_SHORT).show();
                        if (isAutoLogin) {
                            Log.d(TAG, "Logout forzado debido a error cargando perfil (autoLogin).");
                            logoutFirebaseUser();
                        } else {
                            enableLoginButtons(true);
                        }
                    }
                });
    }

    private void saveUserDetailsToPrefs(String email, String userRole) {
        SharedPreferences prefs = getSharedPreferences("CuadraSmartPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("logged_user_email", email);
        editor.putString("logged_user_role", userRole);
        editor.putString("override_role", userRole);
        editor.apply();
        Log.d(TAG, "Email '" + email + "' y rol '" + userRole + "' guardados en SharedPreferences.");
    }

    private void updateFCMToken(String userId) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }
                    String token = task.getResult();
                    Log.d(TAG, "FCM Token: " + token + " para UID: " + userId);
                    if (userId != null && token != null) {
                        dbFirestore.collection("users").document(userId)
                                .update("fcmToken", token)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM Token actualizado/creado en Firestore para UID: " + userId))
                                .addOnFailureListener(e -> Log.w(TAG, "Error actualizando FCM Token en Firestore para UID: " + userId, e));
                    } else {
                        Log.w(TAG, "No se pudo actualizar FCM Token, userId o token es null. UserID: " + userId);
                    }
                });
    }

    private void enableLoginButtons(boolean showRegisterSupervisorButton) {
        if (btnLogin != null) btnLogin.setEnabled(true);
        if (btnGoToRegisterSupervisor != null) {
            btnGoToRegisterSupervisor.setEnabled(true);
            if (showRegisterSupervisorButton) {
                btnGoToRegisterSupervisor.setVisibility(View.VISIBLE);
            }
            // No lo ocultamos si no se especifica, configureUiForNormalLogin se encarga
        }
    }

    private void navigateTo(Class<?> activityClass) {
        Intent intent = new Intent(LoginActivity.this, activityClass);
        startActivity(intent);
        finish();
    }

    private void logoutFirebaseUser() {
        Log.d(TAG, "logoutFirebaseUser: Cerrando sesión de Firebase y limpiando SharedPreferences.");
        mAuth.signOut();
        SharedPreferences prefs = getSharedPreferences("CuadraSmartPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("logged_user_email");
        editor.remove("logged_user_role");
        editor.remove("override_role");
        editor.remove("selected_store"); // Asegurarse de limpiar la tienda seleccionada
        editor.apply();
        Log.i(TAG, "logoutFirebaseUser: SharedPreferences limpiadas.");

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}