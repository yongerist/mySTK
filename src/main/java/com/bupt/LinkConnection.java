package com.bupt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class LinkConnection {
    private String lcId;    // 链接连接 ID

    private String lcName;  // 链接连接名称

    private String lnpId1;  // 链接连接的第一个节点 ID

    private String lnpId2;  // 链接连接的第二个节点 ID

    private Integer bandwidth;  // 链接连接的带宽

    //TODO：这是什么？
    private String gvnType;
    private String gvnId;


    @Override
    public String toString() {
        return "LinkConnection{" +
                "lcId='" + lcId + '\'' +
                ", lcName='" + lcName + '\'' +
                ", lnpId1='" + lnpId1 + '\'' +
                ", lnpId2='" + lnpId2 + '\'' +
                ", bandwidth=" + bandwidth +
                ", gvnType='" + gvnType + '\'' +
                ", gvnId='" + gvnId + '\'' +
                '}';
    }
}
