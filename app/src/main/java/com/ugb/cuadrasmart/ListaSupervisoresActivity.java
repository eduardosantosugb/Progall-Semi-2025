package com.ugb.cuadrasmart;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot; // Para el log

import java.util.ArrayList;
import java.util.List;

public class ListaSupervisoresActivity extends AppCompatActivity implements SupervisorAdapter.OnSupervisorClickListener {

    private static final String TAG = "ListaSupervisoresAct"; // TAG más corto para filtrar fácil

    private RecyclerView rvListaSupervisores;
    private SupervisorAdapter adapter;
    private List<Supervisor> supervisorListInternal; // Renombrado para evitar confusión
    private FirebaseFirestore dbFirestore;
    private FirebaseAuth mAuth;
    private TextView tvNoSupervisores;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_supervisores);
        Log.d(TAG, "onCreate: Iniciando actividad.");

        toolbar = findViewById(R.id.toolbarListaSupervisores);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        rvListaSupervisores = findViewById(R.id.rvListaSupervisores);
        tvNoSupervisores = findViewById(R.id.tvNoSupervisores);

        dbFirestore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        supervisorListInternal = new ArrayList<>();

        adapter = new SupervisorAdapter(supervisorListInternal, this);
        rvListaSupervisores.setLayoutManager(new LinearLayoutManager(this));
        rvListaSupervisores.setAdapter(adapter);

        Log.d(TAG, "onCreate: Adaptador y RecyclerView configurados. Llamando a loadSupervisores...");
        loadSupervisores();
    }

    private void loadSupervisores() {
        Log.d(TAG, "loadSupervisores: Iniciando carga de supervisores...");
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Usuario no autenticado. Por favor, inicie sesión.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "loadSupervisores: currentUser es NULL. Finalizando actividad.");
            // Considera redirigir a LoginActivity
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        String currentUserUid = currentUser.getUid();
        String currentUserEmail = currentUser.getEmail(); // Para logueo
        Log.i(TAG, "loadSupervisores: Usuario actual UID: " + currentUserUid + ", Email: " + currentUserEmail);

        // Limpiar la lista antes de cargar para evitar duplicados si se llama múltiples veces
        supervisorListInternal.clear();
        Log.d(TAG, "loadSupervisores: Lista interna de supervisores limpiada.");

        dbFirestore.collection("users")
                .whereEqualTo("role", "supervisor") // Asegúrate que el campo 'role' y el valor 'supervisor' sean exactos
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot == null) {
                            Log.e(TAG, "loadSupervisores: querySnapshot es NULL después de una tarea exitosa.");
                            tvNoSupervisores.setText("Error al cargar datos (snapshot nulo).");
                            tvNoSupervisores.setVisibility(View.VISIBLE);
                            rvListaSupervisores.setVisibility(View.GONE);
                            return;
                        }

                        Log.d(TAG, "loadSupervisores: Consulta a Firestore exitosa. Documentos encontrados: " + querySnapshot.size());

                        for (QueryDocumentSnapshot document : querySnapshot) {
                            String docId = document.getId();
                            String name = document.getString("name");
                            String email = document.getString("email");
                            String role = document.getString("role"); // Para verificar

                            Log.d(TAG, "Procesando documento Firestore: ID=" + docId + ", Name=" + name + ", Email=" + email + ", Role=" + role);

                            // Verificar si el rol es realmente "supervisor" (ya filtrado, pero doble chequeo no daña)
                            if (!"supervisor".equals(role)) {
                                Log.w(TAG, "Documento con ID " + docId + " no tiene role='supervisor'. Omitiendo.");
                                continue;
                            }

                            // No agregar el usuario actual a la lista
                            if (!docId.equals(currentUserUid)) {
                                Supervisor supervisor = new Supervisor(); // Usar constructor vacío
                                supervisor.setUid(docId);
                                supervisor.setName(name != null ? name : "Nombre Desconocido");
                                supervisor.setEmail(email != null ? email : "Email Desconocido");
                                // No es necesario setear el rol aquí ya que lo filtramos y verificamos

                                supervisorListInternal.add(supervisor);
                                Log.i(TAG, "Supervisor AÑADIDO a la lista: " + supervisor.getName() + " (UID: " + supervisor.getUid() + ")");
                            } else {
                                Log.d(TAG, "Omitiendo usuario actual de la lista: " + name + " (UID: " + docId + ")");
                            }
                        }

                        Log.d(TAG, "loadSupervisores: Total supervisores (excluyendo actual) en supervisorListInternal: " + supervisorListInternal.size());
                        adapter.updateData(new ArrayList<>(supervisorListInternal)); // Pasar una nueva copia de la lista al adaptador

                        if (supervisorListInternal.isEmpty()) {
                            Log.i(TAG, "loadSupervisores: La lista de otros supervisores está VACÍA.");
                            tvNoSupervisores.setText("No hay otros supervisores disponibles para chatear.");
                            tvNoSupervisores.setVisibility(View.VISIBLE);
                            rvListaSupervisores.setVisibility(View.GONE);
                        } else {
                            Log.i(TAG, "loadSupervisores: Mostrando " + supervisorListInternal.size() + " supervisor(es).");
                            tvNoSupervisores.setVisibility(View.GONE);
                            rvListaSupervisores.setVisibility(View.VISIBLE);
                        }

                    } else {
                        Log.e(TAG, "Error obteniendo lista de supervisores de Firestore: ", task.getException());
                        Toast.makeText(ListaSupervisoresActivity.this, "Error al cargar supervisores.", Toast.LENGTH_SHORT).show();
                        tvNoSupervisores.setText("Error al cargar supervisores desde la base de datos.");
                        tvNoSupervisores.setVisibility(View.VISIBLE);
                        rvListaSupervisores.setVisibility(View.GONE);
                    }
                });
    }

    @Override
    public void onSupervisorClick(Supervisor supervisor) {
        if (supervisor == null || supervisor.getUid() == null) {
            Log.e(TAG, "onSupervisorClick: El objeto supervisor o su UID es null.");
            Toast.makeText(this, "Error al seleccionar supervisor.", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.i(TAG, "Supervisor seleccionado para chat: " + supervisor.getName() + " con UID: " + supervisor.getUid());
        Intent intent = new Intent(this, ChatPrivadoActivity.class);
        intent.putExtra("recipient_uid", supervisor.getUid());
        intent.putExtra("recipient_name", supervisor.getName());
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}