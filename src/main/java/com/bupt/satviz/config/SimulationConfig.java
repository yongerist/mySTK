package com.bupt.satviz.config;

import com.bupt.satviz.model.GroundStation;
import com.bupt.satviz.model.KeplerianElements;
import lombok.Getter;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor; // 导入 Constructor 以便更安全地解析


import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects; // 导入 Objects 用于 null 检查

@Getter
public class SimulationConfig {

    private AbsoluteDate startDate;
    private AbsoluteDate endDate;
    private List<KeplerianElements> satelliteOrbits;
    private List<GroundStation> groundStations;
    // 可以选择性地添加其他配置参数
    // private double minElevationDeg;
    // private double coverageHalfAngleDeg;
    // private double maxInterSatDistanceMeters;

    public SimulationConfig(String configFile) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configFile)) {
            if (inputStream == null) {
                throw new RuntimeException("配置文件未找到：" + configFile);
            }


            Yaml yaml = new Yaml(new Constructor(Map.class));
            Map<String, Object> root = yaml.load(inputStream);

            if (root == null || !(root.get("simulation") instanceof Map)) {
                throw new RuntimeException("配置文件格式错误：缺少 'simulation' 根节点或其不是 Map 类型");
            }
            Map<String, Object> simulation = (Map<String, Object>) root.get("simulation");

            // 解析时间
            String startTimeStr = getString(simulation, "startTime");
            double durationSeconds = getDouble(simulation, "durationSeconds");
            this.startDate = new AbsoluteDate(startTimeStr, TimeScalesFactory.getUTC());
            this.endDate = startDate.shiftedBy(durationSeconds);

            // --- 解析卫星配置列表 ---
            Object satellitesObj = simulation.get("satellites");
            if (!(satellitesObj instanceof List)) {
                throw new RuntimeException("配置文件错误：'satellites' 必须是一个列表");
            }
            List<Map<String, Object>> satList = (List<Map<String, Object>>) satellitesObj;
            this.satelliteOrbits = new ArrayList<>();

            for (Map<String, Object> satMap : satList) {
                double semiMajorAxis = getDouble(satMap, "semiMajorAxis");
                double eccentricity = getDouble(satMap, "eccentricity");
                double inclination = getDouble(satMap, "inclination");
                double raan = getDouble(satMap, "raan");
                double argPerigee = getDouble(satMap, "argPerigee");
                // --- 直接读取 YAML 中提供的最终 trueAnomaly ---
                double trueAnomaly = getDouble(satMap, "trueAnomaly");
                // --- 读取结束 ---
                String epochStr = getString(satMap, "epoch");
                AbsoluteDate epoch = new AbsoluteDate(epochStr, TimeScalesFactory.getUTC());

                // 当前代码假设 YAML 已包含最终的 trueAnomaly
                KeplerianElements element = new KeplerianElements(
                        semiMajorAxis, eccentricity, inclination,
                        raan, argPerigee, trueAnomaly, epoch);
                satelliteOrbits.add(element);
            }

            // --- 解析地面站配置列表 ---
            Object groundStationsObj = simulation.get("groundStations");
            if (!(groundStationsObj instanceof List)) {
                throw new RuntimeException("配置文件错误：'groundStations' 必须是一个列表");
            }
            List<Map<String, Object>> gsList = (List<Map<String, Object>>) groundStationsObj;
            this.groundStations = new ArrayList<>();
            for (int i = 0; i < gsList.size(); i++) { // 使用索引 i 作为 ID
                Map<String, Object> gsMap = gsList.get(i);
                double lat = getDouble(gsMap, "lat");
                double lon = getDouble(gsMap, "lon");
                double alt = getDouble(gsMap, "alt");
                groundStations.add(new GroundStation(i, lat, lon, alt));
            }

            // --- 可选：解析其他可见性参数 ---
            /*
            this.minElevationDeg = getDoubleOrDefault(simulation, "minElevationDeg", 0.0);
            this.coverageHalfAngleDeg = getDoubleOrDefault(simulation, "coverageHalfAngleDeg", 90.0); // 默认不约束
            this.maxInterSatDistanceMeters = getDoubleOrDefault(simulation, "maxInterSatDistanceMeters", Double.POSITIVE_INFINITY); // 默认不约束
            */

        } catch (Exception e) {
            // 包装原始异常以保留堆栈跟踪
            throw new RuntimeException("加载 SimulationConfig 失败：" + e.getMessage(), e);
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        throw new RuntimeException("配置文件错误：键 '" + key + "' 的值不是 String 类型或缺失");
    }

    private String getStringOrDefault(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }


    private double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        throw new RuntimeException("配置文件错误：键 '" + key + "' 的值不是 Number 类型或缺失");
    }

    private double getDoubleOrDefault(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }
}