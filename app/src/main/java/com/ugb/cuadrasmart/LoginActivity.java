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
import androidx.annotation.NonNull; // Asegúrate de que esta importación esté si la usas en callbacks
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private EditText etUsername, etPassword;
    private Button btnLogin, btnGoToRegisterSupervisor;
    private TextView tvLoginTitle;
    private View tilUsernameLayout, tilPasswordLayout; // Referencias a los TextInputLayouts

    private DatabaseHelper dbHelper;
    private FirebaseAuth mAuth;
    private FirebaseFirestore dbFirestore;

    private boolean isInitialDialogShown = false; // Bandera para controlar el diálogo de primer inicio

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Log.d(TAG, "onCreate: Iniciando LoginActivity.");

        mAuth = FirebaseAuth.getInstance();
        dbFirestore = FirebaseFirestore.getInstance();
        dbHelper = new DatabaseHelper(this);

        initializeViews();
        setupButtonListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: LoginActivity ha vuelto al frente.");
        // Resetear la bandera del diálogo si es necesario o manejarla de otra forma
        // isInitialDialogShown = false; // Si quieres que se pueda mostrar de nuevo en ciertas condiciones
        evaluateStateAndConfigureUI();
    }

    private void initializeViews() {
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoToRegisterSupervisor = findViewById(R.id.btnGoToRegisterSupervisor);
        tvLoginTitle = findViewById(R.id.tvLoginTitle);
        tilUsernameLayout = findViewById(R.id.tilUsername);
        tilPasswordLayout = findViewById(R.id.tilPassword);
        Log.d(TAG, "initializeViews: Vistas inicializadas.");
    }

    private void setupButtonListeners() {
        btnGoToRegisterSupervisor.setOnClickListener(view -> {
            Log.d(TAG, "Botón 'Crear Cuenta de Supervisor' presionado.");
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
        Log.d(TAG, "setupButtonListeners: Listeners configurados.");
    }

    private void evaluateStateAndConfigureUI() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            Log.d(TAG, "evaluateStateAndConfigureUI: Usuario Firebase ACTIVO: " + currentUser.getEmail() + ". Intentando autologin...");
            hideLoginScreenElements(); // Ocultar UI de login mientras se procesa
            checkSelectedStoreAndProceed(currentUser);
        } else {
            Log.d(TAG, "evaluateStateAndConfigureUI: No hay usuario Firebase activo.");
            if (!areUsersInSQLite()) {
                Log.d(TAG, "evaluateStateAndConfigureUI: No hay usuarios en SQLite.");
                configureUiForFirstTime(); // Configura la UI para "Crear Supervisor"
                if (!isInitialDialogShown) { // Solo mostrar el diálogo modal una vez por instancia de actividad
                    showCreateInitialSupervisorAccountDialog();
                    isInitialDialogShown = true;
                }
            } else {
                Log.d(TAG, "evaluateStateAndConfigureUI: Hay usuarios en SQLite. Configurando UI para login normal.");
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
        if (tvLoginTitle != null) tvLoginTitle.setVisibility(View.INVISIBLE); // Para que no cambie el layout bruscamente
        // Aquí podrías mostrar un ProgressBar si el proceso de autologin tarda
    }

    private void configureUiForFirstTime() {
        Log.d(TAG, "configureUiForFirstTime: Configurando UI para primer uso.");
        if (tilUsernameLayout != null) tilUsernameLayout.setVisibility(View.GONE);
        if (etUsername != null) etUsername.setVisibility(View.GONE);
        if (tilPasswordLayout != null) tilPasswordLayout.setVisibility(View.GONE);
        if (etPassword != null) etPassword.setVisibility(View.GONE);
        if (btnLogin != null) btnLogin.setVisibility(View.GONE);

        if (tvLoginTitle != null) {
            tvLoginTitle.setText("Bienvenido a CuadraSmart");
            tvLoginTitle.setVisibility(View.VISIBLE);
        }
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

        if (tvLoginTitle != null) {
            tvLoginTitle.setText(getString(R.string.login_title)); // Título de login normal
            tvLoginTitle.setVisibility(View.VISIBLE);
        }
        if (btnGoToRegisterSupervisor != null) {
            btnGoToRegisterSupervisor.setVisibility(View.GONE); // Oculto hasta que falle un login
            btnGoToRegisterSupervisor.setEnabled(true); // Asegurar que esté habilitado por si se muestra luego
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
            return true; // Asumir que sí hay para evitar bucles de creación si la DB está corrupta.
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private void checkSelectedStoreAndProceed(FirebaseUser firebaseUser) {
        SharedPreferences prefs = getSharedPreferences("CuadraSmartPrefs", MODE_PRIVATE);
        String selectedStore = prefs.getString("selected_store", null); // null si no hay tienda seleccionada
        Log.d(TAG, "checkSelectedStoreAndProceed para: " + firebaseUser.getEmail() + ", Tienda guardada: " + selectedStore);
        // Siempre obtener el rol de Firestore para asegurar consistencia
        fetchUserRoleFromFirestore(firebaseUser.getUid(), firebaseUser.getEmail(), selectedStore, true);
    }

    private void showCreateInitialSupervisorAccountDialog() {
        if (isFinishing() || isDestroyed()) {
            Log.w(TAG, "showCreateInitialSupervisorAccountDialog: Actividad finalizando, no se muestra diálogo.");
            return;
        }
        Log.d(TAG, "Mostrando diálogo de creación inicial de supervisor.");
        new AlertDialog.Builder(this)
                .setTitle("Configuración Inicial Requerida")
                .setMessage("No hay cuentas de administrador configuradas.\n\n¿Desea crear la cuenta principal de supervisor ahora?")
                .setPositiveButton("Sí, Crear Ahora", (dialog, which) -> {
                    Log.d(TAG, "Diálogo inicial: Usuario eligió 'Sí, Crear Ahora'");
                    Intent intent = new Intent(LoginActivity.this, RegistroSupervisorActivity.class);
                    startActivity(intent);
                    // No cerramos LoginActivity, el usuario volverá aquí.
                    // La UI ya está ajustada por configureUiForFirstTime()
                })
                .setNegativeButton("Salir de la App", (dialog, which) -> {
                    Log.d(TAG, "Diálogo inicial: Usuario eligió 'Salir de la App'");
                    Toast.makeText(LoginActivity.this, "Se requiere una cuenta para usar la aplicación.", Toast.LENGTH_LONG).show();
                    finishAffinity(); // Cierra la app completamente
                })
                .setCancelable(false) // Forzar una elección
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
                            // Pasar null para preSelectedStore porque es un login manual, irá a TiendasActivity si no hay tienda.
                            fetchUserRoleFromFirestore(firebaseUser.getUid(), email, null, false);
                        } else {
                            Log.e(TAG, "signInWithEmail:success pero FirebaseUser es null");
                            Toast.makeText(LoginActivity.this, "Error al obtener usuario de Firebase.", Toast.LENGTH_SHORT).show();
                            enableLoginButtons(true); // Mostrar opción de registrar
                            configureUiForNormalLogin(); // Mantener UI de login
                        }
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Autenticación fallida. Verifica tus credenciales.", Toast.LENGTH_LONG).show();
                        enableLoginButtons(true); // Mostrar opción de registrar
                        // No es necesario llamar a configureUiForNormalLogin() aquí porque la UI ya debería estar así.
                    }
                });
    }

    private void fetchUserRoleFromFirestore(String userId, String email, @Nullable String preSelectedStore, boolean isAutoLogin) {
        Log.d(TAG, "fetchUserRoleFromFirestore UID: " + userId + ", Email: " + email + ", AutoLogin: " + isAutoLogin + ", TiendaPrevia: " + preSelectedStore);
        dbFirestore.collection("users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (isDestroyed() || isFinishing()) { // Comprobar estado de la actividad
                        Log.w(TAG, "fetchUserRoleFromFirestore: Actividad destruida o finalizando, se omite la UI.");
                        return;
                    }
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
                            updateFCMToken(userId); // Importante para notificaciones

                            if (preSelectedStore != null) {
                                Log.d(TAG, "Navegando a Dashboard (tienda preseleccionada: " + preSelectedStore + ")");
                                navigateTo(DashboardActivity.class);
                            } else {
                                Log.d(TAG, "Navegando a TiendasActivity (no hay tienda preseleccionada)");
                                navigateTo(TiendasActivity.class);
                            }
                        } else { // Documento de usuario no encontrado en Firestore
                            Log.w(TAG, "Documento de usuario no encontrado en Firestore para UID: " + userId);
                            Toast.makeText(LoginActivity.this, "Datos de perfil no encontrados. Intente registrarse o contacte soporte.", Toast.LENGTH_LONG).show();
                            if (isAutoLogin) {
                                Log.d(TAG, "Logout forzado (autoLogin) porque datos de usuario no están en Firestore.");
                                logoutFirebaseUser(); // Limpiar sesión Firebase "huérfana"
                            } else { // Fallo durante login manual
                                enableLoginButtons(true); // Permitir reintentar o registrar
                                configureUiForNormalLogin(); // Asegurar que la UI de login esté visible
                                if (btnGoToRegisterSupervisor != null) btnGoToRegisterSupervisor.setVisibility(View.VISIBLE);
                            }
                        }
                    } else { // Tarea de Firestore falló
                        Log.e(TAG, "Error al obtener datos del usuario desde Firestore: ", task.getException());
                        Toast.makeText(LoginActivity.this, "Error al cargar perfil. Verifique conexión.", Toast.LENGTH_LONG).show();
                        if (isAutoLogin) {
                            Log.d(TAG, "Logout forzado (autoLogin) debido a error cargando perfil desde Firestore.");
                            logoutFirebaseUser();
                        } else {
                            enableLoginButtons(true);
                            configureUiForNormalLogin();
                            if (btnGoToRegisterSupervisor != null) btnGoToRegisterSupervisor.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    private void saveUserDetailsToPrefs(String email, String userRole) {
        SharedPreferences prefs = getSharedPreferences("CuadraSmartPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("logged_user_email", email);
        editor.putString("logged_user_role", userRole);
        editor.putString("override_role", userRole); // El override inicial es el rol real
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
                    Log.d(TAG, "FCM Token obtenido: " + token + " para UID: " + userId);
                    if (userId != null && token != null) {
                        dbFirestore.collection("users").document(userId)
                                .update("fcmToken", token) // Usar update para no sobreescribir todo el documento
                                .addOnSuccessListener(aVoid -> Log.i(TAG, "FCM Token actualizado en Firestore para UID: " + userId))
                                .addOnFailureListener(e -> {
                                    Log.w(TAG, "Error actualizando FCM Token en Firestore para UID: " + userId + ". Intentando setear si el campo no existe.", e);
                                    // Si el update falla porque el campo no existe (primera vez), intenta con set (merge)
                                    Map<String, Object> tokenData = new HashMap<>();
                                    tokenData.put("fcmToken", token);
                                    dbFirestore.collection("users").document(userId)
                                            .set(tokenData, com.google.firebase.firestore.SetOptions.merge())
                                            .addOnSuccessListener(aVoid2 -> Log.i(TAG, "FCM Token seteado (merge) en Firestore para UID: " + userId))
                                            .addOnFailureListener(e2 -> Log.e(TAG, "Error seteando (merge) FCM Token en Firestore para UID: " + userId, e2));
                                });
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
            // No se oculta aquí explícitamente; configureUiForNormalLogin se encarga de eso.
        }
    }

    private void navigateTo(Class<?> activityClass) {
        if (isFinishing() || isDestroyed()) {
            Log.w(TAG, "navigateTo: Actividad finalizando, no se navega a " + activityClass.getSimpleName());
            return;
        }
        Log.d(TAG, "Navegando a " + activityClass.getSimpleName());
        Intent intent = new Intent(LoginActivity.this, activityClass);
        startActivity(intent);
        finish();
    }

    private void logoutFirebaseUser() {
        Log.i(TAG, "logoutFirebaseUser: Iniciando cierre de sesión completo.");
        mAuth.signOut(); // Cerrar sesión de Firebase

        SharedPreferences prefs = getSharedPreferences("CuadraSmartPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("logged_user_email");
        editor.remove("logged_user_role");
        editor.remove("override_role");
        editor.remove("selected_store"); // Es crucial limpiar esto
        editor.apply();
        Log.d(TAG, "logoutFirebaseUser: SharedPreferences limpiadas (incluyendo selected_store).");

        // Reiniciar LoginActivity para asegurar un estado limpio
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}