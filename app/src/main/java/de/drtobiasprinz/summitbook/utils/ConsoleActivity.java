package de.drtobiasprinz.summitbook.utils;

import android.app.Application;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProviders;

import de.drtobiasprinz.summitbook.R;

public abstract class ConsoleActivity extends AppCompatActivity
        implements ViewTreeObserver.OnGlobalLayoutListener, ViewTreeObserver.OnScrollChangedListener {

    private EditText etInput;
    private ScrollView svOutput;
    private TextView tvOutput;
    private int outputWidth = -1, outputHeight = -1;
    Context context;

    enum Scroll {
        TOP, BOTTOM
    }

    private Scroll scrollRequest;

    public static class ConsoleModel extends ViewModel {
        boolean pendingNewline = false;  // Prevent empty line at bottom of screen
        int scrollChar = 0;              // Character offset of the top visible line.
        int scrollAdjust = 0;            // Pixels by which that line is scrolled above the top
        //   (prevents movement when keyboard hidden/shown).
    }

    private ConsoleModel consoleModel;

    protected Task task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        consoleModel = ViewModelProviders.of(this).get(ConsoleModel.class);
        task = ViewModelProviders.of(this).get(getTaskClass());
        setContentView(resId("layout", "activity_console"));
        createInput();
        createOutput();
        Utils.fixEdgeToEdge(findViewById(R.id.constraintLayout));
    }

    protected abstract Class<? extends Task> getTaskClass();

    private void createInput() {
        etInput = findViewById(resId("id", "etInput"));

        // Strip formatting from pasted text.
        etInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void afterTextChanged(Editable e) {
                for (CharacterStyle cs : e.getSpans(0, e.length(), CharacterStyle.class)) {
                    e.removeSpan(cs);
                }
            }
        });

        // At least on API level 28, if an ACTION_UP is lost during a rotation, then the app
        // (or any other app which takes focus) will receive an endless stream of ACTION_DOWNs
        // until the key is pressed again. So we react to ACTION_UP instead.
        etInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getAction() == KeyEvent.ACTION_UP)) {
                String text = etInput.getText().toString() + "\n";
                etInput.setText("");
                output(span(text, new StyleSpan(Typeface.BOLD)));
                scrollTo(Scroll.BOTTOM);
                task.onInput(text);
            }

            // If we return false on ACTION_DOWN, we won't be given the ACTION_UP.
            return true;
        });

        task.inputEnabled.observe(this, enabled -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (Boolean.TRUE.equals(enabled)) {
                etInput.setVisibility(View.VISIBLE);
                etInput.setEnabled(true);

                // requestFocus alone doesn't always bring up the soft keyboard during startup
                // on the Nexus 4 with API level 22: probably some race condition. (After
                // rotation with input *already* enabled, the focus may be overridden by
                // onRestoreInstanceState, which will run after this observer.)
                etInput.requestFocus();
                imm.showSoftInput(etInput, InputMethodManager.SHOW_IMPLICIT);
            } else {
                // Disable rather than hide, otherwise tvOutput gets a gray background on API
                // level 26, like tvCaption in the main menu when you press an arrow key.
                etInput.setEnabled(false);
                imm.hideSoftInputFromWindow(tvOutput.getWindowToken(), 0);
            }
        });
    }

    private void createOutput() {
        svOutput = findViewById(resId("id", "svOutput"));
        svOutput.getViewTreeObserver().addOnGlobalLayoutListener(this);

        tvOutput = findViewById(resId("id", "tvOutput"));
        // noinspection WrongConstant
        tvOutput.setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE);
        // Don't start observing task.output yet: we need to restore the scroll position first so
        // we maintain the scrolled-to-bottom state.
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        // Don't restore the UI state unless we have the non-UI state as well.
        if (task.getState() != Thread.State.NEW) {
            super.onRestoreInstanceState(savedInstanceState);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Needs to be in onResume rather than onStart because onRestoreInstanceState runs
        // between them.
        if (task.getState() == Thread.State.NEW) {
            task.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveScroll();  // Necessary to save bottom position in case we've never scrolled.
    }

    // This callback is run after onResume, after each layout pass. If a view's size, position
    // or visibility has changed, the new values will be visible here.
    @Override
    public void onGlobalLayout() {
        if (outputWidth != svOutput.getWidth() || outputHeight != svOutput.getHeight()) {
            // Can't register this listener in onCreate on API level 15
            // (https://stackoverflow.com/a/35054919).
            if (outputWidth == -1) {
                svOutput.getViewTreeObserver().addOnScrollChangedListener(this);
            }

            // Either we've just started up, or the keyboard has been hidden or shown.
            outputWidth = svOutput.getWidth();
            outputHeight = svOutput.getHeight();
            restoreScroll();
        } else if (scrollRequest != null) {
            int y = switch (scrollRequest) {
                case TOP -> 0;
                case BOTTOM -> tvOutput.getHeight();
            };

            // Don't use smooth scroll, because if an output call happens while it's animating
            // towards the bottom, isScrolledToBottom will believe we've left the bottom and
            // auto-scrolling will stop. Don't use fullScroll either, because not only does it use
            // smooth scroll, it also grabs focus.
            svOutput.scrollTo(0, y);
            scrollRequest = null;
        }
    }

    @Override
    public void onScrollChanged() {
        saveScroll();
    }

    // After a rotation, a ScrollView will restore the previous pixel scroll position. However, due
    // to re-wrapping, this may result in a completely different piece of text being visible. We'll
    // try to maintain the text position of the top line, unless the view is scrolled to the bottom,
    // in which case we'll maintain that. Maintaining the bottom line will also cause a scroll
    // adjustment when the keyboard's hidden or shown.
    private void saveScroll() {
        if (isScrolledToBottom()) {
            consoleModel.scrollChar = tvOutput.getText().length();
            consoleModel.scrollAdjust = 0;
        } else {
            int scrollY = svOutput.getScrollY();
            Layout layout = tvOutput.getLayout();
            if (layout != null) {  // See note in restoreScroll
                int line = layout.getLineForVertical(scrollY);
                consoleModel.scrollChar = layout.getLineStart(line);
                consoleModel.scrollAdjust = scrollY - layout.getLineTop(line);
            }
        }
    }

    private void restoreScroll() {
        removeCursor();

        // getLayout sometimes returns null even when called from onGlobalLayout. The
        // documentation says this can happen if the "text or width has recently changed", but
        // does not define "recently". See Electron Cash issues #1330 and #1592.
        Layout layout = tvOutput.getLayout();
        if (layout != null) {
            int line = layout.getLineForOffset(consoleModel.scrollChar);
            svOutput.scrollTo(0, layout.getLineTop(line) + consoleModel.scrollAdjust);
        }

        // If we are now scrolled to the bottom, we should stick there. (scrollTo probably won't
        // trigger onScrollChanged unless the scroll actually changed.)
        saveScroll();

        task.output.removeObservers(this);
        task.output.observe(this, this::output);
    }

    private boolean isScrolledToBottom() {
        int visibleHeight = (svOutput.getHeight() - svOutput.getPaddingTop() -
                svOutput.getPaddingBottom());
        int maxScroll = Math.max(0, tvOutput.getHeight() - visibleHeight);
        return (svOutput.getScrollY() >= maxScroll);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater mi = getMenuInflater();
        mi.inflate(resId("menu", "top_bottom"), menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == resId("id", "menu_top")) {
            scrollTo(Scroll.TOP);
        } else if (id == resId("id", "menu_bottom")) {
            scrollTo(Scroll.BOTTOM);
        } else {
            return false;
        }
        return true;
    }

    public static Spannable span(CharSequence text, Object... spans) {
        Spannable spanText = new SpannableStringBuilder(text);
        for (Object span : spans) {
            spanText.setSpan(span, 0, text.length(), 0);
        }
        return spanText;
    }

    private void output(CharSequence text) {
        removeCursor();
        if (consoleModel.pendingNewline) {
            tvOutput.append("\n");
            consoleModel.pendingNewline = false;
        }
        if (text.charAt(text.length() - 1) == '\n') {
            tvOutput.append(text.subSequence(0, text.length() - 1));
            consoleModel.pendingNewline = true;
        } else {
            tvOutput.append(text);
        }

        Editable scrollback = (Editable) tvOutput.getText();
        // Because tvOutput has freezesText enabled, letting it get too large can cause a
        // TransactionTooLargeException. The limit isn't in the saved state itself, but in the
        // Binder transaction which transfers it to the system server. So it doesn't happen if
        // you're rotating the screen, but it does happen when you press Back.
        //
        // The exception message shows the size of the failed transaction, so I can determine from
        // experiment that the limit is about 500 KB, and each character consumes 4 bytes.
        int MAX_SCROLL_BACK_LEN = 100000;
        if (scrollback.length() > MAX_SCROLL_BACK_LEN) {
            scrollback.delete(0, MAX_SCROLL_BACK_LEN / 10);
        }

        // Changes to the TextView height won't be reflected by getHeight until after the
        // next layout pass, so isScrolledToBottom is safe here.
        if (isScrolledToBottom()) {
            scrollTo(Scroll.BOTTOM);
        }
    }

    // Don't actually scroll until the next onGlobalLayout, when we'll know what the new TextView
    // height is.
    private void scrollTo(Scroll request) {
        // The "top" button should take priority over an auto-scroll.
        if (scrollRequest != Scroll.TOP) {
            scrollRequest = request;
            svOutput.requestLayout();
        }
    }

    // Because we've set textIsSelectable, the TextView will create an invisible cursor (i.e. a
    // zero-length selection) during startup, and re-create it if necessary whenever the user taps
    // on the view. When a TextView is focused and it has a cursor, it will adjust its containing
    // ScrollView whenever the text changes in an attempt to keep the cursor on-screen.
    // textIsSelectable implies focusable, so if there are no other focusable views in the layout,
    // then it will always be focused.
    //
    // To avoid interference from this, we'll remove any cursor before we adjust the scroll.
    // A non-zero-length selection is left untouched and may affect the scroll in the normal way,
    // which is fine because it'll only exist if the user deliberately created it.
    private void removeCursor() {
        Spannable text = (Spannable) tvOutput.getText();
        int selStart = Selection.getSelectionStart(text);
        int selEnd = Selection.getSelectionEnd(text);

        // When textIsSelectable is set, the buffer type after onRestoreInstanceState is always
        // Spannable, regardless of the value of bufferType. It would then become Editable (and
        // have a cursor added), during the first call to append(). Make that happen now so we can
        // remove the cursor before append() is called.
        if (!(text instanceof Editable)) {
            tvOutput.setText(text, TextView.BufferType.EDITABLE);
            text = (Editable) tvOutput.getText();

            // setText removes any existing selection, at least on API level 26.
            if (selStart >= 0) {
                Selection.setSelection(text, selStart, selEnd);
            }
        }

        if (selStart >= 0 && selStart == selEnd) {
            Selection.removeSelection(text);
        }

    }

    public int resId(String type, String name) {
        return Utils.resId(this, type, name);
    }

    // =============================================================================================

    public static abstract class Task extends AndroidViewModel {

        private Thread.State state = Thread.State.NEW;

        public void start() {
            new Thread(() -> {
                try {
                    Task.this.run();
                    output(spanColor("[Finished]", resId("color", "console_meta")));
                } finally {
                    inputEnabled.postValue(false);
                    state = Thread.State.TERMINATED;
                }
            }).start();
            state = Thread.State.RUNNABLE;
        }

        public Thread.State getState() {
            return state;
        }

        public MutableLiveData<Boolean> inputEnabled = new MutableLiveData<>();
        public BufferedLiveEvent<CharSequence> output = new BufferedLiveEvent<>();

        public Task(Application app) {
            super(app);
            inputEnabled.setValue(false);
        }

        /**
         * Override this method to provide the task's implementation. It will be called on a
         * background thread.
         */
        public abstract void run();

        /**
         * Called on the UI thread each time the user enters some input, A trailing newline is
         * always included. The base class implementation does nothing.
         */
        public void onInput(String text) {
        }

        public void output(final CharSequence text) {
            if (text.length() == 0) return;
            output.postValue(text);
        }

        public void outputError(CharSequence text) {
            output(spanColor(text, resId("color", "console_error")));
        }

        public Spannable spanColor(CharSequence text, int colorId) {
            int color = ContextCompat.getColor(this.getApplication(), colorId);
            return span(text, new ForegroundColorSpan(color));
        }

        public int resId(String type, String name) {
            return Utils.resId(getApplication(), type, name);
        }
    }

}
