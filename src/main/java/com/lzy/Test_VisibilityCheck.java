
package com.lzy;

import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.DataContext;
import org.orekit.data.DirectoryCrawler;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.io.File;


public class Test_VisibilityCheck {

    /**
     * Private constructor for utility class.
     */
    private Test_VisibilityCheck() {
    }


    public static void main(final String[] args) {
        // 替换为实际的 orekit 数据目录路径
        File orekitData = new File("/Users/liuzhengyang/Downloads/orekit-data");
        // 通过 DataContext 获取默认的数据提供管理器
        DataContext.getDefault().getDataProvidersManager().addProvider(new DirectoryCrawler(orekitData));


        // 初始状态定义 ： date， orbit
        final TimeScale utc = TimeScalesFactory.getUTC(); // TimeScalesFactory 用于获取常用的时间尺度（例如 UTC）。
        final AbsoluteDate initialDate = new AbsoluteDate(2025, 1, 1, 4, 0, 00.000, utc);
        final Propagator kepler = getPropagator(initialDate);

        // 地球和坐标系  FramesFactory 提供了获取常用参考系的方法
        // 获取地心固定参考系（ITRF），使用 IERS 2010 标准。
        final Frame ecef = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        // BodyShape 用于描述地球的形状。
        // 使用 WGS84 椭球模型描述地球形状。
        final BodyShape earth = ReferenceEllipsoid.getWgs84(ecef);

        // 站： GeodeticPoint 表示地球表面的一个点（以纬度、经度和海拔表示）。
        final double longitude = FastMath.toRadians(-0.13);
        final double latitude = FastMath.toRadians(7.05);
        final double altitude = 0.;
        final GeodeticPoint station1 = new GeodeticPoint(latitude, longitude, altitude);
        // TopocentricFrame 表示以该站点station1为原点的局部坐标系
        final TopocentricFrame sta1Frame = new TopocentricFrame(earth, station1, "station1");

        // 事件定义
        final double maxcheck = 10.0; // 最大检查间隔（单位秒），指定事件检测器在轨道传播时 每隔多久检查一次事件；
        final double threshold = 0.001; // 求解精度；
        // elevation：设定的最低仰角（5°，转换为弧度），只有当卫星的仰角大于此值时，认为卫星对地面站可见。
        final double elevation = FastMath.toRadians(5.0);
        // 当卫星刚进入可见区域（increasing 为 true）或离开可见区域（increasing 为 false）时，
        // 打印站点名称、状态（开始或结束）以及事件发生时的日期。
        final EventDetector sta1Visi =
                new ElevationDetector(maxcheck, threshold, sta1Frame).
                        withConstantElevation(elevation). //固定仰角阈值elevation。
                        withHandler((s, d, increasing) -> {  // 指定事件处理器
                    System.out.println(" Visibility on " +
                            ((ElevationDetector) d).getTopocentricFrame().getName() +
                            (increasing ? " 开始： " : " 结束： ") +
                            s.getDate().toStringWithoutUtcOffset(utc, 3));
//                    return increasing ? Action.CONTINUE : Action.STOP;
                    return Action.CONTINUE;
                });

        //将上述定义的 ElevationDetector 添加到 propagator 中，
        // 保证在轨道传播过程中检测卫星与地面站之间的可见性事件。
        kepler.addEventDetector(sta1Visi);

        // 传播 7200 秒（2小时）的轨道状态
        final SpacecraftState finalState = kepler.propagate(initialDate.shiftedBy(72000.));
//        System.out.println(" 持续时间 : " + finalState.getDate().durationFrom(initialDate));

    }


    private static Propagator getPropagator(AbsoluteDate initialDate) {
        double semiMajorAxis = 6878.14 * 1000; // 半长轴 (7000 km)
        double eccentricity = 0.0;   // 偏心率 近圆轨道
        double inclination = 90.0;     // 倾角 (90°)
        double raan = 0.0;           // 升交点赤经 (0°)
        double argumentOfPerigee = 0.0; // 近地点真距 (0°)
        double trueAnomaly = 0.0;    // 真近点角 (0°)
        AbsoluteDate startDate = new AbsoluteDate(2025, 1, 1, 4, 0, 0.0, TimeScalesFactory.getUTC());
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
                startDate, // 轨道模型的起始时间
                Constants.WGS84_EARTH_MU // 地球的引力常数
        );
        return new KeplerianPropagator(orbit);
    }

}
