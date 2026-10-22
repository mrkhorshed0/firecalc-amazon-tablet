package com.firecalc.tablet;

import java.io.Serializable;

/**
 * Model representing a single calculation record in history tape.
 */
public class HistoryItem implements Serializable {
    private final String expression;
    private final String result;
    private final String formattedTime;

    public HistoryItem(String expression, String result, String formattedTime) {
        this.expression = expression;
        this.result = result;
        this.formattedTime = formattedTime;
    }

    public String getExpression() {
        return expression;
    }

    public String getResult() {
        return result;
    }

    public String getFormattedTime() {
        return formattedTime;
    }
}
