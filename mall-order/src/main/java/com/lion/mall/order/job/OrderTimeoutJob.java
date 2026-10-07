package com.lion.mall.order.job;

import com.lion.mall.order.service.OrderService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 订单超时自动关单任务
 * <p>
 * 扫描「待支付且创建时间已超过阈值」的订单，逐个关闭并回滚库存。
 * 每条订单一个独立事务，单条失败不影响其余订单。
 * <p>
 * 任务参数（在 admin 后台的 Job 里填写 executor_param）：
 * <ul>
 *   <li>留空 → 默认 30 分钟 / 单次 100 条</li>
 *   <li>"30" → 超时 30 分钟</li>
 *   <li>"30,100" → 超时 30 分钟，单次最多处理 100 条</li>
 * </ul>
 *
 * @author lion
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutJob {

    /** 默认超时分钟数：下单后超过该时间仍未支付则关单 */
    private static final int DEFAULT_TIMEOUT_MINUTES = 30;
    /** 单次最多处理条数：防止一次性扫描过多拖垮服务 */
    private static final int DEFAULT_LIMIT = 100;

    private final OrderService orderService;

    @XxlJob("orderTimeoutHandler")
    public void execute() {
        int timeoutMinutes = DEFAULT_TIMEOUT_MINUTES;
        int limit = DEFAULT_LIMIT;

        String param = XxlJobHelper.getJobParam();
        if (param != null && !param.isBlank()) {
            String[] arr = param.split(",");
            try {
                timeoutMinutes = Integer.parseInt(arr[0].trim());
                if (arr.length > 1) {
                    limit = Integer.parseInt(arr[1].trim());
                }
            } catch (NumberFormatException e) {
                XxlJobHelper.log("任务参数格式错误（应为 超时分钟数[,单次条数]），本次使用默认值 {}/{}",
                        DEFAULT_TIMEOUT_MINUTES, DEFAULT_LIMIT);
            }
        }

        // 分片参数：admin 用「分片广播」策略时，每个实例只处理自己那一片，互不重复；
        // 非分片策略下 getShardTotal()=0、getShardIndex()=-1，此时不做分片过滤（等价于全量扫描）。
        int shardTotal = XxlJobHelper.getShardTotal();
        int shardIndex = XxlJobHelper.getShardIndex();

        // 1. 只查 ID：列表轻量化，避免大对象在内存中停留
        List<Long> orderIds = orderService.findTimeoutOrderIds(timeoutMinutes, limit, shardTotal, shardIndex);
        if (orderIds.isEmpty()) {
            XxlJobHelper.handleSuccess("无超时订单（阈值 " + timeoutMinutes + " 分钟）");
            return;
        }

        // 2. 逐单处理：每单独立事务，失败互不影响
        int success = 0;
        int fail = 0;
        for (Long orderId : orderIds) {
            try {
                orderService.closeTimeoutOrder(orderId);
                success++;
            } catch (Exception e) {
                fail++;
                log.error("关闭超时订单失败，orderId={}", orderId, e);
                XxlJobHelper.log("关闭超时订单失败，orderId=%s：%s", orderId, e.getMessage());
            }
        }

        String msg = String.format("超时关单完成：成功 %d 单，失败 %d 单（阈值 %d 分钟，分片 %d/%d）",
                success, fail, timeoutMinutes, shardIndex, shardTotal);
        XxlJobHelper.log(msg);
        log.info(msg);
        if (fail > 0) {
            XxlJobHelper.handleFail(msg);
        } else {
            XxlJobHelper.handleSuccess(msg);
        }
    }
}
