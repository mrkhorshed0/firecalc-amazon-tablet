package com.firecalc.tablet;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ViewModel managing calculation state, orientation changes, and history tape.
 * Preserves user calculations across Fire Tablet portrait/landscape rotations.
 */
public class CalculatorViewModel extends ViewModel {

    private final CalculatorEngine engine = new CalculatorEngine();

    private final MutableLiveData<String> expression = new MutableLiveData<>("");
    private final MutableLiveData<String> result = new MutableLiveData<>("0");
    private final MutableLiveData<String> livePreview = new MutableLiveData<>("");
    private final MutableLiveData<CalculatorEngine.AngleMode> angleMode = new MutableLiveData<>(CalculatorEngine.AngleMode.DEGREE);
    private final MutableLiveData<Boolean> hasMemory = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isScientificExpanded = new MutableLiveData<>(false);
    private final MutableLiveData<List<HistoryItem>> history = new MutableLiveData<>(new ArrayList<>());

    private boolean isNewCalculation = true;

    public LiveData<String> getExpression() { return expression; }
    public LiveData<String> getResult() { return result; }
    public LiveData<String> getLivePreview() { return livePreview; }
    public LiveData<CalculatorEngine.AngleMode> getAngleMode() { return angleMode; }
    public LiveData<Boolean> getHasMemory() { return hasMemory; }
    public LiveData<Boolean> getIsScientificExpanded() { return isScientificExpanded; }
    public LiveData<List<HistoryItem>> getHistory() { return history; }

    public void toggleScientificExpanded() {
        Boolean curr = isScientificExpanded.getValue();
        isScientificExpanded.setValue(curr == null || !curr);
    }

    public void toggleAngleMode() {
        engine.toggleAngleMode();
        angleMode.setValue(engine.getAngleMode());
        updateLivePreview();
    }

    public void appendInput(String text) {
        String curr = expression.getValue() != null ? expression.getValue() : "";

        if (isNewCalculation && isOperator(text)) {
            // Continue with previous result
            String lastResult = result.getValue();
            if (lastResult != null && !lastResult.equals("0") && !lastResult.startsWith("Error") && !lastResult.startsWith("Invalid") && !lastResult.startsWith("Cannot")) {
                curr = lastResult.replace(",", "");
            }
            isNewCalculation = false;
        } else if (isNewCalculation) {
            curr = "";
            isNewCalculation = false;
        }

        // Prevent invalid double operators like ++ or ×÷
        if (!curr.isEmpty() && isOperator(text)) {
            char lastChar = curr.charAt(curr.length() - 1);
            if (isOperatorChar(lastChar) && !text.equals("-")) {
                curr = curr.substring(0, curr.length() - 1);
            }
        }

        curr += text;
        expression.setValue(curr);
        updateLivePreview();
    }

    public void appendFunction(String functionName) {
        String curr = expression.getValue() != null ? expression.getValue() : "";
        if (isNewCalculation) {
            curr = "";
            isNewCalculation = false;
        }

        curr += functionName + "(";
        expression.setValue(curr);
        updateLivePreview();
    }

    public void backspace() {
        String curr = expression.getValue();
        if (curr != null && !curr.isEmpty()) {
            // Check for multi-letter function prefixes (e.g., sin(, cos(, tan(, log(, ln()
            if (curr.endsWith("sin(") || curr.endsWith("cos(") || curr.endsWith("tan(") || curr.endsWith("log(") || curr.endsWith("sqrt(")) {
                int cutIdx = curr.lastIndexOf('(');
                while (cutIdx > 0 && Character.isLetter(curr.charAt(cutIdx - 1))) {
                    cutIdx--;
                }
                curr = curr.substring(0, cutIdx);
            } else if (curr.endsWith("ln(")) {
                curr = curr.substring(0, curr.length() - 3);
            } else {
                curr = curr.substring(0, curr.length() - 1);
            }

            expression.setValue(curr);
            if (curr.isEmpty()) {
                result.setValue("0");
                livePreview.setValue("");
                isNewCalculation = true;
            } else {
                updateLivePreview();
            }
        }
    }

