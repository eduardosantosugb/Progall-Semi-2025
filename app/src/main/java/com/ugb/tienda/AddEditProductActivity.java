package com.ugb.tienda;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AddEditProductActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_GALLERY_IMAGE = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;

    // Constantes para identificar la acción pendiente
    private static final int ACTION_CAPTURE = 1;
    private static final int ACTION_GALLERY = 2;
    private int pendingAction = 0;

    private EditText etCode, etDescription, etBrand, etPresentation, etPrice;
    private ImageView imageViewProduct;
    private Button btnSave, btnCancel;

    private DBHelper dbHelper;
    private Product currentProduct = null;
    private String imagePath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_product);

        etCode = findViewById(R.id.etCode);
        etDescription = findViewById(R.id.etDescription);
        etBrand = findViewById(R.id.etBrand);
        etPresentation = findViewById(R.id.etPresentation);
        etPrice = findViewById(R.id.etPrice);
        imageViewProduct = findViewById(R.id.imageViewProduct);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        dbHelper = new DBHelper(this);

        // Si se pasó un ID de producto, cargarlo para editar
        Intent intent = getIntent();
        if (intent.hasExtra("productId")) {
            int productId = intent.getIntExtra("productId", -1);
            if (productId != -1) {
                loadProduct(productId);
            }
        }

        imageViewProduct.setOnClickListener(view -> showImagePickerDialog());

        btnSave.setOnClickListener(view -> saveProduct());

        btnCancel.setOnClickListener(view -> finish());
    }

    private void loadProduct(int productId) {
        currentProduct = dbHelper.getProductById(productId);
        if (currentProduct != null) {
            etCode.setText(currentProduct.getCode());
            etDescription.setText(currentProduct.getDescription());
            etBrand.setText(currentProduct.getBrand());
            etPresentation.setText(currentProduct.getPresentation());
            etPrice.setText(String.valueOf(currentProduct.getPrice()));
            imagePath = currentProduct.getImagePath();
            ImageUtils.loadImage(this, imagePath, imageViewProduct);
        }
    }

    private void showImagePickerDialog() {
        String[] options = { "Tomar Foto", "Elegir de la Galería" };
        new AlertDialog.Builder(this)
                .setTitle("Seleccionar Imagen")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) { // Tomar foto
                        if (checkAndRequestPermissionsForCapture()) {
                            dispatchTakePictureIntent();
                        } else {
                            pendingAction = ACTION_CAPTURE;
                        }
                    } else { // Elegir de la galería
                        if (checkAndRequestPermissionsForGallery()) {
                            openGallery();
                        } else {
                            pendingAction = ACTION_GALLERY;
                        }
                    }
                })
                .show();
    }

    // Para capturar foto solo se necesita el permiso de CAMERA
    private boolean checkAndRequestPermissionsForCapture() {
        boolean cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        if (!cameraPermission) {
            ActivityCompat.requestPermissions(this,
                    new String[]{ Manifest.permission.CAMERA },
                    PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    // Para la galería se requiere el permiso de READ_EXTERNAL_STORAGE
    private boolean checkAndRequestPermissionsForGallery() {
        boolean readStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
        if (!readStoragePermission) {
            ActivityCompat.requestPermissions(this,
                    new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE },
                    PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    // Se omite WRITE_EXTERNAL_STORAGE porque usamos getExternalFilesDir(), el cual es privado para la app

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            if (granted) {
                if (pendingAction == ACTION_CAPTURE) {
                    dispatchTakePictureIntent();
                } else if (pendingAction == ACTION_GALLERY) {
                    openGallery();
                }
                pendingAction = 0;
            } else {
                Toast.makeText(this, "Se requieren permisos para continuar", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch(IOException ex) {
                Toast.makeText(this, "Error al crear archivo de imagen", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        getPackageName() + ".provider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        imagePath = image.getAbsolutePath();
        return image;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                ImageUtils.loadImage(this, imagePath, imageViewProduct);
            } else if (requestCode == REQUEST_GALLERY_IMAGE && data != null) {
                Uri selectedImageUri = data.getData();
                imagePath = getRealPathFromURI(selectedImageUri);
                ImageUtils.loadImage(this, imagePath, imageViewProduct);
            }
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if(cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            cursor.close();
            return path;
        }
        return "";
    }

    // Método para guardar o actualizar el producto en la base de datos
    private void saveProduct() {
        String code = etCode.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String brand = etBrand.getText().toString().trim();
        String presentation = etPresentation.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();

        if (code.isEmpty() || description.isEmpty() || brand.isEmpty() ||
                presentation.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch(NumberFormatException e) {
            Toast.makeText(this, "Precio inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentProduct == null) {
            Product newProduct = new Product();
            newProduct.setCode(code);
            newProduct.setDescription(description);
            newProduct.setBrand(brand);
            newProduct.setPresentation(presentation);
            newProduct.setPrice(price);
            newProduct.setImagePath(imagePath);

            long id = dbHelper.addProduct(newProduct);
            if (id > 0) {
                Toast.makeText(this, "Producto agregado", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error al agregar producto", Toast.LENGTH_SHORT).show();
            }
        } else {
            currentProduct.setCode(code);
            currentProduct.setDescription(description);
            currentProduct.setBrand(brand);
            currentProduct.setPresentation(presentation);
            currentProduct.setPrice(price);
            currentProduct.setImagePath(imagePath);

            int rowsAffected = dbHelper.updateProduct(currentProduct);
            if (rowsAffected > 0) {
                Toast.makeText(this, "Producto actualizado", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Error al actualizar producto", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
