package com.ugb.cuadrasmart;

import android.Manifest; // Para el permiso
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager; // Para checkSelfPermission
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull; // Para @NonNull
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat; // Para requestPermissions
import androidx.core.content.ContextCompat; // Para checkSelfPermission

// Importación de Firebase Auth
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser; // Para obtener el usuario actual

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 123; // Código para la solicitud de permiso

    private TextView tvWelcome, tvCurrentStore, tvSupervisorModulesTitle;
    private Button btnToggleRole, btnRegistroTurno, btnHistorial, btnReportes,
            btnAdministrarCajeros, btnAdministrarTiendas, btnChatPrivado,
            btnCambiarTienda, btnLogout;

    private SharedPreferences prefs;
    private String currentRoleOverride;
    private String loggedUserRole;
    private DatabaseHelper dbHelper;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        configureStatusBar();
        Log.d(TAG, "onCreate: Iniciando Dashboard.");

        mAuth = FirebaseAuth.getInstance();

        // Verificar sesión de Firebase
        if (mAuth.getCurrentUser() == null) {
            redirectToLogin();
            return;
        }

        // Solicitar permiso de notificaciones si es necesario (Android 13+)
        checkAndRequestNotificationPermission(); // <-- NUEVA LLAMADA

        // Inicializar vistas
        initializeViews();

        dbHelper = new DatabaseHelper(this);
        prefs = getSharedPreferences("CuadraSmartPrefs", MODE_PRIVATE);

        loadAndDisplayUserData();
        setupButtonListeners();

        Log.d(TAG, "onCreate: Dashboard configurado.");
    }

    private void initializeViews() {
        tvWelcome = findViewById(R.id.tvWelcome);
        tvCurrentStore = findViewById(R.id.tvCurrentStore);
        tvSupervisorModulesTitle = findViewById(R.id.tvSupervisorModulesTitle);
        btnToggleRole = findViewById(R.id.btnToggleRole);
        btnRegistroTurno = findViewById(R.id.btnRegistroTurno);
        btnHistorial = findViewById(R.id.btnHistorial);
        btnReportes = findViewById(R.id.btnReportes);
        btnAdministrarCajeros = findViewById(R.id.btnAdministrarCajeros);
        btnAdministrarTiendas = findViewById(R.id.btnAdministrarTiendas);
        btnChatPrivado = findViewById(R.id.btnChatPrivado);
        btnCambiarTienda = findViewById(R.id.btnCambiarTienda);
        btnLogout = findViewById(R.id.btnLogout);
        Log.d(TAG, "initializeViews: Vistas encontradas.");
    }

    private void loadAndDisplayUserData() {
        loggedUserRole = prefs.getString("logged_user_role", "cajero");
        currentRoleOverride = prefs.getString("override_role", loggedUserRole);
        String selectedStore = prefs.getString("selected_store", ""); // Default a vacío
        String loggedInUserEmail = prefs.getString("logged_user_email", "Usuario");

        Log.d(TAG, "loadAndDisplayUserData: Rol Real=" + loggedUserRole + ", Vista Actual=" + currentRoleOverride +
                ", Tienda=" + selectedStore + ", Email=" + loggedInUserEmail);

        if (TextUtils.isEmpty(selectedStore) || "Tienda no seleccionada".equals(selectedStore)) {
            Toast.makeText(this, "Por favor, seleccione una tienda.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(DashboardActivity.this, TiendasActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
            return;
        }

        String userName = loggedInUserEmail;
        if (loggedInUserEmail != null && loggedInUserEmail.contains("@")) {
            userName = loggedInUserEmail.split("@")[0];
            userName = userName.substring(0, 1).toUpperCase() + userName.substring(1);
        }
        tvWelcome.setText("¡Hola, " + userName + "!\nBienvenido a CuadraSmart");
        tvCurrentStore.setText("Tienda: " + selectedStore);
        updateUIForRole();
    }

    private void setupButtonListeners() {
        btnToggleRole.setOnClickListener(v -> toggleRole());
        btnRegistroTurno.setOnClickListener(v -> navigateTo(RegistroTurnoActivity.class));
        btnHistorial.setOnClickListener(v -> navigateTo(HistorialActivity.class));
        btnReportes.setOnClickListener(v -> navigateTo(ReportesActivity.class));
        btnAdministrarCajeros.setOnClickListener(v -> navigateTo(AdministrarCajerosActivity.class));
        btnAdministrarTiendas.setOnClickListener(v -> navigateTo(AdministrarTiendasActivity.class));
        btnChatPrivado.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, ListaSupervisoresActivity.class);
            startActivity(intent);
        });
        btnCambiarTienda.setOnClickListener(v -> handleChangeStore());
        btnLogout.setOnClickListener(v -> logoutUser());
        Log.d(TAG, "setupButtonListeners: Listeners configurados.");
    }

    private void redirectToLogin() {
        Log.w(TAG, "redirectToLogin: No hay usuario de Firebase. Redirigiendo a LoginActivity.");
        Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handleChangeStore() {
        Log.d(TAG, "Botón Cambiar Tienda presionado.");
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("selected_store");
        editor.apply();
        Log.d(TAG, "Tienda seleccionada eliminada de SharedPreferences.");
        Intent intent = new Intent(DashboardActivity.this, TiendasActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // --- LÓGICA DE PERMISO DE NOTIFICACIONES ---
    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permiso POST_NOTIFICATIONS ya concedido.");
                // El permiso ya está concedido, puedes realizar acciones que dependan de él si es necesario aquí.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // Opcional: Muestra una UI explicando por qué necesitas el permiso.
                // Esto es útil si el usuario denegó el permiso previamente.
                Log.d(TAG, "Se debería mostrar rationale para POST_NOTIFICATIONS.");
                // Por ahora, simplemente lo solicitamos de nuevo. Podrías mostrar un diálogo aquí.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_POST_NOTIFICATIONS);
            }
            else {
                // Solicitar el permiso directamente
                Log.d(TAG, "Solicitando permiso POST_NOTIFICATIONS.");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_POST_NOTIFICATIONS);
            }
        } else {
            Log.d(TAG, "No se requiere permiso POST_NOTIFICATIONS (API < 33).");
            // En versiones anteriores a Android 13, las notificaciones están habilitadas por defecto.
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permiso POST_NOTIFICATIONS CONCEDIDO por el usuario.");
                // Permiso concedido. Puedes realizar acciones que dependan de este permiso si es necesario.
            } else {
                Log.w(TAG, "Permiso POST_NOTIFICATIONS DENEGADO por el usuario.");
                Toast.makeText(this, "Las notificaciones de chat podrían no funcionar correctamente sin este permiso.", Toast.LENGTH_LONG).show();
                // Aquí podrías guiar al usuario a los ajustes de la app si quieres que lo habiliten manualmente.
            }
        }
        // Aquí irían otros `else if` para otros requestCodes de permisos si los tuvieras en esta actividad.
    }
    // --- FIN LÓGICA DE PERMISO DE NOTIFICACIONES ---


    private void configureStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window window = getWindow();
            View decorView = window.getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.background));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.background));
        }
    }

    private void navigateTo(Class<?> activityClass) {
        Intent intent = new Intent(DashboardActivity.this, activityClass);
        startActivity(intent);
    }

    private void toggleRole() {
        if (!"supervisor".equals(loggedUserRole)) {
            Toast.makeText(this, "Solo los supervisores pueden cambiar la vista.", Toast.LENGTH_SHORT).show();
            return;
        }
        if ("supervisor".equals(currentRoleOverride)) {
            currentRoleOverride = "cajero";
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("override_role", currentRoleOverride);
            editor.apply();
            updateUIForRole();
        } else {
            showSupervisorPasswordDialog();
        }
    }

    private void showSupervisorPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Verificación de Supervisor");
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_supervisor_password, (ViewGroup) findViewById(android.R.id.content), false);
        final EditText etPasswordInput = viewInflated.findViewById(R.id.etSupervisorPasswordDialog);
        builder.setView(viewInflated);
        builder.setPositiveButton("Confirmar", (dialog, which) -> {
            String enteredPassword = etPasswordInput.getText().toString();
            if (TextUtils.isEmpty(enteredPassword)) {
                Toast.makeText(DashboardActivity.this, "Ingrese la contraseña", Toast.LENGTH_SHORT).show();
                return;
            }
            FirebaseUser firebaseUser = mAuth.getCurrentUser();
            if (firebaseUser == null || firebaseUser.getEmail() == null) {
                Toast.makeText(DashboardActivity.this, "Error: Sesión no válida.", Toast.LENGTH_LONG).show();
                return;
            }
            String supervisorEmail = firebaseUser.getEmail();
            mAuth.signInWithEmailAndPassword(supervisorEmail, enteredPassword) // Re-autenticar
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            currentRoleOverride = "supervisor";
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("override_role", currentRoleOverride);
                            editor.apply();
                            updateUIForRole();
                            Toast.makeText(DashboardActivity.this, "Vista cambiada a Supervisor", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(DashboardActivity.this, "Contraseña de supervisor incorrecta", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateUIForRole() {
        boolean isSupervisorView = "supervisor".equals(currentRoleOverride);
        Log.d(TAG, "updateUIForRole: Vista=" + (isSupervisorView ? "Supervisor" : "Cajero") + ", Rol real: " + loggedUserRole);

        if ("supervisor".equals(loggedUserRole)) {
            btnToggleRole.setVisibility(View.VISIBLE);
            btnToggleRole.setText(isSupervisorView ? "Cambiar a Vista Cajero" : "Cambiar a Vista Supervisor");
        } else {
            btnToggleRole.setVisibility(View.GONE);
        }
        btnRegistroTurno.setVisibility(View.VISIBLE);
        btnHistorial.setVisibility(View.VISIBLE);

        if (tvSupervisorModulesTitle != null) tvSupervisorModulesTitle.setVisibility(isSupervisorView ? View.VISIBLE : View.GONE);
        btnReportes.setVisibility(isSupervisorView ? View.VISIBLE : View.GONE);
        btnAdministrarCajeros.setVisibility(isSupervisorView ? View.VISIBLE : View.GONE);
        btnAdministrarTiendas.setVisibility(isSupervisorView ? View.VISIBLE : View.GONE);
        btnChatPrivado.setVisibility(isSupervisorView ? View.VISIBLE : View.GONE);
    }

    private void logoutUser() {
        new AlertDialog.Builder(this)
                .setTitle("Cerrar Sesión")
                .setMessage("¿Está seguro?")
                .setPositiveButton("Sí, Cerrar Sesión", (dialog, which) -> {
                    if (mAuth != null) mAuth.signOut();
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.remove("logged_user_email");
                    editor.remove("logged_user_role");
                    editor.remove("override_role");
                    editor.remove("selected_store");
                    editor.apply();
                    Log.i(TAG, "SharedPreferences limpiadas para logout.");
                    redirectToLogin();
                    Toast.makeText(DashboardActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}