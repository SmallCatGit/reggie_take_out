package com.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.CustomException;
import com.reggie.dto.SetmealDto;
import com.reggie.entity.Setmeal;
import com.reggie.entity.SetmealDish;
import com.reggie.mapper.SetmealMapper;
import com.reggie.service.SetmealDishService;
import com.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {
    @Autowired
    private SetmealDishService setmealDishService;

    /**
     * 新增套餐，同时保存套餐和菜品的关联关系
     *
     * @param setmealDto
     */
    @Override
    @Transactional
    public void saveWithDish(SetmealDto setmealDto) {
        // 保存套餐基本信息，操作setmeal表，执行insert操作
        this.save(setmealDto);

        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();

        // 保存setmeal_dish表中的套餐id（setmealId）
        setmealDishes = setmealDishes.stream().map(item -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());

        // 保存套餐和菜品的关联信息，操作setmeal_dish表，执行insert操作
        setmealDishService.saveBatch(setmealDishes);

    }

    /**
     * 删除套餐，同时删除套餐和菜品的关联数据
     *
     * @param ids
     */
    @Override
    @Transactional
    public void removeWithDish(List<Long> ids) {
        // sql：select count(*) from setmeal where id in(...) and status = 1
        // 查询套餐状态，确定是否可以删除
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        // 查询所有满足条件的id
        queryWrapper.in(Setmeal::getId, ids);
        // 查询所有满足条件的状态
        queryWrapper.eq(Setmeal::getStatus, 1);

        // 计算满足条件的数量
        int count = this.count(queryWrapper);
        if (count > 0) {
            // 不能删除，抛出一个业务异常（自定义异常类抛出）
            throw new CustomException("套餐正在售卖，不能删除");
        }

        // 如果可以删除，先删除套餐表中的数据-->setmeal
        this.removeByIds(ids);

        // sql:delete from setmeal_dish where setmeal_id in (...)
        // 构造一个setmeal_dish条件构造器
        LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        // 添加满足setmeal_id的条件，查出来删除
        lambdaQueryWrapper.in(SetmealDish::getSetmealId, ids);

        // 再删除关系表中的数据-->setmeal_dish
        setmealDishService.remove(lambdaQueryWrapper);

    }

    /**
     * 根据套餐id查询套餐信息和套餐中的菜品信息
     *
     * @param id
     * @return
     */
    @Override
    public SetmealDto getByIdWithDish(Long id) {
        // 查询套餐表中的基本信息，封装为一个套餐对象
        Setmeal setmeal = this.getById(id);

        // 创建一个SetmealDto对象，并且拷贝套餐表中的数据
        SetmealDto setmealDto = new SetmealDto();
        BeanUtils.copyProperties(setmeal, setmealDto);

        // 根据套餐id，查询套餐口味表中的口味信息
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, id);

        // 将口味信息赋值给SetmealDto对象
        List<SetmealDish> setmealDishes = setmealDishService.list(queryWrapper);
        setmealDto.setSetmealDishes(setmealDishes);

        return setmealDto;
    }

    /**
     * 更新套餐信息，同时更新套餐中菜品信息
     *
     * @param setmealDto
     */
    @Override
    public void updateWithSetmeal(SetmealDto setmealDto) {
        // 更新套餐表中的信息
        this.updateById(setmealDto);

        // 删除套餐菜品表(SetmealDish)中的菜品数据，用于后面统一更新保存
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, setmealDto.getId());
        // 删除原有的菜品数据
        setmealDishService.remove(queryWrapper);


        // 获取套餐id（setmeal_id），用于后面设置套餐菜品中的套餐id
        Long setmealId = setmealDto.getId();

        // 目的：设置套餐菜品中的套餐id
        // 获取套餐中的菜品
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        setmealDishes = setmealDishes.stream().map(item -> {
            // 设置套餐菜品中的套餐id
            item.setSetmealId(setmealId);
            return item;
        }).collect(Collectors.toList());

        // 更新套餐数据
        setmealDishService.saveBatch(setmealDishes);
    }
}
