package com.bupt.satviz.calculation;

import com.bupt.satviz.model.KeplerianElements;
import com.bupt.satviz.model.PropagationResult;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

/**
 * 轨道传播器
 * 使用Orekit库进行轨道传播
 */
public class OrbitPropagator {
    Frame inertialFrame = FramesFactory.getEME2000();
    Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
    OneAxisEllipsoid earth = new OneAxisEllipsoid(
            Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING,
            earthFrame);
    /*
     *  使用Orekit的开普勒轨道传播器
     *  将给定轨道参数的卫星传播到目标时间
     *  返回包含位置和变化率的结果对象
     *  输出接口说明：PropagationResult 中可以通过相应的getter获取结果。
     *  例如 result.getPosition() 返回 GeodeticPoint，包含卫星纬度、经度（弧度）和高度（米）；
     *  result.getLatRateDegPerSec() 等方法提供变化率（纬度/经度为度每秒，高度为米每秒）。
     */
    public PropagationResult propagate(KeplerianElements orbitElem, AbsoluteDate targetDate) {
        // 1. 构造开普勒轨道和传播器
        KeplerianOrbit orbit = orbitElem.toOrbit(inertialFrame, Constants.WGS84_EARTH_MU);
        Propagator keplerProp = new KeplerianPropagator(orbit);

        // 2. 将轨道传播到目标时间，得到卫星状态
        SpacecraftState state = keplerProp.propagate(targetDate);

        // 3. 获取卫星在惯性系下的轨道坐标 (位置、速度)
        PVCoordinates pvInertial = state.getPVCoordinates(inertialFrame);

        // 4. 计算从惯性系转换到地球固定系 (ITRF) 的变换，并获得卫星在ITRF下的坐标
        Transform inertialToEarth = inertialFrame.getTransformTo(earthFrame, targetDate);
        PVCoordinates pvEarth = inertialToEarth.transformPVCoordinates(pvInertial);

        // 5. 将地心直角坐标转换为大地坐标 (经纬度、高度)
        GeodeticPoint geoPoint = earth.transform(pvEarth.getPosition(), earthFrame, targetDate);

        // 6. 数值微分计算经纬高变化率（以很小的dt计算差分）
        double dt = 0.1; // 时间步长0.1秒
        SpacecraftState stateLater = keplerProp.propagate(targetDate.shiftedBy(dt));
        PVCoordinates pvEarthLater = inertialFrame.getTransformTo(earthFrame, stateLater.getDate())
                .transformPVCoordinates(stateLater.getPVCoordinates(inertialFrame));
        GeodeticPoint geoPointLater = earth.transform(pvEarthLater.getPosition(), earthFrame, stateLater.getDate());

        // 计算每秒变化量
        double latRate = Math.toDegrees(geoPointLater.getLatitude() - geoPoint.getLatitude()) / dt;
        double lonRate = Math.toDegrees(geoPointLater.getLongitude() - geoPoint.getLongitude()) / dt;
        double altRate = (geoPointLater.getAltitude() - geoPoint.getAltitude()) / dt;

        // 7. 封装结果
        return new PropagationResult(geoPoint, latRate, lonRate, altRate);
    }
}
