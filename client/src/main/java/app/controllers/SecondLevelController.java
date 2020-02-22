package app.controllers;

import app.MainController;

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
