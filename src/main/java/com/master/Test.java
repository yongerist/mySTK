package com.master;

import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DirectoryCrawler;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.io.File;
import java.util.List;

public class Test {
    public static void main(String[] args) {
        // 替换为实际的 orekit 数据目录路径
        File orekitData = new File("/Users/liuzhengyang/Downloads/orekit-data");
        // 通过 DataContext 获取默认的数据提供管理器
        DataContext.getDefault().getDataProvidersManager().addProvider(new DirectoryCrawler(orekitData));
        /*
         *  WGS84椭球作为地球模型
         *  惯性系采用地心惯性系 EME2000
         *  地心固定系采用 ITRF
         */
//        Frame inertialFrame = FramesFactory.getEME2000();
//        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
//        OneAxisEllipsoid earth = new OneAxisEllipsoid(
//                Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
//                Constants.WGS84_EARTH_FLATTENING,
//                earthFrame);
        // ===== 初始化环境（假定已加载 Orekit 数据） =====
        Frame inertialFrame = FramesFactory.getEME2000();
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        OneAxisEllipsoid earth = ReferenceEllipsoid.getWgs84(earthFrame);
        AbsoluteDate startDate = new AbsoluteDate(2025, 1, 1, 4, 0, 0.0, TimeScalesFactory.getUTC());
        AbsoluteDate endDate = startDate.shiftedBy(7200.0);  // 预测2小时

// 定义一颗卫星的轨道六根数和历元
        KeplerianElements satOrbit = new KeplerianElements(
                6878.14e3, 0.0, 90.0, 0.0, 0.0, 0.0, startDate);  // 近圆极轨道, 轨道根数参考时刻为 startDate

// 1. 轨道传播模块：计算卫星在某一时刻的位置和速度变化率
        OrbitPropagator propagator = new OrbitPropagator();
        AbsoluteDate targetTime = startDate.shiftedBy(3600.0);  // 一小时后
        PropagationResult result = propagator.propagate(satOrbit, targetTime);
        GeodeticPoint pos = result.getPosition();
        System.out.printf("卫星在%s的位置: 纬度%.2f°, 经度%.2f°, 高度%.1f km%n",
                targetTime, Math.toDegrees(pos.getLatitude()), Math.toDegrees(pos.getLongitude()), pos.getAltitude() / 1000.0);
        System.out.printf("地面轨迹变化率: 纬度%.3f°/s，经度%.3f°/s，高度%.1f m/s%n",
                result.getLatRateDegPerSec(), result.getLonRateDegPerSec(), result.getAltRateMetersPerSec());

// 2. 卫星-地面站可见性模块：计算地面站可见窗口
        double stationLat = 36.1922, stationLon = -160.165, stationAlt = 0.0;
        GroundStationVisibilityAnalyzer gsAnalyzer = new GroundStationVisibilityAnalyzer(0.0, 45.0);
        List<VisibilityWindow> gsWindows = gsAnalyzer.computeVisibility(satOrbit, startDate, endDate, stationLat, stationLon, stationAlt);
        for (VisibilityWindow w : gsWindows) {
            System.out.println("地面站可见窗口: 开始=" + w.getStartTime() +
                    ", 结束=" + (w.getEndTime() != null ? w.getEndTime() : "仍可见") +
                    ", 持续=" + w.getDurationSeconds() + "秒");
        }

// 3. 卫星-卫星可见性模块：计算两星之间的可见窗口
        KeplerianElements satOrbit2 = new KeplerianElements(
                6878.14e3, 0.0, 45.0, 0.0, 0.0, 5.0, startDate);  // 第二颗卫星轨道（不同倾角和初始相位）
        InterSatelliteVisibilityAnalyzer satAnalyzer = new InterSatelliteVisibilityAnalyzer(5_000_000.0); // 最大距离5000 km
        List<VisibilityWindow> satWindows = satAnalyzer.computeVisibility(satOrbit, satOrbit2, startDate, endDate);
        for (VisibilityWindow w : satWindows) {
            System.out.println("星间可见窗口: 开始=" + w.getStartTime() +
                    ", 结束=" + (w.getEndTime() != null ? w.getEndTime() : "仍可见") +
                    ", 持续=" + w.getDurationSeconds() + "秒");
        }


    }
}
