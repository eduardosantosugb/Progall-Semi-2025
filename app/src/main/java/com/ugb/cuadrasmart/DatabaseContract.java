package com.ugb.cuadrasmart;

import android.provider.BaseColumns;

public final class DatabaseContract {

    // Evita la instanciación accidental
    private DatabaseContract() {}

    // Tabla de Usuarios
    public static final class UserEntry implements BaseColumns {
        public static final String TABLE_NAME = "users";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_EMAIL = "email";
        public static final String COLUMN_PASSWORD = "password";
        public static final String COLUMN_ROLE = "role"; // "supervisor" o "cajero"
    }

    // Tabla de Turnos
    public static final class TurnoEntry implements BaseColumns {
        public static final String TABLE_NAME = "turnos";
        public static final String COLUMN_FECHA = "fecha";
        public static final String COLUMN_HORA_INICIO = "hora_inicio";
        public static final String COLUMN_HORA_CIERRE = "hora_cierre";
        public static final String COLUMN_INICIO_DESCANSO = "inicio_descanso";
        public static final String COLUMN_FIN_DESCANSO = "fin_descanso";
        public static final String COLUMN_NUMERO_CAJA = "numero_caja";
        public static final String COLUMN_CAJERO = "cajero";
        public static final String COLUMN_BILLETES = "billetes";
        public static final String COLUMN_MONEDAS = "monedas";
        public static final String COLUMN_CHEQUES = "cheques";
        public static final String COLUMN_VENTAS_ESPERADAS = "ventas_esperadas";
        public static final String COLUMN_DISCREPANCIA = "discrepancia";
        public static final String COLUMN_JUSTIFICACION = "justificacion";
        public static final String COLUMN_EVIDENCIA = "evidencia";
        public static final String COLUMN_TIENDA = "tienda";
    }

    // Tabla de Mensajes de Chat
    public static final class ChatMessageEntry implements BaseColumns {
        public static final String TABLE_NAME = "chat_messages";
        public static final String COLUMN_SENDER = "sender";
        public static final String COLUMN_RECEIVER = "receiver";
        public static final String COLUMN_CONTENT = "content";
        public static final String COLUMN_MESSAGE_TYPE = "message_type";
        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final String COLUMN_URI = "uri";
        public static final String COLUMN_STATUS = "status";
    }

    // Tabla de Tiendas
    public static final class TiendaEntry implements BaseColumns {
        public static final String TABLE_NAME = "tiendas";
        public static final String COLUMN_NOMBRE = "nombre";
    }
}
