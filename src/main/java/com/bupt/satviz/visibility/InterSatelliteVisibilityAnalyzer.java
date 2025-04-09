package com.bupt.satviz.visibility;


import com.bupt.satviz.model.KeplerianElements;
import com.bupt.satviz.model.VisibilityWindow;
import org.hipparchus.geometry.euclidean.threed.Line;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.propagation.Propagator;
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
    private double maxDistance;

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
     * @param orbitElem1 卫星1轨道参数（KeplerianElements）
     * @param orbitElem2 卫星2轨道参数（KeplerianElements）
     * @param start      开始时间
     * @param end        结束时间
     * @return 可见性窗口列表
     * @throws OrekitException
     */
    public List<VisibilityWindow> computeVisibility(KeplerianElements orbitElem1,
                                                    KeplerianElements orbitElem2,
                                                    AbsoluteDate start, AbsoluteDate end)
            throws OrekitException {
        // 1. 构造两颗卫星的轨道和传播器
        KeplerianOrbit orbit1 = orbitElem1.toOrbit(inertialFrame, Constants.WGS84_EARTH_MU);
        KeplerianOrbit orbit2 = orbitElem2.toOrbit(inertialFrame, Constants.WGS84_EARTH_MU);
        Propagator propagator1 = new KeplerianPropagator(orbit1);
        Propagator propagator2 = new KeplerianPropagator(orbit2);

        // 2. 定义自定义事件检测器：
        // 2.1 视线无遮挡检测器
        LineOfSightDetector losDetector = new LineOfSightDetector(propagator2, earth)
                .withMaxCheck(10.0).withThreshold(1e-6);
        // 2.2 最大距离检测器
        MaxRangeDetector rangeDetector = new MaxRangeDetector(propagator2, maxDistance)
                .withMaxCheck(10.0).withThreshold(1e-6);

        // 3. 组合两个检测器（逻辑与）
        EventDetector combinedDetector = BooleanDetector.andCombine(losDetector, rangeDetector)
                .withHandler(new RecordAndContinue());

        // 4. 将组合检测器添加到卫星1的传播器中
        propagator1.addEventDetector(combinedDetector);

        // 检查初始状态是否已经满足可见条件
        SpacecraftState initState = propagator1.getInitialState();
        // 如果初始状态满足条件，则记录窗口起点
        AbsoluteDate windowStart = (combinedDetector.g(initState) > 0) ? initState.getDate() : null;

        // 5. 传播卫星状态（事件会被记录）
        propagator1.propagate(start, end);

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
                    double duration = windowEnd.durationFrom(windowStart);
                    windows.add(new VisibilityWindow(windowStart, windowEnd, duration));
                    windowStart = null;
                }
            }
        }
        // 如果最后仍处于可见状态，则记录最后一个窗口（结束时间为 null）
        if (windowStart != null) {
            double duration = end.durationFrom(windowStart);
            windows.add(new VisibilityWindow(windowStart, null, duration));
        }
        return windows;
    }


    // 遮挡检测器
    private static class LineOfSightDetector extends AbstractDetector<LineOfSightDetector> {
        private final Propagator otherSatProp;
        private final OneAxisEllipsoid earth;

        // 新构造器：传入所有参数
        public LineOfSightDetector(Propagator otherSatProp, OneAxisEllipsoid earth,
                                   AdaptableInterval maxCheck, double threshold, int maxIter, EventHandler handler) {
            super(maxCheck, threshold, maxIter, handler);
            this.otherSatProp = otherSatProp;
            this.earth = earth;
        }

        // 原有构造器调用默认参数
        public LineOfSightDetector(Propagator otherSatProp, OneAxisEllipsoid earth) {
            this(otherSatProp, earth, state -> AbstractDetector.DEFAULT_MAXCHECK, 1e-3, 100, new RecordAndContinue());
        }

        @Override
        public double g(SpacecraftState state) {
            AbsoluteDate t = state.getDate();
            Vector3D pos1 = state.getPVCoordinates(earthFrame).getPosition();
            Vector3D pos2 = otherSatProp.propagate(t).getPVCoordinates(earthFrame).getPosition();
            if (pos1.distance(pos2) < 1e-6) {
                // 两卫星几乎重合时，直接返回1.0避免归一化错误
                return 1.0;
            }
            Line line = new Line(pos1, pos2, 1e-3);
            // 若地球与连线无交点，则视线无遮挡
            return earth.getIntersectionPoint(line, pos1, earthFrame, t) == null ? 1.0 : -1.0;
        }

        @Override
        protected LineOfSightDetector create(AdaptableInterval newMaxCheck, double newThreshold,
                                             int newMaxIter, EventHandler newHandler) {
            return new LineOfSightDetector(otherSatProp, earth, newMaxCheck, newThreshold, newMaxIter, newHandler);
        }
    }
    // 最大距离探测器
    private static class MaxRangeDetector extends AbstractDetector<MaxRangeDetector> {
        private final Propagator otherSatProp;
        private final double maxDistance;

        public MaxRangeDetector(Propagator otherSatProp, double maxDistance,
                                AdaptableInterval maxCheck, double threshold, int maxIter, EventHandler handler) {
            super(maxCheck, threshold, maxIter, handler);
            this.otherSatProp = otherSatProp;
            this.maxDistance = maxDistance;
        }

        public MaxRangeDetector(Propagator otherSatProp, double maxDistance) {
            this(otherSatProp, maxDistance, state -> AbstractDetector.DEFAULT_MAXCHECK, 1e-3, 100, new RecordAndContinue());
        }

        @Override
        public double g(SpacecraftState state) {
            AbsoluteDate t = state.getDate();
            Vector3D pos1 = state.getPVCoordinates(inertialFrame).getPosition();
            Vector3D pos2 = otherSatProp.propagate(t).getPVCoordinates(inertialFrame).getPosition();
            double distance = pos1.distance(pos2);
            return maxDistance - distance;
        }

        @Override
        protected MaxRangeDetector create(AdaptableInterval newMaxCheck, double newThreshold,
                                          int newMaxIter, EventHandler newHandler) {
            return new MaxRangeDetector(otherSatProp, maxDistance, newMaxCheck, newThreshold, newMaxIter, newHandler);
        }
    }



}