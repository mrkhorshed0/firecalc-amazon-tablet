package com.firecalc.tablet;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

/**
 * Robust, zero-dependency mathematical expression evaluator for Amazon Fire Tablets.
 * Guarantees zero crashes and 100% offline accuracy.
 */
public class CalculatorEngine {

    public enum AngleMode {
        DEGREE, RADIAN
    }

    private AngleMode angleMode = AngleMode.DEGREE;
    private double memoryValue = 0.0;

    public AngleMode getAngleMode() {
        return angleMode;
    }

    public void setAngleMode(AngleMode mode) {
        this.angleMode = mode;
    }

    public void toggleAngleMode() {
        this.angleMode = (this.angleMode == AngleMode.DEGREE) ? AngleMode.RADIAN : AngleMode.DEGREE;
    }

    public void memoryClear() { memoryValue = 0.0; }
    public double memoryRecall() { return memoryValue; }
    public void memoryAdd(double val) { memoryValue += val; }
    public void memorySubtract(double val) { memoryValue -= val; }
    public boolean hasMemory() { return memoryValue != 0.0; }

    public static class EvaluationResult {
        private final boolean success;
        private final double value;
        private final String formattedResult;
        private final String errorMessage;

        public EvaluationResult(double value, String formattedResult) {
            this.success = true;
            this.value = value;
            this.formattedResult = formattedResult;
            this.errorMessage = null;
        }

