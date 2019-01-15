package com.dji.videostreamdecodingsample.models;

import dji.common.model.LocationCoordinate2D;

public class PeriodicalStateData {
    private int AircraftBattery=-1;
    private int RemoteControllerBattery=-1;
    private int flightControllerGPSSatelliteCount=-1;
    private int remoteControllerSwitchMode=-1;
    private int aircraftBatteryPercentageNeededToGoHome=-1;
    private int flightTime=-1;
    private boolean sensorBeingUsedFlightAssistant=false;
    private boolean firstReading=false;
    private double homeLatitude;
    private double homeLongitude;
    private double aircraftLatitude;
    private double aircraftLongitude;

    public double getAircraftLatitude() {
        return aircraftLatitude;
    }

    public void setAircraftLatitude(double aircraftLatitude) {
        this.aircraftLatitude = aircraftLatitude;
    }

    public double getAircraftLongitude() {
        return aircraftLongitude;
    }

    public void setAircraftLongitude(double aircraftLongitude) {
        this.aircraftLongitude = aircraftLongitude;
    }

    public double getHomeLatitude() {
        return homeLatitude;
    }

    public void setHomeLatitude(double homeLatitude) {
        this.homeLatitude = homeLatitude;
    }

    public double getHomeLongitude() {
        return homeLongitude;
    }

    public void setHomeLongitude(double homeLongitude) {
        this.homeLongitude = homeLongitude;
    }
    public boolean isSameHomeLocation(double currentLatitude, double currentLongitude){
        if(currentLatitude==homeLatitude&&currentLongitude==homeLongitude)
        return true;
        return false;
    }
    public boolean isSameAircraftLocation(double currentLatitude, double currentLongitude){
         if(distanciaCoord(currentLatitude,currentLongitude,aircraftLatitude,aircraftLongitude)>0.005)
            return true;
        return false;
    }
    public double distanciaCoord(double lat1, double lng1, double lat2, double lng2) {
        //double radioTierra = 3958.75;//en millas
        double radioTierra = 6371;//en kilómetros
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double sindLat = Math.sin(dLat / 2);
        double sindLng = Math.sin(dLng / 2);
        double va1 = Math.pow(sindLat, 2) + Math.pow(sindLng, 2)
                * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));
        double va2 = 2 * Math.atan2(Math.sqrt(va1), Math.sqrt(1 - va1));
        double distancia = radioTierra * va2;

        return distancia;
    }
    public boolean isFirstReading() {
        return firstReading;
    }
    public void setFirstReading(boolean firstReading) {
        this.firstReading = firstReading;
    }
    public int getAircraftBattery() {
        return AircraftBattery;
    }

    public void setAircraftBattery(int aircraftBattery) {
        AircraftBattery = aircraftBattery;
    }

    public int getRemoteControllerBattery() {
        return RemoteControllerBattery;
    }

    public void setRemoteControllerBattery(int remoteControllerBattery) {
        RemoteControllerBattery = remoteControllerBattery;
    }

    public int getFlightControllerGPSSatelliteCount() {
        return flightControllerGPSSatelliteCount;
    }

    public void setFlightControllerGPSSatelliteCount(int flightControllerGPSSatelliteCount) {
        this.flightControllerGPSSatelliteCount = flightControllerGPSSatelliteCount;
    }

    public int getRemoteControllerSwitchMode() {
        return remoteControllerSwitchMode;
    }

    public void setRemoteControllerSwitchMode(int remoteControllerSwitchMode) {
        this.remoteControllerSwitchMode = remoteControllerSwitchMode;
    }
    public boolean isSensorBeingUsedFlightAssistant() {
        return sensorBeingUsedFlightAssistant;
    }

    public void setSensorBeingUsedFlightAssistant(boolean sensorBeingUsedFlightAssistant) {
        this.sensorBeingUsedFlightAssistant = sensorBeingUsedFlightAssistant;
    }
    public int getAircraftBatteryPercentageNeededToGoHome() {
        return aircraftBatteryPercentageNeededToGoHome;
    }

    public void setAircraftBatteryPercentageNeededToGoHome(int aircraftBatteryPercentageNeededToGoHome) {
        this.aircraftBatteryPercentageNeededToGoHome = aircraftBatteryPercentageNeededToGoHome;
    }
    public int getFlightTime() {
        return flightTime;
    }

    public void setFlightTime(int flightTime) {
        this.flightTime = flightTime;
    }
}
