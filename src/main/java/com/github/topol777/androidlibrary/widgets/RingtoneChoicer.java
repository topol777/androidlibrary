package com.github.topol777.androidlibrary.widgets;

import android.app.Activity;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.Fragment;

import com.github.topol777.androidlibrary.app.Storage;

// Ringtones/Alarms stored on sdcard with url context://media/extenral/.... requires READ_EXTERNAL_STORAGE.
//
// W/MediaPlayer: Couldn't open file on client side; trying server side:
// java.lang.SecurityException: Permission Denial: reading com.android.providers.media.MediaProvider uri content://media/external/audio/media/17722
// from pid=697, uid=10204
// requires android.permission.READ_EXTERNAL_STORAGE, or grantUriPermission()
//
// context.grantUriPermission("com.android.providers.media.MediaProvider", Uri.parse("content://media/external/images/media"), Intent.FLAG_GRANT_READ_URI_PERMISSION);
public class RingtoneChoicer extends OpenChoicer {
    public static final String MEDIA = "media";
    public static final String EXTERNAL = "external";

    public Fragment f;
    public int type;
    public Uri def;
    public int result;
    public Boolean silent = new Boolean(false);

    Uri permresult; // tmp result before permission dialog

    public RingtoneChoicer() {
        super(OpenFileDialog.DIALOG_TYPE.FILE_DIALOG, true);
    }

    public void setRingtone(Fragment f, int type, Uri def, String title, int result) {
        setRingtone(f, type, def, result);
        this.title = title;
    }

    public void setRingtone(Fragment f, int type, Uri def, int result) {
        setRingtone(f, type, result);
        this.def = def;
    }

    public void setRingtone(Fragment f, int type, int result) {
        activityCheck(f.getActivity());
        this.context = f.getContext();
        this.f = f;
        this.type = type;
        this.result = result;
    }

    @Override
    public void show(Uri old) {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, type)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, old);
        if (silent != null)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, silent);
        if (def != null) {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, def);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        }
        if (title != null)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, title);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        if (!readonly)
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        f.startActivityForResult(intent, result);
    }

    public void onActivityResult(int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            onCancel();
            onDismiss();
            return;
        }

        Uri u = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

        if (u == null) { // user selected none
            onCancel();
            onDismiss();
            return;
        }

        String m = u.getAuthority();
        if (m.equals(MEDIA)) {
            String l = u.getPathSegments().get(0);
            if (l.equals(EXTERNAL)) {
                if (((OpenChoicer) this).f == null) { // permissions not set, but we url set to external.
                    Toast.makeText(context, com.github.topol777.androidlibrary.R.string.not_permitted, Toast.LENGTH_SHORT).show();
                    show(u); // show open again
                    return;
                }
                if (!Storage.permitted(((OpenChoicer) this).f, perms, permsresult)) { // permission dialog shown
                    permresult = u;
                    return;
                }
            }
        }
        onResult(u, false);
        onDismiss();
    }

    @Override
    public void onRequestPermissionsResult(String[] permissions, int[] grantResults) {
        if (Storage.permitted(context, permissions)) {
            onResult(permresult, false);
            onDismiss();
        } else {
            onRequestPermissionsFailed(permissions);
        }
    }

    @Override
    public void onRequestPermissionsFailed(String[] permissions) {
        Toast.makeText(context, com.github.topol777.androidlibrary.R.string.not_permitted, Toast.LENGTH_SHORT).show();
        show(permresult);
    }
}
