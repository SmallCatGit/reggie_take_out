package com.reggie.controller;

import com.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

/**
 * 文件上传和下载
 */
@RestController
@RequestMapping("/common")
@Slf4j
public class CommonController {
    // 动态获取配置路径
    @Value("${reggie.path}")
    private String basePath;

    /**
     * 文件上传
     *
     * @param file
     * @return
     */
    @PostMapping("/upload")
    public R<String> upload(MultipartFile file) {
        // file是个临时文件，需要转存到指定位置，否则本次请求结束后临时文件就会删除
        log.info(file.toString());

        // 获取原始文件名
        String originalFilename = file.getOriginalFilename();
        // 根据.截取文件名后缀（带.的）
        String suffix = originalFilename != null ?
                originalFilename.substring(originalFilename.lastIndexOf(".")) : null;

        // 使用UUID重新生成文件名，防止文件名称重复造成文件覆盖
        String fileName = UUID.randomUUID().toString() + suffix;// 动态拼接成对应的文件

        // 创建一个目录对象,并且获取配置文件下的目录
        File dir = new File(basePath);
        // 判断当前目录是否存在
        if (!dir.exists()) {
            // 如果目录不存在，则创建目录
            dir.mkdirs();
        }

        // 将临时文件转存到指定位置
        try {
            file.transferTo(new File(basePath + fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return R.success(fileName);
    }

    /**
     * 文件下载
     *
     * @param name
     * @param response
     */
    @GetMapping("/download")
    public void download(String name, HttpServletResponse response) {
        try {
            // 输入流，通过数据流读取文件内容
            FileInputStream fileInputStream = new FileInputStream(new File(basePath + name));

            // 输出流，通过输出流将文件写回浏览器，在浏览器上展示图片
            ServletOutputStream outputStream = response.getOutputStream();

            // 可以设置响应回去的文件类型（这里已知的是图片文件）
            response.setContentType("image/jpeg");

            // 用于判断写入是否结束
            int len = 0;
            // 用于存储写入的流数据
            byte[] bytes = new byte[1024];
            // 根据输入流读取数据，直到结束
            while ((len = fileInputStream.read(bytes)) != -1) {
                // 根据输出流向浏览器写入数据,从第一个写（0），到最后结束（len）
                outputStream.write(bytes, 0, len);
                // 刷新一下
                outputStream.flush();
            }

            // 关闭流，释放资源
            outputStream.close();
            fileInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
