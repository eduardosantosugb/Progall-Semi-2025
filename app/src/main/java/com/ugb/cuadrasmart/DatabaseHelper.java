package com.ugb.cuadrasmart;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "cuadrasmart.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TAG = "DatabaseHelper";

    // Tabla Usuarios
    public static final String TABLE_USUARIOS = "usuarios";
    public static final String COLUMN_USR_ID = "_id";
    public static final String COLUMN_USR_EMAIL = "email";
    public static final String COLUMN_USR_PASSWORD = "password";
    public static final String COLUMN_USR_ROL = "rol"; // "cajero" o "supervisor"

    // Tabla Registros
    public static final String TABLE_REGISTROS = "registros";
    public static final String COLUMN_REG_ID = "_id";
    public static final String COLUMN_REG_FECHA = "fecha";
    public static final String COLUMN_REG_HORA_INICIO = "hora_inicio";
    public static final String COLUMN_REG_HORA_CIERRE = "hora_cierre";
    public static final String COLUMN_REG_NUMERO_CAJA = "numero_caja";
    public static final String COLUMN_REG_CAJERO = "cajero"; // Se puede almacenar el email del cajero
    public static final String COLUMN_REG_BILLETES = "billetes";
    public static final String COLUMN_REG_MONEDAS = "monedas";
    public static final String COLUMN_REG_CHEQUES = "cheques";
    public static final String COLUMN_REG_VENTAS_ESPERADAS = "ventas_esperadas";
    public static final String COLUMN_REG_DISCREPANCIA = "discrepancia";
    public static final String COLUMN_REG_JUSTIFICACION = "justificacion";
    public static final String COLUMN_REG_EVIDENCE = "evidencia"; // URI de la imagen, en forma de String
    public static final String COLUMN_REG_TIENDA = "tienda"; // Nombre de la tienda en la que se realizó el turno

    // Sentencias SQL para crear las tablas
    private static final String CREATE_TABLE_USUARIOS =
            "CREATE TABLE " + TABLE_USUARIOS + " ("
                    + COLUMN_USR_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_USR_EMAIL + " TEXT UNIQUE NOT NULL, "
                    + COLUMN_USR_PASSWORD + " TEXT NOT NULL, "
                    + COLUMN_USR_ROL + " TEXT NOT NULL"
                    + ");";

    private static final String CREATE_TABLE_REGISTROS =
            "CREATE TABLE " + TABLE_REGISTROS + " ("
                    + COLUMN_REG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_REG_FECHA + " TEXT NOT NULL, "
                    + COLUMN_REG_HORA_INICIO + " TEXT NOT NULL, "
                    + COLUMN_REG_HORA_CIERRE + " TEXT NOT NULL, "
                    + COLUMN_REG_NUMERO_CAJA + " INTEGER NOT NULL, "
                    + COLUMN_REG_CAJERO + " TEXT NOT NULL, "
                    + COLUMN_REG_BILLETES + " REAL NOT NULL, "
                    + COLUMN_REG_MONEDAS + " REAL NOT NULL, "
                    + COLUMN_REG_CHEQUES + " REAL NOT NULL, "
                    + COLUMN_REG_VENTAS_ESPERADAS + " REAL NOT NULL, "
                    + COLUMN_REG_DISCREPANCIA + " REAL NOT NULL, "
                    + COLUMN_REG_JUSTIFICACION + " TEXT, "
                    + COLUMN_REG_EVIDENCE + " TEXT, "
                    + COLUMN_REG_TIENDA + " TEXT NOT NULL"
                    + ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Crear las tablas
        db.execSQL(CREATE_TABLE_USUARIOS);
        db.execSQL(CREATE_TABLE_REGISTROS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Para actualizar la base de datos, se pueden eliminar las tablas y recrearlas (en una versión real, migrar datos)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USUARIOS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REGISTROS);
        onCreate(db);
    }

    // Método para autenticar al usuario. Retorna el rol ("cajero" o "supervisor") o null si las credenciales no coinciden.
    public String authenticateUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String role = null;
        Cursor cursor = db.query(TABLE_USUARIOS,
                new String[]{COLUMN_USR_ROL},
                COLUMN_USR_EMAIL + " = ? AND " + COLUMN_USR_PASSWORD + " = ?",
                new String[]{email, password},
                null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            role = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USR_ROL));
            cursor.close();
        }
        return role;
    }

    // Método para insertar un registro de turno
    public boolean insertarRegistro(String fecha, String horaInicio, String horaCierre, int numeroCaja, String cajero,
                                    double billetes, double monedas, double cheques, double ventasEsperadas,
                                    double discrepancia, String justificacion, String evidencia, String tienda) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_REG_FECHA, fecha);
        values.put(COLUMN_REG_HORA_INICIO, horaInicio);
        values.put(COLUMN_REG_HORA_CIERRE, horaCierre);
        values.put(COLUMN_REG_NUMERO_CAJA, numeroCaja);
        values.put(COLUMN_REG_CAJERO, cajero);
        values.put(COLUMN_REG_BILLETES, billetes);
        values.put(COLUMN_REG_MONEDAS, monedas);
        values.put(COLUMN_REG_CHEQUES, cheques);
        values.put(COLUMN_REG_VENTAS_ESPERADAS, ventasEsperadas);
        values.put(COLUMN_REG_DISCREPANCIA, discrepancia);
        values.put(COLUMN_REG_JUSTIFICACION, justificacion);
        values.put(COLUMN_REG_EVIDENCE, evidencia);
        values.put(COLUMN_REG_TIENDA, tienda);
        long result = db.insert(TABLE_REGISTROS, null, values);
        return result != -1;
    }

    // Método para actualizar un registro de turno
    public boolean actualizarRegistro(int id, String fecha, String horaInicio, String horaCierre, int numeroCaja, String cajero,
                                      double billetes, double monedas, double cheques, double ventasEsperadas,
                                      double discrepancia, String justificacion, String evidencia, String tienda) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_REG_FECHA, fecha);
        values.put(COLUMN_REG_HORA_INICIO, horaInicio);
        values.put(COLUMN_REG_HORA_CIERRE, horaCierre);
        values.put(COLUMN_REG_NUMERO_CAJA, numeroCaja);
        values.put(COLUMN_REG_CAJERO, cajero);
        values.put(COLUMN_REG_BILLETES, billetes);
        values.put(COLUMN_REG_MONEDAS, monedas);
        values.put(COLUMN_REG_CHEQUES, cheques);
        values.put(COLUMN_REG_VENTAS_ESPERADAS, ventasEsperadas);
        values.put(COLUMN_REG_DISCREPANCIA, discrepancia);
        values.put(COLUMN_REG_JUSTIFICACION, justificacion);
        values.put(COLUMN_REG_EVIDENCE, evidencia);
        values.put(COLUMN_REG_TIENDA, tienda);
        int rows = db.update(TABLE_REGISTROS, values, COLUMN_REG_ID + " = ?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

    // Método para obtener registros (puedes implementar filtros adicionales según necesidad)
    public Cursor obtenerRegistros() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_REGISTROS, null, null, null, null, null, COLUMN_REG_FECHA + " DESC");
    }
}
