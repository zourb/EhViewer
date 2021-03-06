/*
 * Copyright (C) 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.hippo.ehviewer.BuildConfig;
import com.hippo.ehviewer.Constants;
import com.hippo.ehviewer.client.EhClient;
import com.hippo.util.Messenger;

public final class Config {

    private static SharedPreferences sConfigPre;

    private Config() {
    }

    public static void initialize(Context context) {
        sConfigPre = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static boolean getBoolean(String key, boolean defValue) {
        return sConfigPre.getBoolean(key, defValue);
    }

    public static void putBoolean(String key, boolean value) {
        sConfigPre.edit().putBoolean(key, value).apply();
    }

    public static int getInt(String key, int defValue) {
        return sConfigPre.getInt(key, defValue);
    }

    public static void putInt(String key, int value) {
        sConfigPre.edit().putInt(key, value).apply();
    }

    public static long getLong(String key, long defValue) {
        return sConfigPre.getLong(key, defValue);
    }

    public static void putLong(String key, long value) {
        sConfigPre.edit().putLong(key, value).apply();
    }

    public static float getFloat(String key, float defValue) {
        return sConfigPre.getFloat(key, defValue);
    }

    public static void putFloat(String key, float value) {
        sConfigPre.edit().putFloat(key, value).apply();
    }

    public static String getString(String key, String defValue) {
        return sConfigPre.getString(key, defValue);
    }

    public static void putString(String key, String value) {
        sConfigPre.edit().putString(key, value).apply();
    }

    /******  ******/

    private static final String KEY_EH_SOURCE = "eh_source";
    private static final int DEFAULT_EH_SOURCE = BuildConfig.DEBUG ?
            EhClient.SOURCE_LOFI : EhClient.SOURCE_G;

    public static int getEhSource() {
        int value = getInt(KEY_EH_SOURCE, DEFAULT_EH_SOURCE);
        if (value < EhClient.SOURCE_G || value > EhClient.SOURCE_LOFI) {
            value = DEFAULT_EH_SOURCE;
        }
        return value;
    }

    public static void putEhSource(int value) {
        if (value < EhClient.SOURCE_G || value > EhClient.SOURCE_LOFI) {
            value = DEFAULT_EH_SOURCE;
        }
        putInt(KEY_EH_SOURCE, value);
        Messenger.getInstance().notify(Constants.MESSENGER_ID_EH_SOURCE, value);
    }

    /****** Advance ******/

    private static final String KEY_SHOW_APPLICATION_STATS = "show_application_stats";
    private static final boolean DEFAULT_SHOW_APPLICATION_STATS = BuildConfig.DEBUG;

    public static boolean getShowApplicationStats() {
        return getBoolean(KEY_SHOW_APPLICATION_STATS, DEFAULT_SHOW_APPLICATION_STATS);
    }

    public static void putShowApplicationStats(boolean value) {
        putBoolean(KEY_SHOW_APPLICATION_STATS, value);
    }
}
