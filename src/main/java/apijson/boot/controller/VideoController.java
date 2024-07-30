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

    private static final String VIDEO_PATH = "C:\\Users\\Administrator\\Videos\\test.mp4";

    @GetMapping("/preview")
    public void previewVideo(HttpServletResponse response, @RequestHeader HttpHeaders headers) {

        try {
            Path path = Paths.get(VIDEO_PATH);
            long fileLength = Files.size(path);
            List<HttpRange> ranges = headers.getRange();

            if (ranges.isEmpty()) {
                // 无范围请求，返回整个文件
                response.setContentType("video/mp4");
                response.setContentLengthLong(fileLength);
                Files.copy(path, response.getOutputStream());
            } else {
                // 处理范围请求
                HttpRange range = ranges.get(0);
                long start = range.getRangeStart(fileLength);
                long end = range.getRangeEnd(fileLength);
                long rangeLength = end - start + 1;

                response.setContentType("video/mp4");
                response.setContentLengthLong(rangeLength);
                response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
                response.setStatus(HttpStatus.PARTIAL_CONTENT.value());

                // 使用 RandomAccessFile 读取指定部分
                try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
                    file.seek(start);
                    byte[] buffer = new byte[8192]; // 缓冲区
                    long remaining = rangeLength;
                    int read;
                    while (remaining > 0 && (read = file.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
                        response.getOutputStream().write(buffer, 0, read);
                        remaining -= read;
                    }
                }
            }
            response.getOutputStream().flush();
        } catch (IOException e) {
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
}
