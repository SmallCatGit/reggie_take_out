package com.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.entity.*;
import com.reggie.mapper.OrderMapper;
import com.reggie.service.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private UserService userService;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private OrderDetailService orderDetailService;

    // 用户下单（账单支付）
    @Override
    @Transactional
    public void submit(Orders orders) {
        // 获得当前用户id
        Long userId = BaseContext.getThreadLocal();

        // 查询当前用户的购物车数据
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, userId);
        List<ShoppingCart> shoppingCartList = shoppingCartService.list(queryWrapper);

        if (shoppingCartList == null || shoppingCartList.size() == 0) {
            throw new CustomException("购物车为空，不需要支付");
        }

        // 查询用户数据
        User user = userService.getById(userId);

        // 查询收货地址数据
        Long addressBookId = orders.getAddressBookId();
        AddressBook addressBook = addressBookService.getById(addressBookId);
        if (addressBook == null) {
            throw new CustomException("用户收货地址为空，不能下单");
        }

        // 构造总金额变量(原子操作，保证在多线程、高并发下计算也没问题)
        AtomicInteger amount = new AtomicInteger(0);

        // 调用框架中的类生成订单号
        long orderId = IdWorker.getId();

        // 遍历购物车数据，计算总金额和封装订单明细数据
        List<OrderDetail> orderDetails = shoppingCartList.stream().map(item -> {
            // 创建订单明细实体
            OrderDetail orderDetail = new OrderDetail();
            // 设置订单id
            orderDetail.setOrderId(orderId);
            // 设置当前菜品或者套餐的份数
            orderDetail.setNumber(item.getNumber());
            // 设置口味信息
            orderDetail.setDishFlavor(item.getDishFlavor());
            // 设置菜品id
            orderDetail.setDishId(item.getDishId());
            // 设置套餐id
            orderDetail.setSetmealId(item.getSetmealId());
            // 设置菜品名称或者套餐名称
            orderDetail.setName(item.getName());
            // 设置图片
            orderDetail.setImage(item.getImage());
            // 设置当前金额，是单份金额
            orderDetail.setAmount(item.getAmount());

            /*//拷贝购物车中的信息到订单明细表中(和上面注释的信息相同)
            BeanUtils.copyProperties(item, orderDetail);*/

            /*item.getAmount().multiply(new BigDecimal(item.getNumber())).intValue()对当前这个菜品或者套餐金额累加
            （比如：一个啤酒8块，订单里面有两个，计算结果就是8 x 2 = 16）*/
            // 计算购物车总金额
            amount.addAndGet(item.getAmount().multiply(new BigDecimal(item.getNumber())).intValue());
            return orderDetail;

        }).collect(Collectors.toList());

        // 向订单表中插入数据，一条数据（无论多少个菜品和套餐，对应的购物车只有一个，也就只有一个订单）
        // 1、先设置订单表中未传递过来的数据
        // 1-1设置订单号
        orders.setNumber(String.valueOf(orderId));
        // 1-2设置订单id
        orders.setId(orderId);
        // 1-3设置订单状态，待派送
        orders.setStatus(2);
        // 1-4设置用户id
        orders.setUserId(userId);
        // 1-5下单时间
        orders.setOrderTime(LocalDateTime.now());
        // 1-6设置下单时间
        orders.setCheckoutTime(LocalDateTime.now());
        // 1-7设置订单总金额
        orders.setAmount(new BigDecimal(amount.get()));
        // 1-8设置订单电话
        orders.setPhone(addressBook.getPhone());
        // 1-9设置用户姓名
        orders.setUserName(user.getName());
        // 1-10设置收货人
        orders.setConsignee(addressBook.getConsignee());
        // 1-11设置收货地址
        orders.setAddress((addressBook.getProvinceName() == null ? "" : addressBook.getProvinceName())
                + (addressBook.getCityName() == null ? "" : addressBook.getCityName())
                + (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName())
                + (addressBook.getDetail() == null ? "" : addressBook.getDetail()));
        this.save(orders);

        // 向订单明细表插入数据，可能是多条数据
        orderDetailService.saveBatch(orderDetails);

        // 清空购物车(根据前面构造的购物车查询对象，清空购物车)
        shoppingCartService.remove(queryWrapper);
    }
}
