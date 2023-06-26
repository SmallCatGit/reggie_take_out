package com.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常捕获处理器，进行全局异常处理
 */
// 通知，指定拦截哪些controller，annotations注解-->指定加了对应注解的controller
@ControllerAdvice(annotations = {RestController.class, Controller.class})
@ResponseBody
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 异常处理方法，处理SQLIntegrityConstraintViolationException类型异常
     *
     * @return
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public R<String> exceptionHandler(SQLIntegrityConstraintViolationException ex) {
        // 打印异常信息
        log.error(ex.getMessage());

        // 判断异常是否都是重复异常，根据异常信息判断
        if (ex.getMessage().contains("Duplicate entry")) {
            // 根据空格进行分割异常信息 Duplicate entry 'zhangsan' for key 'idx_username'
            String[] split = ex.getMessage().split(" ");
            // 拼接需要的信息
            String msg = split[2] + "已存在";
            return R.error(msg);
        }


        // 给前端返回错误信息
        return R.error("未知错误");
    }

    /**
     * 异常处理方法，处理自定义的异常
     *
     * @return
     */
    @ExceptionHandler(CustomException.class)
    public R<String> exceptionHandler(CustomException ex) {
        // 打印异常信息
        log.error(ex.getMessage());

        // 给前端返回错误信息
        return R.error(ex.getMessage());
    }
}
