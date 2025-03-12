package com.ugb.tienda;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "tienda.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_PRODUCTS = "products";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_CODE = "code";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_BRAND = "brand";
    public static final String COLUMN_PRESENTATION = "presentation";
    public static final String COLUMN_PRICE = "price";
    public static final String COLUMN_IMAGE_PATH = "imagePath";

    // Sentencia SQL para crear la tabla de productos
    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_PRODUCTS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_CODE + " TEXT, " +
                    COLUMN_DESCRIPTION + " TEXT, " +
                    COLUMN_BRAND + " TEXT, " +
                    COLUMN_PRESENTATION + " TEXT, " +
                    COLUMN_PRICE + " REAL, " +
                    COLUMN_IMAGE_PATH + " TEXT" +
                    ");";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
        Log.d("DBHelper", "Tabla " + TABLE_PRODUCTS + " creada con columnas: " +
                COLUMN_ID + ", " +
                COLUMN_CODE + ", " +
                COLUMN_DESCRIPTION + ", " +
                COLUMN_BRAND + ", " +
                COLUMN_PRESENTATION + ", " +
                COLUMN_PRICE + ", " +
                COLUMN_IMAGE_PATH);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Eliminar la tabla existente y crearla de nuevo
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        onCreate(db);
    }

    // Insertar un nuevo producto
    public long addProduct(Product product) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CODE, product.getCode());
        values.put(COLUMN_DESCRIPTION, product.getDescription());
        values.put(COLUMN_BRAND, product.getBrand());
        values.put(COLUMN_PRESENTATION, product.getPresentation());
        values.put(COLUMN_PRICE, product.getPrice());
        values.put(COLUMN_IMAGE_PATH, product.getImagePath());

        long id = db.insert(TABLE_PRODUCTS, null, values);
        db.close();
        return id;
    }

    // Actualizar un producto existente
    public int updateProduct(Product product) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CODE, product.getCode());
        values.put(COLUMN_DESCRIPTION, product.getDescription());
        values.put(COLUMN_BRAND, product.getBrand());
        values.put(COLUMN_PRESENTATION, product.getPresentation());
        values.put(COLUMN_PRICE, product.getPrice());
        values.put(COLUMN_IMAGE_PATH, product.getImagePath());

        int rowsAffected = db.update(TABLE_PRODUCTS, values, COLUMN_ID + " = ?", new String[]{ String.valueOf(product.getId()) });
        db.close();
        return rowsAffected;
    }

    // Eliminar un producto
    public int deleteProduct(int productId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = db.delete(TABLE_PRODUCTS, COLUMN_ID + " = ?", new String[]{ String.valueOf(productId) });
        db.close();
        return rowsDeleted;
    }

    // Obtener todos los productos y devolverlos como una lista
    public List<Product> getAllProductsList() {
        List<Product> productList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PRODUCTS, null, null, null, null, null, COLUMN_ID + " DESC");
        if (cursor != null) {
            // Registrar las columnas para depuración
            String[] columns = cursor.getColumnNames();
            Log.d("DBHelper", "Columnas en getAllProductsList: " + String.join(", ", columns));

            while (cursor.moveToNext()) {
                Product product = new Product();

                int index = cursor.getColumnIndex(COLUMN_ID);
                if (index >= 0) product.setId(cursor.getInt(index));

                index = cursor.getColumnIndex(COLUMN_CODE);
                if (index >= 0) product.setCode(cursor.getString(index));

                index = cursor.getColumnIndex(COLUMN_DESCRIPTION);
                if (index >= 0) product.setDescription(cursor.getString(index));

                index = cursor.getColumnIndex(COLUMN_BRAND);
                if (index >= 0) product.setBrand(cursor.getString(index));

                index = cursor.getColumnIndex(COLUMN_PRESENTATION);
                if (index >= 0) product.setPresentation(cursor.getString(index));

                index = cursor.getColumnIndex(COLUMN_PRICE);
                if (index >= 0) product.setPrice(cursor.getDouble(index));

                index = cursor.getColumnIndex(COLUMN_IMAGE_PATH);
                if (index >= 0) product.setImagePath(cursor.getString(index));

                productList.add(product);
            }
            cursor.close();
        }
        db.close();
        return productList;
    }

    // Obtener un producto por su ID y devolverlo como objeto Product
    public Product getProductById(int productId) {
        Product product = null;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_PRODUCTS, null, COLUMN_ID + " = ?", new String[]{ String.valueOf(productId) }, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            product = new Product();

            int index = cursor.getColumnIndex(COLUMN_ID);
            if (index >= 0) product.setId(cursor.getInt(index));

            index = cursor.getColumnIndex(COLUMN_CODE);
            if (index >= 0) product.setCode(cursor.getString(index));

            index = cursor.getColumnIndex(COLUMN_DESCRIPTION);
            if (index >= 0) product.setDescription(cursor.getString(index));

            index = cursor.getColumnIndex(COLUMN_BRAND);
            if (index >= 0) product.setBrand(cursor.getString(index));

            index = cursor.getColumnIndex(COLUMN_PRESENTATION);
            if (index >= 0) product.setPresentation(cursor.getString(index));

            index = cursor.getColumnIndex(COLUMN_PRICE);
            if (index >= 0) product.setPrice(cursor.getDouble(index));

            index = cursor.getColumnIndex(COLUMN_IMAGE_PATH);
            if (index >= 0) product.setImagePath(cursor.getString(index));
        }
        if (cursor != null) {
            cursor.close();
        }
        db.close();
        return product;
    }
}
