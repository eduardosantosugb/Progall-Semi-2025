package com.ugb.cuadrasmart;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper"; // Definición del TAG para logging
    private static final String DATABASE_NAME = "cuadrasmart.db";
    private static final int DATABASE_VERSION = 3; // Mantén o incrementa si cambias esquema

    // --- Sentencias SQL para crear las tablas (sin cambios respecto a tu versión) ---
    private static final String SQL_CREATE_USERS =
            "CREATE TABLE IF NOT EXISTS " + DatabaseContract.UserEntry.TABLE_NAME + " (" +
                    DatabaseContract.UserEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DatabaseContract.UserEntry.COLUMN_NAME + " TEXT, " +
                    DatabaseContract.UserEntry.COLUMN_EMAIL + " TEXT UNIQUE NOT NULL, " +
                    DatabaseContract.UserEntry.COLUMN_PASSWORD + " TEXT NOT NULL, " +
                    DatabaseContract.UserEntry.COLUMN_ROLE + " TEXT NOT NULL" +
                    ");";
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
    private static final String SQL_CREATE_CHAT =
            "CREATE TABLE IF NOT EXISTS " + DatabaseContract.ChatMessageEntry.TABLE_NAME + " (" +
                    DatabaseContract.ChatMessageEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DatabaseContract.ChatMessageEntry.COLUMN_SENDER + " TEXT, " +
                    DatabaseContract.ChatMessageEntry.COLUMN_RECEIVER + " TEXT, " +
                    DatabaseContract.ChatMessageEntry.COLUMN_CONTENT + " TEXT, " +
                    DatabaseContract.ChatMessageEntry.COLUMN_MESSAGE_TYPE + " TEXT, " +
                    DatabaseContract.ChatMessageEntry.COLUMN_TIMESTAMP + " TEXT, " + // Considerar cambiar a INTEGER si almacenas millis
                    DatabaseContract.ChatMessageEntry.COLUMN_URI + " TEXT, " +
                    DatabaseContract.ChatMessageEntry.COLUMN_STATUS + " TEXT" +
                    ");";
    private static final String SQL_CREATE_TIENDA =
            "CREATE TABLE IF NOT EXISTS " + DatabaseContract.TiendaEntry.TABLE_NAME + " (" +
                    DatabaseContract.TiendaEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    DatabaseContract.TiendaEntry.COLUMN_NOMBRE + " TEXT UNIQUE NOT NULL" +
                    ");";

    // --- Constructor ---
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // --- Creación de la Base de Datos ---
    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "onCreate: Creando base de datos (versión " + DATABASE_VERSION + ")...");
        try {
            db.execSQL(SQL_CREATE_USERS);
            Log.d(TAG, "onCreate: Tabla Usuarios creada.");
            db.execSQL(SQL_CREATE_TURNOS);
            Log.d(TAG, "onCreate: Tabla Turnos creada.");
            db.execSQL(SQL_CREATE_CHAT);
            Log.d(TAG, "onCreate: Tabla Chat creada.");
            db.execSQL(SQL_CREATE_TIENDA);
            Log.d(TAG, "onCreate: Tabla Tiendas creada.");
            insertInitialTiendas(db); // Insertar tiendas iniciales
            Log.i(TAG, "onCreate: Base de datos creada e inicializada exitosamente.");
        } catch (Exception e) {
            Log.e(TAG, "onCreate: Error al crear las tablas o insertar datos iniciales", e);
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
                long result = db.insertWithOnConflict(DatabaseContract.TiendaEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
                if (result != -1) {
                    Log.d(TAG, "insertInitialTiendas: Tienda inicial '" + nombreTienda + "' insertada.");
                } else {
                    Log.d(TAG, "insertInitialTiendas: Tienda inicial '" + nombreTienda + "' ya existía o error ignorado.");
                }
            }
            db.setTransactionSuccessful();
            Log.d(TAG, "insertInitialTiendas: Inserción de tiendas iniciales completada.");
        } catch (Exception e) {
            Log.e(TAG, "insertInitialTiendas: Error insertando tienda inicial", e);
        } finally {
            db.endTransaction();
        }
    }

    // --- Actualización de la Base de Datos ---
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "onUpgrade: Actualizando base de datos de versión " + oldVersion + " a " + newVersion + ". ¡Se borrarán los datos existentes!");
        try {
            db.execSQL("DROP TABLE IF EXISTS " + DatabaseContract.UserEntry.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + DatabaseContract.TurnoEntry.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + DatabaseContract.ChatMessageEntry.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + DatabaseContract.TiendaEntry.TABLE_NAME);
            onCreate(db);
        } catch (Exception e) {
            Log.e(TAG, "onUpgrade: Error al actualizar la base de datos", e);
        }
    }

    // --- Métodos CRUD y de Autenticación ---

    // --- USUARIOS ---
    public boolean authenticateUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        boolean exists = false;
        try {
            cursor = db.query(
                    DatabaseContract.UserEntry.TABLE_NAME,
                    new String[]{DatabaseContract.UserEntry._ID},
                    DatabaseContract.UserEntry.COLUMN_EMAIL + "=? AND " + DatabaseContract.UserEntry.COLUMN_PASSWORD + "=?",
                    new String[]{email.toLowerCase(), password}, null, null, null, "1" // Comparar con email en minúsculas
            );
            exists = (cursor != null && cursor.getCount() > 0);
        } catch (Exception e) {
            Log.e(TAG, "authenticateUser: Error autenticando a " + email, e);
        } finally {
            if (cursor != null) cursor.close();
        }
        Log.d(TAG, "authenticateUser: " + email + " -> " + (exists ? "Éxito" : "Fallo"));
        return exists;
    }

    public String getPasswordByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        String password = null;
        try {
            cursor = db.query(
                    DatabaseContract.UserEntry.TABLE_NAME,
                    new String[]{DatabaseContract.UserEntry.COLUMN_PASSWORD},
                    DatabaseContract.UserEntry.COLUMN_EMAIL + "=?",
                    new String[]{email.toLowerCase()}, // Comparar con email en minúsculas
                    null, null, null, "1"
            );
            if (cursor != null && cursor.moveToFirst()) {
                int passwordColumnIndex = cursor.getColumnIndex(DatabaseContract.UserEntry.COLUMN_PASSWORD);
                if (passwordColumnIndex != -1) {
                    password = cursor.getString(passwordColumnIndex);
                } else {
                    Log.e(TAG, "getPasswordByEmail: Columna '" + DatabaseContract.UserEntry.COLUMN_PASSWORD + "' no encontrada para email: " + email);
                }
            } else {
                Log.w(TAG, "getPasswordByEmail: No se encontró usuario con email: " + email);
            }
        } catch (Exception e) {
            Log.e(TAG, "getPasswordByEmail: Error al obtener contraseña para " + email, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.d(TAG, "getPasswordByEmail: " + email + " -> " + (password != null ? "Contraseña obtenida" : "No encontrada o error"));
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
            Log.w(TAG, "insertUser: Intento de insertar usuario con datos nulos o vacíos. Nombre: " + name + ", Email: " + email + ", Rol: " + role);
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
            if (result == -1) {
                Log.w(TAG, "insertUser: Error al insertar usuario '" + emailLowerCase + "'. El email podría ya existir.");
            } else {
                Log.i(TAG, "insertUser: Usuario '" + emailLowerCase + "' insertado con rol '" + role + "', ID: " + result);
            }
        } catch (Exception e) {
            Log.e(TAG, "insertUser: Excepción al insertar usuario '" + emailLowerCase + "'", e);
        }
        return result != -1;
    }

    /**
     * Verifica si un usuario con el email dado ya existe en la base de datos.
     * Compara el email en minúsculas para consistencia.
     * @param email El email a verificar.
     * @return true si el usuario existe, false en caso contrario.
     */
    public boolean checkIfUserExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        boolean exists = false;
        if (email == null || email.trim().isEmpty()) {
            Log.w(TAG, "checkIfUserExists: Email proporcionado es nulo o vacío.");
            return false;
        }
        try {
            String emailLowerCase = email.trim().toLowerCase();
            cursor = db.query(DatabaseContract.UserEntry.TABLE_NAME,
                    new String[]{DatabaseContract.UserEntry._ID},
                    DatabaseContract.UserEntry.COLUMN_EMAIL + "=?",
                    new String[]{emailLowerCase},
                    null, null, null, "1");
            exists = (cursor != null && cursor.getCount() > 0);
        } catch (Exception e) {
            Log.e(TAG, "checkIfUserExists: Error verificando si el usuario '" + email + "' existe.", e);
        } finally {
            if(cursor != null) {
                cursor.close();
            }
        }
        Log.d(TAG, "checkIfUserExists: Usuario '" + email + "' " + (exists ? "existe." : "no existe."));
        return exists;
    }


    public boolean deleteCajero(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = 0;
        try {
            rowsDeleted = db.delete(
                    DatabaseContract.UserEntry.TABLE_NAME,
                    DatabaseContract.UserEntry._ID + "=? AND " + DatabaseContract.UserEntry.COLUMN_ROLE + "=?",
                    new String[]{String.valueOf(id), "cajero"}
            );
            if (rowsDeleted > 0) {
                Log.i(TAG, "deleteCajero: Cajero con ID " + id + " eliminado. Filas afectadas: " + rowsDeleted);
            } else {
                Log.w(TAG, "deleteCajero: No se encontró o no se pudo eliminar el cajero con ID " + id);
            }
        } catch (Exception e) {
            Log.e(TAG, "deleteCajero: Error al eliminar cajero con ID " + id, e);
        }
        return rowsDeleted > 0;
    }

    // --- TURNOS ---
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
        try {
            result = db.insert(DatabaseContract.TurnoEntry.TABLE_NAME, null, values);
            if (result != -1) Log.i(TAG, "insertTurno: Turno insertado para cajero '" + cajero + "' en tienda '" + tienda + "', ID: " + result);
            else Log.w(TAG, "insertTurno: Falla al insertar turno para cajero '" + cajero + "' en tienda '" + tienda + "'.");
        } catch (Exception e) {
            Log.e(TAG, "insertTurno: Error al insertar turno", e);
        }
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
        // ... (resto de los put para updateTurno, igual que antes)
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
            if (rowsAffected > 0) Log.i(TAG, "updateTurno: Turno ID " + id + " actualizado.");
            else Log.w(TAG, "updateTurno: No se actualizó turno ID " + id + ". ¿No existe?");
        } catch (Exception e) {
            Log.e(TAG, "updateTurno: Error al actualizar turno ID " + id, e);
        }
        return rowsAffected > 0;
    }

    public Cursor getTurnoById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            String selection = DatabaseContract.TurnoEntry._ID + "=?";
            String[] selectionArgs = {String.valueOf(id)};
            cursor = db.query(DatabaseContract.TurnoEntry.TABLE_NAME, null, selection, selectionArgs, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                Log.d(TAG, "getTurnoById: Turno con ID " + id + " encontrado.");
            } else {
                Log.w(TAG, "getTurnoById: Turno con ID " + id + " no encontrado.");
            }
        } catch (Exception e) {
            Log.e(TAG, "getTurnoById: Error al obtener turno por ID " + id, e);
        }
        return cursor;
    }

    public Cursor getAllTurnos() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(DatabaseContract.TurnoEntry.TABLE_NAME, null, null, null, null, null,
                    DatabaseContract.TurnoEntry.COLUMN_FECHA + " DESC, " + DatabaseContract.TurnoEntry.COLUMN_HORA_INICIO + " DESC");
            if (cursor != null) Log.d(TAG, "getAllTurnos: Obtenidos " + cursor.getCount() + " turnos.");
        } catch (Exception e) {
            Log.e(TAG, "getAllTurnos: Error al obtener todos los turnos", e);
        }
        return cursor;
    }

    // --- CHAT ---
    public boolean insertChatMessage(String sender, String receiver, String content, String messageType,
                                     String timestamp, String uri, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.ChatMessageEntry.COLUMN_SENDER, sender);
        // ... (resto de los put para insertChatMessage, igual que antes)
        values.put(DatabaseContract.ChatMessageEntry.COLUMN_RECEIVER, receiver);
        values.put(DatabaseContract.ChatMessageEntry.COLUMN_CONTENT, content);
        values.put(DatabaseContract.ChatMessageEntry.COLUMN_MESSAGE_TYPE, messageType);
        values.put(DatabaseContract.ChatMessageEntry.COLUMN_TIMESTAMP, timestamp);
        values.put(DatabaseContract.ChatMessageEntry.COLUMN_URI, uri);
        values.put(DatabaseContract.ChatMessageEntry.COLUMN_STATUS, status);
        long result = -1;
        try {
            result = db.insert(DatabaseContract.ChatMessageEntry.TABLE_NAME, null, values);
            if (result != -1) Log.d(TAG, "insertChatMessage: Mensaje insertado de " + sender + " a " + receiver);
            else Log.w(TAG, "insertChatMessage: Falla al insertar mensaje de " + sender + " a " + receiver);
        } catch (Exception e) {
            Log.e(TAG, "insertChatMessage: Error insertando mensaje", e);
        }
        return result != -1;
    }

    public Cursor getChatMessages(String user1, String user2) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            String selection = "(" + DatabaseContract.ChatMessageEntry.COLUMN_SENDER + "=? AND " +
                    DatabaseContract.ChatMessageEntry.COLUMN_RECEIVER + "=?) OR (" +
                    DatabaseContract.ChatMessageEntry.COLUMN_SENDER + "=? AND " +
                    DatabaseContract.ChatMessageEntry.COLUMN_RECEIVER + "=?)";
            String[] selectionArgs = {user1, user2, user2, user1}; // Emails deben ser comparados consistentemente (ej. minúsculas)
            cursor = db.query(DatabaseContract.ChatMessageEntry.TABLE_NAME, null, selection, selectionArgs,
                    null, null, DatabaseContract.ChatMessageEntry.COLUMN_TIMESTAMP + " ASC");
            if (cursor != null) Log.d(TAG, "getChatMessages: Obtenidos " + cursor.getCount() + " mensajes entre " + user1 + " y " + user2);
        } catch (Exception e) {
            Log.e(TAG, "getChatMessages: Error al obtener mensajes entre " + user1 + " y " + user2, e);
        }
        return cursor;
    }

    // --- TIENDAS ---
    public Cursor getAllTiendas() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(DatabaseContract.TiendaEntry.TABLE_NAME, null, null, null, null, null,
                    DatabaseContract.TiendaEntry.COLUMN_NOMBRE + " ASC");
            if (cursor != null) Log.d(TAG, "getAllTiendas: Obtenidas " + cursor.getCount() + " tiendas.");
        } catch (Exception e) {
            Log.e(TAG, "getAllTiendas: Error al obtener todas las tiendas", e);
        }
        return cursor;
    }

    // La función checkIfTiendaExists ya la tenías, la mantengo.
    private boolean checkIfTiendaExists(String nombre) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        boolean exists = false;
        try {
            cursor = db.query(DatabaseContract.TiendaEntry.TABLE_NAME, new String[]{"1"},
                    DatabaseContract.TiendaEntry.COLUMN_NOMBRE + "=?", new String[]{nombre},
                    null, null, null, "1");
            exists = (cursor != null && cursor.getCount() > 0);
        } catch (Exception e) {
            Log.e(TAG, "checkIfTiendaExists: Error verificando tienda '" + nombre + "'", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return exists;
    }

    public boolean insertTienda(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            Log.w(TAG, "insertTienda: Intento de insertar tienda con nombre vacío.");
            return false;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        String nombreTrimmed = nombre.trim();
        values.put(DatabaseContract.TiendaEntry.COLUMN_NOMBRE, nombreTrimmed);
        long result = -1;
        try {
            result = db.insertWithOnConflict(DatabaseContract.TiendaEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
            if (result == -1) {
                if (checkIfTiendaExists(nombreTrimmed)) { // Usar la versión trimeada aquí también
                    Log.w(TAG, "insertTienda: Tienda '" + nombreTrimmed + "' ya existe.");
                } else {
                    Log.w(TAG, "insertTienda: Error desconocido (o constraint UNIQUE) al insertar tienda '" + nombreTrimmed + "'.");
                }
            } else {
                Log.i(TAG, "insertTienda: Tienda '" + nombreTrimmed + "' insertada con ID: " + result);
            }
        } catch (Exception e) {
            Log.e(TAG, "insertTienda: Excepción al insertar tienda '" + nombreTrimmed + "'", e);
        }
        return result != -1;
    }

    public boolean updateTienda(int id, String nuevoNombre) {
        if (nuevoNombre == null || nuevoNombre.trim().isEmpty()) {
            Log.w(TAG, "updateTienda: Intento de actualizar tienda ID " + id + " con nombre vacío.");
            return false;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        String nombreTrimmed = nuevoNombre.trim();
        values.put(DatabaseContract.TiendaEntry.COLUMN_NOMBRE, nombreTrimmed);
        int rowsAffected = 0;
        try {
            rowsAffected = db.update(DatabaseContract.TiendaEntry.TABLE_NAME, values,
                    DatabaseContract.TiendaEntry._ID + "=?", new String[]{String.valueOf(id)});
            if (rowsAffected > 0) {
                Log.i(TAG, "updateTienda: Tienda ID " + id + " actualizada a '" + nombreTrimmed + "'. Filas: " + rowsAffected);
            } else {
                Log.w(TAG, "updateTienda: No se actualizó tienda ID " + id + ". ¿No existe o el nombre es el mismo?");
            }
        } catch (SQLiteConstraintException e) {
            Log.w(TAG, "updateTienda: Error de constraint (UNIQUE) al actualizar tienda ID " + id + " a '" + nombreTrimmed + "'. ¿Nombre duplicado?", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "updateTienda: Error general al actualizar tienda ID " + id + " a '" + nombreTrimmed + "'", e);
        }
        return rowsAffected > 0;
    }

    public boolean deleteTienda(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsDeleted = 0;
        try {
            rowsDeleted = db.delete(DatabaseContract.TiendaEntry.TABLE_NAME,
                    DatabaseContract.TiendaEntry._ID + "=?", new String[]{String.valueOf(id)});
            if (rowsDeleted > 0) {
                Log.i(TAG, "deleteTienda: Tienda con ID " + id + " eliminada. Filas: " + rowsDeleted);
            } else {
                Log.w(TAG, "deleteTienda: No se encontró o no se pudo eliminar la tienda con ID " + id);
            }
        } catch (Exception e) {
            Log.e(TAG, "deleteTienda: Error al eliminar tienda con ID " + id, e);
        }
        return rowsDeleted > 0;
    }
}