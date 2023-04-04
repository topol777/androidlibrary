package com.github.topol777.androidlibrary.widgets;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class DialogFragmentCompat extends DialogFragment {

    public AlertDialog.Builder builder;
    public AlertDialog d;
    public View v;

    @Override
    public AlertDialog onCreateDialog(Bundle savedInstanceState) {
        builder = new AlertDialog.Builder(getActivity());
        onCreateDialog(builder, savedInstanceState);
        d = builder.create();
        return d;
    }

    public void onCreateDialog(AlertDialog.Builder builder, Bundle savedInstanceState) {
        v = createView(LayoutInflater.from(getContext()), null, savedInstanceState);
        builder.setView(v);
    }

    @Nullable
    @Override
    public View getView() {
        return null; // return null, or api10 crash 'IllegalStateException DialogFragment can not be attached to a container view'
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return v;
    }

    public View createView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) { // override
        return null;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        d.setView(view); // with out setView api10 crash due to wrapping of child view 'IllegalStateException: The specified child already has a parent'
    }

}
