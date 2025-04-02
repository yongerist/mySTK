package com.bupt;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 查询一个天基节点时，应该返回的数据。
 */
//TODO：计算结果出来后，要去用这个类拿数据。
@Data
public class SpaceBasedNodeResponse {
    private String spaceBasedNodeId; // 节点名字

    private BigDecimal latitude; // 纬度

    private BigDecimal longitude; // 经度

    private Double altitude; // 高度

    private Double lonRate; // 经度变化率

    private Double latRate; // 纬度变化率

    private Double altRate; // 高度变化率

    @Override
    public String toString() {
        return "SpaceBasedNodeSTK{" +
                "spaceBasedNodeId='" + spaceBasedNodeId + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", altitude=" + altitude +
                ", lonRate=" + lonRate +
                ", latRate=" + latRate +
                ", altRate=" + altRate +
                '}';
    }

}
