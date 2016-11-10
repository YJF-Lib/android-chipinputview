package cn.com.jfyuan.mail.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.com.jfyuan.mail.R;

public class ChipInputView extends ScrollView {

    private static final String TAG = "ChipInputView";
    private static final int CHIP_HEIGHT = 25; // dp
    private static final int SPACING_LEFT = 4; // dp
    private static final int SPACING_TOP = 4; // dp
    private static final int SPACING_RIGHT = 4; // dp
    private static final int SPACING_BOTTOM = 4; // dp
    public static final int DEFAULT_VERTICAL_SPACING = 4; // dp
    private int mVerticalSpacing = DEFAULT_VERTICAL_SPACING;
    private int mChipsTextColor = Color.BLACK;
    private int mChipsTextColorClicked = Color.WHITE;
    private int mChipsTextColorErrorClicked = Color.RED;
    private float mDensity;
    private RelativeLayout mChipsContainer;
    private ChipsListener mChipsListener;
    private ChipsEditText mEditText;
    private ChipsVerticalLinearLayout mRootChipsLayout;
    private EditTextListener mEditTextListener;
    private List<Chip> mChipList = new ArrayList<>();
    private Object mCurrentEditTextSpan;
    private ChipValidator mChipsValidator;

    public ChipInputView(Context context) {
        super(context);
        init();
    }

