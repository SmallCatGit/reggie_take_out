package com.reggie.filter;

import com.alibaba.fastjson.JSON;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 检查用户是否完成登录
 */
@Slf4j
@WebFilter(filterName = "loginCheckFilter", urlPatterns = "/*")
public class LoginCheckFilter implements Filter {
    //路径匹配器，支持通配符
    public static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        /*//{}相当于占位符，用于显示后面的内容
        log.info("拦截到请求：{}", request.getRequestURI());*/

        //1、获取本次请求的URI
        String requestURI = request.getRequestURI();// /backend/index.html
        log.info("拦截到请求：{}", requestURI);

        //定义不需要处理的请求路径
        String[] urls = new String[]{
                "/employee/login",
                "/employee/logout",
                "/backend/**",
                "/front/**",
                "/common/**",
                "/user/login",//移动端登录
                "/user/sendMsg"//移动端发送短信
        };

        //2、判断本次请求是否需要处理，也就是检查登录状态
        boolean check = check(urls, requestURI);

        //3、如果不需要处理，则直接放行
        if (check) {
            log.info("本次请求{}不需要处理", requestURI);
            //放行
            filterChain.doFilter(request, response);
            return;
        }

        //4-1和4-2会互相影响，一个登录后直接访问另一个页面也会登录！！！！！！！！！！！！！！！

        //4-1、判断登录状态，如果已登录，则直接放行
        if (request.getSession().getAttribute("employee") != null) {
            log.info("用户已登录，用户id为：{}", request.getSession().getAttribute("employee"));

            //获取当前用户id
            Long empId = (Long) request.getSession().getAttribute("employee");
            //将id设置进线程
            BaseContext.setCurrentId(empId);

            /*//获取当前线程id
            long id = Thread.currentThread().getId();
            log.info("线程id为：{}", id);*/

            filterChain.doFilter(request, response);
            return;
        }

        //4-2、判断移动端用户登录状态，如果已登录，则直接放行
        if (request.getSession().getAttribute("user") != null) {
            log.info("移动端用户已登录，id为：{}", request.getSession().getAttribute("user"));

            //获取id
            Long userId = (Long) request.getSession().getAttribute("user");
            //将id设置进线程中保存
            BaseContext.setCurrentId(userId);

            //放行
            filterChain.doFilter(request, response);
            return;

        }

        log.info("用户未登录！！");
        //5、如果未登录则返回未登录结果，通过输出流的方式向客户端页面响应数据
        response.getWriter().write(JSON.toJSONString(R.error("NOTLOGIN")));
        return;

    }

    /**
     * 路径匹配，检查本次请求是否需要放行
     *
     * @param urls
     * @param requestURI
     * @return
     */
    public boolean check(String[] urls, String requestURI) {
        for (String url : urls) {
            boolean match = PATH_MATCHER.match(url, requestURI);
            if (match) {
                return true;
            }
        }
        return false;
    }
}
