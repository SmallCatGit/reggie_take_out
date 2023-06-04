package com.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.dto.DishDto;
import com.reggie.entity.Dish;

import java.util.List;

public interface DishService extends IService<Dish> {

    //新增菜品，同时插入菜品对应的口味数据，需要操作两张表：dish、dish_flavor
    public void savaWithFlavor (DishDto dishDto);

    //根据菜品id查询菜品信息和口味信息
    public DishDto getByIdWithFlavor(Long id);

    //更新菜品信息，同时更新口味信息
    public void updateWithFlavor(DishDto dishDto);

    //根据菜品id删除对应的菜品和菜品口味信息
    public void removeWithDishId(List<Long> ids);
}
