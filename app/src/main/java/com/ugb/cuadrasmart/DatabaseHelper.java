package com.ugb.cuadrasmart;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Nombre y versión de la base de datos
    private static final String DATABASE_NAME = "cuadrasmart.db";
    private static final int DATABASE_VERSION = 1;

    // Tabla de Supervisores (para login)
    public static final String TABLE_SUPERVISORES = "supervisores";
    public static final String COLUMN_SUP_ID = "id";
    public static final String COLUMN_SUP_EMAIL = "email";
    public static final String COLUMN_SUP_PASSWORD = "password";

    // Tabla de Cajeros (para administrar nombres)
    public static final String TABLE_CAJEROS = "cajeros";
    public static final String COLUMN_CAJ_ID = "id";
    public static final String COLUMN_CAJ_NOMBRE = "nombre";

    // Tabla de Registros (turnos y ventas)
    public static final String TABLE_REGISTROS = "registros";
    public static final String COLUMN_REG_ID = "id";
    public static final String COLUMN_REG_FECHA = "fecha";
    public static final String COLUMN_REG_HORA_INICIO = "horaInicio";
    public static final String COLUMN_REG_HORA_CIERRE = "horaCierre";
    public static final String COLUMN_REG_NUMERO_CAJA = "numeroCaja";
    public static final String COLUMN_REG_CAJERO = "cajero";
    public static final String COLUMN_REG_BILLETES = "billetes";
    public static final String COLUMN_REG_MONEDAS = "monedas";
    public static final String COLUMN_REG_CHEQUES = "cheques";
    public static final String COLUMN_REG_DISCREPANCIA = "discrepancia";
    public static final String COLUMN_REG_JUSTIFICACION = "justificacion";
    public static final String COLUMN_REG_EVIDENCIA = "evidencia"; // Ruta o nombre de archivo de la imagen

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Crear las tablas
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createSupervisores = "CREATE TABLE " + TABLE_SUPERVISORES + " ("
                + COLUMN_SUP_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_SUP_EMAIL + " TEXT UNIQUE, "
                + COLUMN_SUP_PASSWORD + " TEXT)";
        db.execSQL(createSupervisores);

        String createCajeros = "CREATE TABLE " + TABLE_CAJEROS + " ("
                + COLUMN_CAJ_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_CAJ_NOMBRE + " TEXT)";
        db.execSQL(createCajeros);

        String createRegistros = "CREATE TABLE " + TABLE_REGISTROS + " ("
                + COLUMN_REG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_REG_FECHA + " TEXT, "
                + COLUMN_REG_HORA_INICIO + " TEXT, "
                + COLUMN_REG_HORA_CIERRE + " TEXT, "
                + COLUMN_REG_NUMERO_CAJA + " INTEGER, "
                + COLUMN_REG_CAJERO + " TEXT, "
                + COLUMN_REG_BILLETES + " REAL, "
                + COLUMN_REG_MONEDAS + " REAL, "
                + COLUMN_REG_CHEQUES + " REAL, "
                + COLUMN_REG_DISCREPANCIA + " REAL, "
                + COLUMN_REG_JUSTIFICACION + " TEXT, "
                + COLUMN_REG_EVIDENCIA + " TEXT)";
        db.execSQL(createRegistros);
    }

    // Actualización de la base de datos
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SUPERVISORES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CAJEROS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REGISTROS);
        onCreate(db);
    }

    // Métodos para Supervisores
    public boolean registrarSupervisor(String email, String password) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SUP_EMAIL, email);
        values.put(COLUMN_SUP_PASSWORD, password);
        long result = db.insert(TABLE_SUPERVISORES, null, values);
        return result != -1;
    }

    public boolean autenticarSupervisor(String email, String password) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_SUPERVISORES + " WHERE "
                + COLUMN_SUP_EMAIL + "=? AND " + COLUMN_SUP_PASSWORD + "=?";
        Cursor cursor = db.rawQuery(query, new String[]{email, password});
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        return exists;
    }

    // Métodos para Cajeros
    public boolean agregarCajero(String nombre) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CAJ_NOMBRE, nombre);
        long result = db.insert(TABLE_CAJEROS, null, values);
        return result != -1;
    }

    public Cursor obtenerCajeros() {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_CAJEROS;
        return db.rawQuery(query, null);
    }

    public boolean eliminarCajero(int id) {
        SQLiteDatabase db = getWritableDatabase();
        int result = db.delete(TABLE_CAJEROS, COLUMN_CAJ_ID + "=?", new String[]{String.valueOf(id)});
        return result > 0;
    }

    // Métodos para Registros
    public boolean insertarRegistro(String fecha, String horaInicio, String horaCierre, int numeroCaja,
                                    String cajero, double billetes, double monedas, double cheques,
                                    double discrepancia, String justificacion, String evidencia) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_REG_FECHA, fecha);
        values.put(COLUMN_REG_HORA_INICIO, horaInicio);
        values.put(COLUMN_REG_HORA_CIERRE, horaCierre);
        values.put(COLUMN_REG_NUMERO_CAJA, numeroCaja);
        values.put(COLUMN_REG_CAJERO, cajero);
        values.put(COLUMN_REG_BILLETES, billetes);
        values.put(COLUMN_REG_MONEDAS, monedas);
        values.put(COLUMN_REG_CHEQUES, cheques);
        values.put(COLUMN_REG_DISCREPANCIA, discrepancia);
        values.put(COLUMN_REG_JUSTIFICACION, justificacion);
        values.put(COLUMN_REG_EVIDENCIA, evidencia);
        long result = db.insert(TABLE_REGISTROS, null, values);
        return result != -1;
    }

    public Cursor obtenerRegistros() {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_REGISTROS;
        return db.rawQuery(query, null);
    }

    public boolean actualizarRegistro(int id, String fecha, String horaInicio, String horaCierre, int numeroCaja,
                                      String cajero, double billetes, double monedas, double cheques,
                                      double discrepancia, String justificacion, String evidencia) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_REG_FECHA, fecha);
        values.put(COLUMN_REG_HORA_INICIO, horaInicio);
        values.put(COLUMN_REG_HORA_CIERRE, horaCierre);
        values.put(COLUMN_REG_NUMERO_CAJA, numeroCaja);
        values.put(COLUMN_REG_CAJERO, cajero);
        values.put(COLUMN_REG_BILLETES, billetes);
        values.put(COLUMN_REG_MONEDAS, monedas);
        values.put(COLUMN_REG_CHEQUES, cheques);
        values.put(COLUMN_REG_DISCREPANCIA, discrepancia);
        values.put(COLUMN_REG_JUSTIFICACION, justificacion);
        values.put(COLUMN_REG_EVIDENCIA, evidencia);
        int result = db.update(TABLE_REGISTROS, values, COLUMN_REG_ID + "=?", new String[]{String.valueOf(id)});
        return result > 0;
    }
}