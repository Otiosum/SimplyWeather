package akos.online.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseOpenHelper extends SQLiteOpenHelper {
    //Class member variables
    private static final String DB_NAME = "added_cities.db";
    private static final int DB_VERSION = 1;

    //Database manipulation strings
    private static final String DB_CREATE =
            "CREATE TABLE cities (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    "name TEXT NOT NULL" +
            ")";
    private static final String DB_DELETE = "DROP TABLE IF EXISTS cities";

    public DatabaseOpenHelper(Context context) {
           super(context, DB_NAME, null, DB_VERSION);
    }

    public void onCreate (SQLiteDatabase db) {
        db.execSQL(DB_CREATE);
    }

    public void onUpgrade (SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DB_DELETE);
        onCreate(db);
    }

}
