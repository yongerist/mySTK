package com.bupt.satviz.visibility;


import com.bupt.satviz.model.KeplerianElements;
import com.bupt.satviz.model.VisibilityWindow;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.propagation.BoundedPropagator; // 使用 BoundedPropagator
import org.orekit.propagation.Propagator;       // 使用 Propagator 接口
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
import org.orekit.propagation.SpacecraftState;
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
    private final double minElevationDeg;
    private final double coverageHalfAngleDeg;
    private final boolean useCoverageConstraint;

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
     * 计算卫星与地面站在指定时间段内的可见性窗口（使用星历）
     *
     * @param primarySatEphemeris 卫星的星历。
     * @param start         开始时间
     * @param end           结束时间
     * @param stationLatDeg 地面站纬度（度）
     * @param stationLonDeg 地面站经度（度）
     * @param stationAlt    地面站海拔（米）
     * @return 可见性窗口列表
     * @throws OrekitException
     */
    public List<VisibilityWindow> computeVisibility(BoundedPropagator primarySatEphemeris,
                                                    AbsoluteDate start, AbsoluteDate end,
                                                    double stationLatDeg, double stationLonDeg, double stationAlt)
            throws OrekitException {
        // --- 直接使用传入的星历作为事件驱动器 ---
        Propagator eventDriver = primarySatEphemeris;

        // --- 为事件检测设置姿态 (如果需要视场约束) ---
        AttitudeProvider attitudeToSet = null;
        if (useCoverageConstraint) {
            // 设置卫星姿态为对地定向即卫星始终以机体坐标系的+Z轴指向地球中心
            attitudeToSet = new NadirPointing(inertialFrame, earth);
            eventDriver.setAttitudeProvider(attitudeToSet);
        }
        // --- 姿态设置结束 ---


        // 2. 构造地面站 TopocentricFrame，以地面站为原点，本地水平面为参考，用于仰角和视线计算
        GeodeticPoint stationGeo = new GeodeticPoint(
                Math.toRadians(stationLatDeg),
                Math.toRadians(stationLonDeg),
                stationAlt
        );
        // 确保地面站名称的唯一性，尤其在并发环境中
        String stationName = "GS_" + stationLatDeg + "_" + stationLonDeg + "_" + Thread.currentThread().getId();
        TopocentricFrame stationFrame = new TopocentricFrame(earth, stationGeo, stationName);

        // 3. 定义 ElevationDetector，要求仰角大于 minElevationDeg
        // 这里的仰角是指卫星与地面站之间的视线与地平面的夹角
        // 设置所需的最小仰角角度（例如0度表示刚过地平线即算可见，ElevationDetector 的 g 函数在卫星过地平线时由负变正触发事件。）
        ElevationDetector elevDet = new ElevationDetector(stationFrame)
                .withConstantElevation(Math.toRadians(minElevationDeg));

        // 4. 定义 FieldOfViewDetector（覆盖半角）
        // 例如：半角45°表示只有当卫星与地面站的视线在卫星本地垂直方向45°以内时才算在可覆盖范围内。
        // 设置视场的主轴为地面站局部的正上方向（通常为 Vector3D.PLUS_K），视场半角为45°，旋转角为0° ，fov被附加在卫星上
        FieldOfViewDetector fovDet = null;
        if (useCoverageConstraint) {
            FieldOfView fov = new CircularFieldOfView(
                    Vector3D.PLUS_K,
                    Math.toRadians(coverageHalfAngleDeg),
                    0.0
            );
            // 容易误解的点：不在视线内:返回 false；    在视线内：返回 true
            fovDet = new FieldOfViewDetector(stationFrame, fov);
        }


        // 5. 组合检测器：如果使用覆盖约束，则同时满足仰角和视场；否则仅用仰角检测
        // 组合逻辑：elevDet AND (NOT fovDet)
        EventDetector visibilityDetector;
        double maxCheckInterval = 10.0; // 最大检查间隔 (秒)
        double threshold = 1e-6;       // 收敛阈值
        if (useCoverageConstraint && fovDet != null) {
            visibilityDetector = BooleanDetector.andCombine(elevDet, BooleanDetector.notCombine(fovDet))
                    .withMaxCheck(maxCheckInterval)
                    .withThreshold(threshold)
                    .withHandler(new RecordAndContinue()); // TODO：后续看能否注释掉（下面的也是）
        } else {
            visibilityDetector = elevDet
                    .withMaxCheck(maxCheckInterval)
                    .withThreshold(threshold)
                    .withHandler(new RecordAndContinue());
        }

        // 准备事件驱动器 (星历)
        // 清除任何可能在星历生成阶段遗留的探测器 (非常重要)
        eventDriver.clearEventsDetectors();
        // 添加我们关心的可见性探测器
        eventDriver.addEventDetector(visibilityDetector);

        // --- 检查初始状态 ---
        // 使用星历获取开始时刻的精确状态
        SpacecraftState initialState = eventDriver.propagate(start);
        double initialG = visibilityDetector.g(initialState);
        // g >= 0 表示初始时刻就满足可见性条件
        AbsoluteDate windowStart = (initialG >= 0) ? initialState.getDate() : null;

        // --- 使用星历驱动事件检测循环 ---
        // 这个 propagate 调用将使用星历内部的插值状态来驱动事件，
        // 不会执行新的轨道动力学计算。
        eventDriver.propagate(start, end);

        // --- 处理事件结果 (逻辑不变) ---
        // 从事件处理器中获取事件记录，生成可见性窗口列表
        RecordAndContinue handler = (RecordAndContinue) visibilityDetector.getHandler();
        List<RecordAndContinue.Event> events = handler.getEvents();
        List<VisibilityWindow> windows = new ArrayList<>();

        for (RecordAndContinue.Event ev : events) {
            if (ev.isIncreasing()) { // 进入可见 (g 从负到正)
                // 只有当当前没有未结束的窗口时，才记录新的开始时间
                if (windowStart == null) {
                    windowStart = ev.getState().getDate();
                }
            } else { // 离开可见 (g 从正到负)
                // 必须已经记录了窗口开始时间，才能形成一个完整的窗口
                if (windowStart != null) {
                    AbsoluteDate windowEnd = ev.getState().getDate();
                    // 避免持续时间极短或为零的窗口 (可能由数值误差引起)
                    if (windowEnd.durationFrom(windowStart) > 1e-9) {
                        double duration = windowEnd.durationFrom(windowStart);
                        windows.add(new VisibilityWindow(windowStart, windowEnd, duration));
                    }
                    windowStart = null; // 重置窗口开始时间，等待下一个进入事件
                }
            }
        }

        // 处理在仿真结束时窗口仍然打开的情况
        if (windowStart != null) {
            // 确保窗口开始时间不晚于仿真结束时间
            if (!windowStart.isAfter(end)) {
                double duration = end.durationFrom(windowStart);
                // 避免结束时产生零长度窗口
                if (duration > 1e-9) {
                    // 结束时间设为 null，表示窗口在仿真结束时被截断
                    windows.add(new VisibilityWindow(windowStart, null, duration));
                }
            }
        }

        // 注意：如果设置了临时姿态，理论上应该在这里恢复原始姿态，
        // 但对于 BoundedPropagator，其状态通常是不可变的，所以恢复可能不是必须的，
        // 且并发环境下修改共享对象（如果星历被共享）的姿态提供者需要小心。
        // 在当前每个 Task 处理独立星历对象副本的情况下，通常没问题。
        return windows;
    }

}

        //AbsoluteDate windowStart = null;
        //for (RecordAndContinue.Event ev : events) {
        //    if (ev.isIncreasing()) {
        //        windowStart = ev.getState().getDate();
        //    } else { // isDecreasing
        //        if (windowStart != null) {
        //            AbsoluteDate windowEnd = ev.getState().getDate();
        //            double duration = windowEnd.durationFrom(windowStart);
        //            windows.add(new VisibilityWindow(windowStart, windowEnd, duration));
        //            windowStart = null;
        //        }
        //    }
        //}
        // 若传播结束时仍在可见状态
        // if (windowStart != null) {
        //    double duration = end.durationFrom(windowStart);
        //    windows.add(new VisibilityWindow(windowStart, null, duration));
        //}

