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

}