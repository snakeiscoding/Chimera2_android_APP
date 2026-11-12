package com.jht.chimera.ui.machinesettings.adapters.management;

/**
 * Created by Giancarlo.Cuadros on 12/28/2018.
 */

public class ConsoleItem {

    private String text;
    private String status;
    private boolean isSelected = false;

    public ConsoleItem(String text, String status) {
        this.text = text;
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public String getText() {
        return text;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }


    public boolean isSelected() {
        return isSelected;
    }
}
