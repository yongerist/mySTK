package com.access;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.events.Action;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.NadirPointing;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.DataContext;
import org.orekit.data.DirectoryCrawler;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.geometry.fov.CircularFieldOfView;
import org.orekit.geometry.fov.FieldOfView;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.events.BooleanDetector;
import org.orekit.propagation.events.ElevationDetector;
import org.orekit.propagation.events.EventDetector;
import org.orekit.propagation.events.FieldOfViewDetector;
import org.orekit.propagation.events.handlers.EventHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.io.File;
/*
 * 3*2颗卫星网络 与 1个地面站 的可见性分析
 */


public class Test {
    public static void main(String[] args) {
        // 替换为实际的 orekit 数据目录路径
        File orekitData = new File("/Users/liuzhengyang/Downloads/orekit-data");
        // 通过 DataContext 获取默认的数据提供管理器
        DataContext.getDefault().getDataProvidersManager().addProvider(new DirectoryCrawler(orekitData));

        // 准备好六颗卫星
        // 轨道参数：半长轴、偏心率、倾角、升交点赤经、近地点真距、真近点角
        double semiMajorAxis = 6878.14 * 1000; // 半长轴 (7000 km)
        double eccentricity = 0.0;   // 偏心率 近圆轨道
        double inclination = 90.0;     // 倾角 (90°)
        double inclination2 = 85.0;     // 倾角 (85°)
        double inclination3 = 80.0;     // 倾角 (80°)
        double raan = 0.0;           // 升交点赤经 (0°)
        double argumentOfPerigee = 0.0; // 近地点真距 (0°)
        double trueAnomaly = 0.0;    // 真近点角 (0°)
        double trueAnomaly2 = 5.0;    // 真近点角 (0°)
        AbsoluteDate startDate = new AbsoluteDate(2025, 1, 1, 4, 0, 0.0, TimeScalesFactory.getUTC());
        var inertialFrame = FramesFactory.getEME2000();
        // 创建轨道
        KeplerianOrbit orbit11 = new KeplerianOrbit(semiMajorAxis, eccentricity, Math.toRadians(inclination), Math.toRadians(raan), Math.toRadians(argumentOfPerigee), Math.toRadians(trueAnomaly), PositionAngleType.TRUE, inertialFrame, startDate, Constants.WGS84_EARTH_MU);
        KeplerianOrbit orbit12 = new KeplerianOrbit(semiMajorAxis, eccentricity, Math.toRadians(inclination), Math.toRadians(raan), Math.toRadians(argumentOfPerigee), Math.toRadians(trueAnomaly2), PositionAngleType.TRUE, inertialFrame, startDate, Constants.WGS84_EARTH_MU);

        KeplerianOrbit orbit21 = new KeplerianOrbit(semiMajorAxis, eccentricity, Math.toRadians(inclination2), Math.toRadians(raan), Math.toRadians(argumentOfPerigee), Math.toRadians(trueAnomaly), PositionAngleType.TRUE, inertialFrame, startDate, Constants.WGS84_EARTH_MU);
        KeplerianOrbit orbit22 = new KeplerianOrbit(semiMajorAxis, eccentricity, Math.toRadians(inclination2), Math.toRadians(raan), Math.toRadians(argumentOfPerigee), Math.toRadians(trueAnomaly2), PositionAngleType.TRUE, inertialFrame, startDate, Constants.WGS84_EARTH_MU);

        KeplerianOrbit orbit31 = new KeplerianOrbit(semiMajorAxis, eccentricity, Math.toRadians(inclination3), Math.toRadians(raan), Math.toRadians(argumentOfPerigee), Math.toRadians(trueAnomaly), PositionAngleType.TRUE, inertialFrame, startDate, Constants.WGS84_EARTH_MU);
        KeplerianOrbit orbit32 = new KeplerianOrbit(semiMajorAxis, eccentricity, Math.toRadians(inclination3), Math.toRadians(raan), Math.toRadians(argumentOfPerigee), Math.toRadians(trueAnomaly2), PositionAngleType.TRUE, inertialFrame, startDate, Constants.WGS84_EARTH_MU);

        KeplerianPropagator cir1Sat1 = new KeplerianPropagator(orbit11);
        KeplerianPropagator cir1Sat2 = new KeplerianPropagator(orbit12);

        KeplerianPropagator cir2Sat1 = new KeplerianPropagator(orbit21);
        KeplerianPropagator cir2Sat2 = new KeplerianPropagator(orbit22);

        KeplerianPropagator cir3Sat1 = new KeplerianPropagator(orbit31);
        KeplerianPropagator cir3Sat2 = new KeplerianPropagator(orbit32);

        //准备初始数据
        final TimeScale utc = TimeScalesFactory.getUTC();
        final Frame ecef = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        final BodyShape earth = ReferenceEllipsoid.getWgs84(ecef);

        // 准备地面站
        final double longitude = FastMath.toRadians(-160.165);
        final double latitude = FastMath.toRadians(36.1922);
        final double altitude = 0.;
        final GeodeticPoint station1 = new GeodeticPoint(latitude, longitude, altitude);
        final TopocentricFrame sta1Frame = new TopocentricFrame(earth, station1, "station1");


        //准备探测器
        final FieldOfView fov = new CircularFieldOfView(Vector3D.PLUS_K, FastMath.toRadians(45), FastMath.toRadians(0));
        final FieldOfViewDetector fd = new FieldOfViewDetector(sta1Frame, fov);
        final ElevationDetector ed = new ElevationDetector(sta1Frame).withConstantElevation(0.);
        final double maxCheckInterval = 60.0;
        final BooleanDetector detector = BooleanDetector.andCombine(ed, BooleanDetector.notCombine(fd))
                .withMaxCheck(maxCheckInterval)
                .withHandler(new EventHandler() {
                    private AbsoluteDate start;

                    @Override
                    public Action eventOccurred(SpacecraftState s, EventDetector detector, boolean increasing) throws OrekitException {
                        BooleanDetector booleanDetector = (BooleanDetector) detector;
                        if (increasing) {
                            start = s.getDate();
                            System.out.println("Visibility on " + sta1Frame.getName() + " 开始： " + s.getDate());
                        } else {
                            final double duration = s.getDate().durationFrom(start);
                            System.out.println("Visibility on " + sta1Frame.getName() + " 结束： " + s.getDate());
                            System.out.println("Pass duration: " + duration + " s");
                        }
                        return Action.CONTINUE;
                    }
                });

        NadirPointing nadirLaw = new NadirPointing(FramesFactory.getEME2000(), earth);
        cir1Sat1.setAttitudeProvider(nadirLaw);
        cir1Sat2.setAttitudeProvider(nadirLaw);
        cir2Sat1.setAttitudeProvider(nadirLaw);
        cir2Sat2.setAttitudeProvider(nadirLaw);
        cir3Sat1.setAttitudeProvider(nadirLaw);
        cir3Sat2.setAttitudeProvider(nadirLaw);

        System.out.println("cir1Sat1");
        cir1Sat1.addEventDetector(detector);
        final SpacecraftState finalState = cir1Sat1.propagate(startDate.shiftedBy(3600.));

        System.out.println("cir1Sat2");
        cir1Sat2.addEventDetector(detector);
        final SpacecraftState finalState2 = cir1Sat2.propagate(startDate.shiftedBy(3600.));

        System.out.println("cir2Sat1");
        cir2Sat1.addEventDetector(detector);
        final SpacecraftState finalState3 = cir2Sat1.propagate(startDate.shiftedBy(3600.));

        System.out.println("cir2Sat2");
        cir2Sat2.addEventDetector(detector);
        final SpacecraftState finalState4 = cir2Sat2.propagate(startDate.shiftedBy(3600.));

        System.out.println("cir3Sat1");
        cir3Sat1.addEventDetector(detector);
        final SpacecraftState finalState5 = cir3Sat1.propagate(startDate.shiftedBy(3600.));

        System.out.println("cir3Sat2");
        cir3Sat2.addEventDetector(detector);
        final SpacecraftState finalState6 = cir3Sat2.propagate(startDate.shiftedBy(3600.));
    }
}
