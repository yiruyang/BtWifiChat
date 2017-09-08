package com.example.admin.btwifichat.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.admin.btwifichat.R;
import com.example.admin.btwifichat.util.TipTool;

/**
 * Created by admin on 2017/4/1.
 */

public class MyDatabaseHelper extends SQLiteOpenHelper {

    public static final String CREATE_BUTTON="create table Button("
    +"id integer primary key autoincrement, "
    +"name text, "
    +"position integer, "
    +"message integer)";

    public static final String NAME_KEY="name";
    public static final String MESSAGE_KEY="message";
    public static final String POSITION_KEY="position";
    public static final String TABLE_NAME="Button";


    private Context mContext;
    private static SQLiteDatabase mDb;

    public MyDatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        mContext=context;

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_BUTTON);
        TipTool.showToast(mContext,mContext.getResources().getString(R.string.create_success));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("drop table if exists Button");
        onCreate(db);
    }
}
