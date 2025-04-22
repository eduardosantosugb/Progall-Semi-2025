package com.ugb.tiendacouchdb;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "tienda.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_PRODUCTS = "products";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_CODE = "code";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_BRAND = "brand";
    public static final String COLUMN_PRESENTATION = "presentation";
    public static final String COLUMN_PRICE = "price";
    public static final String COLUMN_COSTO = "costo";
    public static final String COLUMN_GANANCIA = "ganancia";
    public static final String COLUMN_IMAGE = "image";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_PRODUCTS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_CODE + " TEXT NOT NULL, " +
                    COLUMN_DESCRIPTION + " TEXT NOT NULL, " +
                    COLUMN_BRAND + " TEXT NOT NULL, " +
                    COLUMN_PRESENTATION + " TEXT NOT NULL, " +
                    COLUMN_PRICE + " REAL NOT NULL, " +
                    COLUMN_COSTO + " REAL NOT NULL, " +
                    COLUMN_GANANCIA + " REAL NOT NULL, " +
                    COLUMN_IMAGE + " TEXT NOT NULL);";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        onCreate(db);
    }

    public boolean insertProduct(Product product) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CODE, product.getCodigo());
        values.put(COLUMN_DESCRIPTION, product.getDescripcion());
        values.put(COLUMN_BRAND, product.getMarca());
        values.put(COLUMN_PRESENTATION, product.getPresentacion());
        values.put(COLUMN_PRICE, product.getPrecio());
        values.put(COLUMN_COSTO, product.getCosto());
        values.put(COLUMN_GANANCIA, product.getGanancia());
        values.put(COLUMN_IMAGE, product.getImagen());

        long result = db.insert(TABLE_PRODUCTS, null, values);
        db.close();
        return result != -1;
    }

    public boolean updateProduct(Product product) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CODE, product.getCodigo());
        values.put(COLUMN_DESCRIPTION, product.getDescripcion());
        values.put(COLUMN_BRAND, product.getMarca());
        values.put(COLUMN_PRESENTATION, product.getPresentacion());
        values.put(COLUMN_PRICE, product.getPrecio());
        values.put(COLUMN_COSTO, product.getCosto());
        values.put(COLUMN_GANANCIA, product.getGanancia());
        values.put(COLUMN_IMAGE, product.getImagen());

        int rows = db.update(TABLE_PRODUCTS, values, COLUMN_ID + " = ?",
                new String[] { String.valueOf(product.getId()) });
        db.close();
        return rows > 0;
    }

    public boolean deleteProduct(int productId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_PRODUCTS, COLUMN_ID + " = ?",
                new String[] { String.valueOf(productId) });
        db.close();
        return rows > 0;
    }

    public List<Product> getAllProducts() {
        List<Product> productList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_PRODUCTS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Product product = new Product();
                product.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                product.setCodigo(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CODE)));
                product.setDescripcion(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)));
                product.setMarca(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BRAND)));
                product.setPresentacion(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRESENTATION)));
                product.setPrecio(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PRICE)));
                product.setCosto(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_COSTO)));
                product.setGanancia(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_GANANCIA)));
                product.setImagen(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE)));

                productList.add(product);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return productList;
    }
}
