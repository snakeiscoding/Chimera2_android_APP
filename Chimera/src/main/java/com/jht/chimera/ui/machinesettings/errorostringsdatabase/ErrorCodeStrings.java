package com.jht.chimera.ui.machinesettings.errorostringsdatabase;

public class ErrorCodeStrings {
    public String hex_code;
    public int decimal_code;
    public String severity;
    public String description;
    public String cause;
    public String troubleshooting;
    public String notes;
    public String updates;


    public ErrorCodeStrings(String hexCode, String cause, String description, String troubleshooting) {
        this.hex_code = hexCode;
        this.description = description;
        this.cause = cause;
        this.troubleshooting = troubleshooting;
    }
}
