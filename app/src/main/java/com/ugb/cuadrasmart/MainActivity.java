package com.ugb.cuadrasmart;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnRegistroTurno, btnHistorial, btnReportes, btnAdministrarCajeros, btnGraficas, btnCerrarSesion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Referenciar elementos del layout
        btnRegistroTurno = findViewById(R.id.btnRegistroTurno);
        btnHistorial = findViewById(R.id.btnHistorial);
        btnReportes = findViewById(R.id.btnReportes);
        btnAdministrarCajeros = findViewById(R.id.btnAdministrarCajeros);

        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);

        // Navegación a Registro de Turno
        btnRegistroTurno.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, RegistroTurnoActivity.class));
            }
        });

        // Navegación a Historial
        btnHistorial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, HistorialActivity.class));
            }
        });

        // Navegación a Reportes
        btnReportes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ReportesActivity.class));
            }
        });

        // Navegación a Administración de Cajeros
        btnAdministrarCajeros.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AdministrarCajerosActivity.class));
            }
        });



        // Cerrar sesión con confirmación (código existente)
        btnCerrarSesion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Mostrar diálogo de confirmación...
            }
        });
    }
}