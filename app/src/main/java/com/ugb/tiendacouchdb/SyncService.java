package com.ugb.tiendacouchdb;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import java.util.List;

public class SyncService {

    private DatabaseHelper dbHelper;
    private Context context;

    public SyncService(Context context) {
        this.context = context;
        dbHelper = new DatabaseHelper(context);
    }

    public void syncProducts() {
        // Obtener todos los productos de la base de datos local
        final List<Product> products = dbHelper.getAllProducts();

        // Enviar cada producto al servidor
        for (final Product product : products) {
            WebServiceClient.sendProduct(product, new WebServiceClient.WebServiceCallback() {
                @Override
                public void onSuccess(String response) {
                    // Aquí se podría marcar el producto como sincronizado en la base de datos local
                }

                @Override
                public void onError(Exception e) {
                    // Manejo de errores (por ejemplo, registrar en log o mostrar un mensaje)
                }
            });
        }
        // Para efectos de demostración se notifica al usuario
        Toast.makeText(context, "Sincronización completa", Toast.LENGTH_SHORT).show();
    }
}
