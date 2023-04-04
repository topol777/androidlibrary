package com.github.topol777.androidlibrary.preferences;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;

import com.github.topol777.androidlibrary.R;
import com.github.topol777.androidlibrary.net.HttpClient;
import com.github.topol777.androidlibrary.widgets.WebViewCustom;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.nio.charset.Charset;

//
// <com.github.topol777.androidlibrary.preferences.AboutPreferenceCompat
//   app:html="@raw/about"
//   android:persistent="false" />
//
public class AboutPreferenceCompat extends DialogPreference {
    public static final String V = "v";

    int id;

    public static String getApplicationName(Context context) {
        ApplicationInfo a = context.getApplicationInfo();
        int id = a.labelRes;
        return id == 0 ? a.nonLocalizedLabel.toString() : context.getString(id); // a.loadLabel() for external package
    }

    public static String getVersion(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            return getVersion(pm, context);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getVersion(PackageManager pm, Context context) throws PackageManager.NameNotFoundException {
        PackageInfo pInfo = pm.getPackageInfo(context.getPackageName(), 0);
        return V + pInfo.versionName;
    }

    public static void setName(TextView t) {
        t.setText(getApplicationName(t.getContext()));
    }

    public static void setVersion(TextView ver) {
        try {
            Context context = ver.getContext();
            PackageManager pm = context.getPackageManager();
            setVersion(pm, ver);
        } catch (PackageManager.NameNotFoundException e) {
            ver.setVisibility(View.GONE);
        }
    }

    public static void setVersion(PackageManager pm, TextView ver) throws PackageManager.NameNotFoundException {
        ver.setText(getVersion(pm, ver.getContext()));
    }

    public static View buildTitle(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View title = inflater.inflate(R.layout.about_title, null);
        TextView t = (TextView) title.findViewById(R.id.about_title_name);
        TextView v = (TextView) title.findViewById(R.id.about_title_version);

        setName(t);
        setVersion(v);

        return title;
    }

    public static WebViewCustom buildView(Context context, int id) {
        WebViewCustom web = new WebViewCustom(context) {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, final String url) {
                openUrl(getContext(), url);
                return true;
            }
        };
        web.getSettings().setBuiltInZoomControls(false);
        try {
            Resources res = context.getResources();
            InputStream is = res.openRawResource(id);
            String html = IOUtils.toString(is, Charset.defaultCharset());
            web.loadHtmlWithBaseURL("", html, "");
        } catch (Exception e) {
            web.loadHtmlWithBaseURL("", HttpClient.toStackTrace(e), "");
        }
        return web;
    }

    public static AlertDialog.Builder buildDialog(final Context context, int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCustomTitle(buildTitle(context));
        builder.setView(buildView(context, id));
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        return builder;
    }

    public static void openUrl(final Context context, final String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        context.startActivity(browserIntent);
    }

    public static void openUrlDialog(final Context context, final String url) {
        AlertDialog.Builder b = new AlertDialog.Builder(context);
        b.setTitle(R.string.open_browser);
        b.setMessage(R.string.are_you_sure);
        b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface d, int which) {
                try {
                    openUrl(context, url);
                } catch (Exception e) {
                    AlertDialog.Builder b = new AlertDialog.Builder(context);
                    b.setTitle("Error");
                    b.setMessage(e.getMessage());
                    b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    b.show();
                }
                d.cancel();
            }
        });
        b.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        AlertDialog d = b.create();
        d.show();
    }

    public static Dialog showDialog(final Context context, int id) {
        Dialog d = buildDialog(context, id).create();
        d.show();
        return d;
    }

    public static int getResourceId(Context context, String res) { // get resource id from String "R.raw.about" -> id
        try {
            String[] rr = res.split("\\."); // R.raw.about
            String name = rr[2];
            String type = rr[1];
            return context.getResources().getIdentifier(name, type, context.getPackageName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public AboutPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        create(attrs, defStyleAttr);
    }

    public AboutPreferenceCompat(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        create(attrs, 0);
    }

    public AboutPreferenceCompat(Context context) {
        this(context, null);
        create(null, 0);
    }

    void create(AttributeSet attrs, int defStyleAttr) {
        if (attrs != null) {
            final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.AboutPreferenceCompat, defStyleAttr, 0);
            id = a.getResourceId(R.styleable.AboutPreferenceCompat_html, -1);
        }
        setPersistent(false);
        setSummary(getApplicationName(getContext()) + " " + getVersion(getContext()));
        setTitle(getContext().getString(R.string.menu_about));
    }

    @Override
    public void onClick() {
        showDialog(getContext(), id);
    }

    public void setDialog(int id) {
        this.id = id;
    }

    public void setRawId(int id) {
        this.id = id;
    }
}
