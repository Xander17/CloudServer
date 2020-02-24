package ru.kornev.cloudcommon.services.transfer.resources;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Progress {
    private long maxValue;
    private DoubleProperty progress;
    private StringProperty stringProgress;

    public Progress() {
        progress = new SimpleDoubleProperty(0);
        stringProgress = new SimpleStringProperty();
        maxValue = 0;
    }

    public void resetProgress() {
        Platform.runLater(() -> {
            progress.setValue(0);
            stringProgress.setValue("");
        });
    }

    public void addProgress(double addValue) {
        Platform.runLater(() -> {
            progress.setValue(progress.getValue() + addValue / maxValue);
            stringProgress.setValue(String.format("%.2f%%", progress.getValue() * 100));
        });
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public StringProperty stringProgressProperty() {
        return stringProgress;
    }

    public void setMaxValue(long maxValue) {
        this.maxValue = maxValue;
    }
}
