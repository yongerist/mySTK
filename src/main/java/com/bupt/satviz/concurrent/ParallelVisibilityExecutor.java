package com.bupt.satviz.concurrent;

import com.bupt.satviz.model.GroundStation;
import com.bupt.satviz.model.KeplerianElements;
import com.bupt.satviz.model.SatResult;
import com.bupt.satviz.model.VisibilityWindow;
import com.bupt.satviz.visibility.GroundStationVisibilityAnalyzer;
import com.bupt.satviz.visibility.InterSatelliteVisibilityAnalyzer;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.time.AbsoluteDate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ParallelVisibilityExecutor {
    /**
     * 并发计算所有卫星的可见性窗口。
     * @param allEphemerides 所有卫星的星历列表
     * @param groundStations 地面站列表
     * @param startDate      仿真开始时间
     * @param endDate        仿真结束时间
     * @return 每颗卫星的可见性结果列表
     * @throws InterruptedException 如果等待任务完成时线程被中断
     * @throws Exception 如果任务执行过程中抛出其他异常
     */
    public static List<SatResult> computeAllVisibilities(List<BoundedPropagator> allEphemerides,
                                                         List<GroundStation> groundStations,
                                                         AbsoluteDate startDate,
                                                         AbsoluteDate endDate) throws Exception {
        int numSatellites = allEphemerides.size();
        // 1. 创建固定大小的线程池（线程数可设为CPU核心数）
        int numThreads = Runtime.getRuntime().availableProcessors();
        System.out.println("  启动并行计算，使用 " + numThreads + " 个线程.");

        // 2. 创建固定大小线程池
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        // 3. 为每颗卫星创建可见性计算任务并提交到线程池
        List<Future<SatResult>> futures = new ArrayList<>();
        System.out.println("  为 " + numSatellites + " 颗卫星创建并提交可见性计算任务...");
        for (int satIndex = 0; satIndex < numSatellites; satIndex++) {
            // 创建任务，传递当前卫星的星历和其他所需数据
            Callable<SatResult> task = new SatelliteVisibilityTask(
                    satIndex,                       // 卫星 ID
                    allEphemerides.get(satIndex),   // 主卫星的星历
                    groundStations,                 // 所有地面站
                    allEphemerides,                 // 所有卫星的星历列表
                    startDate,
                    endDate
            );
            futures.add(executor.submit(task));
        }
        System.out.println("  所有任务已提交.");

        // 4. 收集所有任务的执行结果
        System.out.println("  等待并收集计算结果...");
        List<SatResult> allResults = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            Future<SatResult> future = futures.get(i);
            try {
                // future.get() 会阻塞直到任务完成或抛出异常
                SatResult result = future.get();
                allResults.add(result);
                // 可选：报告进度
                if ((i + 1) % 10 == 0 || i == futures.size() - 1) {
                    System.out.println("    已收集 " + (i + 1) + " / " + futures.size() + " 个任务的结果.");
                }
            } catch (InterruptedException e) {
                // 如果主线程在等待时被中断
                Thread.currentThread().interrupt(); // 重新设置中断状态
                System.err.println("  结果收集中断！");
                throw e; // 重新抛出中断异常
            } catch (Exception e) { // 捕获任务执行中可能抛出的其他异常
                System.err.println("  获取卫星 #" + i + " 的计算结果时出错: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                e.printStackTrace(); // 打印详细堆栈信息
            }
        }
        System.out.println("  所有计算结果收集完毕.");

        // 4. 关闭线程池
        System.out.println("  正在关闭线程池...");
        executor.shutdown(); // 不再接受新任务，等待现有任务完成
        return allResults;
    }
}
