package com.bupt;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DirectoryCrawler;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.orbits.KeplerianOrbit;

import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;



// 卫星轨道服务类
public class SatelliteOrbitService {

    static {
        // 替换为实际的 orekit 数据目录路径
        File orekitData = new File("D:\\Code\\code_need\\orekit-data");
        // 通过 DataContext 获取默认的数据提供管理器
        DataContext.getDefault().getDataProvidersManager().addProvider(new DirectoryCrawler(orekitData));
    }
    //Frame 和 FramesFactory：用于定义参考系，如 地心惯性系（EME2000） 和 国际地球参考框架（ITRF）。
    private final Frame inertialFrame = FramesFactory.getEME2000(); // 假设为EME2000惯性系
    private final Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true); // ITRF地固系
    // 地球模型 OneAxisEllipsoid：用于将卫星的地心直角坐标 转换为地理坐标（纬度、经度、高度）。
    private final OneAxisEllipsoid earthModel = new OneAxisEllipsoid(
            Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING,
            earthFrame
    );

    public SatelliteState calculateState(OrbitParams params) {
        // 1. 参数预处理， 将角度转换为弧度
        double inclinationRad = Math.toRadians(params.getInclination());
        double raanRad = Math.toRadians(params.getRaan());
        double trueAnomalyRad = Math.toRadians(params.getTrueAnomaly());

        // 2. 构建轨道模型
        // 绝对时间： AbsoluteDate：表示时间戳，可用于计算轨道随时间的演化。
        AbsoluteDate date = new AbsoluteDate(
                params.getTimestamp(),  //AbsoluteDate 需要一个字符串或者 Date 类型的时间戳。
                TimeScalesFactory.getUTC()
        );
        //  KeplerianOrbit：基于 开普勒轨道参数（半长轴、偏心率、倾角等）计算卫星的 位置和速度。
        KeplerianOrbit orbit = new KeplerianOrbit(
                params.getSemimajorAxis(),
                params.getEccentricity(),
                inclinationRad,
                raanRad,
                0.0, // 假设近地点幅角为0（根据问题描述）
                trueAnomalyRad,
                PositionAngleType.TRUE,
                inertialFrame,
                date,
                Constants.WGS84_EARTH_MU
        );

        // 3.获取航天器ECI状态
        SpacecraftState state = new SpacecraftState(orbit);

        // 3. 坐标系转换（ECI → ECEF）  PVCoordinates：用于存储卫星的 位置（Position）和速度（Velocity） 向量。
        Transform inertialToEarth = inertialFrame.getTransformTo(earthFrame, state.getDate()); // 获取转换
        PVCoordinates pvEarth = inertialToEarth.transformPVCoordinates(state.getPVCoordinates(inertialFrame)); // 转换为ECEF坐标

        // 4. 地理坐标转换 将卫星的地心直角坐标 转换为地理坐标（纬度、经度、高度）。
        //  GeodeticPoint：用于表示地理坐标（纬度、经度、高度）。
        GeodeticPoint geoPoint = earthModel.transform(pvEarth.getPosition(), earthFrame, null);

        // 5. 计算变化率（数值微分法）
        SpacecraftState stateDelta = state.shiftedBy(0.1); // 假设时间间隔为0.1秒
        PVCoordinates pvDelta = inertialFrame.getTransformTo(earthFrame, stateDelta.getDate())
                .transformPVCoordinates(stateDelta.getPVCoordinates(inertialFrame));
        GeodeticPoint geoDelta = earthModel.transform(pvDelta.getPosition(), earthFrame, null);

        double latRate = (geoDelta.getLatitude() - geoPoint.getLatitude()); // 纬度变化率（度/秒）
        double lonRate = (geoDelta.getLongitude() - geoPoint.getLongitude()); // 经度变化率（度/秒）
        double altRate = (geoDelta.getAltitude() - geoPoint.getAltitude()); // 高度变化率（米/秒）
        // 6. 返回结果
        return new SatelliteState(
                Math.toDegrees(geoPoint.getLatitude()),
                Math.toDegrees(geoPoint.getLongitude()),
                geoPoint.getAltitude(),
                latRate,
                lonRate,
                altRate
        );
    }
}





