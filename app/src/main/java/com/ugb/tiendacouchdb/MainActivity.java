package com.ugb.tiendacouchdb;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private List<Product> productList;
    private FloatingActionButton fabAdd;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configurar el Toolbar y asignarlo como ActionBar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Inicializar el RecyclerView y el FloatingActionButton
        recyclerView = findViewById(R.id.recyclerViewProducts);
        fabAdd = findViewById(R.id.fabAddProduct);

        // Inicializar la base de datos y cargar la lista de productos
        dbHelper = new DatabaseHelper(this);
        productList = new ArrayList<>();
        productList.addAll(dbHelper.getAllProducts());

        // Configurar el adaptador y RecyclerView
        adapter = new ProductAdapter(productList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Acción del FAB: abrir la actividad para agregar/editar producto
        fabAdd.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, AddEditProductActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Actualizar la lista de productos
        List<Product> updatedList = dbHelper.getAllProducts();
        adapter.setProductList(updatedList);

    }

    // Inflar el menú (búsqueda y sincronización)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // Configurar el SearchView para filtrar productos
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        if (searchView != null) {
            searchView.setQueryHint("Buscar productos...");
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    adapter.getFilter().filter(query);
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    adapter.getFilter().filter(newText);
                    return true;
                }
            });
        }
        return true;
    }

    // Manejo de los ítems del menú
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_sync) {
            // Llamar al servicio de sincronización (SyncService)
            SyncService syncService = new SyncService(MainActivity.this);
            syncService.syncProducts();
            Toast.makeText(MainActivity.this, "Sincronización iniciada", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
