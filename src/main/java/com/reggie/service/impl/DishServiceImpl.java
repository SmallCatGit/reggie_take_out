package com.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.CustomException;
import com.reggie.dto.DishDto;
import com.reggie.entity.Dish;
import com.reggie.entity.DishFlavor;
import com.reggie.mapper.DishMapper;
import com.reggie.service.DishFlavorService;
import com.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {
    @Autowired
    private DishFlavorService dishFlavorService;

    /**
     * 新增菜品，同时保存菜品对应的口味数据
     *
     * @param dishDto
     */
    @Override
    @Transactional// 涉及多张表的控制，于是要加上事务。并在启动类上开启事务支持，才能让这个注解生效
    public void savaWithFlavor(DishDto dishDto) {
        // 保存菜品的基本信息到菜品表dish
        this.save(dishDto);

        // 获取菜品id
        Long dishId = dishDto.getId();

        // 获取菜品口味
        List<DishFlavor> flavors = dishDto.getFlavors();
        // 通过stream流遍历集合，将缺少的菜品id赋上传过来的值
        flavors = flavors.stream().map(item -> {
            // 每个item就是一个实体，设置实体的菜品id
            item.setDishId(dishId);
            return item;
        }).collect(Collectors.toList());// 再转化为集合

        // 保存菜品口味数据到菜品口味表dish_flavor
        dishFlavorService.saveBatch(flavors);
    }

    /**
     * 根据菜品id查询菜品信息和口味信息
     *
     * @param id
     * @return
     */
    @Override
    public DishDto getByIdWithFlavor(Long id) {
        // 查询菜品基本信息，从dish表查询
        Dish dish = this.getById(id);

        // 拷贝一个DishDto对象
        DishDto dishDto = new DishDto();
        BeanUtils.copyProperties(dish, dishDto);

        // 查询当前菜品对应的口味信息，从dish_flavor表查询
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, dish.getId());

        List<DishFlavor> flavors = dishFlavorService.list(queryWrapper);
        dishDto.setFlavors(flavors);

        return dishDto;
    }

    /**
     * 更新菜品信息，同时更新口味信息
     *
     * @param dishDto
     */
    @Override
    @Transactional
    public void updateWithFlavor(DishDto dishDto) {
        // 更新dish表基本信息
        this.updateById(dishDto);

        // 清理当前菜品对应的口味信息--dish_flavor表的delete操作
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId, dishDto.getId());

        dishFlavorService.remove(queryWrapper);

        // 添加当前提交过来的口味数据--dish_flavor表的insert操作
        // 获取dishId
        Long dishId = dishDto.getId();

        List<DishFlavor> flavors = dishDto.getFlavors();

        // 设置flavors中的dishId值
        flavors = flavors.stream().map(item -> {
            item.setDishId(dishId);
            return item;
        }).collect(Collectors.toList());

        dishFlavorService.saveBatch(flavors);
    }

    /**
     * 根据菜品id删除对应的菜品和菜品口味信息
     *
     * @param ids
     */
    @Override
    @Transactional
    public void removeWithDishId(List<Long> ids) {
        // 创建查询构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();

        // SQL:select count(*) from dish where id in(...) and status = ?(子查询。查询中嵌套查询，可以查询多个数据)
        // 根据id查询菜品信息
        queryWrapper.in(Dish::getId, ids);
        // 根据状态查询是否起售
        queryWrapper.eq(Dish::getStatus, 1);

        // 判断是否满足条件，不满足的报业务异常(起售状态不能删除)
        int count = count(queryWrapper);
        if (count > 0) {
            throw new CustomException("菜品正在售卖，不能删除");
        }

        // 满足条件的先根据菜品id删除菜品信息
        this.removeByIds(ids);

        // SQL:delete from dish_flavor where dish_id in (...)
        // 再删除菜品口味信息
        LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(DishFlavor::getDishId, ids);

        dishFlavorService.remove(lambdaQueryWrapper);
    }
}
