package com.bupt;

import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import java.io.File;

public class OrekitInit {
    static {
        // 替换为实际的 orekit 数据目录路径
        File orekitData = new File("D:\\Code\\code_need\\orekit-data");
        // 通过 DataContext 获取默认的数据提供管理器
        DataContext.getDefault().getDataProvidersManager().addProvider(new DirectoryCrawler(orekitData));
    }
}