package com.lzy;

import com.bupt.Facility;
import com.bupt.Satellite;
import org.orekit.data.DataContext;
import org.orekit.data.DirectoryCrawler;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;

import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

import org.orekit.utils.IERSConventions;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Transform;
import java.io.File;

public class Keplerian_Propagator {
    public static void main(String[] args) {
        // 替换为实际的 orekit 数据目录路径
        File orekitData = new File("/Users/liuzhengyang/Downloads/orekit-data");
        // 通过 DataContext 获取默认的数据提供管理器
        DataContext.getDefault().getDataProvidersManager().addProvider(new DirectoryCrawler(orekitData));


        // 轨道参数：半长轴、偏心率、倾角、升交点赤经、近地点真距、真近点角
        double semiMajorAxis = 6878.14 * 1000; // 半长轴 (7000 km)
        double eccentricity = 0.0;   // 偏心率 近圆轨道
        double inclination = 90.0;     // 倾角 (90°)
        double raan = 0.0;           // 升交点赤经 (0°)
        double argumentOfPerigee = 0.0; // 近地点真距 (0°)
        double trueAnomaly = 0.0;    // 真近点角 (0°)
        // 定义起始时间（UTC）
        AbsoluteDate startDate = new AbsoluteDate(2025, 1, 1, 4, 0, 0.0, TimeScalesFactory.getUTC());
        AbsoluteDate targetDate = new AbsoluteDate(2025, 1, 1, 5, 0, 0.0, TimeScalesFactory.getUTC()); // 1小时后

        // 轨道参考系（惯性系 EME2000） ECI
        var inertialFrame = FramesFactory.getEME2000();

        // 创建轨道
        KeplerianOrbit orbit = new KeplerianOrbit(
                semiMajorAxis,
                eccentricity,
                Math.toRadians(inclination),
                Math.toRadians(raan),
                Math.toRadians(argumentOfPerigee),
                Math.toRadians(trueAnomaly),
                PositionAngleType.TRUE, // 真近点角
                inertialFrame, //  EME2000 参考框架 、 地心惯性系
                startDate, // 轨道模型的起始时间
                Constants.WGS84_EARTH_MU // 地球的引力常数
        );

        // 创建 KeplerianPropagator -- 轨道传播器，用于计算卫星在不同时间的状态。
        KeplerianPropagator propagator = new KeplerianPropagator(orbit);

        // 传播到目标时间     获取 targetDate 的轨道状态 SpacecraftState。
        SpacecraftState targetState = propagator.propagate(targetDate);

        // 获取 PVCoordinates（位置 + 速度）
        PVCoordinates pvCoordinates = targetState.getPVCoordinates(inertialFrame);

        // 8. 获取 ECEF（ITRF）参考系
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

        // 9. 计算惯性系（ECI）到地固系（ITRF）的转换
        Transform inertialToEarth = inertialFrame.getTransformTo(earthFrame, targetDate);

        // 10. 转换到 ECEF（ITRF）
        PVCoordinates pvEarth = inertialToEarth.transformPVCoordinates(pvCoordinates);

        // 11. 定义 WGS84 地球模型（用于大地坐标（经纬度+高度）转换）
        OneAxisEllipsoid earthModel = new OneAxisEllipsoid(
                Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING,
                earthFrame);

        // 12. 获取地理坐标（纬度、经度、高度）
        GeodeticPoint geoPoint = earthModel.transform(pvEarth.getPosition(), earthFrame, null);

        // 13. 计算变化率（数值微分法，1秒后）
        SpacecraftState stateDelta = targetState.shiftedBy(0.1);
        PVCoordinates pvDelta = inertialFrame.getTransformTo(earthFrame, stateDelta.getDate())
                .transformPVCoordinates(stateDelta.getPVCoordinates(inertialFrame));
        GeodeticPoint geoDelta = earthModel.transform(pvDelta.getPosition(), earthFrame, null);

        double latRate = Math.toDegrees(geoDelta.getLatitude() - geoPoint.getLatitude()); // 纬度变化率（度/秒）
        double lonRate = Math.toDegrees(geoDelta.getLongitude() - geoPoint.getLongitude()); // 经度变化率（度/秒）
        double altRate = geoDelta.getAltitude() - geoPoint.getAltitude(); // 高度变化率（米/秒）

        // 14. 输出结果 （数值取决于 轨道参数 和 传播时间 ）
        System.out.println("卫星纬度（°）: " + Math.toDegrees(geoPoint.getLatitude()));
        System.out.println("卫星经度（°）: " + Math.toDegrees(geoPoint.getLongitude()));
        System.out.println("卫星高度（km）: " + geoPoint.getAltitude() / 1000.0);
        System.out.println("纬度变化率（°/s）: " + latRate);
        System.out.println("经度变化率（°/s）: " + lonRate);
        System.out.println("高度变化率（km/s）: " + altRate / 1000.0);


    }
}





