package com.bupt.satviz.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.orekit.time.AbsoluteDate;

/**
 * 可见性窗口类
 * 用于存储指定时间段内的可见性窗口
 * 说明：如果某一窗口在给定预测区间结束时仍未结束，
 * endTime 设为 null，
 * durationSeconds 表示从开始到预测结束的时长。
 * 通常根据事件检测结果来生成这些窗口对象。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VisibilityWindow {
    private AbsoluteDate startTime;
    private AbsoluteDate endTime;
    private double durationSeconds;
}
