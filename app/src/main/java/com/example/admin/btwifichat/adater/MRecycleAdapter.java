package com.example.admin.btwifichat.adater;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.example.admin.btwifichat.R;
import com.example.admin.btwifichat.activity.ControlActivity;
import com.example.admin.btwifichat.bean.ItemEntity;
import com.example.admin.btwifichat.sqlite.MyDatabaseHelper;

import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * Created by admin on 2017/3/28.
 */

public class MRecycleAdapter extends RecyclerView.Adapter<MRecycleAdapter.ViewHolder> {

    private SQLiteDatabase db;
    private LayoutInflater inflater;
    private List<ItemEntity> entityList;
    private List<ItemEntity> tempList;
    private Context context;

    //为了调用sendmessage方法
    private ControlActivity mControlActivity;
    //用数据库保存button设置的信息
    private MyDatabaseHelper mDbHelper;

    private String name;
    private boolean isExist;


    public MRecycleAdapter(Context context, ControlActivity activity){
        this.context=context;
        mControlActivity=activity;
        inflater= LayoutInflater.from(context);

        entityList=new ArrayList<>();
        tempList=new ArrayList<>();

        mDbHelper=new MyDatabaseHelper(context,"button.db",null,13);
        db = mDbHelper.getWritableDatabase();
    }

    public void addData(List<ItemEntity> dd){
        entityList.addAll(dd);
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        ViewHolder holder;
        View convertView = inflater.inflate(R.layout.item_set, parent, false);

        holder=new ViewHolder(convertView);

        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {

        Log.i(TAG, "onBindViewHolder: ");

        if (holder!=null){

            final ItemEntity entity = getItemEntity(position);

            if (entity!=null){
                Log.i(TAG, "onBindViewHolder: entity=="+entity.toString());
                holder.actionBtn.setText(entity.getName());
            }

            holder.actionBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (entity!=null){
                        String message = entity.getMessage();
                        Log.i(TAG, "onClick: message=="+message);
                        mControlActivity.sendMessage(message);
                    }

                }
            });

            holder.actionBtn.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    set(position);
                    return true;
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return entityList.size();
    }

    /**
     * 将editview上的信息存进数据库
     * */
    public void set(final int position){

        Log.i(TAG, "set: ");
        View view = inflater.inflate(R.layout.dialog_set, null);
        final EditText setName = (EditText) view.findViewById(R.id.dialog_set_name);
        final EditText setMsg = (EditText) view.findViewById(R.id.dialog_set_msg);

        AlertDialog.Builder builder=new AlertDialog.Builder(context);
        builder.setTitle("自定义设置");
        builder.setView(view);

        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                isExist=false;//每回设置默认都是添加操作，后续会继续判断
                name = setName.getText().toString().trim();
                String msg = setMsg.getText().toString().trim();

                ContentValues values = new ContentValues();
                values.put(MyDatabaseHelper.NAME_KEY,name);
                values.put(MyDatabaseHelper.MESSAGE_KEY,msg);
                values.put(MyDatabaseHelper.POSITION_KEY,position);
                Log.i(TAG, "onClick: position=="+position);

                Cursor cursor = db.query(
                        MyDatabaseHelper.TABLE_NAME, null, null, null, null, null, null);

                //如果此行已有数据则执行修改操作否则执行插入操作
                if (cursor.moveToFirst()){
                    do {

                        int id = cursor.getInt(cursor.getColumnIndex(MyDatabaseHelper.POSITION_KEY));
                        if (id==position){

                            isExist=true;
                            String oldName = cursor.getString(cursor.getColumnIndex(MyDatabaseHelper.NAME_KEY));
                            db.update(MyDatabaseHelper.TABLE_NAME,values,"name = ?",new String[]{oldName});

                            break;
                        }
                    }while(cursor.moveToNext());

                }
                if (!isExist){
                    db.insert(MyDatabaseHelper.TABLE_NAME,null,values);
                }

                values.clear();
                MRecycleAdapter.this.notifyDataSetChanged();

            }
        });

        builder.setNegativeButton("取消",null);
        builder.show();
    }

    /**
     * 从数据库中读取数据,返回id与position相同的entity
     * */
    public ItemEntity getItemEntity(int position){

        Cursor cursor = db.query(
                MyDatabaseHelper.TABLE_NAME, null,null, null, null, null, null);

        Log.i(TAG, "getItemEntity: count=="+cursor.getCount());

        tempList.clear();
        //把数据库中的数据全部读取出来，封装在entity中加进集合里面
        if (cursor.moveToFirst()){

            do {
                String name = cursor.getString(cursor.getColumnIndex(MyDatabaseHelper.NAME_KEY));
                String message = cursor.getString(cursor.getColumnIndex(MyDatabaseHelper.MESSAGE_KEY));
                int id = cursor.getInt(cursor.getColumnIndex(MyDatabaseHelper.POSITION_KEY));
                Log.i(TAG, "getItemEntity: name=="+name+"   message=="+message+"    id=="+id);
                tempList.add(new ItemEntity(name,message,id));
            }
            while (cursor.moveToNext());

        }

        //返回与点击位置对应的entity
        if (tempList.size()>0){
            for (ItemEntity entity : tempList) {
                Log.i(TAG, "getItemEntity: entity=="+entity.toString());
                if (entity.getId()==position){
                    return entity;
                }
            }
        }
        return null;
    }


    class ViewHolder extends RecyclerView.ViewHolder {

        private Button actionBtn;

        public ViewHolder(View view){
            super(view);
            actionBtn = ((Button) view.findViewById(R.id.action_btn));
        }
    }
}
