package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.entity.Orders;
import com.reggie.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 订单
 */
@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 订单支付
     *
     * @param orders
     * @return
     */
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders) {
        log.info("订单数据:{}", orders);
        orderService.submit(orders);
        return R.success("支付成功");
    }

    /**
     * 订单明细分页查询
     *
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String number, String beginTime, String endTime) {

        log.info("page:{},pageSize:{},number:{},beginTime:{},endTime:{}", page, pageSize, number, beginTime, endTime);
        //构造分页构造器
        Page<Orders> ordersPage = new Page<>(page, pageSize);

        //创建条件构造器
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();

        //添加查询条件
        //SQL:select * from orders where number like ? and checkout_time between ? and ? order by checkout_time Desc
        //如果number不为空，使用like模糊查询
        queryWrapper.like(StringUtils.isNotEmpty(number), Orders::getNumber, number);
        //如果开始和结束时间不为空，使用between添加时间区间
        queryWrapper.between(StringUtils.isNotEmpty(beginTime) && StringUtils.isNotEmpty(endTime), Orders::getCheckoutTime, beginTime, endTime);
        //按照付款时间降序排序
        queryWrapper.orderByDesc(Orders::getCheckoutTime);

        //根据条件执行分页查询
        orderService.page(ordersPage, queryWrapper);

        return R.success(ordersPage);
    }

    /**
     * 派送订单
     * @param order
     * @return
     */
    @PutMapping
    public R<String> deliver(@RequestBody Orders order) {
        log.info("order:{}", order);

        LambdaUpdateWrapper<Orders> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Orders::getId, order.getId());
        updateWrapper.set(Orders::getStatus, order.getStatus());

        //SQL:update orders set status = ？ where id = ？
        orderService.update(updateWrapper);
        return R.success("订单状态更新成功");
    }
}
