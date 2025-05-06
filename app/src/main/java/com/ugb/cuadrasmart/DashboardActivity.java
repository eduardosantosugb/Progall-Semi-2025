package com.ugb.cuadrasmart;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log; // Import Log
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity"; // TAG para logs

    // UI Elements
    private TextView tvWelcome;
    private Button btnToggleRole, btnRegistroTurno, btnHistorial, btnReportes,
            btnAdministrarCajeros, btnAdministrarTiendas, btnChatPrivado,
            btnCambiarTienda; // Añadir el nuevo botón

    // Otros
    private SharedPreferences prefs;
    private String currentRoleOverride; // "supervisor" o "cajero"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: Iniciando Dashboard.");

        // --- Encontrar Vistas ---
        tvWelcome = findViewById(R.id.tvWelcome);
        btnToggleRole = findViewById(R.id.btnToggleRole);
        btnRegistroTurno = findViewById(R.id.btnRegistroTurno);
        btnHistorial = findViewById(R.id.btnHistorial);
        btnReportes = findViewById(R.id.btnReportes);
        btnAdministrarCajeros = findViewById(R.id.btnAdministrarCajeros);
        btnAdministrarTiendas = findViewById(R.id.btnAdministrarTiendas);
        btnChatPrivado = findViewById(R.id.btnChatPrivado);
        btnCambiarTienda = findViewById(R.id.btnCambiarTienda); // Encontrar nuevo botón
        Log.d(TAG, "onCreate: Vistas encontradas.");

        // --- Cargar Preferencias y Rol ---
        prefs = getSharedPreferences("CuadraSmartPrefs", MODE_PRIVATE);
        currentRoleOverride = prefs.getString("override_role", "supervisor"); // Default a supervisor
        String selectedStore = prefs.getString("selected_store", "Tienda no seleccionada");
        Log.d(TAG, "onCreate: Rol actual=" + currentRoleOverride + ", Tienda=" + selectedStore);

        // --- Configurar UI Inicial ---
        updateUIForRole(); // Actualizar visibilidad de botones según rol
        tvWelcome.setText("Bienvenido a CuadraSmart\nTienda: " + selectedStore); // Mostrar tienda

        // --- Configurar Listeners ---
        btnToggleRole.setOnClickListener(v -> {
            Log.d(TAG, "Botón ToggleRole presionado.");
            toggleRole();
        });

        // Listeners de Navegación
        btnRegistroTurno.setOnClickListener(v -> navigateTo(RegistroTurnoActivity.class));
        btnHistorial.setOnClickListener(v -> navigateTo(HistorialActivity.class));
        btnReportes.setOnClickListener(v -> navigateTo(ReportesActivity.class));
        btnAdministrarCajeros.setOnClickListener(v -> navigateTo(AdministrarCajerosActivity.class));
        btnAdministrarTiendas.setOnClickListener(v -> navigateTo(AdministrarTiendasActivity.class));
        btnChatPrivado.setOnClickListener(v -> navigateTo(ChatPrivadoActivity.class));

        // Listener para Cambiar Tienda
        if (btnCambiarTienda != null) {
            btnCambiarTienda.setOnClickListener(v -> {
                Log.d(TAG, "Botón Cambiar Tienda presionado.");
                Intent intent = new Intent(DashboardActivity.this, TiendasActivity.class);
                // Limpiar SharedPreferences de la tienda actual podría ser una opción, pero
                // simplemente ir a TiendasActivity y finish() fuerza a re-seleccionar.
                // SharedPreferences.Editor editor = prefs.edit();
                // editor.remove("selected_store");
                // editor.apply();
                startActivity(intent);
                finish(); // Cierra este Dashboard para obligar a elegir tienda de nuevo
            });
        } else {
            Log.e(TAG, "onCreate: Botón Cambiar Tienda (btnCambiarTienda) no encontrado!");
        }

        Log.d(TAG, "onCreate: Listeners configurados.");
    }

    /**
     * Método helper para navegar a otra actividad.
     * @param activityClass La clase de la Activity a la que navegar.
     */
    private void navigateTo(Class<?> activityClass) {
        Log.d(TAG, "Navegando a: " + activityClass.getSimpleName());
        Intent intent = new Intent(DashboardActivity.this, activityClass);
        startActivity(intent);
    }


    /**
     * Alterna el rol override entre "supervisor" y "cajero" y actualiza la UI.
     */
    private void toggleRole() {
        if ("supervisor".equals(currentRoleOverride)) {
            currentRoleOverride = "cajero";
        } else {
            currentRoleOverride = "supervisor";
        }
        Log.d(TAG, "toggleRole: Nuevo rol override = " + currentRoleOverride);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("override_role", currentRoleOverride);
        editor.apply();
        updateUIForRole(); // Actualizar visibilidad de botones
    }

    /**
     * Actualiza la visibilidad de los botones del menú según el rol override.
     */
    private void updateUIForRole() {
        boolean isSupervisor = "supervisor".equals(currentRoleOverride);
        Log.d(TAG, "updateUIForRole: Actualizando para rol " + (isSupervisor ? "Supervisor" : "Cajero"));

        btnToggleRole.setText(isSupervisor ? "Cambiar a Cajero" : "Cambiar a Supervisor");

        // Botones visibles para ambos roles (o al menos para cajero)
        btnRegistroTurno.setVisibility(View.VISIBLE);
        btnHistorial.setVisibility(View.VISIBLE);

        // Botones solo para Supervisor
        btnReportes.setVisibility(isSupervisor ? View.VISIBLE : View.GONE);
        btnAdministrarCajeros.setVisibility(isSupervisor ? View.VISIBLE : View.GONE);
        btnAdministrarTiendas.setVisibility(isSupervisor ? View.VISIBLE : View.GONE);
        btnChatPrivado.setVisibility(isSupervisor ? View.VISIBLE : View.GONE);

        // El botón de cambiar tienda debe ser siempre visible en el Dashboard
        if (btnCambiarTienda != null) {
            btnCambiarTienda.setVisibility(View.VISIBLE);
        }
    }
}