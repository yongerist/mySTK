package com.bupt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 卫星状态结果类
@Getter
@Data
@AllArgsConstructor
@NoArgsConstructor
public  class SatelliteState {
    private  double latitude;      // 纬度（度）
    private  double longitude;     // 经度（度）
    private  double altitude;      // 高度（米）
    private  double latRate;       // 纬度变化率（度/秒）
    private  double lonRate;       // 经度变化率（度/秒）
    private  double altRate;       // 高度变化率（米/秒）


    @Override
    public String toString() {
        return "SatelliteState{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", altitude=" + altitude +
                ", latRate=" + latRate +
                ", lonRate=" + lonRate +
                ", altRate=" + altRate +
                '}';
    }
}
