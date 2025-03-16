package com.ugb.tienda;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ProductDetailActivity extends AppCompatActivity {

    private ImageView imageViewDetail;
    private TextView tvCode, tvDescription, tvBrand, tvPresentation, tvPrice;
    private Button btnEdit, btnDelete;

    private DBHelper dbHelper;
    private Product currentProduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        imageViewDetail = findViewById(R.id.imageViewDetail);
        tvCode = findViewById(R.id.tvCode);
        tvDescription = findViewById(R.id.tvDescription);
        tvBrand = findViewById(R.id.tvBrand);
        tvPresentation = findViewById(R.id.tvPresentation);
        tvPrice = findViewById(R.id.tvPrice);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);

        dbHelper = new DBHelper(this);

        // Obtener el ID del producto desde el Intent
        int productId = getIntent().getIntExtra("productId", -1);
        if (productId != -1) {
            loadProduct(productId);
        } else {
            Toast.makeText(this, "Producto no encontrado", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ProductDetailActivity.this, AddEditProductActivity.class);
                intent.putExtra("productId", currentProduct.getId());
                startActivity(intent);
                finish();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmDeletion();
            }
        });
    }

    /**
     * Carga el producto usando el método getProductById del DBHelper
     * y actualiza la interfaz.
     */
    private void loadProduct(int productId) {
        currentProduct = dbHelper.getProductById(productId);
        if (currentProduct != null) {
            tvCode.setText("Código: " + currentProduct.getCode());
            tvDescription.setText("Descripción: " + currentProduct.getDescription());
            tvBrand.setText("Marca: " + currentProduct.getBrand());
            tvPresentation.setText("Presentación: " + currentProduct.getPresentation());
            tvPrice.setText("Precio: " + currentProduct.getPrice());
            ImageUtils.loadImage(this, currentProduct.getImagePath(), imageViewDetail);
        } else {
            Toast.makeText(this, "Producto no encontrado", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Muestra un diálogo para confirmar la eliminación del producto.
     */
    private void confirmDeletion() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_product))
                .setMessage(getString(R.string.delete_confirmation))
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteProduct();
                    }
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }

    /**
     * Elimina el producto de la base de datos y muestra un mensaje.
     */
    private void deleteProduct() {
        int rowsDeleted = dbHelper.deleteProduct(currentProduct.getId());
        if (rowsDeleted > 0) {
            Toast.makeText(this, "Producto eliminado", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Error al eliminar producto", Toast.LENGTH_SHORT).show();
        }
    }
}
