package com.labs.dm.auto_tethering_lite.service;

/**
 * Created by Daniel Mroczka on 5/6/2016.
 */
public enum ServiceAction {

    TETHER_ON,
    TETHER_OFF,
    INTERNET_ON,
    INTERNET_OFF;

    private final boolean on;
    private final boolean tethering;
    private final boolean internet;

    ServiceAction() {
        this.on = name().contains("ON");
        this.internet = name().contains("INTERNET");
        this.tethering = name().contains("TETHER");
    }

    ServiceAction(boolean tethering, boolean internet, boolean on) {
        this.tethering = tethering;
        this.internet = internet;
        this.on = on;
    }

    public boolean isOn() {
        return on;
    }

    public boolean isTethering() {
        return tethering;
    }

    public boolean isInternet() {
        return internet;
    }

    @Override
    public String toString() {
        return name() + ", isTethering=" + isTethering() + ", isInternet=" + isInternet() + ", state=" + isOn();
    }
}
