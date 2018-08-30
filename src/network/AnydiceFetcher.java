package network;

import okhttp3.*;

import java.io.IOException;


public class AnydiceFetcher {

    private String responseJson;

    //Sends AnyDice command to website and returns the response as a JSON string
    public AnydiceFetcher(String command) {
        OkHttpClient client = new OkHttpClient();

        MediaType mediaType = MediaType.parse("multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW");
        RequestBody body = RequestBody.create(mediaType, "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\nContent-Disposition: form-data; name=\"program\"\r\n\r\n" + command + "\n\r\n------WebKitFormBoundary7MA4YWxkTrZu0gW--");
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
            responseJson = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getResponseJson() {
        return responseJson;
    }
}
