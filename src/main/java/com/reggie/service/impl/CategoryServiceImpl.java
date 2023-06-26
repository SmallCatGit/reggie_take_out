package com.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.CustomException;
import com.reggie.entity.Category;
import com.reggie.entity.Dish;
import com.reggie.entity.Setmeal;
import com.reggie.mapper.CategoryMapper;
import com.reggie.service.CategoryService;
import com.reggie.service.DishService;
import com.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {
    /**
     * 注入菜品的service
     */
    @Autowired
    private DishService dishService;

    /**
     * 注入套餐的service
     */
    @Autowired
    private SetmealService setmealService;

    /**
     * 根据id删除分类，但是删除之前需要判断这个分类是否关联了其他的东西
     *
     * @param id
     */
    @Override
    public void remove(Long id) {
        // 目的：查询当前分类是否关联了菜品，如果已关联，抛出一个业务异常

        // 构建菜品条件构造器
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        // 添加查询条件，根据分类id进行查询
        dishLambdaQueryWrapper.eq(Dish::getCategoryId, id);
        // 统计查询数量
        int dishCount = dishService.count(dishLambdaQueryWrapper);
        if (dishCount > 0) {
            // 已关联菜品，抛出一个业务异常
            throw new CustomException("当前分类下关联了菜品，不能删除");
        }

        // 目的：查询当前分类是否关联了套餐，如果已关联，抛出一个业务异常
        // 构建套餐条件构造器
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        // 添加查询条件，根据分类id进行查询
        setmealLambdaQueryWrapper.eq(Setmeal::getCategoryId, id);
        // 统计查询数量
        int setmealCount = setmealService.count(setmealLambdaQueryWrapper);
        if (setmealCount > 0) {
            // 已关联套餐，抛出一个业务异常
            throw new CustomException("当前分类下关联了套餐，不能删除");
        }
        // 正常删除
        super.removeById(id);
    }
}
