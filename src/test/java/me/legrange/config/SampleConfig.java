package me.legrange.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

public final class SampleConfig {

    @NotEmpty
    private  String string;

    @Min(value=4)
    private  int integer;

    private  boolean bool;

    public String getString() {
        return string;
    }

    public int getInteger() {
        return integer;
    }

    public boolean isBool() {
        return bool;
    }
}
