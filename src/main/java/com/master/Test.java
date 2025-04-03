package com.master;

import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DirectoryCrawler;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

import java.io.File;

public class Test {
    public static void main(String[] args) {
        // 替换为实际的 orekit 数据目录路径
        File orekitData = new File("/Users/liuzhengyang/Downloads/orekit-data");
        // 通过 DataContext 获取默认的数据提供管理器
        DataContext.getDefault().getDataProvidersManager().addProvider(new DirectoryCrawler(orekitData));
        /*
         *  WGS84椭球作为地球模型
         *  惯性系采用地心惯性系 EME2000
         *  地心固定系采用 ITRF
         */
        Frame inertialFrame = FramesFactory.getEME2000();
        Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        OneAxisEllipsoid earth = new OneAxisEllipsoid(
                Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_FLATTENING,
                earthFrame);


    }
}
