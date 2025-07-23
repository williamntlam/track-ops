package com.trackops.server.adapters.input.web.dto;

public class AddressDTO {

    private String streetAddress;
    private String city;
    private String postalCode;
    private String country;
    private String phoneNumber;

    public AddressDTO() {

    }

    public AddressDTO(String streetAddress, String city, String postalCode, String country, String phoneNumber) {

        this.streetAddress = streetAddress;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
        this.phoneNumber = phoneNumber; 

    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    // this.country = country;
    // this.phoneNumber = phoneNumber; 

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

}