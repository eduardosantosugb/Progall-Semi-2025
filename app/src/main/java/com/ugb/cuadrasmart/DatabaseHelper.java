package com.ugb.cuadrasmart;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "cuadrasmart.db";
    private static final int DATABASE_VERSION = 3; // Mantén o incrementa si cambias esquema

    // SQL para crear tabla Usuarios
    private static final String SQL_CREATE_USERS =
            "CREATE TABLE IF NOT EXISTS " + DatabaseContract.UserEntry.TABLE_NAME + " (" +
                    DatabaseContract.UserEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DatabaseContract.UserEntry.COLUMN_NAME + " TEXT, " +
                    DatabaseContract.UserEntry.COLUMN_EMAIL + " TEXT UNIQUE NOT NULL, " +
                    DatabaseContract.UserEntry.COLUMN_PASSWORD + " TEXT NOT NULL, " +
                    DatabaseContract.UserEntry.COLUMN_ROLE + " TEXT NOT NULL" +
                    ");";

    // SQL para crear tabla Turnos
    private static final String SQL_CREATE_TURNOS =
            "CREATE TABLE IF NOT EXISTS " + DatabaseContract.TurnoEntry.TABLE_NAME + " (" +
                    DatabaseContract.TurnoEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DatabaseContract.TurnoEntry.COLUMN_FECHA + " TEXT, " +
                    DatabaseContract.TurnoEntry.COLUMN_HORA_INICIO + " TEXT, " +
                    DatabaseContract.TurnoEntry.COLUMN_HORA_CIERRE + " TEXT, " +
                    DatabaseContract.TurnoEntry.COLUMN_INICIO_DESCANSO + " TEXT, " +
                    DatabaseContract.TurnoEntry.COLUMN_FIN_DESCANSO + " TEXT, " +
                    DatabaseContract.TurnoEntry.COLUMN_NUMERO_CAJA + " INTEGER, " +
                    DatabaseContract.TurnoEntry.COLUMN_CAJERO + " TEXT, " +
                    DatabaseContract.TurnoEntry.COLUMN_BILLETES + " REAL, " +
                    DatabaseContract.TurnoEntry.COLUMN_MONEDAS + " REAL, " +
                    DatabaseContract.TurnoEntry.COLUMN_CHEQUES + " REAL, " +
                    DatabaseContract.TurnoEntry.COLUMN_VENTAS_ESPERADAS + " REAL, " +
                    DatabaseContract.TurnoEntry.COLUMN_DISCREPANCIA + " REAL, " +
                    DatabaseContract.TurnoEntry.COLUMN_JUSTIFICACION + " TEXT, " +
                    DatabaseContract.TurnoEntry.COLUMN_EVIDENCIA + " TEXT, " +
                    DatabaseContract.TurnoEntry.COLUMN_TIENDA + " TEXT" +
                    ");";

    // SQL para crear tabla Chat
    private static final String SQL_CREATE_CHAT =
            "CREATE TABLE IF NOT EXISTS " + DatabaseContract.ChatMessageEntry.TABLE_NAME + " (" +
                    DatabaseContract.ChatMessageEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DatabaseContract.ChatMessageEntry.COLUMN_SENDER + " TEXT, " +
                    DatabaseContract.ChatMessageEntry.COLUMN_RECEIVER + " TEXT, " +
                    DatabaseContract.ChatMessageEntry.COLUMN_CONTENT + " TEXT, " +
                    DatabaseContract.ChatMessageEntry.COLUMN_MESSAGE_TYPE + " TEXT, " +
                    DatabaseContract.ChatMessageEntry.COLUMN_TIMESTAMP + " TEXT, " +
                    DatabaseContract.ChatMessageEntry.COLUMN_URI + " TEXT, " +
                    DatabaseContract.ChatMessageEntry.COLUMN_STATUS + " TEXT" +
                    ");";

    // SQL para crear tabla Tiendas
    private static final String SQL_CREATE_TIENDA =
            "CREATE TABLE IF NOT EXISTS " + DatabaseContract.TiendaEntry.TABLE_NAME + " (" +
                    DatabaseContract.TiendaEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DatabaseContract.TiendaEntry.COLUMN_NOMBRE + " TEXT UNIQUE NOT NULL" +
                    ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "onCreate: Creando base de datos (versión " + DATABASE_VERSION + ")...");
        try {
            db.execSQL(SQL_CREATE_USERS);
            db.execSQL(SQL_CREATE_TURNOS);
            db.execSQL(SQL_CREATE_CHAT);
            db.execSQL(SQL_CREATE_TIENDA);
            insertInitialTiendas(db);
            Log.i(TAG, "onCreate: Base de datos creada e inicializada.");
        } catch (Exception e) {
            Log.e(TAG, "onCreate: Error al crear tablas", e);
        }
    }

    private void insertInitialTiendas(SQLiteDatabase db) {
        Log.d(TAG, "insertInitialTiendas: Insertando tiendas iniciales...");
        String[] tiendasIniciales = {"Tienda Norte", "Tienda Sur", "Tienda Centro", "Tienda Este", "Tienda Oeste"};
        ContentValues values = new ContentValues();
        db.beginTransaction();
        try {
            for (String nombreTienda : tiendasIniciales) {
                values.clear();
                values.put(DatabaseContract.TiendaEntry.COLUMN_NOMBRE, nombreTienda);
                db.insertWithOnConflict(DatabaseContract.TiendaEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "insertInitialTiendas: Error insertando", e);
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "onUpgrade: Actualizando DB de v" + oldVersion + " a v" + newVersion + ". Datos antiguos eliminados.");
        db.execSQL("DROP TABLE IF EXISTS " + DatabaseContract.UserEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DatabaseContract.TurnoEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DatabaseContract.ChatMessageEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DatabaseContract.TiendaEntry.TABLE_NAME);
        onCreate(db);
    }

    // --- MÉTODOS DE USUARIO ---
    public boolean authenticateUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        boolean exists = false;
        try {
            cursor = db.query(DatabaseContract.UserEntry.TABLE_NAME,
                    new String[]{DatabaseContract.UserEntry._ID},
                    DatabaseContract.UserEntry.COLUMN_EMAIL + "=? AND " + DatabaseContract.UserEntry.COLUMN_PASSWORD + "=?",
                    new String[]{email.toLowerCase(), password}, null, null, null, "1");
            exists = (cursor != null && cursor.getCount() > 0);
        } catch (Exception e) {
            Log.e(TAG, "authenticateUser: Error", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return exists;
    }

    public String getPasswordByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String password = null;
        try {
            cursor = db.query(DatabaseContract.UserEntry.TABLE_NAME,
                    new String[]{DatabaseContract.UserEntry.COLUMN_PASSWORD},
                    DatabaseContract.UserEntry.COLUMN_EMAIL + "=?",
                    new String[]{email.toLowerCase()}, null, null, null, "1");
            if (cursor != null && cursor.moveToFirst()) {
                int colIndex = cursor.getColumnIndex(DatabaseContract.UserEntry.COLUMN_PASSWORD);
                if(colIndex != -1) password = cursor.getString(colIndex);
            }
        } catch (Exception e) {
            Log.e(TAG, "getPasswordByEmail: Error", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return password;
    }

    public boolean insertSupervisor(String name, String email, String password) {
        return insertUser(name, email, password, "supervisor");
    }

    public boolean insertCajero(String name, String email, String password) {
        return insertUser(name, email, password, "cajero");
    }

    private boolean insertUser(String name, String email, String password, String role) {
        if (name == null || email == null || password == null || role == null ||
                name.trim().isEmpty() || email.trim().isEmpty() || password.isEmpty() || role.trim().isEmpty()) {
            Log.w(TAG, "insertUser: Datos nulos o vacíos.");
            return false;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.UserEntry.COLUMN_NAME, name.trim());
        String emailLowerCase = email.trim().toLowerCase();
        values.put(DatabaseContract.UserEntry.COLUMN_EMAIL, emailLowerCase);
        values.put(DatabaseContract.UserEntry.COLUMN_PASSWORD, password);
        values.put(DatabaseContract.UserEntry.COLUMN_ROLE, role.trim());
        long result = -1;
        try {
            result = db.insertWithOnConflict(DatabaseContract.UserEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            if (result == -1) Log.w(TAG, "insertUser: FALLO. Email '" + emailLowerCase + "' podría ya existir.");
            else Log.i(TAG, "insertUser: ÉXITO. Email '" + emailLowerCase + "', Rol '" + role + "', ID: " + result);
        } catch (Exception e) {
            Log.e(TAG, "insertUser: EXCEPCIÓN para '" + emailLowerCase + "'", e);
            return false;
        }
        return result != -1;
    }

    public boolean checkIfUserExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        boolean exists = false;
        if (email == null || email.trim().isEmpty()) return false;
        try {
            String emailLowerCase = email.trim().toLowerCase();
            cursor = db.query(DatabaseContract.UserEntry.TABLE_NAME,
                    new String[]{DatabaseContract.UserEntry._ID},
                    DatabaseContract.UserEntry.COLUMN_EMAIL + "=?",
                    new String[]{emailLowerCase}, null, null, null, "1");
            exists = (cursor != null && cursor.getCount() > 0);
        } catch (Exception e) {
            Log.e(TAG, "checkIfUserExists: Error", e);
        } finally {
            if(cursor != null) cursor.close();
        }
        return exists;
    }

    public boolean deleteCajero(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = 0;
        try {
            rowsDeleted = db.delete(DatabaseContract.UserEntry.TABLE_NAME,
                    DatabaseContract.UserEntry._ID + "=? AND " + DatabaseContract.UserEntry.COLUMN_ROLE + "=?",
                    new String[]{String.valueOf(id), "cajero"});
            if (rowsDeleted > 0) Log.i(TAG, "deleteCajero: ID " + id + " eliminado.");
            else Log.w(TAG, "deleteCajero: ID " + id + " no encontrado o no eliminado.");
        } catch (Exception e) {
            Log.e(TAG, "deleteCajero: Error para ID " + id, e);
        }
        return rowsDeleted > 0;
    }

    // --- MÉTODOS DE TURNOS ---
    public boolean insertTurno(String fecha, String horaInicio, String horaCierre, String inicioDescanso,
                               String finDescanso, int numeroCaja, String cajero, double billetes,
                               double monedas, double cheques, double ventasEsperadas, double discrepancy,
                               String justificacion, String evidencia, String tienda) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.TurnoEntry.COLUMN_FECHA, fecha);
        values.put(DatabaseContract.TurnoEntry.COLUMN_HORA_INICIO, horaInicio);
        values.put(DatabaseContract.TurnoEntry.COLUMN_HORA_CIERRE, horaCierre);
        values.put(DatabaseContract.TurnoEntry.COLUMN_INICIO_DESCANSO, inicioDescanso);
        values.put(DatabaseContract.TurnoEntry.COLUMN_FIN_DESCANSO, finDescanso);
        values.put(DatabaseContract.TurnoEntry.COLUMN_NUMERO_CAJA, numeroCaja);
        values.put(DatabaseContract.TurnoEntry.COLUMN_CAJERO, cajero);
        values.put(DatabaseContract.TurnoEntry.COLUMN_BILLETES, billetes);
        values.put(DatabaseContract.TurnoEntry.COLUMN_MONEDAS, monedas);
        values.put(DatabaseContract.TurnoEntry.COLUMN_CHEQUES, cheques);
        values.put(DatabaseContract.TurnoEntry.COLUMN_VENTAS_ESPERADAS, ventasEsperadas);
        values.put(DatabaseContract.TurnoEntry.COLUMN_DISCREPANCIA, discrepancy);
        values.put(DatabaseContract.TurnoEntry.COLUMN_JUSTIFICACION, justificacion);
        values.put(DatabaseContract.TurnoEntry.COLUMN_EVIDENCIA, evidencia);
        values.put(DatabaseContract.TurnoEntry.COLUMN_TIENDA, tienda);
        long result = -1;
        try { result = db.insert(DatabaseContract.TurnoEntry.TABLE_NAME, null, values); }
        catch (Exception e) { Log.e(TAG, "insertTurno: Error", e); }
        return result != -1;
    }

    public boolean updateTurno(int id, String fecha, String horaInicio, String horaCierre, String inicioDescanso,
                               String finDescanso, int numeroCaja, String cajero, double billetes,
                               double monedas, double cheques, double ventasEsperadas, double discrepancy,
                               String justificacion, String evidencia, String tienda) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.TurnoEntry.COLUMN_FECHA, fecha);
        values.put(DatabaseContract.TurnoEntry.COLUMN_HORA_INICIO, horaInicio);
        values.put(DatabaseContract.TurnoEntry.COLUMN_HORA_CIERRE, horaCierre);
        values.put(DatabaseContract.TurnoEntry.COLUMN_INICIO_DESCANSO, inicioDescanso);
        values.put(DatabaseContract.TurnoEntry.COLUMN_FIN_DESCANSO, finDescanso);
        values.put(DatabaseContract.TurnoEntry.COLUMN_NUMERO_CAJA, numeroCaja);
        values.put(DatabaseContract.TurnoEntry.COLUMN_CAJERO, cajero);
        values.put(DatabaseContract.TurnoEntry.COLUMN_BILLETES, billetes);
        values.put(DatabaseContract.TurnoEntry.COLUMN_MONEDAS, monedas);
        values.put(DatabaseContract.TurnoEntry.COLUMN_CHEQUES, cheques);
        values.put(DatabaseContract.TurnoEntry.COLUMN_VENTAS_ESPERADAS, ventasEsperadas);
        values.put(DatabaseContract.TurnoEntry.COLUMN_DISCREPANCIA, discrepancy);
        values.put(DatabaseContract.TurnoEntry.COLUMN_JUSTIFICACION, justificacion);
        values.put(DatabaseContract.TurnoEntry.COLUMN_EVIDENCIA, evidencia);
        values.put(DatabaseContract.TurnoEntry.COLUMN_TIENDA, tienda);
        int rowsAffected = 0;
        try {
            rowsAffected = db.update(DatabaseContract.TurnoEntry.TABLE_NAME, values,
                    DatabaseContract.TurnoEntry._ID + "=?", new String[]{String.valueOf(id)});
        } catch (Exception e) { Log.e(TAG, "updateTurno: Error para ID " + id, e); }
        return rowsAffected > 0;
    }

    public Cursor getTurnoById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(DatabaseContract.TurnoEntry.TABLE_NAME, null,
                    DatabaseContract.TurnoEntry._ID + "=?", new String[]{String.valueOf(id)},
                    null, null, null);
        } catch (Exception e) { Log.e(TAG, "getTurnoById: Error para ID " + id, e); }
        return cursor;
    }

    public Cursor getAllTurnos() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(DatabaseContract.TurnoEntry.TABLE_NAME, null, null, null, null, null,
                    DatabaseContract.TurnoEntry.COLUMN_FECHA + " DESC, " + DatabaseContract.TurnoEntry.COLUMN_HORA_INICIO + " DESC");
        } catch (Exception e) { Log.e(TAG, "getAllTurnos: Error", e); }
        return cursor;
    }

    // NUEVO MÉTODO PARA ELIMINAR TURNO
    public boolean deleteTurnoById(int turnoId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = 0;
        try {
            Log.d(TAG, "deleteTurnoById: Intentando eliminar turno con ID: " + turnoId);
            rowsDeleted = db.delete(
                    DatabaseContract.TurnoEntry.TABLE_NAME,
                    DatabaseContract.TurnoEntry._ID + "=?",
                    new String[]{String.valueOf(turnoId)}
            );
            if (rowsDeleted > 0) {
                Log.i(TAG, "deleteTurnoById: Turno con ID " + turnoId + " eliminado. Filas: " + rowsDeleted);
            } else {
                Log.w(TAG, "deleteTurnoById: No se eliminó turno con ID " + turnoId + ". Filas: " + rowsDeleted);
            }
        } catch (Exception e) {
            Log.e(TAG, "deleteTurnoById: Error al eliminar turno ID " + turnoId, e);
        }
        return rowsDeleted > 0;
    }

    // --- MÉTODOS DE CHAT ---
    public boolean insertChatMessage(String sender, String receiver, String content, String messageType,
                                     String timestamp, String uri, String status) {
        // ... (sin cambios)
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.ChatMessageEntry.COLUMN_SENDER, sender);
        values.put(DatabaseContract.ChatMessageEntry.COLUMN_RECEIVER, receiver);
        values.put(DatabaseContract.ChatMessageEntry.COLUMN_CONTENT, content);
        values.put(DatabaseContract.ChatMessageEntry.COLUMN_MESSAGE_TYPE, messageType);
        values.put(DatabaseContract.ChatMessageEntry.COLUMN_TIMESTAMP, timestamp);
        values.put(DatabaseContract.ChatMessageEntry.COLUMN_URI, uri);
        values.put(DatabaseContract.ChatMessageEntry.COLUMN_STATUS, status);
        long result = -1;
        try { result = db.insert(DatabaseContract.ChatMessageEntry.TABLE_NAME, null, values); }
        catch (Exception e) { Log.e(TAG, "insertChatMessage: Error", e); }
        return result != -1;
    }

    public Cursor getChatMessages(String user1, String user2) {
        // ... (sin cambios)
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            String selection = "(" + DatabaseContract.ChatMessageEntry.COLUMN_SENDER + "=? AND " +
                    DatabaseContract.ChatMessageEntry.COLUMN_RECEIVER + "=?) OR (" +
                    DatabaseContract.ChatMessageEntry.COLUMN_SENDER + "=? AND " +
                    DatabaseContract.ChatMessageEntry.COLUMN_RECEIVER + "=?)";
            String[] selectionArgs = {user1, user2, user2, user1};
            cursor = db.query(DatabaseContract.ChatMessageEntry.TABLE_NAME, null, selection, selectionArgs,
                    null, null, DatabaseContract.ChatMessageEntry.COLUMN_TIMESTAMP + " ASC");
        } catch (Exception e) { Log.e(TAG, "getChatMessages: Error", e); }
        return cursor;
    }

    // --- MÉTODOS DE TIENDAS ---
    public Cursor getAllTiendas() {
        // ... (sin cambios)
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(DatabaseContract.TiendaEntry.TABLE_NAME, null, null, null, null, null,
                    DatabaseContract.TiendaEntry.COLUMN_NOMBRE + " ASC");
        } catch (Exception e) { Log.e(TAG, "getAllTiendas: Error", e); }
        return cursor;
    }

    private boolean checkIfTiendaExists(String nombre) {
        // ... (sin cambios)
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        boolean exists = false;
        try {
            cursor = db.query(DatabaseContract.TiendaEntry.TABLE_NAME, new String[]{"1"},
                    DatabaseContract.TiendaEntry.COLUMN_NOMBRE + "=?", new String[]{nombre},
                    null, null, null, "1");
            exists = (cursor != null && cursor.getCount() > 0);
        } catch (Exception e) { Log.e(TAG, "checkIfTiendaExists: Error para '" + nombre + "'", e);
        } finally { if (cursor != null) cursor.close(); }
        return exists;
    }

    public boolean insertTienda(String nombre) {
        // ... (sin cambios)
        if (nombre == null || nombre.trim().isEmpty()) return false;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        String nombreTrimmed = nombre.trim();
        values.put(DatabaseContract.TiendaEntry.COLUMN_NOMBRE, nombreTrimmed);
        long result = -1;
        try {
            result = db.insertWithOnConflict(DatabaseContract.TiendaEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            if (result == -1 && checkIfTiendaExists(nombreTrimmed)) Log.w(TAG, "insertTienda: Tienda '" + nombreTrimmed + "' ya existe.");
            else if (result != -1) Log.i(TAG, "insertTienda: Tienda '" + nombreTrimmed + "' insertada ID: " + result);
        } catch (Exception e) { Log.e(TAG, "insertTienda: Excepción para '" + nombreTrimmed + "'", e); return false; }
        return result != -1;
    }

    public boolean updateTienda(int id, String nuevoNombre) {
        // ... (sin cambios)
        if (nuevoNombre == null || nuevoNombre.trim().isEmpty()) return false;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        String nombreTrimmed = nuevoNombre.trim();
        values.put(DatabaseContract.TiendaEntry.COLUMN_NOMBRE, nombreTrimmed);
        int rowsAffected = 0;
        try {
            rowsAffected = db.update(DatabaseContract.TiendaEntry.TABLE_NAME, values,
                    DatabaseContract.TiendaEntry._ID + "=?", new String[]{String.valueOf(id)});
        } catch (SQLiteConstraintException e) { Log.w(TAG, "updateTienda: Constraint error para ID " + id, e); return false;
        } catch (Exception e) { Log.e(TAG, "updateTienda: Error general para ID " + id, e); return false; }
        return rowsAffected > 0;
    }

    public boolean deleteTienda(int id) {
        // ... (sin cambios)
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = 0;
        try {
            rowsAffected = db.delete(DatabaseContract.TiendaEntry.TABLE_NAME,
                    DatabaseContract.TiendaEntry._ID + "=?", new String[]{String.valueOf(id)});
        } catch (Exception e) { Log.e(TAG, "deleteTienda: Error para ID " + id, e); }
        return rowsAffected > 0;
    }
}