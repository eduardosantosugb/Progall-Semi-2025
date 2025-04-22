package com.ugb.tiendacouchdb;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class ProductDetailsActivity extends AppCompatActivity {

    private ImageView imgProductDetail;
    private TextView tvCode, tvDescription, tvBrand, tvPresentation, tvPrice;
    private Button btnEdit, btnDelete;
    private Product product;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        // Inicialización de vistas
        imgProductDetail = findViewById(R.id.imgProductDetail);
        tvCode = findViewById(R.id.tvCode);
        tvDescription = findViewById(R.id.tvDescription);
        tvBrand = findViewById(R.id.tvBrand);
        tvPresentation = findViewById(R.id.tvPresentation);
        tvPrice = findViewById(R.id.tvPrice);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);

        dbHelper = new DatabaseHelper(this);

        // Recibir el producto enviado por Intent
        if (getIntent() != null && getIntent().hasExtra("product")) {
            product = (Product) getIntent().getSerializableExtra("product");
            loadProductDetails(product);
        } else {
            Toast.makeText(this, "Producto no encontrado", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Acción para editar el producto
        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ProductDetailsActivity.this, AddEditProductActivity.class);
                intent.putExtra("product", product);
                startActivity(intent);
                finish();
            }
        });

        // Acción para eliminar el producto
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirmDeletion();
            }
        });
    }

    private void loadProductDetails(Product product) {
        tvCode.setText("Código: " + product.getCodigo());
        tvDescription.setText("Descripción: " + product.getDescripcion());
        tvBrand.setText("Marca: " + product.getMarca());
        tvPresentation.setText("Presentación: " + product.getPresentacion());
        tvPrice.setText("Precio: $" + product.getPrecio());

        // Se asume que la imagen se guarda como una URI válida
        imgProductDetail.setImageURI(Uri.parse(product.getImagen()));
    }

    private void confirmDeletion() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Eliminar Producto");
        builder.setMessage("¿Estás seguro de eliminar este producto?");
        builder.setPositiveButton("Sí", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                if (dbHelper.deleteProduct(product.getId())) {
                    Toast.makeText(ProductDetailsActivity.this, "Producto eliminado", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(ProductDetailsActivity.this, "Error al eliminar el producto", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("No", null);
        builder.show();
    }
}
