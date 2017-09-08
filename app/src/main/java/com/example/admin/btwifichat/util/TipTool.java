package com.example.admin.btwifichat.util;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.widget.Toast;

import com.example.admin.btwifichat.R;

/**
 * Created by admin on 2017/3/7.
 */

public class TipTool {

    public static void showToast(Context context, CharSequence message){

        Toast.makeText(context,message, Toast.LENGTH_SHORT).show();
    }

    public static void showDialog(Context context, String title, String message){

        AlertDialog dialog=new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok,null)
                .create();
        dialog.show();
    }

    public static void showProgressDialog(Context context,String title,String message){

        ProgressDialog progressDialog = new ProgressDialog(context);
        if (title!=null)
        progressDialog.setTitle(title);
        progressDialog.setMessage(message);
        progressDialog.show();

    }
}
