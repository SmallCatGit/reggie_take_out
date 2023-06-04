package com.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.dto.SetmealDto;
import com.reggie.entity.Setmeal;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {

    //新增套餐，同时保存套餐和菜品的关联关系
    public void saveWithDish (SetmealDto setmealDto);

    //删除套餐，同时删除套餐和菜品的关联数据
    public void removeWithDish(List<Long> ids);

    //根据套餐id查询套餐信息和套餐中的菜品信息
    public SetmealDto getByIdWithDish(Long id);

    //更新套餐信息，同时更新套餐中菜品信息
    public void updateWithSetmeal(SetmealDto setmealDto);
}
