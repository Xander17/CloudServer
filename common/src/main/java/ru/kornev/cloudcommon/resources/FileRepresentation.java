package ru.kornev.cloudcommon.resources;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import ru.kornev.cloudcommon.services.LogServiceCommon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileRepresentation {
    private StringProperty name;
    private StringProperty length;
    private StringProperty date;

    public FileRepresentation(Path path) {
        try {
            this.name = new SimpleStringProperty(path.getFileName().toString());
            this.length = new SimpleStringProperty(getStringLength(Files.size(path)));
            this.date = new SimpleStringProperty(getFormattedDate(Files.getLastModifiedTime(path).toMillis()));
        } catch (IOException e) {
            LogServiceCommon.APP.error("File representation creation error", path.toString(), path.getFileName().toString(), e.toString());
        }
    }

    public FileRepresentation(String name, long length, long dateMillis) {
        this.name = new SimpleStringProperty(name);
        this.length = new SimpleStringProperty(getStringLength(length));
        this.date = new SimpleStringProperty(getFormattedDate(dateMillis));
    }

    private String getStringLength(long length) {
        if (length < 1024) return length + "b";
        else if (length < 1024 * 1024) return String.format("%.2fKb", length / 1024.);
        else if (length < 1024 * 1024 * 1024) return String.format("%.2fMb", length / (1024. * 1024));
        else return String.format("%.2fGb", length / (1024. * 1024 * 1024));
    }

    private String getFormattedDate(long millis) {
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-YY HH:mm:ss");
        return format.format(new Date(millis));
    }

    public String getName() {
        return name.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getLength() {
        return length.get();
    }

    public void setLength(String length) {
        this.length.set(length);
    }

    public StringProperty lengthProperty() {
        return length;
    }

    public String getDate() {
        return date.get();
    }

    public void setDate(String date) {
        this.date.set(date);
    }

    public StringProperty dateProperty() {
        return date;
    }

    @Override
    public String toString() {
        return name.get();
    }
}
