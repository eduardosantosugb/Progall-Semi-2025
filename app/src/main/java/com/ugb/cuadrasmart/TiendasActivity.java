package com.ugb.cuadrasmart;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class TiendasActivity extends AppCompatActivity {

    private GridView gridTiendas;
    private List<Tienda> tiendaList;
    private TiendasAdapter adapter;

    public static final String PREFS_NAME = "CuadraSmartPrefs";
    public static final String KEY_SELECTED_TIENDA = "selected_tienda";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tiendas);

        gridTiendas = findViewById(R.id.gridTiendas);
        tiendaList = new ArrayList<>();

        // Agregar tiendas ficticias para la versión académica.
        // Asegúrate de tener los iconos correspondientes en res/drawable.
        tiendaList.add(new Tienda("Tienda Norte", R.drawable.ic_tienda_norte));
        tiendaList.add(new Tienda("Tienda Sur", R.drawable.ic_tienda_sur));
        tiendaList.add(new Tienda("Tienda Centro", R.drawable.ic_tienda_centro));
        tiendaList.add(new Tienda("Tienda Este", R.drawable.ic_tienda_este));
        tiendaList.add(new Tienda("Tienda Oeste", R.drawable.ic_tienda_oeste));

        adapter = new TiendasAdapter(this, tiendaList);
        gridTiendas.setAdapter(adapter);

        // Al seleccionar una tienda, se guarda la elección globalmente y se redirige al menú principal.
        gridTiendas.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Tienda tiendaSeleccionada = tiendaList.get(position);
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(KEY_SELECTED_TIENDA, tiendaSeleccionada.getNombre());
                editor.apply();

                // Redirigir a MainActivity
                Intent intent = new Intent(TiendasActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
