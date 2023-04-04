package com.github.topol777.androidlibrary.widgets;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.database.DataSetObserver;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.github.topol777.androidlibrary.R;
import com.github.topol777.androidlibrary.app.MainApplication;
import com.github.topol777.androidlibrary.app.Storage;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenFileDialog extends AlertDialog.Builder {
    public static final String TAG = OpenFileDialog.class.getSimpleName();

    public enum DIALOG_TYPE {
        FILE_DIALOG,
        FOLDER_DIALOG,
        BOOTH
    }

    public static final String ANDROID_STORAGE = "ANDROID_STORAGE"; // environment variable, TODO: add 'EXTERNAL_STORAGE' and 'SECONDARY_STORAGE'. do not have device to test
    public static final String DEFAULT_STORAGE_PATH = "/storage";
    public static final Pattern DEFAULT_STORAGE_PATTERN = Pattern.compile("\\w\\w\\w\\w-\\w\\w\\w\\w");

    public static final String[] PERMISSIONS_RO = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
    public static final String[] PERMISSIONS_RW = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    public static final String UP = "[..]";
    public static final String ROOT = "/";

    protected static int FOLDER_ICON = R.drawable.ic_folder;
    protected static int FILE_ICON = R.drawable.ic_file;

    protected static Map<File, Set<File>> CACHE = new TreeMap<>(); // cache folders, keep folder visible, when android shows _none_

    protected String title;
    protected File currentPath;
    protected TextView free;
    protected TextView path;
    protected TextView message;
    protected RecyclerView listView;
    protected FileAdapter adapter;
    protected DialogInterface.OnShowListener onshow;
    protected DialogInterface.OnClickListener neutral;

    protected int paddingLeft;
    protected int paddingRight;
    protected int paddingBottom;
    protected int paddingTop;
    protected Runnable changeFolder;

    // file / folder readonly dialog selection or output directory? also shows readonly folder tooltip.
    protected boolean readonly = false;
    // allow select files, or just select directory
    protected DIALOG_TYPE type = DIALOG_TYPE.BOOTH;

    protected Button positive; // enable / disable OK

    protected RecyclerView.AdapterDataObserver observer = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            OpenFileDialog.this.onChanged();
        }
    };

    public static long getTotalBytes(StatFs fs) {
        if (Build.VERSION.SDK_INT >= 18)
            return fs.getTotalBytes();
        else
            return fs.getBlockCount() * (long) fs.getBlockSize();
    }

    public static File getPortable() { // portable location /storage
        String path = System.getenv(OpenFileDialog.ANDROID_STORAGE);
        if (path == null || path.isEmpty())
            path = OpenFileDialog.DEFAULT_STORAGE_PATH;
        return new File(path);
    }

    public static File[] getPortableList() { // portable formatted sdcards
        File portable = getPortable();
        return portable.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                String name = file.getName();
                Matcher m = DEFAULT_STORAGE_PATTERN.matcher(name);
                return m.matches();
            }
        });
    }

    public static void hideKeyboard(Context context, View input) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(input.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    protected static void cache(Context context) {
        cache(context.getExternalCacheDir());
        if (Build.VERSION.SDK_INT >= 19) {
            File[] ff = context.getExternalCacheDirs();
            for (File f : ff)
                cache(f);
        }
    }

    protected static void cache(File path) {
        while (path != null && path.isFile()) // skip file links, go up folder
            path = path.getParentFile();
        if (path == null)
            return;
        File old = path;
        path = path.getParentFile();
        while (path != null) {
            TreeSet<File> list = new TreeSet<>();
            list.add(old);
            Set<File> tmp = CACHE.put(path, list);
            if (tmp != null) {
                for (File f : tmp) {
                    list.add(f);
                }
            }
            old = path;
            path = path.getParentFile();
        }
    }

    public static Display getDefaultDisplay(Context context) {
        return ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
    }

    public static boolean isFile(File f) {
        return !isDirectory(f);
    }

    public static boolean isDirectory(File f) {
        if (f.isDirectory())
            return true;
        if (CACHE.get(f) != null)
            return true;
        return false;
    }

    public static File getDataDir(Context context) {
        return new File(context.getApplicationContext().getApplicationInfo().dataDir);
    }

    public static File getLocalInternal(Context context) {
        File file = context.getFilesDir();
        if (file == null)
            return getDataDir(context);
        return file;
    }

    public static File[] getLocalExternals(Context context, boolean readonly) {
        File[] external = ContextCompat.getExternalFilesDirs(context, "");

        // Starting in KITKAT, no permissions are required to read or write to the getExternalFilesDir;
        // it's always accessible to the calling app.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            if (readonly) {
                if (!Storage.permitted(context, PERMISSIONS_RO))
                    return null;
            } else {
                if (!Storage.permitted(context, PERMISSIONS_RW))
                    return null;
            }
        }

        return external;
    }

    public static Point getScreenSize(Context context) {
        Display d = getDefaultDisplay(context);
        if (Build.VERSION.SDK_INT < 13) {
            return new Point(d.getWidth(), d.getHeight());
        } else {
            Point screeSize = new Point();
            d.getSize(screeSize);
            return screeSize;
        }
    }

    public static int getLinearLayoutMinHeight(Context context) {
        return getScreenSize(context).y;
    }

    public static Drawable getDrawable(Context context, int id) {
        Drawable d = ContextCompat.getDrawable(context, id);
        d = DrawableCompat.wrap(d);
        d.mutate();
        DrawableCompat.setTint(d, ThemeUtils.getThemeColor(context, android.R.attr.colorForeground));
        return d;
    }

    public static class SortFiles implements Comparator<File> {
        @Override
        public int compare(File f1, File f2) {
            if (isDirectory(f1) && isFile(f2))
                return -1;
            else if (isFile(f1) && isDirectory(f2))
                return 1;
            else
                return f1.getPath().compareTo(f2.getPath());
        }
    }

    public static class StorageAdapter implements ListAdapter {
        ArrayList<File> list = new ArrayList<>();
        OpenFileDialog builder;

        public StorageAdapter(OpenFileDialog b) {
            this.builder = b;
            File ext = Environment.getExternalStorageDirectory();
            cache(ext);
            if (ext == null || (!builder.readonly && !ext.canWrite()))
                ext = getLocalInternal(builder.getContext());
            add(ext);
            File data = getDataDir(builder.getContext());
            File datae = builder.getContext().getExternalCacheDir();
            if (datae != null)
                datae = datae.getParentFile();
            if (Build.VERSION.SDK_INT >= 19) {
                File[] ff = getLocalExternals(builder.getContext(), builder.readonly);
                if (ff != null) {
                    for (File f : ff) {
                        if (f == null)
                            continue; // if storage unmounted null file here

                        cache(f);

                        {
                            ArrayList<File> pp = new ArrayList<>();
                            File a = f;
                            StatFs stat = new StatFs(f.getPath());
                            File p = f;
                            while (f != null) {
                                pp.add(f);
                                StatFs fs = new StatFs(f.getPath());
                                if (getTotalBytes(fs) != getTotalBytes(stat))
                                    add(p); // add sdcard root
                                p = f;
                                f = f.getParentFile();
                            }
                            if (!builder.readonly) { // help user find writable root if algorithm above failed
                                for (int i = pp.size() - 1; i >= 0; i--) {
                                    p = pp.get(i);
                                    if (p.canWrite()) {
                                        if (data != null && p.getPath().startsWith(data.getPath())) // skip default /storage/.../data
                                            continue;
                                        if (datae != null && p.getPath().startsWith(datae.getPath())) // skip default /storage/.../data
                                            continue;
                                        if (add(p))
                                            break; // add first root
                                    }
                                }
                            }
                            if (data != null && a.getPath().startsWith(data.getPath())) // skip default /storage/.../files
                                continue;
                            if (datae != null && a.getPath().startsWith(datae.getPath())) // skip default /storage/.../files
                                continue;
                            add(a);
                        }
                    }
                }
            }
            File[] ff = getPortableList();
            if (ff == null)
                return;
            for (File f : ff)
                add(f);
        }

        boolean add(File f) {
            if (f == null)
                return false;
            if (!builder.readonly && !f.canWrite())
                return false;
            for (int i = 0; i < list.size(); i++) {
                String s = list.get(i).getPath();
                String k = f.getPath();
                if (s.equals(k))
                    return true;
                if (Storage.relative(s, k) != null || Storage.relative(k, s) != null) {
                    StatFs fs = new StatFs(f.getPath());
                    StatFs ss = new StatFs(s);
                    if (getTotalBytes(ss) == getTotalBytes(fs))
                        return true;
                }
            }
            list.add(f);
            return true;
        }

        public int find(File c) {
            for (int i = 0; i < list.size(); i++) {
                File f = list.get(i);
                if (c.getPath().startsWith(f.getPath()))
                    return i;
            }
            return -1;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
        }

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(builder.getContext());

                final LinearLayout titlebar = new LinearLayout(builder.getContext());
                titlebar.setOrientation(LinearLayout.HORIZONTAL);
                titlebar.setPadding(builder.paddingLeft, 0, builder.paddingRight, 0);
                titlebar.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                TextView title = (TextView) inflater.inflate(android.R.layout.simple_list_item_1, null);
                title.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                title.setPadding(0, builder.paddingTop, 0, builder.paddingBottom);
                title.setTag("text");

                PathMax textMax = new PathMax(builder.getContext(), title);
                textMax.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                titlebar.addView(textMax);

                TextView free = new TextView(builder.getContext());
                free.setTag("free");
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.gravity = Gravity.CENTER;
                free.setLayoutParams(lp);
                titlebar.addView(free);

                convertView = titlebar;
            }

            File f = list.get(position);
            TextView title = (TextView) convertView.findViewWithTag("text");
            title.setText(f.getPath());
            TextView free = (TextView) convertView.findViewWithTag("free");
            free.setText(MainApplication.formatSize(builder.getContext(), Storage.getFree(f)));

            return convertView;
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return list.isEmpty();
        }
    }

    public static class FileHolder extends RecyclerView.ViewHolder {
        public TextView text;

        public FileHolder(View itemView) {
            super(itemView);
            text = (TextView) itemView.findViewById(android.R.id.text1);
        }

        public FileHolder(ViewGroup p) {
            this(LayoutInflater.from(p.getContext()).inflate(android.R.layout.simple_list_item_1, p, false));
        }
    }

    public static class FileAdapter extends RecyclerView.Adapter<FileHolder> {
        protected Context context;
        protected int selectedIndex = -1;
        protected File currentPath;

        protected int colorSelected;
        protected int colorTransparent;
        protected int iconPadding;

        protected ArrayList<File> files = new ArrayList<>();

        public AdapterView.OnItemLongClickListener onItemLongClickListener;
        public AdapterView.OnItemClickListener onItemClickListener;
        public TextView emptyView; // also used to show fatal errors

        public FileAdapter(Context context, File currentPath) {
            this.context = context;
            this.currentPath = currentPath;
            if (Build.VERSION.SDK_INT >= 23) {
                colorSelected = context.getResources().getColor(android.R.color.holo_blue_dark, context.getTheme());
                colorTransparent = context.getResources().getColor(android.R.color.transparent, context.getTheme());
            } else {
                colorSelected = context.getResources().getColor(R.color.holo_blue_dark);
                colorTransparent = context.getResources().getColor(android.R.color.transparent);
            }
            iconPadding = ThemeUtils.dp2px(context, 5);
        }

        @Override
        public FileHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new FileHolder(parent);
        }

        @Override
        public void onBindViewHolder(final FileHolder h, int position) {
            File file = files.get(position);
            h.text.setText(file.getName());
            if (OpenFileDialog.isDirectory(file))
                setDrawable(h.text, getDrawable(context, FOLDER_ICON));
            else
                setDrawable(h.text, getDrawable(context, FILE_ICON));
            if (selectedIndex == position)
                h.text.setBackgroundColor(colorSelected);
            else
                h.text.setBackgroundColor(colorTransparent);
            h.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (onItemLongClickListener != null)
                        return onItemLongClickListener.onItemLongClick(null, v, h.getAdapterPosition(), -1);
                    return false;
                }
            });
            h.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onItemClickListener != null)
                        onItemClickListener.onItemClick(null, v, h.getAdapterPosition(), -1);
                }
            });
        }

        public void setDrawable(TextView view, Drawable drawable) {
            if (view != null) {
                if (drawable != null) {
                    drawable.setBounds(0, 0, drawable.getIntrinsicWidth() + iconPadding, drawable.getIntrinsicHeight() + iconPadding);
                    view.setCompoundDrawables(drawable, null, null, null);
                } else {
                    view.setCompoundDrawables(null, null, null, null);
                }
            }
        }

        @Override
        public int getItemCount() {
            return files.size();
        }

        protected List<File> cache(File path, FilenameFilter filter) {
            Set<File> list = null;
            File[] ff = path.listFiles(filter);
            if (ff != null)
                list = new TreeSet<>(Arrays.asList(ff));
            if (list == null)
                list = new TreeSet<>();
            Set<File> old = CACHE.get(path);
            if (old != null) {
                for (File f : old) {
                    if (f.exists()) // purge cache from non existen files
                        list.add(f);
                }
            }
            CACHE.put(path, list);

            ArrayList<File> files = new ArrayList<>();
            for (File f : list) {
                String s = f.getName();
                if ((filter == null) || filter.accept(path, s))
                    files.add(f);
            }
            return files;
        }

        public void scan() {
            OpenFileDialog.cache(currentPath);

            if (currentPath.exists() && !isDirectory(currentPath)) // file or symlink
                currentPath = currentPath.getParentFile();

            if (currentPath == null)
                currentPath = new File(ROOT);

            files.clear();

            try {
                List<File> ff = cache(currentPath, null);
                if (ff != null) {
                    if (Build.VERSION.SDK_INT < 11) {
                        for (File f : ff)
                            files.add(f); // API11< has no Collection.addAll()
                    } else {
                        files.addAll(ff);
                    }
                    Collections.sort(files, new SortFiles());
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "listFiles", e);
                if (emptyView != null) {
                    emptyView.setVisibility(View.VISIBLE);
                    emptyView.setText(ErrorDialog.toMessage(e));
                }
            }

            if (emptyView != null)
                emptyView.setVisibility(files.isEmpty() ? View.VISIBLE : View.GONE);

            notifyDataSetChanged();
        }

        public File open(String name) {
            return new File(currentPath, name);
        }
    }

    public static class EditTextDialog extends AlertDialog.Builder {
        public EditText input;

        public EditTextDialog(Context context) {
            super(context);

            input = new EditText(getContext());
            input.setSingleLine(true);
            input.requestFocus();

            setPositiveButton(new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            });
            setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    hide();
                }
            });

            setView(input);
        }

        public AlertDialog.Builder setPositiveButton(final DialogInterface.OnClickListener listener) {
            return setPositiveButton(android.R.string.ok, listener);
        }

        @Override
        public AlertDialog.Builder setPositiveButton(int textId, final DialogInterface.OnClickListener listener) {
            return super.setPositiveButton(textId, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    listener.onClick(dialog, which);
                    hide();
                }
            });
        }

        @Override
        public AlertDialog.Builder setPositiveButton(CharSequence text, final DialogInterface.OnClickListener listener) {
            return super.setPositiveButton(text, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    listener.onClick(dialog, which);
                    hide();
                }
            });
        }

        public void hide() {
            hideKeyboard(getContext(), input);
        }

        @Override
        public AlertDialog create() {
            AlertDialog d = super.create();

            Window w = d.getWindow();
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

            return d;
        }

        public void setText(String s) {
            input.setText(s);
            input.setSelection(s.length());
        }

        public String getText() {
            return input.getText().toString();
        }
    }

    public OpenFileDialog(Context context, DIALOG_TYPE type) {
        super(context);
        this.type = type;
        currentPath = Environment.getExternalStorageDirectory();
        paddingLeft = dp2px(14);
        paddingRight = dp2px(14);
        paddingTop = dp2px(14);
        paddingBottom = dp2px(14);
        cache(context);
    }

    public OpenFileDialog(Context context, DIALOG_TYPE type, boolean readonly) {
        this(context, type);
        setReadonly(readonly);
    }

    public int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getContext().getResources().getDisplayMetrics());
    }

    protected void toast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public AlertDialog create() {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        int dp2 = ThemeUtils.dp2px(getContext(), 2);

        // title layout
        final LinearLayout titlebar = new LinearLayout(getContext());
        titlebar.setOrientation(LinearLayout.HORIZONTAL);
        titlebar.setPadding(paddingLeft, 0, paddingRight, 0);
        titlebar.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        path = (TextView) inflater.inflate(android.R.layout.simple_list_item_1, null);
        path.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        path.setPadding(0, paddingTop, dp2, paddingBottom);

        PathMax textMax = new PathMax(getContext(), path);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        lp.gravity = Gravity.CENTER;
        textMax.setLayoutParams(lp);
        titlebar.addView(textMax);

        free = new TextView(getContext());
        lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        free.setLayoutParams(lp);
        titlebar.addView(free);
        ImageView down = new ImageView(getContext());
        down.setImageResource(R.drawable.ic_expand_more_black_24dp);
        down.setColorFilter(ThemeUtils.getThemeColor(getContext(), R.attr.colorAccent));
        lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        down.setLayoutParams(lp);
        titlebar.addView(down);

        titlebar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final StorageAdapter storage = new StorageAdapter(OpenFileDialog.this);
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setSingleChoiceItems(storage, storage.find(adapter.currentPath), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        adapter.currentPath = storage.list.get(which);
                        rebuildFiles();
                        dialog.dismiss();
                    }
                });
                AlertDialog d = builder.create();
                d.show();
            }
        });
        if (title != null) {
            final LinearLayout titlebarVert = new LinearLayout(getContext());
            titlebarVert.setOrientation(LinearLayout.VERTICAL);
            TextView t = new AppCompatTextView(getContext());
            TextViewCompat.setTextAppearance(t, R.style.TextAppearance_AppCompat_Title);
            t.setText(title);
            t.setPadding(paddingLeft, paddingTop, paddingRight, 0);
            titlebarVert.addView(t);
            titlebarVert.addView(titlebar);
            setCustomTitle(titlebarVert);
        } else {
            setCustomTitle(titlebar);
        }

        // main view, linearlayout
        final LinearLayout main = new LinearLayout(getContext());
        main.setOrientation(LinearLayout.VERTICAL);
        main.setMinimumHeight(getLinearLayoutMinHeight(getContext()));
        main.setPadding(paddingLeft, 0, paddingRight, 0);

        if (adapter == null) {
            adapter = new FileAdapter(getContext(), currentPath);
            adapter.registerAdapterDataObserver(observer);
        }

        // add toolbar (UP / NEWFOLDER)
        {
            LinearLayout toolbar = new LinearLayout(getContext());
            toolbar.setOrientation(LinearLayout.HORIZONTAL);
            toolbar.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            {
                TextView textView = (TextView) LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1, null);
                textView.setText(UP);
                adapter.setDrawable(textView, getDrawable(getContext(), FOLDER_ICON));
                textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                textView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) { // [UP] button
                        File parentDirectory = adapter.currentPath;
                        File old = parentDirectory;
                        if (isDirectory(parentDirectory) || !parentDirectory.exists()) { // allow virtual up
                            parentDirectory = parentDirectory.getParentFile();
                        } else {
                            parentDirectory = parentDirectory.getParentFile();
                            if (parentDirectory != null)
                                parentDirectory = parentDirectory.getParentFile();
                        }

                        if (parentDirectory == null)
                            parentDirectory = old;

                        adapter.currentPath = parentDirectory;
                        rebuildFiles();
                    }
                });
                toolbar.addView(textView);
            }

            if (!readonly) { // show new folder button
                AppCompatButton textView = new AppCompatButton(getContext());
                textView.setPadding(paddingLeft, 0, paddingRight, 0);
                textView.setText(R.string.filedialog_newfolder);
                textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                textView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final EditTextDialog builder = new EditTextDialog(getContext());
                        builder.setTitle(R.string.filedialog_foldername);
                        builder.setText("");
                        builder.setPositiveButton(new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                File f = adapter.open(builder.getText());
                                if (!f.mkdirs())
                                    toast(getContext().getString(R.string.filedialog_unablecreatefolder, builder.getText()));
                                else
                                    toast(getContext().getString(R.string.filedialog_foldercreated, builder.getText()));
                                adapter.scan();
                            }
                        });
                        builder.show();
                    }
                });
                toolbar.addView(textView, lp);
            }

            main.addView(toolbar);

            message = new TextView(getContext());
            message.setGravity(Gravity.CENTER);
            message.setBackgroundColor(0x22222222);
            message.setVisibility(View.GONE);
            main.addView(message);
        }

        // ADD FILES LIST
        {
            listView = new RecyclerView(getContext());
            listView.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter.onItemLongClickListener = new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                    final PopupMenu p = new PopupMenu(getContext(), view);
                    if (!readonly) { // show rename / delete
                        p.getMenu().add(getContext().getString(R.string.filedialog_rename));
                        p.getMenu().add(getContext().getString(R.string.filedialog_delete));
                    }
                    p.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            if (item.getTitle().equals(getContext().getString(R.string.filedialog_rename))) {
                                final File ff = adapter.files.get(position);
                                final EditTextDialog b = new EditTextDialog(getContext());
                                b.setTitle(getContext().getString(R.string.filedialog_foldername));
                                b.setText(ff.getName());
                                b.setPositiveButton(new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        File f = adapter.open(b.getText());
                                        ff.renameTo(f);
                                        toast(getContext().getString(R.string.filedialog_renameto, f.getName()));
                                        adapter.scan();
                                    }
                                });
                                b.show();
                                return true;
                            }
                            if (item.getTitle().equals(getContext().getString(R.string.filedialog_delete))) {
                                File ff = adapter.files.get(position);
                                ff.delete();
                                CACHE.remove(ff);
                                CACHE.get(ff.getParentFile()).remove(ff);
                                toast(getContext().getString(R.string.filedialog_folderdeleted, ff.getName()));
                                adapter.scan();
                                return true;
                            }
                            return false;
                        }
                    });

                    if (p.getMenu().size() != 0) {
                        p.show();
                        return true;
                    }

                    return false;
                }
            };
            adapter.onItemClickListener = new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int index, long l) {
                    File file = adapter.files.get(index);

                    adapter.currentPath = file;

                    if (isDirectory(file)) {
                        rebuildFiles();
                    } else {
                        switch (type) {
                            case FILE_DIALOG:
                            case BOOTH:
                                if (index != adapter.selectedIndex) {
                                    updateSelected(index);
                                } else {
                                    adapter.currentPath = file.getParentFile();
                                    updateSelected(-1);
                                }
                                adapter.notifyDataSetChanged();
                                break;
                            default:
                                Toast.makeText(getContext(), R.string.filedialog_selectfolder, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            };
            main.addView(listView);
        }

        {
            TextView text = (TextView) LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1, null);
            text.setText(getContext().getString(R.string.filedialog_empty));
            text.setVisibility(View.GONE);
            adapter.emptyView = text;
            main.addView(text);
        }

        setView(main);
        setNegativeButton(android.R.string.cancel, null);

        listView.setAdapter(adapter);

        setNeutralButton(getContext().getString(R.string.manual_path), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final EditTextDialog edit = new EditTextDialog(getContext());
                edit.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setCurrentPath(new File(edit.getText()));
                    }
                });
                edit.show();
            }
        });

        final AlertDialog d = super.create();

        onshow = new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                positive = d.getButton(DialogInterface.BUTTON_POSITIVE);
                rebuildFiles();
                // scroll to selected item
                listView.post(new Runnable() {
                    @Override
                    public void run() {
                        listView.scrollToPosition(adapter.selectedIndex);
                    }
                });
                if (neutral != null) {
                    Button n = d.getButton(DialogInterface.BUTTON_NEUTRAL);
                    n.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            neutral.onClick(d, DialogInterface.BUTTON_NEUTRAL);
                        }
                    });
                }
            }
        };

        d.setOnShowListener(onshow);
        return d;
    }

    protected void updateSelected(int i) {
        if (positive != null) {
            switch (type) {
                case FILE_DIALOG:
                    positive.setEnabled(i != -1);
                    break;
                default:
                    positive.setEnabled(true);
            }
        }
        adapter.selectedIndex = i;
    }

    // dialog to set output directory / file or readonly dialog?
    public void setReadonly(boolean b) {
        readonly = b;
    }

    // file select dialog or directory select dialog?
    public void setSelectFiles(DIALOG_TYPE type) {
        this.type = type;
    }

    public void setChangeFolderListener(Runnable r) {
        this.changeFolder = r;
    }

    @Override
    public AlertDialog.Builder setNeutralButton(CharSequence text, DialogInterface.OnClickListener listener) {
        neutral = listener;
        return super.setNeutralButton(text, listener);
    }

    @Override
    public AlertDialog.Builder setNeutralButton(int textId, DialogInterface.OnClickListener listener) {
        neutral = listener;
        return super.setNeutralButton(textId, listener);
    }

    public void setCurrentPath(File path) {
        currentPath = path;
        if (adapter != null) { // created?
            adapter.currentPath = path;
            rebuildFiles();
        }
    }

    public File getCurrentPath() {
        if (adapter == null)
            return currentPath;
        return adapter.currentPath;
    }

    public void rebuildFiles() {
        adapter.scan();

        listView.scrollToPosition(0);
        path.setText(adapter.currentPath.getPath());
        free.setText(MainApplication.formatSize(getContext(), Storage.getFree(adapter.currentPath)));
        if (changeFolder != null)
            changeFolder.run();

        if (!readonly) { // show readonly directory tooltip
            File p = adapter.currentPath;
            while (!p.exists())
                p = p.getParentFile();
            if (!p.canWrite()) {
                message.setText(R.string.filedialog_readonly);
                message.setVisibility(View.VISIBLE);
            } else {
                message.setVisibility(View.GONE);
            }
        }
    }

    protected void onChanged() {
        if (adapter.currentPath.exists() && !adapter.currentPath.isDirectory())  // file or symlink
            updateSelected(adapter.files.indexOf(adapter.currentPath));
        else
            updateSelected(-1);
    }

    public void setAdapter(FileAdapter a) {
        if (adapter != null)
            adapter.unregisterAdapterDataObserver(observer);
        adapter = a;
        if (listView != null)
            listView.setAdapter(a);
        if (a != null)
            a.registerAdapterDataObserver(observer);
    }

    @Override
    public AlertDialog.Builder setTitle(int titleId) {
        title = getContext().getString(titleId);
        return this;
    }

    @Override
    public AlertDialog.Builder setTitle(@Nullable CharSequence t) {
        title = t.toString();
        return this;
    }
}
