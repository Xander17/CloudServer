package ru.kornev.cloudcommon.exceptions;

public class NoEnoughDataException extends Exception {
    public NoEnoughDataException() {
        super();
    }

    public NoEnoughDataException(String message) {
        super(message);
    }
}
