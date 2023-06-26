package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.dto.SetmealDto;
import com.reggie.entity.Category;
import com.reggie.entity.Setmeal;
import com.reggie.service.CategoryService;
import com.reggie.service.SetmealDishService;
import com.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 套餐管理
 */
@Slf4j
@RestController
@RequestMapping("/setmeal")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private CategoryService categoryService;

    /**
     * 新增套餐
     *
     * @param setmealDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody SetmealDto setmealDto) {
        log.info("套餐信息：{}", setmealDto);

        setmealService.saveWithDish(setmealDto);

        return R.success("新增套餐成功");
    }

    /**
     * 套餐分页查询
     *
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name) {
        // 构造分页构造器
        Page<Setmeal> pageInfo = new Page<>(page, pageSize);
        Page<SetmealDto> dtoPage = new Page<>();

        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        // 添加查询条件，根据name进行like模糊查询
        queryWrapper.like(name != null, Setmeal::getName, name);
        // 添加排序条件，根据更新时间，添加降序排序
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        // 进行分页查询
        setmealService.page(pageInfo, queryWrapper);

        // 对象拷贝,records不拷贝，因为两个的泛型不一样
        BeanUtils.copyProperties(pageInfo, dtoPage, "records");
        List<Setmeal> records = pageInfo.getRecords();
        List<SetmealDto> list = records.stream().map(item -> {
            // 创建我们需要的setmealDto对象
            SetmealDto setmealDto = new SetmealDto();
            // 拷贝Stemeal里的属性
            BeanUtils.copyProperties(item, setmealDto);

            // 获取分类id
            Long categoryId = item.getCategoryId();
            // 根据分类id查询出分类对象
            Category category = categoryService.getById(categoryId);
            if (category != null) {
                // 分类名称
                String categoryName = category.getName();
                setmealDto.setCategoryName(categoryName);
            }
            return setmealDto;
        }).collect(Collectors.toList());
        dtoPage.setRecords(list);
        return R.success(dtoPage);
    }

    /**
     * 删除套餐
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids) {
        log.info("ids:{}", ids);

        setmealService.removeWithDish(ids);
        return R.success("套餐删除成功");
    }

    /**
     * 根据套餐id查询套餐信息
     *
     * @param setmeal
     * @return
     */
    @GetMapping("/list")
    public R<List<Setmeal>> list(Setmeal setmeal) {// 传过来的不是json数据，不需要加注解
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(setmeal.getCategoryId() != null, Setmeal::getCategoryId, setmeal.getCategoryId());
        queryWrapper.eq(setmeal.getStatus() != null, Setmeal::getStatus, setmeal.getStatus());
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        // SQL:select * from setmeal where category_id = ? and status = ? order by update_time desc
        List<Setmeal> setmealList = setmealService.list(queryWrapper);

        return R.success(setmealList);
    }

    /**
     * 点击修改按钮后根据套餐id显示套餐信息和菜品信息
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<SetmealDto> getByIdWithSetmeal(@PathVariable Long id) {
        log.info("套餐id:{}", id);

        SetmealDto setmealDto = setmealService.getByIdWithDish(id);

        return R.success(setmealDto);
    }

    /**
     * 修改套餐信息
     *
     * @param setmealDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody SetmealDto setmealDto) {
        log.info("setmealDto:{}", setmealDto);
        setmealService.updateWithSetmeal(setmealDto);
        return R.success("套餐信息修改成功");
    }

    /**
     * 根据套餐id批量起售或停售套餐
     *
     * @param ids
     * @return
     */
    @PostMapping({"/status/0", "/status/1"})
    public R<String> status(@RequestParam List<Long> ids, HttpServletRequest request) {
        log.info("ids:{}", ids);

        // 获取请求地址
        String uri = request.getRequestURI();

        // 获取状态
        String status = uri.substring(uri.lastIndexOf("/") + 1);

        // 创建更新构造器
        LambdaUpdateWrapper<Setmeal> updateWrapper = new LambdaUpdateWrapper<>();

        // 根据状态和id更新数据
        updateWrapper.in(Setmeal::getId, ids);
        updateWrapper.set(Setmeal::getStatus, status);

        // 使用service执行修改
        setmealService.update(updateWrapper);

        return R.success("状态更新成功");
    }
}
