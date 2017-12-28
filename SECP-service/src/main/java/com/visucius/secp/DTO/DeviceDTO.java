package com.visucius.secp.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceDTO {
    @JsonProperty
    private String deviceName;

    @JsonProperty
    private String publicKey;

    @JsonProperty
    private long userID;

    public DeviceDTO() {

    }
    public DeviceDTO(long userID, String deviceName, String publicKey) {
        this.userID = userID;
        this.deviceName = deviceName;
        this.publicKey = publicKey;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public long getUserID() {
        return userID;
    }

    public void setUserID(long userID) {
        this.userID = userID;
    }
}
