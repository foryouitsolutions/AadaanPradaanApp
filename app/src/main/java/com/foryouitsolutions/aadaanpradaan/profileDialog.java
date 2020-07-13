package com.foryouitsolutions.aadaanpradaan;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

public class profileDialog extends AppCompatDialogFragment {
    private EditText user;
    private profileDialogListener profileDialogListener;
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.profile_dialog,null);
        user = view.findViewById(R.id.user);
        builder.setView(view)
                .setTitle("Your Device Name")
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String username = user.getText().toString();
                        profileDialogListener.applyText(username);
                    }
                });


        return builder.create();


    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            profileDialogListener = (profileDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()+ "imp listener");
        }
    }

    public interface profileDialogListener{
        void applyText(String username);

    }
}