    public ChipInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ChipInputView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        return true;
    }

    private void init() {
        mDensity = getResources().getDisplayMetrics().density;
        mChipsContainer = new RelativeLayout(getContext());
        addView(mChipsContainer);
        LinearLayout linearLayout = new LinearLayout(getContext());
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(0, 0);
        linearLayout.setLayoutParams(params);
        linearLayout.setFocusable(true);
        linearLayout.setFocusableInTouchMode(true);
        mChipsContainer.addView(linearLayout);
        mEditText = new ChipsEditText(getContext());
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.leftMargin = (int) (SPACING_LEFT * mDensity);
        layoutParams.topMargin = (int) (SPACING_TOP * mDensity);
        layoutParams.rightMargin = (int) (SPACING_RIGHT * mDensity);
        layoutParams.bottomMargin = (int) (SPACING_BOTTOM * mDensity);
        mEditText.setLayoutParams(layoutParams);
        mEditText.setMinHeight((int) (CHIP_HEIGHT * mDensity));
        mEditText.setPadding(0, 0, 0, 0);
        mEditText.setLineSpacing(mVerticalSpacing, (CHIP_HEIGHT * mDensity) / mEditText.getLineHeight());
        mEditText.setBackgroundColor(Color.argb(0, 0, 0, 0));
        mEditText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_ACTION_UNSPECIFIED);
        mEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        mChipsContainer.addView(mEditText);
        mRootChipsLayout = new ChipsVerticalLinearLayout(getContext(), mVerticalSpacing);
        mRootChipsLayout.setOrientation(LinearLayout.VERTICAL);
        mRootChipsLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mRootChipsLayout.setPadding(0, (int) ((SPACING_TOP + mVerticalSpacing) * mDensity), 0, 0);
        mChipsContainer.addView(mRootChipsLayout);
        initListener();
    }

    private void initListener() {
        mChipsContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditText.requestFocus();
                unSelectAllChips();
            }
        });

        mEditTextListener = new EditTextListener();
        mEditText.addTextChangedListener(mEditTextListener);
        mEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    unSelectAllChips();
                }
            }
        });
    }

    public void addChip(String displayName, Contact contact) {
        addChip(displayName, contact, false, false);
        mEditText.setText("");
        addLeadingMarginSpan();
    }

    public void addChip(String displayName, Contact contact, boolean isIndelible, boolean isModifiable) {
        Chip chip = new Chip(displayName, contact, isIndelible, isModifiable);
        mChipList.add(chip);
        onChipsChanged(true);
        post(new Runnable() {
            @Override
            public void run() {
                fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    public void addChips(List<Contact> contacts, boolean isIndelible, boolean isModifiable) {
        if (null != contacts) {
            for (Contact c : contacts) {
                Chip chip = new Chip(c.getDisplayName(), c, isIndelible, isModifiable);
                mChipList.add(chip);
            }
        }
        onChipsChanged(true);
        post(new Runnable() {
            @Override
            public void run() {
                fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    public void clearAllChips() {
        mChipList.clear();
        onChipsChanged(false);
    }

    public boolean removeChipBy(Contact contact) {
        for (int i = 0; i < mChipList.size(); i++) {
            if (mChipList.get(i).mContact != null && mChipList.get(i).mContact.equals(contact)) {
                mChipList.remove(i);
                onChipsChanged(true);
                return true;
            }
        }
        return false;
    }

    public List<Chip> getChips() {
        return Collections.unmodifiableList(mChipList);
    }

    public boolean hasErrorChip() {
        List<Chip> chips = getChips();
        if (null != chips) {
            for (Chip chip : chips) {
                if (chip.isError) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setChipsListener(ChipsListener chipsListener) {
        this.mChipsListener = chipsListener;
    }

    public void setChipsValidator(ChipValidator chipsValidator) {
        mChipsValidator = chipsValidator;
    }

    public EditText getEditText() {
        return mEditText;
    }

    private void onChipsChanged(final boolean moveCursor) {
        ChipsVerticalLinearLayout.TextLineParams textLineParams = mRootChipsLayout.onChipsChanged(mChipList);
        if (textLineParams == null) {
            post(new Runnable() {
                @Override
                public void run() {
                    onChipsChanged(moveCursor);
                }
            });
            return;
        }
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mEditText.getLayoutParams();
        params.topMargin = (int) ((SPACING_TOP + textLineParams.row * CHIP_HEIGHT) * mDensity) + textLineParams.row * mVerticalSpacing;
        mEditText.setLayoutParams(params);
        addLeadingMarginSpan(textLineParams.lineMargin);
        if (moveCursor) {
            mEditText.setSelection(mEditText.length());
        }
    }

    private void addLeadingMarginSpan(int margin) {
        Spannable spannable = mEditText.getText();
        if (mCurrentEditTextSpan != null) {
            spannable.removeSpan(mCurrentEditTextSpan);
        }
        mCurrentEditTextSpan = new android.text.style.LeadingMarginSpan.LeadingMarginSpan2.Standard(margin, 0);
        spannable.setSpan(mCurrentEditTextSpan, 0, 0, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

        mEditText.setText(spannable);
    }

    private void addLeadingMarginSpan() {
        Spannable spannable = mEditText.getText();
        if (mCurrentEditTextSpan != null) {
            spannable.removeSpan(mCurrentEditTextSpan);
        }
        spannable.setSpan(mCurrentEditTextSpan, 0, 0, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        mEditText.setText(spannable);
    }

    private void onEnterPressed(String text) {
        if (text != null && text.length() > 0) {
            onEmailRecognized(text);
            mEditText.setSelection(0);
        }
    }

    private void onEmailRecognized(String email) {
        onEmailRecognized(new Contact(email, null, email));
    }

    private void onEmailRecognized(Contact contact) {
        Chip chip = new Chip(contact.getDisplayName(), contact, false, true);
        mChipList.add(chip);
        if (mChipsListener != null) {
            mChipsListener.onChipAdded(chip);
        }
        post(new Runnable() {
            @Override
            public void run() {
                onChipsChanged(true);
            }
        });
    }

    private void selectOrDeleteLastChip() {
        if (mChipList.size() > 0) {
            onChipInteraction(mChipList.size() - 1);
        }
    }

    private void onChipInteraction(int position) {
        try {
            Chip chip = mChipList.get(position);
            if (chip != null) {
                onChipInteraction(chip, true);
            }
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "Out of bounds", e);
        }
    }

    private void onChipInteraction(Chip chip, boolean nameClicked) {
        unSelectChipsExcept(chip);
        if (chip.isSelected()) {
            mChipList.remove(chip);
            if (mChipsListener != null) {
                mChipsListener.onChipDeleted(chip);
            }
            onChipsChanged(true);
            if (nameClicked && chip.isModifiable()) {
                mEditText.setText(chip.getContact().getEmailAddress());
                addLeadingMarginSpan();
                mEditText.requestFocus();
                mEditText.setSelection(mEditText.length());
            }
        } else {
            chip.setSelected(true);
            onChipsChanged(false);
        }
    }

    private void unSelectChipsExcept(Chip rootChip) {
        for (Chip chip : mChipList) {
            if (chip != rootChip) {
                chip.setSelected(false);
            }
        }
        onChipsChanged(false);
    }

    private void unSelectAllChips() {
        unSelectChipsExcept(null);
    }

    public InputConnection getInputConnection(InputConnection target) {
        return new KeyInterceptingInputConnection(target);
    }

    private class EditTextListener implements TextWatcher {
        private boolean mIsPasteTextChange = false;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (count > 1) {
                mIsPasteTextChange = true;
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (mIsPasteTextChange) {
                mIsPasteTextChange = false;
                // copy/paste
            } else {
                // no paste text change
                if (s.toString().contains("\n")) {
                    String text = s.toString();
                    text = text.replace("\n", "");
                    while (text.contains("  ")) {
                        text = text.replace("  ", " ");
                    }
                    s.clear();
                    if (text.length() > 1) {
                        onEnterPressed(text);
                    } else {
                        s.append(text);
                    }
                }
            }
        }
    }

    private class KeyInterceptingInputConnection extends InputConnectionWrapper {
        public KeyInterceptingInputConnection(InputConnection target) {
            super(target, true);
        }

        @Override
        public boolean commitText(CharSequence text, int newCursorPosition) {
            return super.commitText(text, newCursorPosition);
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            if (mEditText.length() == 0) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                        selectOrDeleteLastChip();
                        return true;
                    }
                }
            }
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                mEditText.append("\n");
                return true;
            }
            return super.sendKeyEvent(event);
        }

        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            if (mEditText.length() == 0 && beforeLength == 1 && afterLength == 0) {
                return sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                        && sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
            }
            return super.deleteSurroundingText(beforeLength, afterLength);
        }
    }

    class ChipsEditText extends EditText {
        public ChipsEditText(Context context) {
            super(context);
        }

        @Override
        public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
            return getInputConnection(super.onCreateInputConnection(outAttrs));
        }
    }

    class ChipsVerticalLinearLayout extends LinearLayout {
        private List<LinearLayout> mLineLayouts = new ArrayList<>();
        private int mRowSpacing;

        public ChipsVerticalLinearLayout(Context context, int rowSpacing) {
            super(context);
            mRowSpacing = rowSpacing;
            init();
        }

        private void init() {
            setOrientation(VERTICAL);
        }

        private int getViewMeasuredWidth(View view) {
            if (null != view) {
                view.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                return view.getMeasuredWidth();
            }
            return 0;
        }

        public TextLineParams onChipsChanged(List<ChipInputView.Chip> chips) {
            clearChipsViews();
            int width = getWidth();
            if (width == 0) {
                return null;
            }
            int widthSum = 0;//当前行的宽度
            int rowCounter = 0;//行数
            LinearLayout ll = createHorizontalView();
            for (ChipInputView.Chip chip : chips) {
                View chipView = chip.getView();
                int chipViewWidth = getViewMeasuredWidth(chipView);
                if (widthSum + chipViewWidth > width) {//宽度总和大于当前宽度，新起一行
                    rowCounter++;
                    widthSum = 0;
                    ll = createHorizontalView();
                }
                if (chipViewWidth > width) {//单个宽度大于当前宽度，重置文本框宽度防止超出界面
                    chip.resetLabelWidth((int) (width * 0.75));
                    chipViewWidth = getViewMeasuredWidth(chipView);
                }
                widthSum += chipViewWidth;
                ll.addView(chipView);
            }
            if (width - widthSum < width * 0.1f) {
                widthSum = 0;
                rowCounter++;
            }
            if (width == 0) {
                rowCounter = 0;
            }
            return new TextLineParams(rowCounter, widthSum);
        }

        private LinearLayout createHorizontalView() {
            LinearLayout ll = new LinearLayout(getContext());
            ll.setPadding(0, 0, 0, mRowSpacing);
            ll.setOrientation(HORIZONTAL);
            addView(ll);
            mLineLayouts.add(ll);
            return ll;
        }

        private void clearChipsViews() {
            for (LinearLayout linearLayout : mLineLayouts) {
                linearLayout.removeAllViews();
            }
            mLineLayouts.clear();
            removeAllViews();
        }

        class TextLineParams {
            public int row;
            public int lineMargin;

            public TextLineParams(int row, int lineMargin) {
                this.row = row;
                this.lineMargin = lineMargin;
            }
        }
    }

    public class Chip implements OnClickListener {
        private String mLabel;
        private final Contact mContact;
        private final boolean mIsIndelible;
        private final boolean mIsModifiable;
        private RelativeLayout mView;
        private View mIconWrapper;
        private TextView mTextView;
        private ImageView mCloseIcon;
        private ImageView mErrorIcon;
        private boolean isError = false;
        private boolean mIsSelected = false;

        public Chip(String label, Contact contact, boolean isIndelible, boolean isModifiable) {
            this.mLabel = label;
            this.mContact = contact;
            this.mIsIndelible = isIndelible;
            this.mIsModifiable = isModifiable;
            if (null == contact || (mChipsValidator != null && !mChipsValidator.isValid(mContact))) {
                isError = true;
            }
            if (mLabel == null) {
                mLabel = contact.getEmailAddress();
            }
        }

        public void resetLabelWidth(int width) {
            if (null != mTextView) {
                mTextView.setWidth(width);
            }
        }

        public View getView() {
            if (mView == null) {
                mView = (RelativeLayout) inflate(getContext(), R.layout.chips_view, null);
                mView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int) (CHIP_HEIGHT * mDensity)));
                mIconWrapper = mView.findViewById(R.id.layout_icon_wrapper);
                mTextView = (TextView) mView.findViewById(R.id.tv_ch_name);
                mCloseIcon = (ImageView) mView.findViewById(R.id.iv_ch_close);
                mErrorIcon = (ImageView) mView.findViewById(R.id.iv_ch_error);
                mTextView.setTextColor(mChipsTextColor);
                mView.setOnClickListener(this);
                mIconWrapper.setOnClickListener(this);
            }
            updateViews();
            return mView;
        }

        private void updateViews() {
            mTextView.setText(mLabel);
            if (isSelected()) {
                mIconWrapper.setVisibility(VISIBLE);
                if (isError) {
                    mView.setSelected(true);
                    mTextView.setTextColor(mChipsTextColorErrorClicked);
                } else {
                    mView.setSelected(true);
                    mTextView.setTextColor(mChipsTextColorClicked);
                }
            } else {
                mIconWrapper.setVisibility(GONE);
                if (isError) {
                    mErrorIcon.setVisibility(View.VISIBLE);
                } else {
                    mErrorIcon.setVisibility(View.GONE);
                }
                mView.setSelected(false);
                mTextView.setTextColor(mChipsTextColor);
            }
        }

        @Override
        public void onClick(View v) {
            mEditText.clearFocus();
            if (v.getId() == mView.getId()) {
                onChipInteraction(this, true);
            } else {
                onChipInteraction(this, false);
            }
        }

        public boolean isSelected() {
            return mIsSelected;
        }

        public void setSelected(boolean isSelected) {
            if (mIsIndelible) {
                return;
            }
            this.mIsSelected = isSelected;
        }

        public boolean isModifiable() {
            return mIsModifiable;
        }

        public Contact getContact() {
            return mContact;
        }

        @Override
        public boolean equals(Object o) {
            if (mContact != null && o instanceof Contact) {
                return mContact.equals(o);
            }
            return super.equals(o);
        }

    }

    public interface ChipsListener {
        void onChipAdded(Chip chip);

        void onChipDeleted(Chip chip);
    }

    public static abstract class ChipValidator {
        public abstract boolean isValid(Contact contact);
    }

    public static class Contact {
        private String mEmailAddress;
        private String mDisplayName;
        private String mId;

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Contact contact = (Contact) o;
            if (null == mId) {
                if (null != contact.getId()) return false;
                if (null == mEmailAddress) {
                    if (null != contact.getEmailAddress()) return false;
                } else {
                    if (!mEmailAddress.equals(contact.mEmailAddress)) return false;
                }
            } else {
                if (!mId.equals(contact.mId)) return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            if (null == mEmailAddress) {
                if (null == mId) {
                    return 31;
                } else {
                    return mId.hashCode();
                }
            } else {
                return mEmailAddress.hashCode();
            }
        }

        public Contact(String displayName, String id, String emailAddress) {
            mEmailAddress = emailAddress;
            mId = id;
            if (!TextUtils.isEmpty(displayName)) {
                mDisplayName = displayName;
            } else if (!TextUtils.isEmpty(emailAddress)) {
                mDisplayName = mEmailAddress;
            } else {
                mDisplayName = "None";
            }
        }

        public String getEmailAddress() {
            return mEmailAddress;
        }

        public String getDisplayName() {
            return mDisplayName;
        }

        public String getId() {
            return mId;
        }
    }
}
