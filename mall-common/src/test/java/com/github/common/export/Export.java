package com.github.common.export;

import lombok.Data;

@Data
public class Export {

    @ExportColumn("省份")
    private String province;

    @ExportColumn("省份")
    private String city;

    @ExportColumn("省份")
    private String area;

    @ExportColumn("订单笔数")
    private Long orderCount;

    @ExportColumn("商品数量")
    private Long goodsNum;

    @ExportColumn("结算金额")
    private String totalMoney;
}
