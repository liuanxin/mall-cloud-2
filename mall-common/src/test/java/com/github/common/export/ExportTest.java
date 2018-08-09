package com.github.common.export;

import com.github.common.json.JsonUtil;

import java.util.LinkedHashMap;
import java.util.List;

public class ExportTest {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        String s = "[{\"province\":\"广东省\",\"city\":\"深圳市\",\"area\":\"南山区\",\"orderCount\":5,\"goodsNum\":5,\"totalMoney\":\"0.10\"},{\"province\":\"内蒙古自治区\",\"city\":\"包头市\",\"area\":\"固阳县\",\"orderCount\":17,\"goodsNum\":27,\"totalMoney\":\"229.06\"},{\"province\":\"黑龙江省\",\"city\":\"齐齐哈尔市\",\"area\":\"龙江县\",\"orderCount\":244,\"goodsNum\":251,\"totalMoney\":\"297.76\"},{\"province\":\"浙江省\",\"city\":\"杭州市\",\"area\":\"富阳市\",\"orderCount\":2,\"goodsNum\":2,\"totalMoney\":\"0.30\"},{\"province\":\"湖南省\",\"city\":\"长沙市\",\"area\":\"岳麓区\",\"orderCount\":1,\"goodsNum\":1,\"totalMoney\":\"111.00\"},{\"province\":\"湖南省\",\"city\":\"岳阳市\",\"area\":\"岳阳县\",\"orderCount\":126,\"goodsNum\":405,\"totalMoney\":\"2003.96\"},{\"province\":\"广东省\",\"city\":\"深圳市\",\"area\":\"南山区\",\"orderCount\":44,\"goodsNum\":113,\"totalMoney\":\"1.13\"},{\"province\":\"广西壮族自治区\",\"city\":\"桂林市\",\"area\":\"七星区\",\"orderCount\":34,\"goodsNum\":127,\"totalMoney\":\"31006.85\"},{\"province\":\"四川省\",\"city\":\"自贡市\",\"area\":\"荣县\",\"orderCount\":18,\"goodsNum\":76,\"totalMoney\":\"3.85\"}]";
        String t = "{\"province\":\"省份\",\"city\":\"城市\",\"area\":\"县区\",\"orderCount\":\"订单笔数\",\"goodsNum\":\"商品数量\",\"totalMoney\":\"结算金额\"}";

        List<Export> tmpList = JsonUtil.toList(s, Export.class);
        LinkedHashMap<String, String> titleMap = JsonUtil.toObject(t, LinkedHashMap.class);
        FileExport.save(null, "中控辖区销售汇总列表", titleMap, tmpList, "/home/tony/desktop");
        FileExport.save("csv", "中控辖区销售汇总列表", titleMap, tmpList, "/home/tony/desktop");
    }
}
