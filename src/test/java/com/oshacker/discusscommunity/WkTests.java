package com.oshacker.discusscommunity;

import java.io.IOException;

//简单的测试wkhtmltoimage,这样直接写死，不可配置
public class WkTests {

    public static void main(String[] args) {
        String cmd = "d:/wkhtmltopdf/bin/wkhtmltoimage --quality 75  https://www.nowcoder.com E:/IDEAworkspace/wk-images/2.png";
        try {
            Runtime.getRuntime().exec(cmd); //Java程序将上面的命令提交给操作系统后就继续向下执行，而不会等待命令执行完，即命令的执行与程序向下执行是并发的
            System.out.println("ok."); //很可能先输出，因为上面的命令执行可能需要一点时间
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
