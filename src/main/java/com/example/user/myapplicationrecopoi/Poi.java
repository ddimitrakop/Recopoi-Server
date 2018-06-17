package com.example.user.myapplicationrecopoi;

public class Poi {

    public double predictions;
    private int id;
    private String POI;
    private double latidude;
    private double longitude;
    private String photos;
    private String POI_category_id;
    private String POI_name;

    public Poi(){}

    public Poi(int id, double predictions) {
        this.id = id;
        this.predictions = predictions;
    }

    public Poi(String POI, double latidude, double longitude, String photos, String POI_category_id, String POI_name, int id, double predictions) {
        this.POI = POI;
        this.latidude = latidude;
        this.longitude = longitude;
        this.photos = photos;
        this.POI_category_id = POI_category_id;
        this.POI_name = POI_name;
        this.id = id;
        this.predictions = predictions;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPOI() {
        return POI;
    }

    public void setPOI(String POI) {
        this.POI = POI;
    }

    public double getLatidude() {
        return latidude;
    }

    public void setLatidude(double latidude) {
        this.latidude = latidude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getPhotos() {
        return photos;
    }

    public void setPhotos(String photos) {
        this.photos = photos;
    }

    public String getPOI_category_id() {
        return POI_category_id;
    }

    public void setPOI_category_id(String POI_category_id) {
        this.POI_category_id = POI_category_id;
    }

    public String getPOI_name() {
        return POI_name;
    }

    public void setPOI_name(String POI_name) {
        this.POI_name = POI_name;
    }

    public double getPredictions() {
        return predictions;
    }

    public void setPredictions(double predictions) {
        this.predictions = predictions;
    }

    @Override
    public String toString() {
        return "POI : " + POI +
                "\nlatidude : " + latidude +
                "\nlongtitude : " + longitude +
                "\nphotos : " + photos +
                "\nPOI_category_id : " + POI_category_id +
                "\nPOI_name : " + POI_name + "\n";

    }
}