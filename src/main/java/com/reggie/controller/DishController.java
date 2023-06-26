package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.dto.DishDto;
import com.reggie.entity.Category;
import com.reggie.entity.Dish;
import com.reggie.entity.DishFlavor;
import com.reggie.service.CategoryService;
import com.reggie.service.DishFlavorService;
import com.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 菜品管理
 */
@SuppressWarnings("ALL")
@RestController
@Slf4j
@RequestMapping("/dish")
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private CategoryService categoryService;// 注入分类的service

    /**
     * 新增菜品
     *
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto) {
        log.info(dishDto.toString());

        dishService.savaWithFlavor(dishDto);

        return R.success("新增菜品成功");
    }

    /**
     * 菜品信息分页查询
     *
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {

        // 创建分页构造器
        Page<Dish> pageInfo = new Page<>(page, pageSize);
        // 构造dishDto泛型的构造器，用于拷贝dish分页构造器的属性，并且设置页面需要的菜品分类字段
        Page<DishDto> dishDtoPage = new Page<>();

        // 构造条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();

        // 根据name添加过滤条件
        queryWrapper.like(name != null, Dish::getName, name);

        // 根据时间添加降序排序条件
        queryWrapper.orderByDesc(Dish::getUpdateTime);

        // 调用service执行分页查询
        dishService.page(pageInfo, queryWrapper);

        // 对象拷贝，并且忽略Page中的参数records
        BeanUtils.copyProperties(pageInfo, dishDtoPage, "records");

        // 获取records对应的list集合
        List<Dish> records = pageInfo.getRecords();
        // 遍历records，通过流对象，然后map（把每个元素拿出来），通过λ表达式处理，在过程中创建dto对象。最后收集起来赋值给dto对象
        List<DishDto> list = records.stream().map((item) -> {// item 表示Dish，也就是遍历出来的每一个菜品对象
            // 创建DishDto对象
            DishDto dishDto = new DishDto();
            // 将item拷贝给dishDto，目的是为其他属性赋值，item代表每一个Dish对象，里面有Dish对象的属性
            BeanUtils.copyProperties(item, dishDto);

            // 获取每个菜品对应的分类id
            Long categoryId = item.getCategoryId();
            // 通过categoryService和菜品id获得分类对象
            Category category = categoryService.getById(categoryId);

            if (category != null) {
                // 获取分类名称
                String categoryName = category.getName();
                // 给dishDto中的categoryName赋值
                dishDto.setCategoryName(categoryName);
            }

            return dishDto;
        }).collect(Collectors.toList());// 收集对象，赋值给DishDto对象

        // 设置records
        dishDtoPage.setRecords(list);

        return R.success(dishDtoPage);
    }

    /**
     * 根据id查询菜品信息和对应的口味信息
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable Long id) {
        DishDto dishDto = dishService.getByIdWithFlavor(id);
        return R.success(dishDto);
    }

    /**
     * 修改菜品
     *
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto) {
        dishService.updateWithFlavor(dishDto);
        return R.success("修改菜品成功");
    }

    /**
     * 根据条件查询对应的菜品数据（管理页面的需求）
     *
     * @param dish
     * @return
     */
    /*@GetMapping("list")
    public R<List<Dish>> list(Dish dish) {
        //构造条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        //添加查询条件
        queryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());

        //添加状态的查询条件，只有起售的商品才能查询(状态是1)
        queryWrapper.eq(Dish::getStatus, 1);

        //可能有多个值，所以添加一个排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        List<Dish> list = dishService.list(queryWrapper);
        return R.success(list);
    }*/

    /**
     * 根据条件查询对应的菜品数据和菜品口味信息
     *
     * @param dish
     * @return
     */
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish) {
        // 构造条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        // 添加查询条件
        queryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());

        // 添加状态的查询条件，只有起售的商品才能查询(状态是1)
        queryWrapper.eq(Dish::getStatus, 1);

        // 可能有多个值，所以添加一个排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        List<Dish> list = dishService.list(queryWrapper);

        List<DishDto> dishDtoList = list.stream().map(item -> {
            DishDto dishDto = new DishDto();
            // 对象拷贝
            BeanUtils.copyProperties(item, dishDto);

            /*//获取分类id
            Long categoryId = item.getCategoryId();
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);

            //判断对象
            if (category != null) {
                //获取分类名称
                String categoryName = category.getName();
                //在dishDto中设置分类名称
                dishDto.setCategoryName(categoryName);
            }*/

            // 获取菜品id
            Long dishId = item.getId();
            // 构造查询构造器，根据菜品id查询口味表中对应的口味信息
            LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(DishFlavor::getDishId, dishId);
            // SQL:select * from dish_flavor where dish_id = ?
            List<DishFlavor> dishFlavorList = dishFlavorService.list(lambdaQueryWrapper);
            dishDto.setFlavors(dishFlavorList);
            return dishDto;
        }).collect(Collectors.toList());

        return R.success(dishDtoList);
    }

    /**
     * 根据菜品id批量起售或停售
     *
     * @param ids
     * @return
     */
    @PostMapping({"/status/0", "/status/1"})
    public R<String> status(HttpServletRequest request, @RequestParam List<Long> ids) {
        log.info("ids:{}", ids);

        // 获取根路径到地址结尾
        String requestURI = request.getRequestURI();// /dish/status/0
        // 截取出最后的状态信息

        /**
         * substring(): 从指定位置返回字符串的子字符串。
         * lastIndexOf: 返回指定字符在此字符串中最后一次出现处的索引，如果此字符串中没有这样的字符，则返回 -1。
         * */

//        String substring = requestURI.substring(requestURI.lastIndexOf("/"));// /0
        String status = requestURI.substring(requestURI.lastIndexOf("/") + 1);// 0

        // SQL:update dish set status = ? where id in (...)
        LambdaUpdateWrapper<Dish> updateWrapper = new LambdaUpdateWrapper<>();
        // 添加修改条件
        updateWrapper.in(Dish::getId, ids);
        // 添加修改内容
        updateWrapper.set(Dish::getStatus, status);

        dishService.update(updateWrapper);

        return R.success("状态修改成功");
    }

    /**
     * 根据id删除单个菜品或者批量删除菜品
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids) {
        log.info("ids:{}", ids);

        dishService.removeWithDishId(ids);

        return R.success("菜品和菜品口味信息删除成功");
    }
}
