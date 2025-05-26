package com.ugb.cuadrasmart;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build; // Para verificar la versión de Android
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;   // Para getWindow().getDecorView()
import android.view.ViewGroup;
import android.view.Window; // Para getWindow()
// import android.view.WindowManager; // No es estrictamente necesario para este caso si solo usamos flags de SystemUiVisibility
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat; // Para obtener colores

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";

    // UI Elements
    private TextView tvWelcome, tvCurrentStore, tvSupervisorModulesTitle; // tvCurrentStore y tvSupervisorModulesTitle son nuevos
    private Button btnToggleRole, btnRegistroTurno, btnHistorial, btnReportes,
            btnAdministrarCajeros, btnAdministrarTiendas, btnChatPrivado,
            btnCambiarTienda, btnLogout;

    // Otros
    private SharedPreferences prefs;
    private String currentRoleOverride;
    private String loggedUserRole;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Asegúrate que este es el layout correcto

        // Configurar la barra de estado ANTES de cualquier otra cosa que pueda afectar la UI
        configureStatusBar();
        Log.d(TAG, "onCreate: Iniciando Dashboard.");

        // --- Encontrar Vistas ---
        tvWelcome = findViewById(R.id.tvWelcome);
        tvCurrentStore = findViewById(R.id.tvCurrentStore); // Nuevo
        tvSupervisorModulesTitle = findViewById(R.id.tvSupervisorModulesTitle); // Nuevo

        btnToggleRole = findViewById(R.id.btnToggleRole);
        btnRegistroTurno = findViewById(R.id.btnRegistroTurno);
        btnHistorial = findViewById(R.id.btnHistorial);
        btnReportes = findViewById(R.id.btnReportes);
        btnAdministrarCajeros = findViewById(R.id.btnAdministrarCajeros);
        btnAdministrarTiendas = findViewById(R.id.btnAdministrarTiendas);
        btnChatPrivado = findViewById(R.id.btnChatPrivado);
        btnCambiarTienda = findViewById(R.id.btnCambiarTienda);
        btnLogout = findViewById(R.id.btnLogout);
        Log.d(TAG, "onCreate: Vistas encontradas.");

        // --- Inicializar Helpers y Preferencias ---
        dbHelper = new DatabaseHelper(this);
        prefs = getSharedPreferences("CuadraSmartPrefs", MODE_PRIVATE);

        loggedUserRole = prefs.getString("logged_user_role", "cajero");
        currentRoleOverride = prefs.getString("override_role", loggedUserRole);
        String selectedStore = prefs.getString("selected_store", "Tienda no seleccionada");
        String loggedInUserEmail = prefs.getString("logged_user_email", "Usuario");

        Log.d(TAG, "onCreate: Rol Real=" + loggedUserRole + ", Vista Actual=" + currentRoleOverride + ", Tienda=" + selectedStore + ", Email=" + loggedInUserEmail);

        // --- Configurar UI Inicial ---
        // Extraer el nombre del email si es posible, o simplemente usar el email
        String userName = loggedInUserEmail;
        if (loggedInUserEmail.contains("@")) {
            userName = loggedInUserEmail.split("@")[0];
        }
        // Capitalizar la primera letra del nombre de usuario
        if (!TextUtils.isEmpty(userName)) {
            userName = userName.substring(0, 1).toUpperCase() + userName.substring(1);
        }

        tvWelcome.setText("¡Hola, " + userName + "!\nBienvenido a CuadraSmart");
        tvCurrentStore.setText("Tienda: " + selectedStore);

        updateUIForRole(); // Actualizar visibilidad de botones según rol

        // --- Configurar Listeners ---
        btnToggleRole.setOnClickListener(v -> {
            Log.d(TAG, "Botón ToggleRole presionado.");
            toggleRole();
        });

        btnRegistroTurno.setOnClickListener(v -> navigateTo(RegistroTurnoActivity.class));
        btnHistorial.setOnClickListener(v -> navigateTo(HistorialActivity.class));
        btnReportes.setOnClickListener(v -> navigateTo(ReportesActivity.class));
        btnAdministrarCajeros.setOnClickListener(v -> navigateTo(AdministrarCajerosActivity.class));
        btnAdministrarTiendas.setOnClickListener(v -> navigateTo(AdministrarTiendasActivity.class));
        btnChatPrivado.setOnClickListener(v -> navigateTo(ChatPrivadoActivity.class));

        if (btnCambiarTienda != null) {
            btnCambiarTienda.setOnClickListener(v -> {
                Log.d(TAG, "Botón Cambiar Tienda presionado.");
                Intent intent = new Intent(DashboardActivity.this, TiendasActivity.class);
                startActivity(intent);
                finish();
            });
        } else {
            Log.e(TAG, "onCreate: Botón Cambiar Tienda (btnCambiarTienda) no encontrado!");
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                Log.d(TAG, "Botón Cerrar Sesión presionado.");
                logoutUser();
            });
        } else {
            Log.e(TAG, "onCreate: Botón Cerrar Sesión (btnLogout) no encontrado!");
        }

        Log.d(TAG, "onCreate: Listeners configurados.");
    }

    private void configureStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window window = getWindow();
            View decorView = window.getDecorView();
            int currentFlags = decorView.getSystemUiVisibility();
            decorView.setSystemUiVisibility(currentFlags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.background));
            Log.d(TAG, "configureStatusBar: API >= 23. Light status bar configurada.");
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            // Para API 21-22, podrías elegir un color más oscuro si el fondo es muy claro
            // window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryVariant));
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.background)); // O mantener el color de fondo
            Log.d(TAG, "configureStatusBar: API 21-22. Status bar color configurado.");
        }
    }

    private void navigateTo(Class<?> activityClass) {
        Log.d(TAG, "Navegando a: " + activityClass.getSimpleName());
        Intent intent = new Intent(DashboardActivity.this, activityClass);
        startActivity(intent);
    }

    private void toggleRole() {
        if (!"supervisor".equals(loggedUserRole)) {
            Toast.makeText(this, "Solo los supervisores pueden cambiar la vista.", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "toggleRole: Intento de cambio de vista por un no supervisor (" + loggedUserRole + "). Denegado.");
            return;
        }

        if ("supervisor".equals(currentRoleOverride)) {
            currentRoleOverride = "cajero";
            Log.d(TAG, "toggleRole: Cambiando a vista override = " + currentRoleOverride);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("override_role", currentRoleOverride);
            editor.apply();
            updateUIForRole();
        } else {
            Log.d(TAG, "toggleRole: Intentando cambiar a Vista Supervisor. Mostrando diálogo de contraseña.");
            showSupervisorPasswordDialog();
        }
    }

    private void showSupervisorPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Verificación de Supervisor");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_supervisor_password,
                (ViewGroup) findViewById(android.R.id.content), false);
        final EditText etPasswordInput = viewInflated.findViewById(R.id.etSupervisorPasswordDialog);

        builder.setView(viewInflated);

        builder.setPositiveButton("Confirmar", (dialog, which) -> {
            String enteredPassword = etPasswordInput.getText().toString();
            if (TextUtils.isEmpty(enteredPassword)) {
                Toast.makeText(DashboardActivity.this, "Por favor, ingrese la contraseña", Toast.LENGTH_SHORT).show();
                return;
            }

            String supervisorEmail = prefs.getString("logged_user_email", null);
            if (supervisorEmail == null) {
                Toast.makeText(DashboardActivity.this, "Error: No se pudo identificar al supervisor.", Toast.LENGTH_LONG).show();
                Log.e(TAG, "showSupervisorPasswordDialog: logged_user_email es null en SharedPreferences.");
                return;
            }

            String actualPassword = dbHelper.getPasswordByEmail(supervisorEmail);

            if (actualPassword != null && actualPassword.equals(enteredPassword)) {
                currentRoleOverride = "supervisor";
                Log.d(TAG, "showSupervisorPasswordDialog: Contraseña correcta. Cambiando a vista override = " + currentRoleOverride);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("override_role", currentRoleOverride);
                editor.apply();
                updateUIForRole();
                Toast.makeText(DashboardActivity.this, "Vista cambiada a Supervisor", Toast.LENGTH_SHORT).show();
            } else {
                Log.w(TAG, "showSupervisorPasswordDialog: Contraseña incorrecta para " + supervisorEmail);
                Toast.makeText(DashboardActivity.this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> {
            Log.d(TAG, "showSupervisorPasswordDialog: Cancelado por el usuario.");
            dialog.cancel();
        });

        builder.show();
    }

    private void updateUIForRole() {
        boolean isSupervisorView = "supervisor".equals(currentRoleOverride);
        Log.d(TAG, "updateUIForRole: Actualizando para vista " + (isSupervisorView ? "Supervisor" : "Cajero") +
                ". Rol real: " + loggedUserRole);

        // Configurar el botón de cambio de rol
        if ("supervisor".equals(loggedUserRole)) {
            btnToggleRole.setVisibility(View.VISIBLE);
            btnToggleRole.setText(isSupervisorView ? "Cambiar a Vista Cajero" : "Cambiar a Vista Supervisor");
        } else {
            btnToggleRole.setVisibility(View.GONE);
        }

        // Botones visibles para ambos roles (o al menos para cajero en su vista)
        btnRegistroTurno.setVisibility(View.VISIBLE);
        btnHistorial.setVisibility(View.VISIBLE);

        // Título y Módulos de Supervisor
        // Asegurarse que los IDs en el XML coinciden con los usados aquí para los títulos
        if (tvSupervisorModulesTitle != null) { // Chequeo por si acaso
            tvSupervisorModulesTitle.setVisibility(isSupervisorView ? View.VISIBLE : View.GONE);
        }

        btnReportes.setVisibility(isSupervisorView ? View.VISIBLE : View.GONE);
        btnAdministrarCajeros.setVisibility(isSupervisorView ? View.VISIBLE : View.GONE);
        btnAdministrarTiendas.setVisibility(isSupervisorView ? View.VISIBLE : View.GONE);
        btnChatPrivado.setVisibility(isSupervisorView ? View.VISIBLE : View.GONE);

        // Botones siempre visibles si existen
        if (btnCambiarTienda != null) {
            btnCambiarTienda.setVisibility(View.VISIBLE);
        }
        if (btnLogout != null) {
            btnLogout.setVisibility(View.VISIBLE);
        }
    }

    private void logoutUser() {
        new AlertDialog.Builder(this)
                .setTitle("Cerrar Sesión")
                .setMessage("¿Está seguro de que desea cerrar la sesión actual?")
                .setPositiveButton("Sí, Cerrar Sesión", (dialog, which) -> {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.remove("logged_user_email");
                    editor.remove("logged_user_role");
                    editor.remove("override_role");
                    editor.remove("selected_store");
                    editor.apply();
                    Log.i(TAG, "logoutUser: SharedPreferences limpiadas.");

                    Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    Toast.makeText(DashboardActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}