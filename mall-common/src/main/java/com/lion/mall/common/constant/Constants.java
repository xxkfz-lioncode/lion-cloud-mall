package com.lion.mall.common.constant;

/**
 * 通用常量
 *
 * @author lion
 */
public interface Constants {

    /** 网关解析 token 后向下游传递的用户 ID 请求头 */
    String HEADER_USER_ID = "X-User-Id";

    /** Sa-Token 的 token 名称（请求头） */
    String TOKEN_NAME = "satoken";

    /** 订单号前缀 */
    String ORDER_NO_PREFIX = "LM";
}
