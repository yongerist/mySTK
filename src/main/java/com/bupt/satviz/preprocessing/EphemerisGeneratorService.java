package com.bupt.satviz.preprocessing; // 新建包

import com.bupt.satviz.model.KeplerianElements;
import org.orekit.errors.OrekitException; // 明确导入 OrekitException
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.EphemerisGenerator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务类，负责为卫星列表在指定时间范围内生成星历 (BoundedPropagator)。
 */
public class EphemerisGeneratorService {

    // 共享使用的惯性参考系
    private static final Frame inertialFrame = FramesFactory.getEME2000();

    /**
     * 为所有提供的卫星轨道根数生成星历。
     *
     * @param orbitsElements 每个卫星的初始开普勒轨道根数列表。
     * @param startDate      星历生成的开始时间。
     * @param endDate        星历生成的结束时间。
     * @return 一个 BoundedPropagator 对象列表，每个对象对应一颗卫星。
     * @throws org.orekit.errors.OrekitException 如果轨道传播失败。
     */
    public static List<BoundedPropagator> generateEphemerides(List<KeplerianElements> orbitsElements,
                                                            AbsoluteDate startDate,
                                                            AbsoluteDate endDate)
            throws OrekitException { // 明确抛出 OrekitException

        System.out.println("  开始生成星历...");
        List<BoundedPropagator> allEphemerides = new ArrayList<>();
        int totalSatellites = orbitsElements.size();

        for (int i = 0; i < totalSatellites; i++) {
            KeplerianElements ke = orbitsElements.get(i);
            // 将轨道根数转换为 Orekit 的 Orbit 对象
            Orbit initialOrbit = ke.toOrbit(inertialFrame, Constants.WGS84_EARTH_MU);

            // --- 传播器选择 ---
            Propagator propagator = new KeplerianPropagator(initialOrbit);
            // --- 结束传播器选择 ---

            // 从传播器获取星历生成器
            final EphemerisGenerator generator = propagator.getEphemerisGenerator();

            // 在整个时间区间内传播一次以生成星历数据
            // 注意：这里可能会抛出 OrekitException
            propagator.propagate(startDate, endDate);

            // 获取生成的星历对象 (BoundedPropagator)
            allEphemerides.add(generator.getGeneratedEphemeris());

            // 可选：打印生成进度
            if ((i + 1) % 10 == 0 || i == totalSatellites - 1) {
                System.out.println("    已生成 " + (i + 1) + " / " + totalSatellites + " 个卫星的星历.");
            }
        }
        System.out.println("  所有卫星星历生成完毕.");
        return allEphemerides;
    }
}