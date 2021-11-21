package com.carrotproxy.daedalus;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.net.VpnService;
import android.os.Build;
import androidx.preference.PreferenceManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import com.carrotproxy.daedalus.activity.MainActivity;
import com.carrotproxy.daedalus.server.AbstractDnsServer;
import com.carrotproxy.daedalus.server.DnsServer;
import com.carrotproxy.daedalus.server.DnsServerHelper;
import com.carrotproxy.daedalus.service.DaedalusVpnService;
import com.carrotproxy.daedalus.util.Configurations;
import com.carrotproxy.daedalus.util.Logger;
import com.carrotproxy.daedalus.util.Rule;
import com.carrotproxy.daedalus.util.RuleResolver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
public class Daedalus extends Application {

    private static final String SHORTCUT_ID_ACTIVATE = "shortcut_activate";

    public static final List<DnsServer> DNS_SERVERS = new ArrayList<DnsServer>() {{
        add(new DnsServer("158.101.118.131", R.string.server_carrot_iad));
        add(new DnsServer("152.70.177.207", R.string.server_carrot_fra));
        add(new DnsServer("37.233.103.45", R.string.server_carrot_waw));
    }};

    public static final String[] DEFAULT_TEST_DOMAINS = {
            "google.com",
            "twitter.com",
            "youtube.com",
            "facebook.com",
            "wikipedia.org"
    };

    public static Configurations configurations;
    public static String rulePath;
    public static String logPath;
    private static String configPath;

    private static Daedalus instance;
    private SharedPreferences prefs;
    private Thread mResolver;

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
        Logger.init();
        mResolver = new Thread(new RuleResolver());
        mResolver.start();
        initData();
    }

    private void initDirectory(String dir) {
        File directory = new File(dir);
        if (!directory.isDirectory()) {
            Logger.warning(dir + " is not a directory. Delete result: " + directory.delete());
        }
        if (!directory.exists()) {
            Logger.debug(dir + " does not exist. Create result: " + directory.mkdirs());
        }
    }

    private void initData() {
        PreferenceManager.setDefaultValues(this, R.xml.perf_settings, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (getExternalFilesDir(null) != null) {
            rulePath = getExternalFilesDir(null).getPath() + "/rules/";
            logPath = getExternalFilesDir(null).getPath() + "/logs/";
            configPath = getExternalFilesDir(null).getPath() + "/config.json";

            initDirectory(rulePath);
            initDirectory(logPath);
        }

        if (configPath != null) {
            configurations = Configurations.load(new File(configPath));
        } else {
            configurations = new Configurations();
        }
    }

    public static <T> T parseJson(Class<T> beanClass, JsonReader reader) throws JsonParseException {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        return gson.fromJson(reader, beanClass);
    }

    public static void initRuleResolver() {
        ArrayList<String> pendingLoad = new ArrayList<>();
        ArrayList<Rule> usingRules = configurations.getUsingRules();
        if (usingRules != null && usingRules.size() > 0) {
            for (Rule rule : usingRules) {
                if (rule.isUsing()) {
                    pendingLoad.add(rulePath + rule.getFileName());
                }
            }
            if (pendingLoad.size() > 0) {
                String[] arr = new String[pendingLoad.size()];
                pendingLoad.toArray(arr);
                switch (usingRules.get(0).getType()) {
                    case Rule.TYPE_HOSTS:
                        RuleResolver.startLoadHosts(arr);
                        break;
                    case Rule.TYPE_DNAMASQ:
                        RuleResolver.startLoadDnsmasq(arr);
                        break;
                }
            } else {
                RuleResolver.clear();
            }
        } else {
            RuleResolver.clear();
        }
    }

    public static SharedPreferences getPrefs() {
        return getInstance().prefs;
    }

    public static boolean isDarkTheme() {
        return getInstance().prefs.getBoolean("settings_dark_theme", false);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        instance = null;
        prefs = null;
        RuleResolver.shutdown();
        mResolver.interrupt();
        RuleResolver.clear();
        mResolver = null;
        Logger.shutdown();
    }

    public static Intent getServiceIntent(Context context) {
        return new Intent(context, DaedalusVpnService.class);
    }

    public static boolean switchService() {
        if (DaedalusVpnService.isActivated()) {
            deactivateService(instance);
            return false;
        } else {
            prepareAndActivateService(instance);
            return true;
        }
    }

    public static boolean prepareAndActivateService(Context context) {
        Intent intent = VpnService.prepare(context);
        if (intent != null) {
            return false;
        } else {
            activateService(context);
            return true;
        }
    }

    public static void activateService(Context context) {
        activateService(context, false);
    }

    public static void activateService(Context context, boolean forceForeground) {
        DaedalusVpnService.primaryServer = (AbstractDnsServer) DnsServerHelper.getServerById(DnsServerHelper.getPrimary()).clone();
        DaedalusVpnService.secondaryServer = (AbstractDnsServer) DnsServerHelper.getServerById(DnsServerHelper.getSecondary()).clone();
        if ((getInstance().prefs.getBoolean("settings_foreground", false) || forceForeground)
                && Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            Logger.info("Starting foreground service");
            context.startForegroundService(Daedalus.getServiceIntent(context).setAction(DaedalusVpnService.ACTION_ACTIVATE));
        } else {
            Logger.info("Starting background service");
            context.startService(Daedalus.getServiceIntent(context).setAction(DaedalusVpnService.ACTION_ACTIVATE));
        }

        update_carrot_ip();
    }


    public static void update_carrot_ip() {
        Runnable helloRunnable = new Runnable() {
            public void run() {
                try{
                    String carrot_token = getInstance().prefs.getString("carrotproxy_token", "0");
                    URL url = new URL("https://www.carrotproxy.com/daemon/submit_ip?token=" + carrot_token);
                    InputStream is = url.openStream();
                }catch(IOException ex){
                    //do exception handling here
                }
            }
        };

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(helloRunnable, 0, 15, TimeUnit.SECONDS);
    }

    public static void deactivateService(Context context) {
        context.startService(getServiceIntent(context).setAction(DaedalusVpnService.ACTION_DEACTIVATE));
        context.stopService(getServiceIntent(context));
    }

    public static void updateShortcut(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            Logger.info("Updating shortcut");
            boolean activate = DaedalusVpnService.isActivated();
            String notice = activate ? context.getString(R.string.button_text_deactivate) : context.getString(R.string.button_text_activate);
            ShortcutInfo info = new ShortcutInfo.Builder(context, Daedalus.SHORTCUT_ID_ACTIVATE)
                    .setLongLabel(notice)
                    .setShortLabel(notice)
                    .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
                    .setIntent(new Intent(context, MainActivity.class).setAction(Intent.ACTION_VIEW)
                            .putExtra(MainActivity.LAUNCH_ACTION, activate ? MainActivity.LAUNCH_ACTION_DEACTIVATE : MainActivity.LAUNCH_ACTION_ACTIVATE))
                    .build();
            ShortcutManager shortcutManager = (ShortcutManager) context.getSystemService(SHORTCUT_SERVICE);
            shortcutManager.addDynamicShortcuts(Collections.singletonList(info));
        }
    }

    public static void openUri(String uri) {
        try {
            instance.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            Logger.logException(e);
        }
    }

    public static Daedalus getInstance() {
        return instance;
    }


}
