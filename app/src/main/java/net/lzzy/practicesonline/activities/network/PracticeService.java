package net.lzzy.practicesonline.activities.network;

import net.lzzy.practicesonline.activities.constants.ApiConstants;
import net.lzzy.practicesonline.activities.models.Practice;
import net.lzzy.sqllib.JsonConverter;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;

public class PracticeService {


    public static String getPracticesFromServer() throws IOException {
        return ApiService.okGet(ApiConstants.URL_PRACTICES);
    }

    public static List<Practice> getPractice(String json) throws IllegalAccessException, JSONException, InstantiationException {
        JsonConverter<Practice> converter= new JsonConverter<>(Practice.class);
        return converter.getArray(json);
    }


}


