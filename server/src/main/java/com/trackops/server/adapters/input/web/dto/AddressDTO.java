package com.trackops.server.adapters.input.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AddressDTO {

    @NotBlank
    @Size(max = 255)
    private String streetAddress;

    @NotBlank
    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String state;

    @NotBlank
    @Size(max = 20)
    private String postalCode;

    @NotBlank
    @Size(max = 100)
    private String country;

    @Size(max = 50)
    private String phoneNumber;

    public AddressDTO() {

    }

    public AddressDTO(String streetAddress, String city, String state, String postalCode, String country, String phoneNumber) {
        this.streetAddress = streetAddress;
        this.city = city;
        this.state = state;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

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

    @Override
    public String toString() {
        return "AddressDTO{" +
                "streetAddress='" + streetAddress + '\'' +
                ", city='" + city + '\'' +
                ", state='" + state + '\'' +
                ", postalCode='" + postalCode + '\'' +
                ", country='" + country + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddressDTO that = (AddressDTO) o;
        return java.util.Objects.equals(streetAddress, that.streetAddress) &&
                java.util.Objects.equals(city, that.city) &&
                java.util.Objects.equals(state, that.state) &&
                java.util.Objects.equals(postalCode, that.postalCode) &&
                java.util.Objects.equals(country, that.country) &&
                java.util.Objects.equals(phoneNumber, that.phoneNumber);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(streetAddress, city, state, postalCode, country, phoneNumber);
    }
}