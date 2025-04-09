package com.bupt.satviz.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.orekit.bodies.GeodeticPoint;

/**
 * 轨道传播结果类
 * 用于存储卫星在目标时刻的地理坐标和变化率
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PropagationResult {
    private GeodeticPoint position;  // 卫星在指定时刻的位置 (纬度/经度/高度)
    private double latRateDegPerSec;
    private double lonRateDegPerSec;
    private double altRateMetersPerSec;

}
