package com.bupt;

import java.math.BigDecimal;

// 地基节点：终端、基站、卫星地面站
public class Facility {
    private String facilityId;

    private BigDecimal longitude;

    private BigDecimal latitude;



    @Override
    public String toString() {
        return "Facility{" +
                "facilityId='" + facilityId + '\'' +
                ", longitude=" + longitude +
                ", latitude=" + latitude +
                '}';
    }
}
