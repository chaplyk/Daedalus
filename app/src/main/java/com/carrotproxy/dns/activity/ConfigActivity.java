package com.carrotproxy.dns.activity;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.carrotproxy.dns.Daedalus;
import com.carrotproxy.dns.R;
import com.carrotproxy.dns.fragment.ConfigFragment;
import com.carrotproxy.dns.fragment.DnsServerConfigFragment;

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
public class ConfigActivity extends AppCompatActivity {
    public static final String LAUNCH_ACTION_FRAGMENT = "com.carrotproxy.dns.activity.ConfigActivity.LAUNCH_ACTION_FRAGMENT";
    public static final int LAUNCH_FRAGMENT_DNS_SERVER = 0;
    public static final int LAUNCH_FRAGMENT_RULE = 1;

    public static final String LAUNCH_ACTION_ID = "com.carrotproxy.dns.activity.ConfigActivity.LAUNCH_ACTION_ID";
    public static final int ID_NONE = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Daedalus.isDarkTheme()) {
            setTheme(R.style.AppTheme_Dark_NoActionBar);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        Toolbar toolbar = findViewById(R.id.toolbar_config);
        switch (getIntent().getIntExtra(LAUNCH_ACTION_FRAGMENT, LAUNCH_FRAGMENT_DNS_SERVER)) {
            case LAUNCH_FRAGMENT_DNS_SERVER:
                toolbar.setTitle(R.string.config_dns_server);
                break;
            case LAUNCH_FRAGMENT_RULE:
                toolbar.setTitle(R.string.config_rule);
                break;
            default://should never reach this
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Daedalus.configurations.save();
    }
}
