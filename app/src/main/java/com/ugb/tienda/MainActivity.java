package com.ugb.tienda;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView recyclerViewProducts;
    private FloatingActionButton fabAddProduct;
    private SearchView searchView;
    private ProductAdapter productAdapter;
    private List<Product> productList = new ArrayList<>();
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configurar el Toolbar como ActionBar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Inicializar vistas
        recyclerViewProducts = findViewById(R.id.recyclerViewProducts);
        fabAddProduct = findViewById(R.id.fabAddProduct);
        searchView = findViewById(R.id.searchView);

        // Inicializar DBHelper y la lista de productos
        dbHelper = new DBHelper(this);
        productList = new ArrayList<>();

        // Configurar RecyclerView y su adaptador
        recyclerViewProducts.setLayoutManager(new LinearLayoutManager(this));
        productAdapter = new ProductAdapter(this, productList, new ProductAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Product product) {
                // Al hacer clic, se abre la actividad de detalle del producto
                Intent intent = new Intent(MainActivity.this, ProductDetailActivity.class);
                intent.putExtra("productId", product.getId());
                startActivity(intent);
            }
        });
        recyclerViewProducts.setAdapter(productAdapter);

        // Configurar FloatingActionButton para agregar nuevos productos
        fabAddProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AddEditProductActivity.class);
                startActivity(intent);
            }
        });

        // Configurar SearchView para filtrar productos
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener(){
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterProducts(query);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                filterProducts(newText);
                return true;
            }
        });

        // Cargar productos desde la base de datos
        loadProducts();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflar el menú
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_home) {
            // Reiniciar búsqueda y recargar la lista completa
            searchView.setQuery("", false);
            searchView.clearFocus();
            loadProducts();
            return true;
        } else if (id == R.id.action_add) {
            // Abrir la actividad para agregar un nuevo producto
            Intent intent = new Intent(MainActivity.this, AddEditProductActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_search) {
            // Enfocar el SearchView para facilitar la búsqueda
            searchView.requestFocus();
            return true;
        } else if (id == R.id.action_about) {
            // Mostrar un diálogo "Acerca de"
            showAboutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAboutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Acerca de")
                .setMessage("Tienda App\nVersión 1.0\nDesarrollado por UGB")
                .setPositiveButton("OK", null)
                .show();
    }

    private void loadProducts() {
        productList = dbHelper.getAllProductsList();
        productAdapter.setProducts(productList);
    }

    private void filterProducts(String query) {
        List<Product> filteredList = new ArrayList<>();
        for (Product product : productList) {
            if (product.getCode().toLowerCase().contains(query.toLowerCase()) ||
                    product.getDescription().toLowerCase().contains(query.toLowerCase()) ||
                    product.getBrand().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(product);
            }
        }
        productAdapter.setProducts(filteredList);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProducts();
    }
}
