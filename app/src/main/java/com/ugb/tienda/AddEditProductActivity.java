package com.ugb.tienda;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AddEditProductActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_GALLERY_IMAGE = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;

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

        // Inicialización de vistas
        etCode = findViewById(R.id.etCode);
        etDescription = findViewById(R.id.etDescription);
        etBrand = findViewById(R.id.etBrand);
        etPresentation = findViewById(R.id.etPresentation);
        etPrice = findViewById(R.id.etPrice);
        imageViewProduct = findViewById(R.id.imageViewProduct);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        dbHelper = new DBHelper(this);

        // Verificar si se pasó un "productId" para editar
        Intent intent = getIntent();
        if (intent.hasExtra("productId")) {
            int productId = intent.getIntExtra("productId", -1);
            if (productId != -1) {
                loadProduct(productId);
            }
        }

        // Listener para seleccionar imagen
        imageViewProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showImagePickerDialog();
            }
        });

        // Listener para guardar el producto
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveProduct();
            }
        });

        // Listener para cancelar
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    /**
     * Carga los datos del producto a editar.
     */
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

    /**
     * Muestra un diálogo que permite elegir entre tomar foto o seleccionar de la galería.
     */
    private void showImagePickerDialog() {
        String[] options = { getString(R.string.take_photo), getString(R.string.choose_gallery) };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.select_image))
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) { // Tomar foto
                            if (checkAndRequestPermissions()) {
                                dispatchTakePictureIntent();
                            }
                        } else { // Seleccionar de galería
                            if (checkAndRequestPermissions()) {
                                openGallery();
                            }
                        }
                    }
                });
        builder.create().show();
    }

    /**
     * Verifica y solicita los permisos de cámara y lectura de almacenamiento.
     */
    private boolean checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    /**
     * Despacha el intent para capturar una imagen con la cámara.
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error al crear archivo de imagen", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    /**
     * Crea un archivo temporal para guardar la imagen capturada.
     */
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        imagePath = image.getAbsolutePath();
        return image;
    }

    /**
     * Abre la galería para seleccionar una imagen.
     */
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY_IMAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                boolean cameraGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean storageGranted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                if (!cameraGranted || !storageGranted) {
                    Toast.makeText(this, "Se requieren permisos para continuar", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // La imagen se guardó en imagePath
                ImageUtils.loadImage(this, imagePath, imageViewProduct);
            } else if (requestCode == REQUEST_GALLERY_IMAGE) {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    imagePath = getRealPathFromURI(selectedImageUri);
                    ImageUtils.loadImage(this, imagePath, imageViewProduct);
                }
            }
        }
    }

    /**
     * Obtiene la ruta real del archivo a partir del URI seleccionado en la galería.
     */
    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            cursor.close();
            return path;
        }
        return "";
    }

    /**
     * Valida los campos, crea o actualiza el producto y lo guarda en la base de datos.
     */
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
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Precio inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentProduct == null) {
            // Crear nuevo producto
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
            // Actualizar producto existente
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
