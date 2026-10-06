package com.lion.mall.common.result;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 分页响应体
 *
 * @param <T> 列表元素类型
 * @author lion
 */
@Data
public class PageResult<T> implements Serializable {

    /** 当前页 */
    private Long pageNo;
    /** 每页条数 */
    private Long pageSize;
    /** 总条数 */
    private Long total;
    /** 总页数 */
    private Long totalPage;
    /** 列表数据 */
    private List<T> list = Collections.emptyList();

    public static <T> PageResult<T> of(Long pageNo, Long pageSize, Long total, List<T> list) {
        PageResult<T> page = new PageResult<>();
        page.setPageNo(pageNo);
        page.setPageSize(pageSize);
        page.setTotal(total);
        page.setTotalPage(pageSize == null || pageSize == 0 ? 0 : (total + pageSize - 1) / pageSize);
        page.setList(list);
        return page;
    }

    /** 数据类型转换 */
    public static <S, T> PageResult<T> convert(PageResult<S> source, Function<S, T> converter) {
        return of(source.getPageNo(), source.getPageSize(), source.getTotal(),
                source.getList().stream().map(converter).toList());
    }
}
