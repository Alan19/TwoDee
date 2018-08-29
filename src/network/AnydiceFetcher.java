package network;

import logic.StatisticsHandler;
import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;


public class AnydiceFetcher {
    public static void main(String[] args) {
        OkHttpClient client = new OkHttpClient();

        MediaType mediaType = MediaType.parse("multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW");
        RequestBody body = RequestBody.create(mediaType, "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\nContent-Disposition: form-data; name=\"program\"\r\n\r\nfunction: highest N:n of A:s B:s C:s {\nresult: {1..N}@[sort {A, B, C}]\n}\noutput [highest 2 of 1d6 1d8 2d10]\n\r\n------WebKitFormBoundary7MA4YWxkTrZu0gW--");
        Request request = new Request.Builder()
                .url("https://anydice.com/calculator_limited.php")
                .post(body)
                .addHeader("content-type", "multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW")
                .addHeader("Cache-Control", "no-cache")
                .addHeader("Postman-Token", "90607fce-9b5d-4f04-b27a-5788a0bafcd1")
                .build();

        Response response = null;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            String responseJson = response.body().string();
            JSONObject statistics = new JSONObject(responseJson);
            StatisticsHandler statisticsHandler = new StatisticsHandler(statistics);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
