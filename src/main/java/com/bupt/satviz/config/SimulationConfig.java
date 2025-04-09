package com.bupt.satviz.config;

import com.bupt.satviz.model.GroundStation;
import com.bupt.satviz.model.KeplerianElements;
import lombok.Getter;
import org.orekit.data.DataContext;
import org.orekit.data.DirectoryCrawler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public class SimulationConfig {

    // 提供加载好的参数供其他模块使用
    private AbsoluteDate startDate;
    private AbsoluteDate endDate;
    private List<KeplerianElements> satelliteOrbits;
    private List<GroundStation> groundStations;

    /**
     * 从指定的 YAML 配置文件加载仿真参数
     *
     * @param configFile YAML 文件在 resources 下的路径（例如 "simulation_scenario_1.yaml"）
     */
    public SimulationConfig(String configFile) {
        try {
            // 1. 读取 YAML 文件
            Yaml yaml = new Yaml();
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configFile);
            if (inputStream == null) {
                throw new RuntimeException("未找到配置文件：" + configFile);
            }
            Map<String, Object> obj = yaml.load(inputStream);
            Map<String, Object> simulation = (Map<String, Object>) obj.get("simulation");

            // 2. 解析开始时间和持续时长
            String startTimeStr = (String) simulation.get("startTime");
            double durationSeconds = ((Number) simulation.get("durationSeconds")).doubleValue();
            this.startDate = new AbsoluteDate(startTimeStr, TimeScalesFactory.getUTC());
            this.endDate = startDate.shiftedBy(durationSeconds);

            // 3. 解析卫星配置列表
            List<Map<String, Object>> satList = (List<Map<String, Object>>) simulation.get("satellites");
            this.satelliteOrbits = new ArrayList<>();
            for (Map<String, Object> satMap : satList) {
                double semiMajorAxis = ((Number) satMap.get("semiMajorAxis")).doubleValue();
                double eccentricity = ((Number) satMap.get("eccentricity")).doubleValue();
                double inclination = ((Number) satMap.get("inclination")).doubleValue();
                double raan = ((Number) satMap.get("raan")).doubleValue();
                double argPerigee = ((Number) satMap.get("argPerigee")).doubleValue();
                double trueAnomaly = ((Number) satMap.get("trueAnomaly")).doubleValue();
                String epochStr = (String) satMap.get("epoch");
                AbsoluteDate epoch = new AbsoluteDate(epochStr, TimeScalesFactory.getUTC());

                // 注意：KeplerianElements 构造函数顺序需与已有的定义一致
                KeplerianElements element = new KeplerianElements(
                        semiMajorAxis, eccentricity, inclination,
                        raan, argPerigee, trueAnomaly, epoch);
                satelliteOrbits.add(element);
            }

            // 4. 解析地面站配置列表
            List<Map<String, Object>> gsList = (List<Map<String, Object>>) simulation.get("groundStations");
            this.groundStations = new ArrayList<>();
            for (Map<String, Object> gsMap : gsList) {
                double lat = ((Number) gsMap.get("lat")).doubleValue();
                double lon = ((Number) gsMap.get("lon")).doubleValue();
                double alt = ((Number) gsMap.get("alt")).doubleValue();
                groundStations.add(new GroundStation(lat, lon, alt));
            }
        } catch (Exception e) {
            throw new RuntimeException("加载 SimulationConfig 失败：" + e.getMessage(), e);
        }
    }

}
