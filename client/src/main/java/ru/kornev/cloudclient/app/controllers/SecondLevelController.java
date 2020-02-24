package ru.kornev.cloudclient.app.controllers;

import ru.kornev.cloudclient.app.MainController;

public abstract class SecondLevelController {
    private MainController mainController;

    public MainController getMainController() {
        if (mainController == null)
            throw new NullPointerException("Main controller didn't set for " + getClass().getCanonicalName() + " class");
        return mainController;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
}
