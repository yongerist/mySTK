package com.bupt;

import org.orekit.data.DataContext;

import org.orekit.data.DirectoryCrawler;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


public class OrekitTest {
    public static void main(String[] args) {

        // 替换为实际的 orekit 数据目录路径
        File orekitData = new File("D:\\Code\\code_need\\orekit-data");
        // 通过 DataContext 获取默认的数据提供管理器
        DataContext.getDefault().getDataProvidersManager().addProvider(new DirectoryCrawler(orekitData));
        // 创建轨道参数（示例值，实际值可以根据需要调整）
        try {
            // 时间戳字符串
            String timestampStr = "2025-03-04T12:00:00Z";

            // 使用 SimpleDateFormat 解析时间戳字符串为 Date 对象
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // 设置时区为 UTC
            Date timestamp = sdf.parse(timestampStr);

            OrbitParams params = new OrbitParams(
                    7000000,  // 半长轴 7000 km
                    0.001,    // 偏心率
                    45.0,     // 倾角 45°
                    90.0,     // 升交点赤经 90°
                    0.0,      // 近地点真距 0°
                    timestamp    // 时间戳
            );

            // 创建卫星轨道服务实例
            SatelliteOrbitService service = new SatelliteOrbitService();

            // 计算卫星状态
            SatelliteState state = service.calculateState(params);

            // 输出结果
            System.out.println("Latitude: " + state.getLatitude() + "°");
            System.out.println("Longitude: " + state.getLongitude() + "°");
            System.out.println("Altitude: " + state.getAltitude() + " meters");
            System.out.println("Latitude rate: " + state.getLatRate() + " deg/sec");
            System.out.println("Longitude rate: " + state.getLonRate() + " deg/sec");
            System.out.println("Altitude rate: " + state.getAltRate() + " meters/sec");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
