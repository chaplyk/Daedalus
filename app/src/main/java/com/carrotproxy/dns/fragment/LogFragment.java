package com.carrotproxy.dns.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.snackbar.Snackbar;
import com.carrotproxy.dns.Daedalus;
import com.carrotproxy.dns.R;
import com.carrotproxy.dns.util.Logger;

import java.io.FileWriter;

/**
 * Daedalus Project
 *
 * @author iTX Technologies
 * @link https://itxtech.org
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
public class LogFragment extends ToolbarFragment implements Toolbar.OnMenuItemClickListener {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_log, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        refresh();
    }

    private void refresh() {
        ((TextView) getView().findViewById(R.id.textView_log)).setText(Logger.getLog());
    }

    @Override
    public void checkStatus() {
        menu.findItem(R.id.nav_log).setChecked(true);
        toolbar.setTitle(R.string.action_log);
        toolbar.inflateMenu(R.menu.log);
        toolbar.setOnMenuItemClickListener(this);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_delete:
                Logger.init();
                refresh();
                break;
            case R.id.action_refresh:
                refresh();
                break;
            case R.id.action_export:
                break;
        }

        return true;
    }
}
