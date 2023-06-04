package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.R;
import com.reggie.entity.User;
import com.reggie.service.UserService;
import com.reggie.utils.SMSUtils;
import com.reggie.utils.ValidateCodeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

@SuppressWarnings("SpringJavaAutowiredFieldsWarningInspection")
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    /**
     * 发送手机短信验证码
     *
     * @param user
     * @param session
     * @return
     */
    @PostMapping("/sendMsg")
    public R<String> sendMsg(@RequestBody User user, HttpSession session) {
        //获取手机号
        String phone = user.getPhone();
        //判断手机号是否为空，为空响应错误信息
        if (!StringUtils.isNotEmpty(phone)) {
            return R.error("手机号未填写，短信发送失败");
        }

        //生成随机的4位验证码
        String code = ValidateCodeUtils.generateValidateCode(4).toString();
        log.info("code={}", code);

        //调用阿里云提供的短信验证服务API完成短信发送
        SMSUtils.sendMessage("阿里云短信测试", "SMS_154950909", phone, code);

        //将生成的验证码保存到Session中，用于与用户输入的验证码比对
        session.setAttribute(phone, code);

        return R.success("验证码发送成功");
    }

    /**
     * 移动端用户登录
     *
     * @param map
     * @param session
     * @return
     */
    @PostMapping("/login")
    //使用Map<K, V>或者创建dto都可以接收页面传过来的phone和code
    public R<User> login(@RequestBody Map map, HttpSession session) {
        log.info(map.toString());

        //获取手机号
        String phone = map.get("phone").toString();

        //获取验证码
        String code = map.get("code").toString();

        //从Session中获取验证码
        String codeInSession = session.getAttribute(phone).toString();

        //将页面传过来的验证码和Session中的验证码比对
        if (codeInSession != null && codeInSession.equals(code)) {
            //如果比对成功，说明登录成功
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone, phone);

            //获取查询出来的User对象
            User user = userService.getOne(queryWrapper);

            //判断当前手机号是否是新用户，如果是新用户，自动完成账号注册
            if (user == null) {
                //设置user对象里面已知的phone属性
                user = new User();
                user.setPhone(phone);
                user.setStatus(1);

                //调用Service中的sava方法自动完成注册
                userService.save(user);
            }

            //将用户id设置进Session中，用于filter的比对
            session.setAttribute("user", user.getId());

            return R.success(user);
        }


        return R.error("登录失败");
    }
}