    public void clear() {
        expression.setValue("");
        result.setValue("0");
        livePreview.setValue("");
        isNewCalculation = true;
    }

    public void toggleSign() {
        String curr = expression.getValue();
        if (curr == null || curr.isEmpty()) {
            expression.setValue("-");
            isNewCalculation = false;
            return;
        }

        if (curr.startsWith("-")) {
            expression.setValue(curr.substring(1));
        } else {
            expression.setValue("-" + curr);
        }
        updateLivePreview();
    }

    public void calculate() {
        String expr = expression.getValue();
        if (expr == null || expr.trim().isEmpty()) {
            return;
        }

        CalculatorEngine.EvaluationResult eval = engine.evaluate(expr);
        if (eval.isSuccess()) {
            String formatted = eval.getFormattedResult();
            result.setValue(formatted);
            livePreview.setValue("");

            // Add to history
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
            String timeStr = sdf.format(new Date());
            HistoryItem item = new HistoryItem(expr, formatted, timeStr);

            List<HistoryItem> list = history.getValue();
            if (list == null) list = new ArrayList<>();
            list.add(0, item);
            history.setValue(list);

            isNewCalculation = true;
        } else {
            result.setValue(eval.getErrorMessage());
            livePreview.setValue("");
        }
    }

    private void updateLivePreview() {
        String expr = expression.getValue();
        if (expr == null || expr.trim().isEmpty()) {
            livePreview.setValue("");
            return;
        }

        // Only evaluate if expression contains at least one operator or function
        if (hasComputableTokens(expr)) {
            CalculatorEngine.EvaluationResult eval = engine.evaluate(expr);
            if (eval.isSuccess()) {
                livePreview.setValue("= " + eval.getFormattedResult());
            } else {
                livePreview.setValue("");
            }
        } else {
            livePreview.setValue("");
        }
    }

    private boolean hasComputableTokens(String expr) {
        return expr.contains("+") || expr.contains("−") || expr.contains("×") ||
               expr.contains("÷") || expr.contains("%") || expr.contains("^") ||
               expr.contains("sin") || expr.contains("cos") || expr.contains("tan") ||
               expr.contains("log") || expr.contains("ln") || expr.contains("√") ||
               expr.contains("!");
    }

    // Memory operations
    public void memoryClear() {
        engine.memoryClear();
        hasMemory.setValue(false);
    }

    public void memoryRecall() {
        if (engine.hasMemory()) {
            appendInput(CalculatorEngine.formatResult(engine.memoryRecall()));
        }
    }

    public void memoryAdd() {
        double val = getCurrentValue();
        if (!Double.isNaN(val)) {
            engine.memoryAdd(val);
            hasMemory.setValue(engine.hasMemory());
        }
    }

    public void memorySubtract() {
        double val = getCurrentValue();
        if (!Double.isNaN(val)) {
            engine.memorySubtract(val);
            hasMemory.setValue(engine.hasMemory());
        }
    }

    private double getCurrentValue() {
        String resStr = result.getValue();
        if (resStr != null) {
            try {
                return Double.parseDouble(resStr.replace(",", ""));
            } catch (Exception ignored) {}
        }
        return Double.NaN;
    }

    public void loadHistoryItem(HistoryItem item) {
        expression.setValue(item.getExpression());
        result.setValue(item.getResult());
        livePreview.setValue("");
        isNewCalculation = true;
    }

    public void clearHistory() {
        history.setValue(new ArrayList<>());
    }

    private boolean isOperator(String text) {
        return text.equals("+") || text.equals("−") || text.equals("×") || text.equals("÷") || text.equals("%") || text.equals("^");
    }

    private boolean isOperatorChar(char c) {
        return c == '+' || c == '−' || c == '×' || c == '÷' || c == '%' || c == '^';
    }
}
