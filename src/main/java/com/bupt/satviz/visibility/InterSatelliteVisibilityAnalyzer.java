package com.bupt.satviz.visibility;


import com.bupt.satviz.model.KeplerianElements;
import com.bupt.satviz.model.VisibilityWindow;
import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.BoundedPropagator; // 使用 BoundedPropagator
import org.orekit.propagation.Propagator;       // 使用 Propagator 接口
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.events.AdaptableInterval;
import org.orekit.propagation.events.BooleanDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.propagation.events.handlers.RecordAndContinue;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.util.ArrayList;
import java.util.List;

/**
 * 卫星–卫星可见性分析模块
 * 可见：两卫星之间有直接视线且距离不超过一定阈值
 * 输入：两颗卫星的轨道参数（封装在 KeplerianElements 对象中）、开始时间、结束时间；
 * 输出：两颗卫星在该时间段内的可见性窗口列表，每个窗口包含开始时间、结束时间和持续时长。
 */
public class InterSatelliteVisibilityAnalyzer {

    // 最大允许距离（单位：米）
    private final double maxDistance;

    // 公共参考系和地球模型
    private static final Frame inertialFrame = FramesFactory.getEME2000();
    private static final Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
    private static final OneAxisEllipsoid earth = new OneAxisEllipsoid(
            Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING,
            earthFrame);

    public InterSatelliteVisibilityAnalyzer(double maxDistance) {
        this.maxDistance = maxDistance;
    }

    /**
     * 计算两颗卫星在指定时间段内的可见性窗口
     *
     * @param primarySatEphemeris 主卫星（事件驱动器）的星历。
     * @param otherSatEphemeris   另一个卫星的星历。
     * @param start      开始时间
     * @param end        结束时间
     * @return 可见性窗口列表
     * @throws OrekitException
     */
    public List<VisibilityWindow> computeVisibility(BoundedPropagator primarySatEphemeris,
                                                    BoundedPropagator otherSatEphemeris,
                                                    AbsoluteDate start,
                                                    AbsoluteDate end
    )throws OrekitException {
        // --- 直接使用主卫星的星历作为事件驱动器 ---
        Propagator eventDriver = primarySatEphemeris;

        // --- 定义自定义事件探测器 (传入 另一个 卫星的星历) ---
        // 2.1 视线无遮挡检测器
        LineOfSightDetector losDetector = new LineOfSightDetector(otherSatEphemeris, earth)
                .withMaxCheck(10.0).withThreshold(1e-6);
        // 2.2 最大距离检测器
        MaxRangeDetector rangeDetector = new MaxRangeDetector(otherSatEphemeris, maxDistance)
                .withMaxCheck(10.0).withThreshold(1e-6);

        // 3. 组合两个检测器（逻辑与）
        EventDetector combinedDetector = BooleanDetector.andCombine(losDetector, rangeDetector)
                .withHandler(new RecordAndContinue());

        // --- 准备事件驱动器 (主卫星星历) ---
        // 清除可能存在的旧探测器
        eventDriver.clearEventsDetectors();
        // 添加组合探测器
        eventDriver.addEventDetector(combinedDetector);


        // 检查初始状态是否已经满足可见条件
        SpacecraftState initialState = eventDriver.propagate(start);
        double initialG = combinedDetector.g(initialState);
        // g >= 0 表示初始时刻可见
        AbsoluteDate windowStart = (initialG >= 0) ? initialState.getDate() : null;


        // --- 使用主卫星星历驱动事件检测循环 ---
        // 这个 propagate 调用将使用主星历的插值状态驱动，
        // 而探测器内部的 g 函数会使用次星历进行查找。
        eventDriver.propagate(start, end);


        // 6. 从事件处理器中获取事件记录，生成可见性窗口列表
        RecordAndContinue handler = (RecordAndContinue) combinedDetector.getHandler();
        List<RecordAndContinue.Event> events = handler.getEvents();
        List<VisibilityWindow> windows = new ArrayList<>();

        for (RecordAndContinue.Event ev : events) {
            if (ev.isIncreasing()) {
                // 如果窗口起点尚未设置，则用此事件作为窗口开始
                if (windowStart == null) {
                    windowStart = ev.getState().getDate();
                }
            } else { // 下降事件：条件由满足变为不满足
                if (windowStart != null) {
                    AbsoluteDate windowEnd = ev.getState().getDate();
                    if (windowEnd.durationFrom(windowStart) > 1e-9) {
                        double duration = windowEnd.durationFrom(windowStart);
                        windows.add(new VisibilityWindow(windowStart, windowEnd, duration));
                    }
                    windowStart = null;
                }
            }
        }
        // 如果最后仍处于可见状态，则记录最后一个窗口（结束时间为 null）
        if (windowStart != null) {
            if (!windowStart.isAfter(end)) {
                double duration = end.durationFrom(windowStart);
                if (duration > 1e-9) {
                    windows.add(new VisibilityWindow(windowStart, null, duration));
                }
            }
        }
        return windows;
    }


