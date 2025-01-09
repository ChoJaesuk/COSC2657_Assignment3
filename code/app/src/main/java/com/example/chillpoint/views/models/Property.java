package com.example.chillpoint.views.models;

import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.ArrayList;
@IgnoreExtraProperties
public class Property {
    private String id;
    private String name;
    private String description;
    private String address;
    private double pricePerNight;
    private int numOfRooms;
    private int numOfBeds;
    private ArrayList<String> images;
    // Geocoding 결과를 담아둘 임시 필드
    private double latitude;
    private double longitude;

    // Default constructor for Firebase
    public Property() {}

    public Property(String id, String name, String description, String address, double pricePerNight, int numOfRooms, int numOfBeds, ArrayList<String> images,
                    double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.address = address;
        this.pricePerNight = pricePerNight;
        this.numOfRooms = numOfRooms;
        this.numOfBeds = numOfBeds;
        this.images = images;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters and Setters
    public String getId() { return id; } // id Getter
    public void setId(String id) { this.id = id; } // id Setter
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public double getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(double pricePerNight) { this.pricePerNight = pricePerNight; }
    public int getNumOfRooms() { return numOfRooms; }
    public void setNumOfRooms(int numOfRooms) { this.numOfRooms = numOfRooms; }
    public int getNumOfBeds() { return numOfBeds; }
    public void setNumOfBeds(int numOfBeds) { this.numOfBeds = numOfBeds; }
    public ArrayList<String> getImages() { return images; }
    public void setImages(ArrayList<String> images) { this.images = images; }
    // getters/setters ...
    public double getLatitude() { return latitude; }
    public void setLatitude(double lat) { this.latitude = lat; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double lng) { this.longitude = lng; }
}
