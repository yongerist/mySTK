package com.bupt.satviz.visibility;


import com.bupt.satviz.model.KeplerianElements;
import com.bupt.satviz.model.VisibilityWindow;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.NadirPointing;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.geometry.fov.CircularFieldOfView;
import org.orekit.geometry.fov.FieldOfView;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.BooleanDetector;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldOfViewDetector;
import org.orekit.propagation.events.handlers.RecordAndContinue;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.util.ArrayList;
import java.util.List;

/**
 * 卫星–地面站可见性分析模块
 * 输入：卫星轨道参数（封装在 KeplerianElements 对象中）、开始时间、结束时间、地面站坐标（纬度、经度、海拔）
 * 输出：在给定时间段内卫星与地面站的可见性窗口列表，每个窗口包含开始时间、结束时间和持续时长。
 */
public class GroundStationVisibilityAnalyzer {

    // 最小仰角（单位：度）和卫星覆盖半角（单位：度）
    private double minElevationDeg;
    private double coverageHalfAngleDeg;
    private boolean useCoverageConstraint;

    // 公共参考系与地球模型（统一使用 WGS84 椭球和 ITRF 地固系）
    private static final Frame inertialFrame = FramesFactory.getEME2000();
    private static final Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
    private static final OneAxisEllipsoid earth = ReferenceEllipsoid.getWgs84(earthFrame);

    public GroundStationVisibilityAnalyzer(double minElevationDeg, double coverageHalfAngleDeg) {
        this.minElevationDeg = minElevationDeg;
        this.coverageHalfAngleDeg = coverageHalfAngleDeg;
        // 当覆盖半角小于 90° 时使用覆盖约束
        this.useCoverageConstraint = coverageHalfAngleDeg < 90.0;
    }

    /**
     * 计算卫星与地面站在指定时间段内的可见性窗口
     *
     * @param orbitElem     卫星轨道参数（KeplerianElements）
     * @param start         开始时间
     * @param end           结束时间
     * @param stationLatDeg 地面站纬度（度）
     * @param stationLonDeg 地面站经度（度）
     * @param stationAlt    地面站海拔（米）
     * @return 可见性窗口列表
     * @throws OrekitException
     */
    public List<VisibilityWindow> computeVisibility(KeplerianElements orbitElem,
                                                    AbsoluteDate start, AbsoluteDate end,
                                                    double stationLatDeg, double stationLonDeg, double stationAlt)
            throws OrekitException {
        // 1. 构造卫星轨道和传播器
        KeplerianOrbit orbit = orbitElem.toOrbit(inertialFrame, Constants.WGS84_EARTH_MU);
        Propagator propagator = new KeplerianPropagator(orbit);
        // 设置卫星姿态为对地定向即卫星始终以机体坐标系的+Z轴指向地球中心
        propagator.setAttitudeProvider(new NadirPointing(inertialFrame, earth));

        // 2. 构造地面站 TopocentricFrame，以地面站为原点，本地水平面为参考，用于仰角和视线计算
        GeodeticPoint stationGeo = new GeodeticPoint(Math.toRadians(stationLatDeg),
                Math.toRadians(stationLonDeg),
                stationAlt);
        TopocentricFrame stationFrame = new TopocentricFrame(earth, stationGeo, "GroundStation");

        // 3. 定义 ElevationDetector，要求仰角大于 minElevationDeg
        // 这里的仰角是指卫星与地面站之间的视线与地平面的夹角
        // 设置所需的最小仰角角度（例如0度表示刚过地平线即算可见，ElevationDetector 的 g 函数在卫星过地平线时由负变正触发事件。）
        ElevationDetector elevDet = new ElevationDetector(stationFrame)
                .withConstantElevation(Math.toRadians(minElevationDeg));

        // 4. 定义 FieldOfViewDetector（覆盖半角）
        // 例如：半角45°表示只有当卫星与地面站的视线在卫星本地垂直方向45°以内时才算在可覆盖范围内。
        // 设置视场的主轴为地面站局部的正上方向（通常为 Vector3D.PLUS_K），视场半角为45°，旋转角为0° ，fov被附加在卫星上
        FieldOfView fov = new CircularFieldOfView(Vector3D.PLUS_K, Math.toRadians(coverageHalfAngleDeg), 0.0);
        // 容易误解的点：不在视线内:返回 false；    在视线内：返回 true
        FieldOfViewDetector fovDet = new FieldOfViewDetector(stationFrame, fov);



        // 5. 组合检测器：如果使用覆盖约束，则同时满足仰角和视场；否则仅用仰角检测
        // 组合逻辑：elevDet AND (NOT fovDet)
        EventDetector visibilityDetector;
        if (useCoverageConstraint) {
            visibilityDetector = BooleanDetector.andCombine(elevDet, BooleanDetector.notCombine(fovDet))
                    .withMaxCheck(60.0).withThreshold(1e-6)
                    .withHandler(new RecordAndContinue());
        } else {
            visibilityDetector = elevDet.withMaxCheck(60.0).withThreshold(1e-6)
                    .withHandler(new RecordAndContinue());
        }

        // 6. 将检测器添加到传播器，并传播卫星运动
        propagator.addEventDetector(visibilityDetector);
        propagator.propagate(start, end);

        // 7. 从事件处理器中获取事件记录，生成可见性窗口列表
        RecordAndContinue handler = (RecordAndContinue) visibilityDetector.getHandler();
        List<RecordAndContinue.Event> events = handler.getEvents();
        List<VisibilityWindow> windows = new ArrayList<>();
        AbsoluteDate windowStart = null;
        for (RecordAndContinue.Event ev : events) {
            if (ev.isIncreasing()) {
                windowStart = ev.getState().getDate();
            } else { // isDecreasing
                if (windowStart != null) {
                    AbsoluteDate windowEnd = ev.getState().getDate();
                    double duration = windowEnd.durationFrom(windowStart);
                    windows.add(new VisibilityWindow(windowStart, windowEnd, duration));
                    windowStart = null;
                }
            }
        }
        // 若传播结束时仍在可见状态
        if (windowStart != null) {
            double duration = end.durationFrom(windowStart);
            windows.add(new VisibilityWindow(windowStart, null, duration));
        }
        return windows;
    }

}