package com.bupt;

import lombok.*;

import java.util.Date;

// 轨道参数值对象
@Getter
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrbitParams {
    private double semimajorAxis;  // 单位：米
    private double eccentricity;
    private double inclination;   // 单位：度
    private double raan;          // 单位：度
    private double trueAnomaly;    // 单位：度
    private Date timestamp;


}

