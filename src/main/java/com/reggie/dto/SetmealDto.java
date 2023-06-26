package com.reggie.dto;

import com.reggie.entity.Setmeal;
import com.reggie.entity.SetmealDish;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SetmealDto extends Setmeal {

    private List<SetmealDish> setmealDishes;

    // 菜品分类名称
    private String categoryName;
}
