package com.bupt.satviz.calculation;

import com.bupt.satviz.model.KeplerianElements;
import com.bupt.satviz.model.PropagationResult;
import com.bupt.satviz.model.SatelliteState;
import lombok.Getter;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException; // 用于处理无效ID

/**
 * 卫星状态模块 (基于初始轨道根数进行实时传播计算)
 * 输入卫星初始轨道参数列表和目标时间，
 * 输出目标时间内所有卫星或特定卫星的经纬度、高度以及变化率。
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
        OrbitPropagator propagator = new OrbitPropagator();
        for (int i = 0; i < satellites.size(); i++) {
            KeplerianElements ke = satellites.get(i);
            // 传播至目标时间，得到 PropagationResult 对象
            PropagationResult result = propagator.propagate(ke, targetTime);
            // 构造卫星状态对象
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

    // --- 新增方法 ---
    /**
     * 根据卫星ID和目标时间，计算该卫星的精确状态（位置和变化率）。
     * 此方法通过实时轨道传播计算状态。
     *
     * @param satelliteId 要查询的卫星ID
     * @param targetTime  目标绝对时间
     * @param satellites  包含所有卫星初始轨道参数的列表
     * @return 指定卫星在目标时刻的状态对象 (SatelliteState)
     * @throws IndexOutOfBoundsException 如果 satelliteId 无效 (小于0或大于等于列表大小)
     * @throws OrekitException 如果轨道传播计算过程中发生Orekit相关错误
     * @throws RuntimeException 如果发生其他内部错误
     */
    public static SatelliteState getSatelliteStateByIdAndTime(int satelliteId,
                                                              AbsoluteDate targetTime,
                                                              List<KeplerianElements> satellites)
            throws OrekitException { // 明确抛出可能发生的OrekitException

        // 检查ID有效性
        if (satelliteId < 0 || satelliteId >= satellites.size()) {
            throw new IndexOutOfBoundsException("无效的卫星ID: " + satelliteId + ". ID必须在 0 到 " + (satellites.size() - 1) + " 之间。");
        }

        // 获取指定卫星的轨道根数
        KeplerianElements ke = satellites.get(satelliteId);

        // 创建轨道传播器实例
        OrbitPropagator propagator = new OrbitPropagator();

        // 执行轨道传播计算
        PropagationResult result = propagator.propagate(ke, targetTime); // 可能抛出 OrekitException

        // 构建并返回SatelliteState对象
        return new SatelliteState(satelliteId, result.getPosition(),
                result.getLatRateDegPerSec(),
                result.getLonRateDegPerSec(),
                result.getAltRateMetersPerSec());
    }
    // --- 新增方法结束 ---


}
