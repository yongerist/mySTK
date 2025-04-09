package com.bupt.satviz.config;

import org.orekit.data.DataContext;
import org.orekit.data.DirectoryCrawler;

import java.io.File;

/**
 * OrekitConfig 类集中管理 Orekit 的数据加载和初始化操作。
 * 主要用于加载 orekit-data 数据文件，使 Orekit 在运行时能够获取必要的天文和轨道计算数据。
 */
public class OrekitConfig {

    /**
     * 初始化 Orekit 数据，加载 orekit-data 目录中的数据文件。
     */
    public static void initialize() {
        // 指定 orekit-data 数据目录的路径
        File orekitData = new File("orekit-data");

        if (!orekitData.exists() || !orekitData.isDirectory()) {
            throw new RuntimeException("Orekit 数据目录未找到，请检查路径：" + orekitData.getAbsolutePath());
        }

        // 通过 DataContext 加载数据
        DataContext.getDefault()
                   .getDataProvidersManager()
                   .addProvider(new DirectoryCrawler(orekitData));

//        System.out.println("Orekit 已成功加载数据目录：" + orekitData.getAbsolutePath());
    }
}
