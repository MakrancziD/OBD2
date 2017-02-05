package com.example.maki.obd2.utils;

import com.github.pires.obd.commands.ObdCommand;

/**
 * Created by Maki on 2017. 02. 05..
 */

public class CustomObdCommand extends ObdCommand {

    public CustomObdCommand(String command) {
        super(command);
    }

    @Override
    protected void performCalculations() {

    }

    @Override
    public String getFormattedResult() {
        return null;
    }

    @Override
    public String getCalculatedResult() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }
}
