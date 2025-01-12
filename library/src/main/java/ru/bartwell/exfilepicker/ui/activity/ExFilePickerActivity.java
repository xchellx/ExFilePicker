package ru.bartwell.exfilepicker.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ru.bartwell.exfilepicker.ExFilePicker;
import ru.bartwell.exfilepicker.R;
import ru.bartwell.exfilepicker.data.ExFilePickerResult;
import ru.bartwell.exfilepicker.ui.adapter.FilesListAdapter;
import ru.bartwell.exfilepicker.ui.callback.OnListItemClickListener;
import ru.bartwell.exfilepicker.ui.dialog.NewFolderDialog;
import ru.bartwell.exfilepicker.ui.dialog.SortingDialog;
import ru.bartwell.exfilepicker.ui.dialog.StorageDialog;
import ru.bartwell.exfilepicker.utils.ListUtils;
import ru.bartwell.exfilepicker.utils.Utils;

public class ExFilePickerActivity extends AppCompatActivity implements OnListItemClickListener,
        NewFolderDialog.OnNewFolderNameEnteredListener,
        SortingDialog.OnSortingSelectedListener, StorageDialog.OnStorageSelectedListener {

    public static final String EXTRA_CAN_CHOOSE_ONLY_ONE_ITEM = "CAN_CHOOSE_ONLY_ONE_ITEM";
    public static final String EXTRA_SHOW_ONLY_EXTENSIONS = "SHOW_ONLY_EXTENSIONS";
    public static final String EXTRA_EXCEPT_EXTENSIONS = "EXCEPT_EXTENSIONS";
    public static final String EXTRA_IS_NEW_FOLDER_BUTTON_DISABLED = "IS_NEW_FOLDER_BUTTON_DISABLED";
    public static final String EXTRA_IS_SORT_BUTTON_DISABLED = "IS_SORT_BUTTON_DISABLED";
    public static final String EXTRA_IS_QUIT_BUTTON_ENABLED = "IS_QUIT_BUTTON_ENABLED";
    public static final String EXTRA_CHOICE_TYPE = "CHOICE_TYPE";
    public static final String EXTRA_SORTING_TYPE = "SORTING_TYPE";
    public static final String EXTRA_START_DIRECTORY = "START_DIRECTORY";
    public static final String EXTRA_USE_FIRST_ITEM_AS_UP_ENABLED = "USE_FIRST_ITEM_AS_UP_ENABLED";
    public static final String EXTRA_HIDE_HIDDEN_FILES = "HIDE_HIDDEN_FILES";
    public static final String EXTRA_TITLE = "TITLE";
    public static final String PERMISSION_READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE";
    private static final String DIRECTORY_STATE = "DIRECTORY_STATE";
    private static final int REQUEST_CODE_READ_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 2;
    private static final String TOP_DIRECTORY = "/";

    private boolean mCanChooseOnlyOneItem;
    @Nullable
    private String[] mShowOnlyExtensions;
    @Nullable
    private String[] mExceptExtensions;
    private boolean mIsNewFolderButtonDisabled;
    private boolean mIsSortButtonDisabled;
    private boolean mIsQuitButtonEnabled;
    @NonNull
    private ExFilePicker.ChoiceType mChoiceType = ExFilePicker.ChoiceType.ALL;
    @NonNull
    private ExFilePicker.SortingType mSortingType = ExFilePicker.SortingType.NAME_ASC;
    private File mCurrentDirectory;
    private RecyclerView mRecyclerView;
    private View mEmptyView;
    private FilesListAdapter mAdapter;
    private boolean mIsMultiChoiceModeEnabled;
    private boolean mUseFirstItemAsUpEnabled;
    private boolean mHideHiddenFiles;
    private String mTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ex_file_picker);

        handleIntent();
        setupViews();

        if (savedInstanceState != null && savedInstanceState.containsKey(DIRECTORY_STATE)) {
            mCurrentDirectory = new File(savedInstanceState.getString(DIRECTORY_STATE));
        }

        if (ContextCompat.checkSelfPermission(this, PERMISSION_READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            readDirectory(mCurrentDirectory);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{PERMISSION_READ_EXTERNAL_STORAGE}, REQUEST_CODE_READ_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_READ_EXTERNAL_STORAGE:
                readDirectory(mCurrentDirectory);
                break;
            case REQUEST_CODE_WRITE_EXTERNAL_STORAGE:
                showNewFolderDialog();
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onListItemClick(int position) {
        if (mIsMultiChoiceModeEnabled) {
            if (mCanChooseOnlyOneItem) {
                mAdapter.deselect();
            }
            mAdapter.setItemSelected(position, !mAdapter.isItemSelected(position));
        } else {
            if (position == OnListItemClickListener.POSITION_UP) {
                readUpDirectory();
            } else {
                File item = mAdapter.getItem(position);
                if (item.isDirectory()) {
                    mCurrentDirectory = new File(mCurrentDirectory, item.getName());
                    readDirectory(mCurrentDirectory);
                } else {
                    finishWithResult(mCurrentDirectory, item.getName());
                }
            }
        }
    }

    @Override
    public void onListItemLongClick(int position) {
        if (mCanChooseOnlyOneItem)
            return;
        if (!mIsMultiChoiceModeEnabled && position != OnListItemClickListener.POSITION_UP) {
            if (mChoiceType != ExFilePicker.ChoiceType.FILES || !mAdapter.getItem(position).isDirectory()) {
                mAdapter.setItemSelected(position, true);
                setMultiChoiceModeEnabled(true);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            if (mIsMultiChoiceModeEnabled) {
                setMultiChoiceModeEnabled(false);
            } else {
                finish();
            }
        } else if (itemId == R.id.ok) {
            if (mIsMultiChoiceModeEnabled) {
                finishWithResult(mCurrentDirectory, mAdapter.getSelectedItems());
            } else if (mChoiceType == ExFilePicker.ChoiceType.DIRECTORIES) {
                if (isTopDirectory(mCurrentDirectory)) {
                    finishWithResult(mCurrentDirectory, "/");
                } else {
                    finishWithResult(mCurrentDirectory, mCurrentDirectory.getName());
                }
            }
        } else if (itemId == R.id.sort) {
            SortingDialog dialog = new SortingDialog(this);
            dialog.setOnSortingSelectedListener(this);
            dialog.show();
        } else if (itemId == R.id.storage) {
            StorageDialog dialog = new StorageDialog(this);
            dialog.setOnStorageSelectedListener(this);
            dialog.show();
        } else if (itemId == R.id.new_folder) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                showNewFolderDialog();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
            }
        } else if (itemId == R.id.select_all) {
            mAdapter.selectAll();
        } else if (itemId == R.id.deselect) {
            mAdapter.deselect();
        } else if (itemId == R.id.invert_selection) {
            mAdapter.invertSelection();
        } else if (itemId == R.id.change_view) {
            toggleViewMode();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                if (mIsMultiChoiceModeEnabled) {
                    setMultiChoiceModeEnabled(false);
                } else {
                    if (isTopDirectory(mCurrentDirectory) || isStorageTopDirectory(mCurrentDirectory)) {
                        finish();
                    } else {
                        readUpDirectory();
                    }
                }
            } else if (event.getAction() == KeyEvent.ACTION_DOWN && (event.getFlags() & KeyEvent.FLAG_LONG_PRESS) == KeyEvent.FLAG_LONG_PRESS) {
                finish();
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onNewFolderNameEntered(@NonNull String name) {
        if (name.length() > 0) {
            File file = new File(mCurrentDirectory, name);
            if (file.exists()) {
                Toast.makeText(ExFilePickerActivity.this, R.string.efp__folder_already_exists, Toast.LENGTH_SHORT).show();
            } else {
                if (file.mkdir()) {
                    readDirectory(mCurrentDirectory);
                    Toast.makeText(ExFilePickerActivity.this, R.string.efp__folder_created, Toast.LENGTH_SHORT).show();
                } else
                    Toast.makeText(ExFilePickerActivity.this, R.string.efp__folder_not_created, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onSortingSelected(@NonNull ExFilePicker.SortingType sortingType) {
        mSortingType = sortingType;
        mAdapter.sort(mSortingType);
    }

    @Override
    public void onStorageSelected(String path) {
        mCurrentDirectory = new File(path);
        readDirectory(mCurrentDirectory);
    }

    private void readUpDirectory() {
        mCurrentDirectory = mCurrentDirectory.getParentFile();
        readDirectory(mCurrentDirectory);
    }

    private void readDirectory(@NonNull File directory) {
        setTitle(directory);
        mAdapter.setUseFirstItemAsUpEnabled(!isTopDirectory(directory) && !isStorageTopDirectory(directory) && mUseFirstItemAsUpEnabled);
        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            if (mUseFirstItemAsUpEnabled) {
                mRecyclerView.setVisibility(View.VISIBLE);
                mEmptyView.setVisibility(View.GONE);
                mAdapter.setItems(new ArrayList<File>(), mSortingType);
            } else {
                mEmptyView.setVisibility(View.VISIBLE);
                mRecyclerView.setVisibility(View.GONE);
            }
        } else {
            mRecyclerView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
            List<File> list = new ArrayList<>();
            ListUtils.ConditionChecker<File> checker;
            if (mShowOnlyExtensions != null && mShowOnlyExtensions.length > 0 && mChoiceType != ExFilePicker.ChoiceType.DIRECTORIES) {
                final List<String> showOnlyExtensions = Arrays.asList(mShowOnlyExtensions);
                checker = new ListUtils.ConditionChecker<File>() {
                    @Override
                    public boolean check(@NonNull File file) {
                        return file.isDirectory() || showOnlyExtensions.contains(Utils.getFileExtension(file.getName()));
                    }
                };
            } else {
                if (mChoiceType == ExFilePicker.ChoiceType.DIRECTORIES) {
                    checker = new ListUtils.ConditionChecker<File>() {
                        @Override
                        public boolean check(@NonNull File file) {
                            return file.isDirectory();
                        }
                    };
                } else {
                    checker = null;
                }
            }
            ListUtils.copyListWithCondition(files, list, checker);
            if (mExceptExtensions != null && mExceptExtensions.length > 0 && mChoiceType != ExFilePicker.ChoiceType.DIRECTORIES) {
                final List<String> exceptExtensions = Arrays.asList(mExceptExtensions);
                ListUtils.filterList(list, new ListUtils.ConditionChecker<File>() {
                    @Override
                    public boolean check(@NonNull File file) {
                        return !file.isDirectory() && exceptExtensions.contains(Utils.getFileExtension(file.getName()));
                    }
                });
            }
            if (mHideHiddenFiles) {
                ListUtils.filterList(list, new ListUtils.ConditionChecker<File>() {
                    @Override
                    public boolean check(@NonNull File file) {
                        return file.isHidden();
                    }
                });
            }
            mAdapter.setItems(list, mSortingType);
        }
    }

    private void setTitle(@NonNull File directory) {
        String title = mTitle;
        String subtitle;
        if (isTopDirectory(directory)) {
            subtitle = TOP_DIRECTORY;
        } else if (directory.getAbsolutePath().equals(Environment.getExternalStorageDirectory().getAbsolutePath())) {
            subtitle = getString(R.string.efp__internal_storage);
        } else {
            subtitle = directory.getName();
        }
        if (TextUtils.isEmpty(title)) {
            title = subtitle;
            subtitle = null;
        }
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(title);
            ab.setSubtitle(subtitle);
        }
    }

    private void handleIntent() {
        Intent intent = getIntent();
        mCanChooseOnlyOneItem = intent.getBooleanExtra(EXTRA_CAN_CHOOSE_ONLY_ONE_ITEM, false);
        mShowOnlyExtensions = intent.getStringArrayExtra(EXTRA_SHOW_ONLY_EXTENSIONS);
        mExceptExtensions = intent.getStringArrayExtra(EXTRA_EXCEPT_EXTENSIONS);
        mIsNewFolderButtonDisabled = intent.getBooleanExtra(EXTRA_IS_NEW_FOLDER_BUTTON_DISABLED, false);
        mIsSortButtonDisabled = intent.getBooleanExtra(EXTRA_IS_SORT_BUTTON_DISABLED, false);
        mIsQuitButtonEnabled = intent.getBooleanExtra(EXTRA_IS_QUIT_BUTTON_ENABLED, false);
        mChoiceType = (ExFilePicker.ChoiceType) intent.getSerializableExtra(EXTRA_CHOICE_TYPE);
        mSortingType = (ExFilePicker.SortingType) intent.getSerializableExtra(EXTRA_SORTING_TYPE);
        mCurrentDirectory = getStartDirectory(intent);
        mUseFirstItemAsUpEnabled = intent.getBooleanExtra(EXTRA_USE_FIRST_ITEM_AS_UP_ENABLED, false);
        mHideHiddenFiles = intent.getBooleanExtra(EXTRA_HIDE_HIDDEN_FILES, false);
        mTitle = intent.getStringExtra(EXTRA_TITLE);
    }

    private int calculateGridColumnsCount() {
        return (int) (getResources().getDisplayMetrics().widthPixels / getResources().getDimension(R.dimen.files_grid_item_size));
    }

    private void showNewFolderDialog() {
        NewFolderDialog dialog = new NewFolderDialog(this);
        dialog.setOnNewFolderNameEnteredListener(this);
        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        ActionBar ab = getSupportActionBar();
        if (ab != null)
            ab.setDisplayHomeAsUpEnabled(mIsMultiChoiceModeEnabled || mIsQuitButtonEnabled);
        if (mIsMultiChoiceModeEnabled) {
            getMenuInflater().inflate(R.menu.files_list_multi_choice, menu);
        } else {
            getMenuInflater().inflate(R.menu.files_list_single_choice, menu);
            MenuItem menuItem = menu.findItem(R.id.change_view);
            if (!mAdapter.isGridModeEnabled()) {
                menuItem.setIcon(Utils.attrToResId(this, R.attr.efp__ic_action_grid));
                menuItem.setTitle(R.string.efp__action_grid);
            } else {
                menuItem.setIcon(Utils.attrToResId(this, R.attr.efp__ic_action_list));
                menuItem.setTitle(R.string.efp__action_list);
            }
            menu.findItem(R.id.new_folder).setVisible(!mIsNewFolderButtonDisabled);
        }
        menu.findItem(R.id.sort).setVisible(!mIsSortButtonDisabled);
        menu.findItem(R.id.ok).setVisible(mChoiceType == ExFilePicker.ChoiceType.DIRECTORIES || mIsMultiChoiceModeEnabled);
        return super.onCreateOptionsMenu(menu);
    }

    private void toggleViewMode() {
        if (mAdapter.isGridModeEnabled()) {
            mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            mAdapter.setGridModeEnabled(false);
        } else {
            mRecyclerView.setLayoutManager(new GridLayoutManager(this, calculateGridColumnsCount()));
            mAdapter.setGridModeEnabled(true);
        }
        invalidateOptionsMenu();
    }

    private void setMultiChoiceModeEnabled(boolean enabled) {
        if (mIsMultiChoiceModeEnabled == enabled)
            return;
        mIsMultiChoiceModeEnabled = enabled;
        mAdapter.setUseFirstItemAsUpEnabled(!enabled && mUseFirstItemAsUpEnabled && !isTopDirectory(mCurrentDirectory));
        mAdapter.setMultiChoiceModeEnabled(enabled);
        invalidateOptionsMenu();
    }

    private boolean isTopDirectory(@Nullable File directory) {
        return directory != null && TOP_DIRECTORY.equals(directory.getAbsolutePath());
    }

    private boolean isStorageTopDirectory(@Nullable File directory) {

        if (directory == null)
            return false;

        List<String> paths = Utils.getExternalStoragePaths(this);
        for (String path : paths) {
            if (directory.getAbsolutePath().equals(path))
                return true;
        }

        return false;
    }

    private void finishWithResult(@NonNull File path, @NonNull String file) {
        finishWithResult(path, new ArrayList<>(Collections.singletonList(file)));
    }

    private void finishWithResult(@NonNull File path, @NonNull List<String> files) {
        String resultPath = path.getAbsolutePath();
        if (!resultPath.endsWith("/")) {
            resultPath += "/";
        }
        ExFilePickerResult result = new ExFilePickerResult(resultPath, files);
        Intent intent = new Intent();
        intent.putExtra(ExFilePicker.EXTRA_RESULT, result);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void setupViews() {
        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new FilesListAdapter();
        mAdapter.setOnListItemClickListener(this);
        mAdapter.setCanChooseOnlyFiles(mChoiceType == ExFilePicker.ChoiceType.FILES);
        mAdapter.setUseFirstItemAsUpEnabled(mUseFirstItemAsUpEnabled);
        mRecyclerView.setAdapter(mAdapter);

        mEmptyView = findViewById(R.id.empty_view);

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setHomeAsUpIndicator(Utils.attrToResId(this, R.attr.efp__ic_action_cancel));
        }
    }

    @NonNull
    private File getStartDirectory(@NonNull Intent intent) {
        File path = null;
        String startPath = intent.getStringExtra(EXTRA_START_DIRECTORY);
        if (startPath != null && startPath.length() > 0) {
            File tmp = new File(startPath);
            if (tmp.exists()) {
                path = tmp;
                if (tmp.isFile()) {
                    path = tmp.getParentFile();
                }
            }
        }
        if (path == null) {
            List<String> storages = Utils.getExternalStoragePaths(this);
            if (storages.size() > 0) {
                path = new File(storages.get(0));
            }
        }
        if (path == null) {
            path = new File("/");
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                path = Environment.getExternalStorageDirectory();
            }
        }
        return path;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString(DIRECTORY_STATE, mCurrentDirectory.getAbsolutePath());
    }
}
