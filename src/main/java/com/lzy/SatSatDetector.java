package com.lzy;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DirectoryCrawler;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
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
import org.orekit.time.DateTimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.hipparchus.geometry.euclidean.threed.Line;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SatSatDetector {

    // 自定义检测器：视线无遮挡
    public static class LineOfSightDetector extends AbstractDetector<LineOfSightDetector> {
        private final Propagator otherSatPropagator;
        private final OneAxisEllipsoid earth;

        public LineOfSightDetector(Propagator otherSatPropagator, OneAxisEllipsoid earth,
                                   AdaptableInterval maxCheck, double threshold, int maxIter, EventHandler handler) {
            super(maxCheck, threshold, maxIter, handler);
            this.otherSatPropagator = otherSatPropagator;
            this.earth = earth;
        }

        @Override
        public double g(SpacecraftState state) {
            Vector3D pos1 = state.getPosition(earth.getFrame());
            Vector3D pos2 = otherSatPropagator.propagate(state.getDate()).getPosition(earth.getFrame());
            Line line = new Line(pos1, pos2, 1e-3);
            boolean isBlok = earth.getIntersectionPoint(line, pos1, earth.getFrame(), state.getDate()) == null;
//            System.out.print("没有被地球遮挡：" + isBlok + "      ");
            return earth.getIntersectionPoint(line, pos1, earth.getFrame(), state.getDate()) == null ? 1.0 : -1.0;
        }

        @Override
        protected LineOfSightDetector create(AdaptableInterval newMaxCheck, double newThreshold,
                                             int newMaxIter, EventHandler newHandler) {
            return new LineOfSightDetector(otherSatPropagator, earth, newMaxCheck, newThreshold, newMaxIter, newHandler);
        }
    }

    // 自定义检测器：最大距离约束
    public static class MaxRangeDetector extends AbstractDetector<MaxRangeDetector> {
        private final Propagator otherSatPropagator;
        private final double maxDistance;

        public MaxRangeDetector(Propagator otherSatPropagator, double maxDistance,
                                AdaptableInterval maxCheck, double threshold, int maxIter, EventHandler handler) {
            super(maxCheck, threshold, maxIter, handler);
            this.otherSatPropagator = otherSatPropagator;
            this.maxDistance = maxDistance;
        }

        @Override
        public double g(SpacecraftState state) {
            Vector3D pos1 = state.getPosition(FramesFactory.getEME2000());
            Vector3D pos2 = otherSatPropagator.propagate(state.getDate()).getPosition(FramesFactory.getEME2000());
            double distance = pos1.distance(pos2);
//            System.out.println("Distance between satellites: " + distance + " meters");
            boolean isConect = maxDistance - distance > 0.0;
//            System.out.println("卫星之间距离小于最大距离：" + isConect);
            return maxDistance - distance;
        }

        @Override
        protected MaxRangeDetector create(AdaptableInterval newMaxCheck, double newThreshold,
                                          int newMaxIter, EventHandler newHandler) {
            return new MaxRangeDetector(otherSatPropagator, maxDistance, newMaxCheck, newThreshold, newMaxIter, newHandler);
        }
    }

    public static void main(String[] args) {
        // 替换为实际的 orekit 数据目录路径
        File orekitData = new File("/Users/liuzhengyang/Downloads/orekit-data");
        // 通过 DataContext 获取默认的数据提供管理器
        DataContext.getDefault().getDataProvidersManager().addProvider(new DirectoryCrawler(orekitData));

        // 初始状态定义 ： date， orbit
        final TimeScale utc = TimeScalesFactory.getUTC(); // TimeScalesFactory 用于获取常用的时间尺度（例如 UTC）。
        final AbsoluteDate startDate  = new AbsoluteDate(2025, 1, 1, 4, 0, 00.000, utc);
        var inertialFrame = FramesFactory.getEME2000();
        OneAxisEllipsoid earth = new OneAxisEllipsoid(
                Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING,
                inertialFrame
        );
        final Frame ecef = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final BodyShape earthBody = ReferenceEllipsoid.getWgs84(ecef);

        // 定义卫星1和卫星2的轨道和初始状态
        final Propagator sat1 = getPropagator1(startDate);
        final Propagator sat2 = getPropagator2(startDate);

        // -------------------- 3. 创建检测器 -------------------- 
        RecordAndContinue handler = new RecordAndContinue();
        // 视线无遮挡检测器
        LineOfSightDetector losDetector = new LineOfSightDetector(
                sat2, earth,
                state -> AbstractDetector.DEFAULT_MAXCHECK, 1e-3, 100, handler
        ).withMaxCheck(60.0); // 每60秒检查一次

        // 最大距离检测器（5000公里）
        MaxRangeDetector rangeDetector = new MaxRangeDetector(
                sat2, 5000000.0,
                state -> AbstractDetector.DEFAULT_MAXCHECK, 1e-3, 100, handler
        ).withThreshold(1e-6); // 提高收敛精度

        // 组合约束（AND逻辑）
        BooleanDetector combinedDetector = BooleanDetector.andCombine(losDetector, rangeDetector).withHandler(handler);

        // -------------------- 4. 执行分析 -------------------- 
        sat1.addEventDetector(combinedDetector);
        // 获取初始状态
        SpacecraftState initialState = sat1.getInitialState();
        // 手动触发检测
        EventDetector detector = combinedDetector; // 组合后的检测器
        double g = detector.g(initialState);
        boolean isEvent = detector.g(initialState) > 0; // 检查初始状态是否满足条件

        if (isEvent) {
            handler.eventOccurred(initialState, detector, true); // 记录初始事件
        }

        sat1.propagate(startDate, startDate.shiftedBy(7200.0));
        List<RecordAndContinue.Event> events = handler.getEvents();

        // -------------------- 5. 输出结果 --------------------

        List<String> visibilityWindows = new ArrayList<>();
        AbsoluteDate windowStart = null;

        for (RecordAndContinue.Event event : events) {
            if (event.isIncreasing()) {
                // 记录开始时间
                windowStart = event.getState().getDate();
            } else {
                // 匹配到结束时间
                if (windowStart != null) {
                    AbsoluteDate windowEnd = event.getState().getDate();
                    double durationSeconds = windowEnd.durationFrom(windowStart);

                    // 格式化时间
                    String startTime = formatDate(windowStart);
                    String endTime = formatDate(windowEnd);
                    String duration = formatDuration(durationSeconds);

                    visibilityWindows.add(String.format(
                            "开始时间：%s；结束时间：%s；连接时间：%s；",
                            startTime, endTime, duration
                    ));

                    windowStart = null; // 重置，准备下一组
                }
            }
        }
        // 处理未闭合的窗口
        if (windowStart != null) {
            // 获取传播结束时间作为当前时间
            AbsoluteDate currentTime = sat1.getEphemerisGenerator().getGeneratedEphemeris().getMinDate();
            double durationSeconds = currentTime.durationFrom(windowStart);
            visibilityWindows.add(formatWindow(windowStart, null, durationSeconds));
        }
        // 输出结果
        System.out.println("可见性窗口列表：");
        visibilityWindows.forEach(System.out::println);


//        for (RecordAndContinue.Event event : events) {
//            System.out.printf("%s 事件类型：%s%n",
//                    event.getState().getDate(),
//                    event.isIncreasing() ? "可见开始" : "可见结束"
//            );
//        }
    }

    private static Propagator getPropagator1(AbsoluteDate initialDate) {
        double semiMajorAxis = 6878.14 * 1000; // 半长轴 (7000 km)
        double eccentricity = 0.0;   // 偏心率 近圆轨道
        double inclination = 90.0;     // 倾角 (90°)
        double raan = 0.0;           // 升交点赤经 (0°)
        double argumentOfPerigee = 0.0; // 近地点真距 (0°)
        double trueAnomaly = 0.0;    // 真近点角 (0°)
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
                PositionAngleType.TRUE, // 使用真近点角
                inertialFrame, // 使用 EME2000 参考框架 -- 轨道参考系 -- 地心惯性系
                initialDate, // 轨道模型的起始时间
                Constants.WGS84_EARTH_MU // 地球的引力常数
        );
        return new KeplerianPropagator(orbit);
    }

    private static Propagator getPropagator2(AbsoluteDate initialDate) {
        double semiMajorAxis = 6878.14 * 1000; // 半长轴 (7000 km)
        double eccentricity = 0.0;   // 偏心率 近圆轨道
        double inclination = 45.0;     // 倾角 (90°)
        double raan = 0.0;           // 升交点赤经 (0°)
        double argumentOfPerigee = 0.0; // 近地点真距 (0°)
        double trueAnomaly = 5.0;    // 真近点角 (0°)
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
                PositionAngleType.TRUE, // 使用真近点角
                inertialFrame, // 使用 EME2000 参考框架 -- 轨道参考系 -- 地心惯性系
                initialDate, // 轨道模型的起始时间
                Constants.WGS84_EARTH_MU // 地球的引力常数
        );
        return new KeplerianPropagator(orbit);
    }


    // --- 辅助方法：格式化日期和时间 ---
    private static String formatDate(AbsoluteDate date) {
        DateTimeComponents dt = date.getComponents(TimeScalesFactory.getUTC());
        return String.format("%04d-%02d-%02d %02d:%02d:%06.3f",
                dt.getDate().getYear(),
                dt.getDate().getMonth(),
                dt.getDate().getDay(),
                dt.getTime().getHour(),
                dt.getTime().getMinute(),
                dt.getTime().getSecond()
        );
    }

    // --- 辅助方法：格式化持续时间 ---
    private static String formatDuration(double seconds) {
    //        int hours = (int) (seconds / 3600);
    //        int minutes = (int) ((seconds % 3600) / 60);
    //        double remainingSeconds = seconds % 60;
        return String.format("%06.3f秒", seconds);
    }

    // --- 辅助方法：格式化窗口 ---
    private static String formatWindow(AbsoluteDate start, AbsoluteDate end, double durationSeconds) {
        String startTime = formatDate(start);
        String endTime = (end != null) ? formatDate(end) : "   <未结束>   ";
        String duration = (end != null) ?
                formatDuration(durationSeconds) :
                 "   <持续中>   ";
        return String.format("开始时间：%s；结束时间：%s；连接时间：%s；", startTime, endTime, duration);
    }
}
