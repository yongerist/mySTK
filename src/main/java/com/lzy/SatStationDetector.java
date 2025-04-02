package com.lzy;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.NadirPointing;
import org.orekit.bodies.BodyShape;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.BooleanDetector;
import org.orekit.propagation.events.EventDetector;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.DataContext;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.geometry.fov.CircularFieldOfView;
import org.orekit.geometry.fov.FieldOfView;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.FieldOfViewDetector;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.propagation.events.handlers.EventHandler;

import java.io.File;


public class SatStationDetector {
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
        final double longitude = FastMath.toRadians(-160.165);
        final double latitude = FastMath.toRadians(36.1922);
        final double altitude = 0.;
        final GeodeticPoint station1 = new GeodeticPoint(latitude, longitude, altitude);
        // TopocentricFrame 表示以该站点station1为原点的局部坐标系
        final TopocentricFrame sta1Frame = new TopocentricFrame(earth, station1, "station1");


        // 定义 FieldOfViewDetector
        // 这里使用 CircularFieldOfView，设置视场的主轴为地面站局部的正上方向（通常为 Vector3D.PLUS_K），视场半角为45°，旋转角为0°  fov被附加在卫星上
        final FieldOfView fov = new CircularFieldOfView(Vector3D.PLUS_K, FastMath.toRadians(45), FastMath.toRadians(0));
        // 容易误解的点：不在视线内:返回 false；    在视线内：返回 true
        final FieldOfViewDetector fd = new FieldOfViewDetector(sta1Frame, fov);
        // 定义 ElevationDetector，仰角设为0（始终满足）作为组合的一个条件
        final ElevationDetector ed = new ElevationDetector(sta1Frame).withConstantElevation(0.);

        // 使用 BooleanDetector 将两者组合：
        // 组合逻辑：ed AND (NOT fd)
        final double maxCheckInterval = 60.0;
        final BooleanDetector detector = BooleanDetector.andCombine(ed, BooleanDetector.notCombine(fd))
                .withMaxCheck(maxCheckInterval)
                .withHandler(new EventHandler(){
                    private AbsoluteDate start;

                    @Override
                    public Action eventOccurred(SpacecraftState s, EventDetector detector, boolean increasing) throws OrekitException {
                        // 强制转换为 BooleanDetector 以访问组合逻辑
                        BooleanDetector booleanDetector = (BooleanDetector) detector;

                        if (increasing) {
                            // 进入连接区域
                            start = s.getDate();
                            System.out.println("Visibility on " + sta1Frame.getName() + " 开始： " + s.getDate());
                        } else {
                            // 离开连接区域
                            final double duration = s.getDate().durationFrom(start);
                            System.out.println("Visibility on " + sta1Frame.getName() + " 结束： " + s.getDate());
                            System.out.println("Pass duration: " + duration + " s");
                        }
                        return Action.CONTINUE;
                    }
                });
        // 设置卫星的姿态，对准地面
        NadirPointing nadirLaw = new NadirPointing(FramesFactory.getEME2000(), earth);
        kepler.setAttitudeProvider(nadirLaw);
        // 将组合的事件检测器添加到传播器中
        kepler.addEventDetector(detector);

        // 轨道传播：传播一段较长的时间以捕捉整个期间内所有的可见连接事件
        final SpacecraftState finalState = kepler.propagate(initialDate.shiftedBy(3600.));
    }


    private static Propagator getPropagator(AbsoluteDate initialDate) {
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
}