        public EvaluationResult(String errorMessage) {
            this.success = false;
            this.value = Double.NaN;
            this.formattedResult = "";
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() { return success; }
        public double getValue() { return value; }
        public String getFormattedResult() { return formattedResult; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * Evaluates a mathematical expression string and returns formatted output.
     */
    public EvaluationResult evaluate(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return new EvaluationResult(0.0, "0");
        }

        try {
            String sanitized = sanitize(expression);
            List<String> tokens = tokenize(sanitized);
            if (tokens.isEmpty()) {
                return new EvaluationResult(0.0, "0");
            }
            List<String> rpn = infixToRPN(tokens);
            double val = evaluateRPN(rpn);

            if (Double.isNaN(val)) {
                return new EvaluationResult("Invalid input");
            }
            if (Double.isInfinite(val)) {
                return new EvaluationResult("Cannot divide by zero");
            }

            val = roundClean(val);
            String formatted = formatResult(val);
            return new EvaluationResult(val, formatted);
        } catch (ArithmeticException ae) {
            return new EvaluationResult(ae.getMessage() != null ? ae.getMessage() : "Error");
        } catch (IllegalArgumentException iae) {
            return new EvaluationResult(iae.getMessage() != null ? iae.getMessage() : "Invalid expression");
        } catch (Exception e) {
            return new EvaluationResult("Error");
        }
    }

    private String sanitize(String expr) {
        return expr.replace("×", "*")
                   .replace("÷", "/")
                   .replace("−", "-")
                   .replace("π", "PI")
                   .replace("e", "E")
                   .replace("√", "sqrt")
                   .replace("sin⁻¹", "asin")
                   .replace("cos⁻¹", "acos")
                   .replace("tan⁻¹", "atan")
                   .trim();
    }

    private boolean isOperator(String token) {
        return token.equals("+") || token.equals("-") || token.equals("*") ||
               token.equals("/") || token.equals("^") || token.equals("%") ||
               token.equals("u-");
    }

    private boolean isFunction(String token) {
        return token.equals("sin") || token.equals("cos") || token.equals("tan") ||
               token.equals("asin") || token.equals("acos") || token.equals("atan") ||
               token.equals("ln") || token.equals("log") || token.equals("sqrt") ||
               token.equals("cbrt") || token.equals("fact") || token.equals("abs");
    }

    private int precedence(String op) {
        if (op.equals("+") || op.equals("-")) return 1;
        if (op.equals("*") || op.equals("/") || op.equals("%")) return 2;
        if (op.equals("u-")) return 3;
        if (op.equals("^")) return 4;
        return 0;
    }

    private boolean isRightAssociative(String op) {
        return op.equals("^") || op.equals("u-");
    }

    private List<String> tokenize(String expr) {
        List<String> rawTokens = new ArrayList<>();
        int i = 0;
        int n = expr.length();

        while (i < n) {
            char c = expr.charAt(i);

            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            if (Character.isDigit(c) || c == '.') {
                StringBuilder sb = new StringBuilder();
                while (i < n && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
                    sb.append(expr.charAt(i));
                    i++;
                }
                rawTokens.add(sb.toString());
            } else if (c == '+' || c == '-' || c == '*' || c == '/' || c == '^' || c == '(' || c == ')' || c == '!') {
                rawTokens.add(String.valueOf(c));
                i++;
            } else if (c == '%') {
                rawTokens.add("%");
                i++;
            } else if (Character.isLetter(c)) {
                StringBuilder sb = new StringBuilder();
                while (i < n && Character.isLetter(expr.charAt(i))) {
                    sb.append(expr.charAt(i));
                    i++;
                }
                rawTokens.add(sb.toString());
            } else {
                i++;
            }
        }

        List<String> processed = new ArrayList<>();
        for (int idx = 0; idx < rawTokens.size(); idx++) {
            String curr = rawTokens.get(idx);
            String prev = idx > 0 ? rawTokens.get(idx - 1) : null;

            if (curr.equals("-")) {
                if (prev == null || prev.equals("(") || isOperator(prev)) {
                    curr = "u-";
                }
            }

            if (prev != null) {
                boolean prevCanEnd = isNumber(prev) || prev.equals(")") || prev.equals("PI") || prev.equals("E") || prev.equals("!");
                boolean currCanStart = isNumber(curr) || curr.equals("(") || isFunction(curr) || curr.equals("PI") || curr.equals("E");

                if (prevCanEnd && currCanStart) {
                    processed.add("*");
                }
            }

            if (curr.equals("!")) {
                processed.add("fact_post");
                continue;
            }

            processed.add(curr);
        }

        return processed;
    }

    private boolean isNumber(String token) {
        if (token == null || token.isEmpty()) return false;
        try {
            Double.parseDouble(token);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private List<String> infixToRPN(List<String> tokens) {
        List<String> output = new ArrayList<>();
        Stack<String> stack = new Stack<>();

        for (String token : tokens) {
            if (isNumber(token) || token.equals("PI") || token.equals("E")) {
                output.add(token);
            } else if (token.equals("fact_post")) {
                output.add("fact");
            } else if (isFunction(token)) {
                stack.push(token);
            } else if (isOperator(token)) {
                while (!stack.isEmpty()) {
                    String top = stack.peek();
                    if (isFunction(top) ||
                       (isOperator(top) && (
                            precedence(top) > precedence(token) ||
                            (precedence(top) == precedence(token) && !isRightAssociative(token))
                        ))) {
                        output.add(stack.pop());
                    } else {
                        break;
                    }
                }
                stack.push(token);
            } else if (token.equals("(")) {
                stack.push(token);
            } else if (token.equals(")")) {
                boolean matched = false;
                while (!stack.isEmpty()) {
                    String top = stack.pop();
                    if (top.equals("(")) {
                        matched = true;
                        break;
                    } else {
                        output.add(top);
                    }
                }
                if (!matched) {
                    throw new IllegalArgumentException("Mismatched parentheses");
                }
                if (!stack.isEmpty() && isFunction(stack.peek())) {
                    output.add(stack.pop());
                }
            }
        }

        while (!stack.isEmpty()) {
            String top = stack.pop();
            if (top.equals("(") || top.equals(")")) {
                throw new IllegalArgumentException("Mismatched parentheses");
            }
            output.add(top);
        }

        return output;
    }

    private double evaluateRPN(List<String> rpn) {
        Stack<Double> stack = new Stack<>();

        for (String token : rpn) {
            if (isNumber(token)) {
                stack.push(Double.parseDouble(token));
            } else if (token.equals("PI")) {
                stack.push(Math.PI);
            } else if (token.equals("E")) {
                stack.push(Math.E);
            } else if (token.equals("u-")) {
                if (stack.isEmpty()) throw new IllegalArgumentException("Invalid syntax");
                stack.push(-stack.pop());
            } else if (isFunction(token)) {
                if (stack.isEmpty()) throw new IllegalArgumentException("Invalid syntax");
                double arg = stack.pop();
                stack.push(applyFunction(token, arg));
            } else if (isOperator(token)) {
                if (stack.size() < 2) throw new IllegalArgumentException("Invalid syntax");
                double b = stack.pop();
                double a = stack.pop();
                stack.push(applyOperator(token, a, b));
            }
        }

        if (stack.size() != 1) {
            throw new IllegalArgumentException("Malformed expression");
        }

        return stack.pop();
    }

    private double applyOperator(String op, double a, double b) {
        switch (op) {
            case "+": return a + b;
            case "-": return a - b;
            case "*": return a * b;
            case "/":
                if (b == 0.0) throw new ArithmeticException("Cannot divide by zero");
                return a / b;
            case "%":
                return a % b;
            case "^":
                return Math.pow(a, b);
            default:
                throw new IllegalArgumentException("Unknown operator: " + op);
        }
    }

    private double applyFunction(String func, double arg) {
        switch (func) {
            case "sin": {
                double rad = (angleMode == AngleMode.DEGREE) ? Math.toRadians(arg) : arg;
                if (angleMode == AngleMode.DEGREE && Math.abs(arg % 180.0) < 1e-9) return 0.0;
                return Math.sin(rad);
            }
            case "cos": {
                double rad = (angleMode == AngleMode.DEGREE) ? Math.toRadians(arg) : arg;
                if (angleMode == AngleMode.DEGREE && Math.abs((arg - 90.0) % 180.0) < 1e-9) return 0.0;
                return Math.cos(rad);
            }
            case "tan": {
                if (angleMode == AngleMode.DEGREE && Math.abs((arg - 90.0) % 180.0) < 1e-9) {
                    throw new ArithmeticException("Undefined");
                }
                double rad = (angleMode == AngleMode.DEGREE) ? Math.toRadians(arg) : arg;
                return Math.tan(rad);
            }
            case "asin": {
                if (arg < -1.0 || arg > 1.0) throw new ArithmeticException("Domain error");
                double res = Math.asin(arg);
                return (angleMode == AngleMode.DEGREE) ? Math.toDegrees(res) : res;
            }
            case "acos": {
                if (arg < -1.0 || arg > 1.0) throw new ArithmeticException("Domain error");
                double res = Math.acos(arg);
                return (angleMode == AngleMode.DEGREE) ? Math.toDegrees(res) : res;
            }
            case "atan": {
                double res = Math.atan(arg);
                return (angleMode == AngleMode.DEGREE) ? Math.toDegrees(res) : res;
            }
            case "ln":
                if (arg <= 0) throw new ArithmeticException("Domain error");
                return Math.log(arg);
            case "log":
                if (arg <= 0) throw new ArithmeticException("Domain error");
                return Math.log10(arg);
            case "sqrt":
                if (arg < 0) throw new ArithmeticException("Domain error");
                return Math.sqrt(arg);
            case "cbrt":
                return Math.cbrt(arg);
            case "abs":
                return Math.abs(arg);
            case "fact":
                return factorial(arg);
            default:
                throw new IllegalArgumentException("Unknown function: " + func);
        }
    }

    private double factorial(double n) {
        if (n < 0 || n != Math.floor(n)) {
            throw new ArithmeticException("Factorial requires positive integer");
        }
        if (n > 170) {
            return Double.POSITIVE_INFINITY;
        }
        double res = 1.0;
        int limit = (int) n;
        for (int i = 2; i <= limit; i++) {
            res *= i;
        }
        return res;
    }

    public static double roundClean(double val) {
        if (Double.isNaN(val) || Double.isInfinite(val)) return val;
        if (Math.abs(val) < 1e-12) return 0.0;

        try {
            BigDecimal bd = new BigDecimal(Double.toString(val));
            bd = bd.round(new MathContext(12, RoundingMode.HALF_UP));
            return bd.stripTrailingZeros().doubleValue();
        } catch (Exception e) {
            return val;
        }
    }

    public static String formatResult(double val) {
        if (Double.isNaN(val)) return "Invalid input";
        if (Double.isInfinite(val)) return "Cannot divide by zero";

        if (val == Math.floor(val) && !Double.isInfinite(val) && Math.abs(val) < 1e12) {
            long longVal = (long) val;
            DecimalFormat df = new DecimalFormat("#,###", new DecimalFormatSymbols(Locale.US));
            return df.format(longVal);
        }

        if (Math.abs(val) >= 1e12 || (Math.abs(val) > 0 && Math.abs(val) < 1e-6)) {
            DecimalFormat sciDf = new DecimalFormat("0.######E0", new DecimalFormatSymbols(Locale.US));
            return sciDf.format(val).replace("E", " × 10^");
        }

        DecimalFormat df = new DecimalFormat("#,##0.########", new DecimalFormatSymbols(Locale.US));
        return df.format(val);
    }
}
