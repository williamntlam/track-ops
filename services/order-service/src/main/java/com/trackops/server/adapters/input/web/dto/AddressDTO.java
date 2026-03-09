package com.trackops.server.adapters.input.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public class AddressDTO {

    @NotBlank(message = "Street address is required")
    @Size(min = 5, max = 255, message = "Street address must be between 5 and 255 characters")
    private String streetAddress;

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 100, message = "City must be between 2 and 100 characters")
    @Pattern(regexp = "^[A-Za-z\\s\\-']{2,100}$", message = "City must contain only letters, spaces, hyphens, and apostrophes")
    private String city;

    @Size(max = 100, message = "State must not exceed 100 characters")
    @Pattern(regexp = "^[A-Za-z\\s\\-']{0,100}$", message = "State must contain only letters, spaces, hyphens, and apostrophes")
    private String state;

    @NotBlank(message = "Postal code is required")
    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    @Pattern(regexp = "^[A-Za-z0-9\\s-]{3,20}$", message = "Postal code must be 3-20 characters and contain only letters, numbers, spaces, and hyphens")
    private String postalCode;

    @NotBlank(message = "Country is required")
    @Size(max = 100, message = "Country name must not exceed 100 characters")
    @Pattern(regexp = "^[A-Za-z\\s]{2,100}$", message = "Country must be 2-100 characters and contain only letters and spaces")
    private String country;

    @Size(max = 50, message = "Phone number must not exceed 50 characters")
    @Pattern(regexp = "^[+]?[0-9\\s\\-\\(\\)]{7,50}$", message = "Phone number must be 7-50 characters and contain only numbers, spaces, hyphens, parentheses, and optional + prefix")
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