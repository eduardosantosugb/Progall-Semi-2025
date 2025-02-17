package com.ugb.cuadrasmart;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnRegistroTurno, btnHistorial, btnReportes, btnAdministrarCajeros, btnCerrarSesion;

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

        // Configurar clic para Registro de Turno
        btnRegistroTurno.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, RegistroTurnoActivity.class));
            }
        });

        // Configurar clic para Historial
        btnHistorial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, HistorialActivity.class));
            }
        });

        // Configurar clic para Reportes
        btnReportes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, ReportesActivity.class));
            }
        });

        // Configurar clic para Administración de Cajeros
        btnAdministrarCajeros.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AdministrarCajerosActivity.class));
            }
        });

        // Configurar clic para Cerrar Sesión con confirmación
        btnCerrarSesion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Mostrar diálogo de confirmación
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Cerrar Sesión")
                        .setMessage("¿Está seguro que desea cerrar sesión?")
                        .setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Aquí se puede limpiar la sesión (por ejemplo, SharedPreferences) si se ha implementado esa funcionalidad
                                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                                finish();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });
    }
}