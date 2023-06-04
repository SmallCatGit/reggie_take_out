package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.entity.AddressBook;
import com.reggie.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 地址簿
 */
@Slf4j
@RestController
@RequestMapping("/addressBook")
public class AddressBookController {
    @Autowired
    private AddressBookService addressBookService;

    /**
     * 新增地址
     *
     * @param addressBook
     * @return
     */
    @PostMapping
    public R<AddressBook> save(@RequestBody AddressBook addressBook) {
        log.info("addressBook:{}", addressBook);

        //设置用户id，先通过自定义获取线程id类获取当前线程id，再设置
        addressBook.setUserId(BaseContext.getThreadLocal());

        addressBookService.save(addressBook);

        return R.success(addressBook);
    }

    /**
     * 查询当前用户的全部地址
     *
     * @param addressBook
     * @return
     */
    @GetMapping("/list")
    public R<List<AddressBook>> list(AddressBook addressBook) {
        //根据线程中存储的用户id，设置当前用户的user_id
        addressBook.setUserId(BaseContext.getThreadLocal());
        log.info("addressBook:{}", addressBook);

        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        //根据user_id查询数据库
        queryWrapper.eq(addressBook.getUserId() != null, AddressBook::getUserId, addressBook.getUserId());
        //将查询出来的结果按照更新时间降序排序
        queryWrapper.orderByDesc(AddressBook::getUpdateTime);

        //获取查询出来的list
        //SQL：select * from address_book where user_id = ? order by update_time desc
        List<AddressBook> list = addressBookService.list(queryWrapper);
        return R.success(list);
    }

    /**
     * 设置默认地址
     *
     * @param addressBook
     * @return
     */
    @PutMapping("/default")
    public R<AddressBook> setDefault(@RequestBody AddressBook addressBook) {//泛型可以是AddressBook，也可以是String
        log.info("addressBook:{}", addressBook);
        //根据线程查询出该用户下的所有地址
        //构造一个更新构造器
        //目的：SQL：update address_book set is_default = 0 where user_id = ?
        LambdaUpdateWrapper<AddressBook> updateWrapper = new LambdaUpdateWrapper<>();
        //添加更新条件(user_id = ?)
        updateWrapper.eq(AddressBook::getUserId, BaseContext.getThreadLocal());

        //将所有地址设为不是默认（is_default = 0）
        updateWrapper.set(AddressBook::getIsDefault, 0);

        //调用Service更新(执行对应的sql)
        addressBookService.update(updateWrapper);

        //将对应需要设置成默认的地址设置成默认（is_default = 1）
        addressBook.setIsDefault(1);
        //SQL：update address_book set is_default = 1 where id = ?
        addressBookService.updateById(addressBook);

        return R.success(addressBook);
    }

    /**
     * 查询默认地址
     *
     * @return
     */
    @GetMapping("/default")
    public R<AddressBook> getDefault() {
        //构造查询构造器
        LambdaQueryWrapper<AddressBook> queryWrapper = new LambdaQueryWrapper<>();
        //添加查询条件，根据user_id和线程中存储的user_id来查询
        queryWrapper.eq(AddressBook::getUserId, BaseContext.getThreadLocal());
        //添加查询条件，根据默认地址is_default = 1
        queryWrapper.eq(AddressBook::getIsDefault, 1);

        //通过service执行sql查出一个地址对象
        //SQL:select * from address_book where user_id = ? and id_default = ?
        AddressBook addressBook = addressBookService.getOne(queryWrapper);

        //判断对象是否存在
        if (addressBook != null) {
            return R.success(addressBook);
        } else {
            return R.error("该用户未设置默认地址");
        }
    }

    /**
     * 根据收货地址id查询对应收获地址信息
     *（这里点开后标签只能是默认的无，无法自动根据数据库更新）!!!!!!!!!!!!!!!!!!!!!!!
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<AddressBook> getByIdWithAddress(@PathVariable Long id) {
        //根据id获取到对应的AddressBook对象
        AddressBook addressBook = addressBookService.getById(id);

        //判断对象是否存在，存在返回对象给页面
        if (addressBook != null) {
            return R.success(addressBook);
        } else {
            //不存在，给页面返回错误信息
            return R.error("没有这个地址信息");
        }
    }

    /**
     * 保存地址信息
     *
     * @param addressBook
     * @return
     */
    @PutMapping
    public R<String> saveWithIdAddress(@RequestBody AddressBook addressBook) {
        log.info("addressBook: {}", addressBook);
        //SQL：update address_book set (...) where id = ？

        addressBookService.updateById(addressBook);

        return R.success("地址保存成功");
    }

    /**
     * 删除收货地址
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> delete(Long ids) {
        log.info("ids:{}", ids);
        addressBookService.removeById(ids);
        return R.success("地址删除成功");
    }
}
