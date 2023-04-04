package com.github.topol777.androidlibrary.preferences;

import android.content.Context;
import android.os.Build;
import android.os.LocaleList;
import android.support.v7.preference.ListPreference;
import android.util.AttributeSet;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import com.github.topol777.androidlibrary.R;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public class TTSPreferenceCompat extends ListPreference {
    public static String ZZ = "[Developer] Accented English";

    public static void addLocale(HashSet<Locale> list, Locale l) {
        String s = l.toString();
        if (s == null || s.isEmpty())
            return;
        for (Locale m : list) {
            if (m.toString().equals(s))
                return;
        }
        list.add(l);
    }

    public static Locale toLocale(String str) { // use LocaleUtils.toLocale
        String[] ss = str.split("_");
        if (ss.length == 3)
            return new Locale(ss[0], ss[1], ss[2]);
        else if (ss.length == 2)
            return new Locale(ss[0], ss[1]);
        else
            return new Locale(ss[0]);
    }

    public static HashSet<Locale> getInputLanguages(Context context) {
        HashSet<Locale> list = new HashSet<>();
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            LocaleList ll = LocaleList.getDefault();
            for (int i = 0; i < ll.size(); i++)
                addLocale(list, ll.get(i));
        }
        if (Build.VERSION.SDK_INT >= 11) {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                List<InputMethodInfo> ims = imm.getEnabledInputMethodList();
                for (InputMethodInfo m : ims) {
                    List<InputMethodSubtype> ss = imm.getEnabledInputMethodSubtypeList(m, true);
                    for (InputMethodSubtype s : ss) {
                        if (s.getMode().equals("keyboard")) {
                            Locale l = null;
                            if (Build.VERSION.SDK_INT >= 24) {
                                String tag = s.getLanguageTag();
                                if (!tag.isEmpty())
                                    l = Locale.forLanguageTag(tag);
                            }
                            if (l == null)
                                l = toLocale(s.getLocale());
                            addLocale(list, l);
                        }
                    }
                }
            }
        }
        return list;
    }

    public static String formatLocale(Locale l) {
        String n = l.getDisplayLanguage();
        String v = l.toString();
        if (n == null || n.isEmpty() || n.equals(v)) {
            if (v.equals("zz") || v.equals("zz_ZZ"))
                n = ZZ;
            else
                return v;
        }
        return String.format("%s (%s)", n, v);
    }

    public TTSPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        create();
    }

    public TTSPreferenceCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
        create();
    }

    public TTSPreferenceCompat(Context context) {
        super(context);
        create();
    }

    public void create() {
        LinkedHashMap<String, String> mm = new LinkedHashMap<>();
        mm.put("", getContext().getString(R.string.system_default));
        HashSet<Locale> ll = getInputLanguages(getContext());
        if (ll.isEmpty())
            ll.add(Locale.US);
        for (Locale l : ll)
            mm.put(l.toString(), formatLocale(l));
        setEntries(mm);
    }

    public void setEntries(LinkedHashMap<String, String> mm) {
        String def = getValue();
        setEntries(mm.values().toArray(new CharSequence[0]));
        setEntryValues(mm.keySet().toArray(new CharSequence[0]));
        int i = findIndexOfValue(def);
        if (i == -1)
            setValueIndex(0);
        else
            setValueIndex(i);
    }

    @Override
    public void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        super.onSetInitialValue(restoreValue, defaultValue);
        setSummary(getEntry());
    }

    @Override
    protected void notifyChanged() {
        super.notifyChanged();
        setSummary(getEntry());
    }
}
