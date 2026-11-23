package com.university.sms.desktop.fx;

import javafx.application.Application;

/**
 * UNIT 5 - JavaFX (Arindam).
 * Plain launcher class. When JavaFX jars are on the class path (not the module path), the
 * main class must NOT extend {@link Application}; this tiny class calls
 * {@link Application#launch(Class, String...)} for us.
 */
public final class DesktopLauncher {

    private DesktopLauncher() {
    }

    public static void main(String[] args) {
        Application.launch(SmsDesktopApp.class, args);
    }
}
