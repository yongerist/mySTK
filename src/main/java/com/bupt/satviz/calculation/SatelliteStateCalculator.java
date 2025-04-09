package com.bupt.satviz.calculation;

import com.bupt.satviz.model.KeplerianElements;
import com.bupt.satviz.model.PropagationResult;
import com.bupt.satviz.model.SatelliteState;
import lombok.Getter;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;
import java.util.List;

/**
 * 卫星状态模块
 * 输入起始时间、目标时间和卫星信息（KeplerianElements 列表），
 * 输出目标时间内所有卫星的经纬度、高度以及变化率。
 */
public class SatelliteStateCalculator {

    /**
     * 计算所有卫星在目标时间的状态信息。
     *
     * @param satellites 卫星轨道参数列表（KeplerianElements 对象）
     * @param targetTime 目标时间
     * @return 每颗卫星的状态列表，每个状态包含经纬度、高度以及变化率
     */
    public static List<SatelliteState> computeSatelliteStates(List<KeplerianElements> satellites,
                                                                AbsoluteDate targetTime) {
        List<SatelliteState> states = new ArrayList<>();
        OrbitPropagator propagator = new OrbitPropagator(); // 利用已有的轨道传播器
        for (int i = 0; i < satellites.size(); i++) {
            KeplerianElements ke = satellites.get(i);
            // 传播至目标时间，得到 PropagationResult 对象
            PropagationResult result = propagator.propagate(ke, targetTime);
            // 构造卫星状态对象（注意：经纬度单位在 GeodeticPoint 中为弧度，此处打印时可以转换为度）
            SatelliteState state = new SatelliteState(i, result.getPosition(),
                    result.getLatRateDegPerSec(),
                    result.getLonRateDegPerSec(),
                    result.getAltRateMetersPerSec());
            states.add(state);
        }
        return states;
    }

    /**
     * 打印所有卫星的状态信息。
     *
     * @param states 卫星状态列表
     */
    public static void printSatelliteStates(List<SatelliteState> states) {
        for (SatelliteState state : states) {
            GeodeticPoint pos = state.getPosition();
            // 将纬度和经度从弧度转换为度
            double latDeg = Math.toDegrees(pos.getLatitude());
            double lonDeg = Math.toDegrees(pos.getLongitude());
            System.out.println("卫星 #" + state.getSatelliteId() + " 状态:");
            System.out.println("  纬度: " + latDeg + "°");
            System.out.println("  经度: " + lonDeg + "°");
            System.out.println("  高度: " + pos.getAltitude() + " m");
            System.out.println("  纬度变化率: " + state.getLatRateDegPerSec() + " deg/s");
            System.out.println("  经度变化率: " + state.getLonRateDegPerSec() + " deg/s");
            System.out.println("  高度变化率: " + state.getAltRateMetersPerSec() + " m/s");
            System.out.println();
        }
    }
}
