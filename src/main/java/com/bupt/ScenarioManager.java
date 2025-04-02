package com.bupt;


import lombok.Data;

import java.util.Date;
import java.util.List;



@Data
//  管理场景的类（单例）
public class ScenarioManager {
    //    private Long scenarioId; // 场景 ID
    //    private String name; // 场景名称
    private Date startTime; // 场景起始时间
    private Date endTime; // 场景结束时间
    private int timeSpan; // 时间跨度（单位：秒）
    private List<Satellite> satellites; // 卫星列表
    private List<Facility> facilities; // 地面站列表
    private List<LinkConnection> linkConnections; // 链接连接列表


    // 单例模式
    private static  ScenarioManager scenarioManager = null;

    private ScenarioManager() {
    }

    public static ScenarioManager getInstance() {
        if (scenarioManager == null) {
            scenarioManager = new ScenarioManager();
        }
        return scenarioManager;
    }

/*
      设置时间范围
    - 输入：两个 Date 类型的参数，表示场景的起始时间和结束时间
    - 输出：boolean 类型，表示场景时间是否成功设置
    - 作用：设置场景的时间范围，用于控制卫星和地面站等对象的仿真时间
* */
    public boolean setAnalysisInterval(Date startTime, Date endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
        return true;
    }


}
