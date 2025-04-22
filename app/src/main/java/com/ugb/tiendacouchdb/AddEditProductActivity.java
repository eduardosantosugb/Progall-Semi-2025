package com.ugb.tiendacouchdb;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;

public class AddEditProductActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_CAMERA = 100;
    private static final int REQUEST_CODE_GALLERY = 200;
    private static final int PERMISSION_REQUEST_CAMERA = 300;
    private static final int PERMISSION_REQUEST_STORAGE = 400;

    private TextInputLayout layoutCode, layoutDescription, layoutBrand, layoutPresentation, layoutPrice, layoutCost;
    private TextInputEditText editCode, editDescription, editBrand, editPresentation, editPrice, editCost;
    private ImageView imgProduct;
    private Button btnCamera, btnGallery, btnSave;

    private Uri imageUri;
    private Bitmap bitmap;
    private DatabaseHelper dbHelper;
    private boolean isEditMode = false;
    private Product editingProduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_product);

        layoutCode = findViewById(R.id.layoutCode);
        layoutDescription = findViewById(R.id.layoutDescription);
        layoutBrand = findViewById(R.id.layoutBrand);
        layoutPresentation = findViewById(R.id.layoutPresentation);
        layoutPrice = findViewById(R.id.layoutPrice);
        layoutCost = findViewById(R.id.layoutCost);

        editCode = findViewById(R.id.editCode);
        editDescription = findViewById(R.id.editDescription);
        editBrand = findViewById(R.id.editBrand);
        editPresentation = findViewById(R.id.editPresentation);
        editPrice = findViewById(R.id.editPrice);
        editCost = findViewById(R.id.editCost);

        imgProduct = findViewById(R.id.imgProduct);
        btnCamera = findViewById(R.id.btnCamera);
        btnGallery = findViewById(R.id.btnGallery);
        btnSave = findViewById(R.id.btnSave);

        dbHelper = new DatabaseHelper(this);

        if (getIntent().hasExtra("product")) {
            isEditMode = true;
            editingProduct = (Product) getIntent().getSerializableExtra("product");
            loadProductData(editingProduct);
        }

        btnCamera.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
            } else {
                openCamera();
            }
        });

        btnGallery.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_STORAGE);
            } else {
                openGallery();
            }
        });

        btnSave.setOnClickListener(view -> saveProduct());
    }

    private void loadProductData(Product product) {
        editCode.setText(product.getCodigo());
        editDescription.setText(product.getDescripcion());
        editBrand.setText(product.getMarca());
        editPresentation.setText(product.getPresentacion());
        editPrice.setText(String.valueOf(product.getPrecio()));
        editCost.setText(String.valueOf(product.getCosto()));
        if (product.getImagen() != null) {
            imgProduct.setImageURI(Uri.parse(product.getImagen()));
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CODE_CAMERA);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_CODE_GALLERY);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else if (requestCode == PERMISSION_REQUEST_STORAGE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_CODE_CAMERA) {
                bitmap = (Bitmap) data.getExtras().get("data");
                imgProduct.setImageBitmap(bitmap);
            } else if (requestCode == REQUEST_CODE_GALLERY) {
                imageUri = data.getData();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    imgProduct.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void saveProduct() {
        String code = editCode.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        String brand = editBrand.getText().toString().trim();
        String presentation = editPresentation.getText().toString().trim();
        String priceStr = editPrice.getText().toString().trim();
        String costStr = editCost.getText().toString().trim();

        if (code.isEmpty()) {
            layoutCode.setError("El código es requerido");
            return;
        } else layoutCode.setError(null);

        if (description.isEmpty()) {
            layoutDescription.setError("La descripción es requerida");
            return;
        } else layoutDescription.setError(null);

        if (brand.isEmpty()) {
            layoutBrand.setError("La marca es requerida");
            return;
        } else layoutBrand.setError(null);

        if (presentation.isEmpty()) {
            layoutPresentation.setError("La presentación es requerida");
            return;
        } else layoutPresentation.setError(null);

        if (priceStr.isEmpty()) {
            layoutPrice.setError("El precio es requerido");
            return;
        } else layoutPrice.setError(null);

        if (costStr.isEmpty()) {
            layoutCost.setError("El costo es requerido");
            return;
        } else layoutCost.setError(null);

        double price, cost;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            layoutPrice.setError("Precio inválido");
            return;
        }

        try {
            cost = Double.parseDouble(costStr);
        } catch (NumberFormatException e) {
            layoutCost.setError("Costo inválido");
            return;
        }

        if (cost <= 0 || cost >= price) {
            layoutCost.setError("El costo debe ser menor que el precio");
            return;
        }

        double ganancia = ((price - cost) / cost) * 100;

        String imagePath = "";
        if (imageUri != null) {
            imagePath = imageUri.toString();
        } else if (bitmap != null) {
            imagePath = "ruta_imagen_guardada"; // Implementa lógica real si lo necesitas
        } else {
            Toast.makeText(this, "Debe seleccionar o tomar una imagen", Toast.LENGTH_SHORT).show();
            return;
        }

        Product product = new Product();
        product.setCodigo(code);
        product.setDescripcion(description);
        product.setMarca(brand);
        product.setPresentacion(presentation);
        product.setPrecio(price);
        product.setCosto(cost);
        product.setGanancia(ganancia);
        product.setImagen(imagePath);

        boolean result;
        if (isEditMode) {
            product.setId(editingProduct.getId());
            result = dbHelper.updateProduct(product);
        } else {
            result = dbHelper.insertProduct(product);
        }

        if (result) {
            Toast.makeText(this, "Producto guardado exitosamente", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Error al guardar el producto", Toast.LENGTH_SHORT).show();
        }
    }
}
