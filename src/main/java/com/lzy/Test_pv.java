package com.lzy;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.data.DataContext;
import org.orekit.data.DirectoryCrawler;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Test_pv {
    public static void main(String[] args) {

        // 替换为实际的 orekit 数据目录路径
        File orekitData = new File("/Users/liuzhengyang/Downloads/orekit-data");
        // 通过 DataContext 获取默认的数据提供管理器
        DataContext.getDefault().getDataProvidersManager().addProvider(new DirectoryCrawler(orekitData));


        // 轨道参数：半长轴、偏心率、倾角、升交点赤经、近地点真距、真近点角
        double semiMajorAxis = 7000000; // 半长轴 (7000 km)
        double eccentricity = 0.001;   // 偏心率 近圆轨道
        double inclination = 45.0;     // 倾角 (45°)
        double raan = 90.0;           // 升交点赤经 (90°)
        double argumentOfPerigee = 0.0; // 近地点真距 (0°)
        double trueAnomaly = 90.0;    // 真近点角 (90°)
        // 定义起始时间（UTC）
        AbsoluteDate startDate = new AbsoluteDate(2025, 3, 4, 12, 0, 0.0, TimeScalesFactory.getUTC());
        AbsoluteDate targetDate = new AbsoluteDate(2025, 3, 4, 13, 0, 0.0, TimeScalesFactory.getUTC()); // 1小时后

//         轨道参考系（惯性系 EME2000）
        var inertialFrame = FramesFactory.getEME2000();

        // 创建轨道
        KeplerianOrbit orbit = new KeplerianOrbit(
                semiMajorAxis,
                eccentricity,
                Math.toRadians(inclination),
                Math.toRadians(raan),
                Math.toRadians(argumentOfPerigee),
                Math.toRadians(trueAnomaly),
                PositionAngleType.TRUE, // 使用真近点角
                inertialFrame, // 使用 EME2000 参考框架 -- 轨道参考系 -- 地心惯性系
                startDate, // 轨道模型的起始时间
                Constants.WGS84_EARTH_MU // 地球的引力常数
        );

        // 创建 KeplerianPropagator -- 轨道传播器，用于计算卫星在不同时间的状态。
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);

        // 传播到目标时间
        SpacecraftState targetState = propagator.propagate(targetDate);

        // 获取 PVCoordinates（位置 + 速度）
        PVCoordinates pvCoordinates = targetState.getPVCoordinates(inertialFrame);

        // 输出结果
        Vector3D position = pvCoordinates.getPosition();
        Vector3D velocity = pvCoordinates.getVelocity();

        System.out.println("目标时间的卫星位置（m）: " + position);
        System.out.println("目标时间的卫星速度（m/s）: " + velocity);


//        Date specificDate = null;
//        try {
//            // 定义一个日期格式
//            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//
//            // 定义一个特定时间的字符串
//            String dateStr = "2025-03-05 10:15:23";
//
//            // 解析字符串为 Date 对象
//            specificDate = sdf.parse(dateStr);
//
//            // 输出特定时间的时间戳
////            System.out.println("Specific timestamp: " + specificDate.getTime());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        // 将Date对象转换为 AbsoluteDate
//        AbsoluteDate specificAbsoluteDate = new AbsoluteDate(specificDate, TimeScalesFactory.getUTC());
        // 获取位置和速度坐标，位置（Position） 和 速度（Velocity）  ，Coordinates--> 坐标



//        PVCoordinates pvCoordinates = orbit.getPVCoordinates(targetDate, FramesFactory.getEME2000()); // 获取位置和速度坐标
//        Vector3D position = pvCoordinates.getPosition(); // 获取位置向量，单位：米
//        Vector3D velocity = pvCoordinates.getVelocity(); // 获取速度向量，单位：米/秒
//
//        System.out.println("位置: " + position);
//        System.out.println("速度: " + velocity);


    }
}
