package com.ugb.cuadrasmart;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnRegistroTurno, btnHistorial, btnReportes, btnAdministrarCajeros, btnCerrarSesion;
    public static final String PREFS_NAME = "CuadraSmartPrefs";
    public static final String KEY_SELECTED_TIENDA = "selected_tienda";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicialización de botones
        btnRegistroTurno = findViewById(R.id.btnRegistroTurno);
        btnHistorial = findViewById(R.id.btnHistorial);
        btnReportes = findViewById(R.id.btnReportes);
        btnAdministrarCajeros = findViewById(R.id.btnAdministrarCajeros);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);

        // Ejemplo de lectura de la tienda seleccionada globalmente
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String tiendaSeleccionada = prefs.getString(KEY_SELECTED_TIENDA, "Sin Tienda");
        // Puedes mostrar la tienda seleccionada en algún TextView si lo deseas

        // Navegación a Registro de Turno
        btnRegistroTurno.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, RegistroTurnoActivity.class);
                startActivity(intent);
            }
        });

        // Navegación a Historial
        btnHistorial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, HistorialActivity.class);
                startActivity(intent);
            }
        });

        // Navegación a Reportes
        btnReportes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ReportesActivity.class);
                startActivity(intent);
            }
        });

        // Navegación a Administración de Cajeros
        btnAdministrarCajeros.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AdministrarCajerosActivity.class);
                startActivity(intent);
            }
        });

        // Cerrar sesión: aquí puedes borrar las preferencias de sesión o simplemente finalizar la actividad
        btnCerrarSesion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Por ejemplo, puedes limpiar las preferencias y volver al LoginActivity
                SharedPreferences.Editor editor = prefs.edit();
                editor.clear();
                editor.apply();
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