    // 遮挡检测器
    private static class LineOfSightDetector extends AbstractDetector<LineOfSightDetector> {
        private final BoundedPropagator otherSatEphemeris; // 存储次卫星星历
        private final OneAxisEllipsoid earth;

        // 新构造器：传入所有参数
        public LineOfSightDetector(BoundedPropagator otherSatEphemeris, OneAxisEllipsoid earth,
                                   AdaptableInterval maxCheck, double threshold, int maxIter, EventHandler handler) {
            super(maxCheck, threshold, maxIter, handler);
            this.otherSatEphemeris = otherSatEphemeris;
            this.earth = earth;
        }

        // 原有构造器调用默认参数
        public LineOfSightDetector(BoundedPropagator otherSatEphemeris, OneAxisEllipsoid earth) {
            this(otherSatEphemeris, earth, state -> AbstractDetector.DEFAULT_MAXCHECK, 1e-6, 100, new RecordAndContinue());
        }

        @Override
        public double g(SpacecraftState state) throws OrekitException {
            AbsoluteDate t = state.getDate();
            // 获取主卫星在地固系的位置
            Vector3D pos1 = state.getPVCoordinates(earthFrame).getPosition();
            // --- 使用次卫星星历快速获取其在 t 时刻的状态和位置 ---
            SpacecraftState otherState = otherSatEphemeris.propagate(t); // 核心
            Vector3D pos2 = otherState.getPVCoordinates(earthFrame).getPosition();

            if (pos1.distance(pos2) < 1e-6) {
                // 两卫星几乎重合时，直接返回1.0避免数值错误
                return 1.0;
            }
            // 创建连接两点的直线
            Line line = new Line(pos1, pos2, 1e-6); // 设置直线构造容差
            // 若地球与连线无交点，则视线无遮挡
            // getIntersectionPoint 返回 null 表示无交点（无遮挡）
            // 1.0 表示无遮挡 ，-1.0 表示有遮挡
            return earth.getIntersectionPoint(line, pos1, earthFrame, t) == null ? 1.0 : -1.0;
        }

        @Override
        protected LineOfSightDetector create(AdaptableInterval newMaxCheck, double newThreshold,
                                             int newMaxIter, EventHandler newHandler) {
            return new LineOfSightDetector(otherSatEphemeris, earth, newMaxCheck, newThreshold, newMaxIter, newHandler);
        }
    }
    // 最大距离探测器
    private static class MaxRangeDetector extends AbstractDetector<MaxRangeDetector> {
        private final BoundedPropagator otherSatEphemeris;
        private final double maxDistance;

        public MaxRangeDetector(BoundedPropagator otherSatEphemeris, double maxDistance,
                                AdaptableInterval maxCheck, double threshold, int maxIter, EventHandler handler) {
            super(maxCheck, threshold, maxIter, handler);
            this.otherSatEphemeris = otherSatEphemeris;
            this.maxDistance = maxDistance;
        }

        public MaxRangeDetector(BoundedPropagator otherSatEphemeris, double maxDistance) {
            this(otherSatEphemeris, maxDistance, state -> AbstractDetector.DEFAULT_MAXCHECK, 1e-6, 100, new RecordAndContinue());
        }

        @Override
        public double g(SpacecraftState state) throws OrekitException{
            AbsoluteDate t = state.getDate();
            Vector3D pos1 = state.getPVCoordinates(inertialFrame).getPosition();
            // --- 使用次卫星星历快速获取其在 t 时刻的状态和位置 ---
            SpacecraftState otherState = otherSatEphemeris.propagate(t);
            Vector3D pos2 = otherState.getPVCoordinates(inertialFrame).getPosition();
            double distance = pos1.distance(pos2);
            // g 函数 > 0 表示 distance < maxDistance
            return maxDistance - distance;
        }

        @Override
        protected MaxRangeDetector create(AdaptableInterval newMaxCheck, double newThreshold,
                                          int newMaxIter, EventHandler newHandler) {
            return new MaxRangeDetector(otherSatEphemeris, maxDistance, newMaxCheck, newThreshold, newMaxIter, newHandler);
        }
    }



}