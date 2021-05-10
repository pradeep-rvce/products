package com.handson.api.cpre.composite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ServiceAddresses {
    private final String cmp;
    private final String pro;
    private final String rev;
    private final String rec;

    @JsonCreator
    public ServiceAddresses(@JsonProperty("cmp")String cmp,
                            @JsonProperty("pro")String pro,
                            @JsonProperty("rev")String rev,
                            @JsonProperty("rec")String rec) {
        this.cmp = cmp;
        this.pro = pro;
        this.rev = rev;
        this.rec = rec;
    }

    public String getCmp() {
        return cmp;
    }

    public String getPro() {
        return pro;
    }

    public String getRev() {
        return rev;
    }

    public String getRec() {
        return rec;
    }
}