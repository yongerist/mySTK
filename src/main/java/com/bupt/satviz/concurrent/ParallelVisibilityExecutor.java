package com.bupt.satviz.concurrent;

import com.bupt.satviz.model.GroundStation;
import com.bupt.satviz.model.KeplerianElements;
import com.bupt.satviz.model.SatResult;
import com.bupt.satviz.model.VisibilityWindow;
import com.bupt.satviz.visibility.GroundStationVisibilityAnalyzer;
import com.bupt.satviz.visibility.InterSatelliteVisibilityAnalyzer;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ParallelVisibilityExecutor {
    /**
     * 并发计算所有卫星的可见性窗口。
     * @param orbits         所有卫星轨道元素列表
     * @param groundStations 地面站列表
     * @param startDate      仿真开始时间
     * @param endDate        仿真结束时间
     * @return 每颗卫星的可见性结果列表
     */
    public static List<SatResult> computeAllVisibilities(List<KeplerianElements> orbits,
                                                         List<GroundStation> groundStations,
                                                         AbsoluteDate startDate,
                                                         AbsoluteDate endDate) throws Exception {
        // 1. 创建固定大小的线程池（线程数可设为CPU核心数）
        int numThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        // 2. 为每颗卫星创建可见性计算任务并提交到线程池
        List<Future<SatResult>> futures = new ArrayList<>();
        for (int satIndex = 0; satIndex < orbits.size(); satIndex++) {
            Callable<SatResult> task = new SatelliteVisibilityTask(
                    satIndex, orbits.get(satIndex),
                    groundStations, orbits,
                    startDate, endDate);
            futures.add(executor.submit(task));
        }

        // 3. 收集所有任务的执行结果
        List<SatResult> allResults = new ArrayList<>();
        for (Future<SatResult> future : futures) {
            SatResult result = future.get();  // 阻塞等待每个任务完成
            allResults.add(result);
        }

        // 4. 关闭线程池
        executor.shutdown();
        return allResults;
    }
}
