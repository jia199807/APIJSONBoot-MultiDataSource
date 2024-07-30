package apijson.boot.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Controller
@RequestMapping("/video")
public class VideoController {

    // 视频文件路径
    private static final String VIDEO_PATH = "C:\\Users\\Administrator\\Videos\\test.mp4";

    /**
     * 视频预览接口，支持视频流式播放和范围请求。
     *
     * @param response HTTP 响应对象，用于写入视频数据
     * @param headers  HTTP 请求头，包括范围请求头
     */
    @GetMapping("/preview")
    public void previewVideo(HttpServletResponse response, @RequestHeader HttpHeaders headers) {

        try {
            // 获取视频文件路径
            Path path = Paths.get(VIDEO_PATH);
            long fileLength = Files.size(path); // 获取视频文件的总大小
            List<HttpRange> ranges = headers.getRange(); // 获取请求头中的范围请求

            // 如果没有范围请求，直接返回整个视频文件
            if (ranges.isEmpty()) {
                response.setContentType("video/mp4"); // 设置内容类型为视频格式
                response.setContentLengthLong(fileLength); // 设置内容长度
                Files.copy(path, response.getOutputStream()); // 复制视频文件内容到响应输出流
            } else {
                // 处理范围请求
                HttpRange range = ranges.get(0); // 获取第一个范围请求
                long start = range.getRangeStart(fileLength); // 范围开始位置
                long end = range.getRangeEnd(fileLength); // 范围结束位置
                long rangeLength = end - start + 1; // 范围长度

                response.setContentType("video/mp4"); // 设置内容类型为视频格式
                response.setContentLengthLong(rangeLength); // 设置内容长度为范围长度
                response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength); // 设置内容范围头
                response.setStatus(HttpStatus.PARTIAL_CONTENT.value()); // 设置状态码为部分内容

                // 使用 RandomAccessFile 读取文件的指定部分
                try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
                    file.seek(start); // 定位到范围开始位置
                    byte[] buffer = new byte[8192]; // 缓冲区大小
                    long remaining = rangeLength; // 剩余数据长度
                    int read;
                    // 逐块读取文件数据并写入响应输出流
                    while (remaining > 0 && (read = file.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                        response.getOutputStream().write(buffer, 0, read); // 写入响应输出流
                        remaining -= read; // 减少剩余数据长度
                    }
                }
            }
            response.getOutputStream().flush(); // 刷新输出流，确保所有数据写入响应
        } catch (IOException e) {
            // 处理异常情况，设置响应状态为内部服务器错误
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
}
