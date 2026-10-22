package com.firecalc.tablet;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;

/**
 * Main Activity for FireCalc on Amazon Fire Tablets.
 * Designed with zero Google Play Services dependencies for maximum stability on Fire OS.
 */
public class MainActivity extends AppCompatActivity {

    private CalculatorViewModel viewModel;
    private Vibrator vibrator;

    private TextView tvExpression;
    private TextView tvLivePreview;
    private TextView tvResult;
    private TextView btnToggleAngleMode;
    private TextView tvMemoryBadge;
    private View scientificContainer;
    private ImageButton btnScientificToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(CalculatorViewModel.class);
        initVibrator();
        initViews();
        setupKeypadListeners();
        observeViewModel();
    }

    private void initVibrator() {
        try {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        } catch (Exception ignored) {
            vibrator = null;
        }
    }

    private void triggerHaptic(View view) {
        if (view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
        if (vibrator != null && vibrator.hasVibrator()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(15);
                }
            } catch (Exception ignored) {}
        }
    }

    private void initViews() {
        tvExpression = findViewById(R.id.tvExpression);
        tvLivePreview = findViewById(R.id.tvLivePreview);
        tvResult = findViewById(R.id.tvResult);
        btnToggleAngleMode = findViewById(R.id.btnToggleAngleMode);
        tvMemoryBadge = findViewById(R.id.tvMemoryBadge);
        scientificContainer = findViewById(R.id.scientificContainer);
        btnScientificToggle = findViewById(R.id.btnScientificToggle);

        if (btnToggleAngleMode != null) {
            btnToggleAngleMode.setOnClickListener(v -> {
                triggerHaptic(v);
                viewModel.toggleAngleMode();
            });
        }

        if (btnScientificToggle != null) {
            btnScientificToggle.setOnClickListener(v -> {
                triggerHaptic(v);
                viewModel.toggleScientificExpanded();
            });
        }

        ImageButton btnHistory = findViewById(R.id.btnHistory);
        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> {
                triggerHaptic(v);
                showHistoryDialog();
            });
        }

        // Long click on result to copy to clipboard
        if (tvResult != null) {
            tvResult.setOnLongClickListener(v -> {
                String text = tvResult.getText().toString();
                if (!text.isEmpty() && !text.equals("0")) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Calculation Result", text);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(MainActivity.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            });
        }
    }

    private void setupKeypadListeners() {
        // Digits
        setButtonInput(R.id.btn0, "0");
        setButtonInput(R.id.btn1, "1");
        setButtonInput(R.id.btn2, "2");
        setButtonInput(R.id.btn3, "3");
        setButtonInput(R.id.btn4, "4");
        setButtonInput(R.id.btn5, "5");
        setButtonInput(R.id.btn6, "6");
        setButtonInput(R.id.btn7, "7");
        setButtonInput(R.id.btn8, "8");
        setButtonInput(R.id.btn9, "9");
        setButtonInput(R.id.btnDot, ".");

        // Arithmetic
        setButtonInput(R.id.btnAdd, "+");
        setButtonInput(R.id.btnSubtract, "−");
        setButtonInput(R.id.btnMultiply, "×");
        setButtonInput(R.id.btnDivide, "÷");
        setButtonInput(R.id.btnPercent, "%");
        setButtonInput(R.id.btnParenLeft, "(");
        setButtonInput(R.id.btnParenRight, ")");

        // Actions
        View btnClear = findViewById(R.id.btnClear);
        if (btnClear != null) {
            btnClear.setOnClickListener(v -> {
                triggerHaptic(v);
                viewModel.clear();
            });
        }

        View btnBackspace = findViewById(R.id.btnBackspace);
        if (btnBackspace != null) {
            btnBackspace.setOnClickListener(v -> {
                triggerHaptic(v);
                viewModel.backspace();
            });
        }

        View btnPlusMinus = findViewById(R.id.btnPlusMinus);
        if (btnPlusMinus != null) {
            btnPlusMinus.setOnClickListener(v -> {
                triggerHaptic(v);
                viewModel.toggleSign();
            });
        }

        View btnEquals = findViewById(R.id.btnEquals);
        if (btnEquals != null) {
            btnEquals.setOnClickListener(v -> {
                triggerHaptic(v);
                viewModel.calculate();
            });
        }

        // Memory buttons
        View btnMC = findViewById(R.id.btnMC);
        if (btnMC != null) btnMC.setOnClickListener(v -> { triggerHaptic(v); viewModel.memoryClear(); });

        View btnMR = findViewById(R.id.btnMR);
        if (btnMR != null) btnMR.setOnClickListener(v -> { triggerHaptic(v); viewModel.memoryRecall(); });

        View btnMPlus = findViewById(R.id.btnMPlus);
        if (btnMPlus != null) btnMPlus.setOnClickListener(v -> { triggerHaptic(v); viewModel.memoryAdd(); });

        View btnMMinus = findViewById(R.id.btnMMinus);
        if (btnMMinus != null) btnMMinus.setOnClickListener(v -> { triggerHaptic(v); viewModel.memorySubtract(); });

        // Scientific functions
        setFunctionInput(R.id.btnSin, "sin");
        setFunctionInput(R.id.btnCos, "cos");
        setFunctionInput(R.id.btnTan, "tan");
        setFunctionInput(R.id.btnAsin, "sin⁻¹");
        setFunctionInput(R.id.btnAcos, "cos⁻¹");
        setFunctionInput(R.id.btnAtan, "tan⁻¹");
        setFunctionInput(R.id.btnLn, "ln");
        setFunctionInput(R.id.btnLog, "log");
        setFunctionInput(R.id.btnSqrt, "√");

        setButtonInput(R.id.btnPower, "^");
        setButtonInput(R.id.btnPi, "π");
        setButtonInput(R.id.btnE, "e");
        setButtonInput(R.id.btnFactorial, "!");

        // Landscape-specific power shortcuts
        setButtonInput(R.id.btnSquare, "^2");
        setButtonInput(R.id.btnCube, "^3");

        View btnInverse = findViewById(R.id.btnInverse);
        if (btnInverse != null) {
            btnInverse.setOnClickListener(v -> {
                triggerHaptic(v);
                viewModel.appendInput("1÷(");
            });
        }
    }

    private void setButtonInput(int viewId, String input) {
        View v = findViewById(viewId);
        if (v != null) {
            v.setOnClickListener(view -> {
                triggerHaptic(view);
                viewModel.appendInput(input);
            });
        }
    }

    private void setFunctionInput(int viewId, String functionName) {
        View v = findViewById(viewId);
        if (v != null) {
            v.setOnClickListener(view -> {
                triggerHaptic(view);
                viewModel.appendFunction(functionName);
            });
        }
    }

    private void observeViewModel() {
        viewModel.getExpression().observe(this, expr -> {
            if (tvExpression != null) {
                tvExpression.setText(expr.isEmpty() ? "0" : expr);
            }
        });

        viewModel.getResult().observe(this, res -> {
            if (tvResult != null) {
                tvResult.setText(res);
            }
        });

        viewModel.getLivePreview().observe(this, preview -> {
            if (tvLivePreview != null) {
                if (preview != null && !preview.isEmpty()) {
                    tvLivePreview.setText(preview);
                    tvLivePreview.setVisibility(View.VISIBLE);
                } else {
                    tvLivePreview.setVisibility(View.INVISIBLE);
                }
            }
        });

        viewModel.getAngleMode().observe(this, mode -> {
            if (btnToggleAngleMode != null) {
                btnToggleAngleMode.setText(mode == CalculatorEngine.AngleMode.DEGREE ? R.string.deg_mode : R.string.rad_mode);
            }
        });

        viewModel.getHasMemory().observe(this, hasMem -> {
            if (tvMemoryBadge != null) {
                tvMemoryBadge.setVisibility(hasMem ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getIsScientificExpanded().observe(this, isExpanded -> {
            if (scientificContainer != null) {
                scientificContainer.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void showHistoryDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_history, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        RecyclerView rvHistory = dialogView.findViewById(R.id.rvHistory);
        TextView tvEmpty = dialogView.findViewById(R.id.tvEmptyHistory);
        View btnClear = dialogView.findViewById(R.id.btnClearHistory);
        View btnClose = dialogView.findViewById(R.id.btnCloseHistory);

        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        HistoryAdapter adapter = new HistoryAdapter(item -> {
            viewModel.loadHistoryItem(item);
            dialog.dismiss();
        });
        rvHistory.setAdapter(adapter);

        viewModel.getHistory().observe(this, list -> {
            if (list == null || list.isEmpty()) {
                rvHistory.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
            } else {
                rvHistory.setVisibility(View.VISIBLE);
                tvEmpty.setVisibility(View.GONE);
                adapter.setItems(list);
            }
        });

        btnClear.setOnClickListener(v -> {
            triggerHaptic(v);
            viewModel.clearHistory();
            Toast.makeText(this, R.string.history_cleared, Toast.LENGTH_SHORT).show();
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}
