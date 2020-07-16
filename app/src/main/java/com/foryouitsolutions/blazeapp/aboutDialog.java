package com.foryouitsolutions.blazeapp;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

public class aboutDialog extends AppCompatDialogFragment {

    private aboutDialog aboutDialog;
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.about_dialog,null);
        TextView tv = view.findViewById(R.id.aboutUs);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tv.setText(Html.fromHtml("<b>Privacy policy</b><br />Blaze app respects the privacy of it's users. The app or the third party libraries used do not track user data of any kind.<br /><br /><b>About us</b><br />Developers at For You IT Solutions (https://foryouitsolutions.github.io/) built Blaze.<br /><br /><b>Open Source Credits</b><br />Blaze uses the following open source libraries:<br />1. Fetch (https://github.com/tonyofrancis/Fetch)<br />2. NanoHTTPD (https://github.com/NanoHttpd/nanohttpd)<br />3. TedPermission (https://github.com/ParkSangGwon/TedPermission)", Html.FROM_HTML_MODE_COMPACT));
        } else {
            tv.setText(Html.fromHtml("<b>Privacy policy</b><br />Blaze app respects the privacy of it's users. The app or the third party libraries used do not track user data of any kind.<br /><br /><b>About us</b><br />Developers at For You IT Solutions (https://foryouitsolutions.github.io/) built Blaze.<br /><br /><b>Open Source Credits</b><br />Blaze uses the following open source libraries:<br />1. Fetch (https://github.com/tonyofrancis/Fetch)<br />2. NanoHTTPD (https://github.com/NanoHttpd/nanohttpd)<br />3. TedPermission (https://github.com/ParkSangGwon/TedPermission)"));
        }
        builder.setView(view)
                .setTitle("App Info")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {


                    }
                });


        return builder.create();


    }

}
