package gr.com.ist.commun.utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.google.common.base.Predicate;

public class ListUtils {
    public static <T> int lastIndexOf(List<T> list, Predicate<? super T> predicate) {
        for (int i = list.size() - 1; i >= 0; i--) {
            if (predicate.apply(list.get(i))) {
                return i;
            }
        }
        return -1;
    }

    public static List<String> readLines(MultipartFile file, String charsetName) {
        List<String> result = new ArrayList<String>();
        try {
            final LineNumberReader reader = new LineNumberReader(new InputStreamReader(file.getInputStream(), charsetName));
            String record = null;
            while ((record = reader.readLine()) != null) {
                result.add(record);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
