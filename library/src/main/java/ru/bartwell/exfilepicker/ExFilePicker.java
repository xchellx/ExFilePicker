package ru.bartwell.exfilepicker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import ru.bartwell.exfilepicker.ui.activity.ExFilePickerActivity;

/**
 * Created by BArtWell on 26.02.2017.
 */

public class ExFilePicker {

    public static final String EXTRA_RESULT = "RESULT";

    private boolean mCanChooseOnlyOneItem;
    @Nullable
    private String[] mShowOnlyExtensions;
    @Nullable
    private String[] mExceptExtensions;
    private boolean mIsNewFolderButtonDisabled;
    private boolean mIsSortButtonDisabled;
    private boolean mIsQuitButtonEnabled;
    @NonNull
    private ChoiceType mChoiceType = ChoiceType.ALL;
    @NonNull
    private SortingType mSortingType = SortingType.NAME_ASC;
    @Nullable
    private String mStartDirectory;
    private boolean mUseFirstItemAsUpEnabled;
    private boolean mHideHiddenFilesEnabled;
    @Nullable
    private String mTitle;

    public ExFilePicker setCanChooseOnlyOneItem(boolean canChooseOnlyOneItem) {
        mCanChooseOnlyOneItem = canChooseOnlyOneItem;
        return this;
    }

    public ExFilePicker setShowOnlyExtensions(@Nullable String... extension) {
        mShowOnlyExtensions = extension;
        return this;
    }

    public ExFilePicker setExceptExtensions(@Nullable String... extension) {
        mExceptExtensions = extension;
        return this;
    }

    public ExFilePicker setNewFolderButtonDisabled(boolean disabled) {
        mIsNewFolderButtonDisabled = disabled;
        return this;
    }

    public ExFilePicker setSortButtonDisabled(boolean disabled) {
        mIsSortButtonDisabled = disabled;
        return this;
    }

    public ExFilePicker setQuitButtonEnabled(boolean enabled) {
        mIsQuitButtonEnabled = enabled;
        return this;
    }

    public ExFilePicker setChoiceType(@NonNull ChoiceType choiceType) {
        mChoiceType = choiceType;
        return this;
    }

    public ExFilePicker setSortingType(@NonNull SortingType sortingType) {
        mSortingType = sortingType;
        return this;
    }

    public ExFilePicker setStartDirectory(@Nullable String startDirectory) {
        mStartDirectory = startDirectory;
        return this;
    }

    public ExFilePicker setUseFirstItemAsUpEnabled(boolean enabled) {
        mUseFirstItemAsUpEnabled = enabled;
        return this;
    }

    public ExFilePicker setHideHiddenFilesEnabled(boolean enabled) {
        mHideHiddenFilesEnabled = enabled;
        return this;
    }

    public ExFilePicker setTitle(@Nullable String title) {
        mTitle = title;
        return this;
    }

    public void start(@NonNull Activity activity, int requestCode) {
        activity.startActivityForResult(createIntent(activity), requestCode);
    }

    public void start(@NonNull android.support.v4.app.Fragment fragment, int requestCode) {
        fragment.startActivityForResult(createIntent(fragment.getContext()), requestCode);
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    public void start(@NonNull android.app.Fragment fragment, int requestCode) {
        Context context;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            context = fragment.getContext();
        } else {
            context = fragment.getActivity();
        }
        fragment.startActivityForResult(createIntent(context), requestCode);
    }

    @NonNull
    private Intent createIntent(@NonNull Context context) {
        Intent intent = new Intent(context, ExFilePickerActivity.class);
        intent.putExtra(ExFilePickerActivity.EXTRA_CAN_CHOOSE_ONLY_ONE_ITEM, mCanChooseOnlyOneItem);
        intent.putExtra(ExFilePickerActivity.EXTRA_SHOW_ONLY_EXTENSIONS, mShowOnlyExtensions);
        intent.putExtra(ExFilePickerActivity.EXTRA_EXCEPT_EXTENSIONS, mExceptExtensions);
        intent.putExtra(ExFilePickerActivity.EXTRA_IS_NEW_FOLDER_BUTTON_DISABLED, mIsNewFolderButtonDisabled);
        intent.putExtra(ExFilePickerActivity.EXTRA_IS_SORT_BUTTON_DISABLED, mIsSortButtonDisabled);
        intent.putExtra(ExFilePickerActivity.EXTRA_IS_QUIT_BUTTON_ENABLED, mIsQuitButtonEnabled);
        intent.putExtra(ExFilePickerActivity.EXTRA_CHOICE_TYPE, mChoiceType);
        intent.putExtra(ExFilePickerActivity.EXTRA_SORTING_TYPE, mSortingType);
        intent.putExtra(ExFilePickerActivity.EXTRA_START_DIRECTORY, mStartDirectory);
        intent.putExtra(ExFilePickerActivity.EXTRA_USE_FIRST_ITEM_AS_UP_ENABLED, mUseFirstItemAsUpEnabled);
        intent.putExtra(ExFilePickerActivity.EXTRA_HIDE_HIDDEN_FILES, mHideHiddenFilesEnabled);
        intent.putExtra(ExFilePickerActivity.EXTRA_TITLE, mTitle);
        return intent;
    }

    public enum ChoiceType {
        ALL, FILES, DIRECTORIES
    }

    public enum SortingType {
        NAME_ASC, NAME_DESC, SIZE_ASC, SIZE_DESC, DATE_ASC, DATE_DESC
    }
}
