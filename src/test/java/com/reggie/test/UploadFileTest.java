package com.reggie.test;

import org.junit.jupiter.api.Test;

public class UploadFileTest {
    @Test
    public void test1 () {
        String fileName = "erwrwrwer.jpg";
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        System.out.println(suffix);
    }
}
