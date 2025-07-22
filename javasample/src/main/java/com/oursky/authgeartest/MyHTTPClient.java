package com.oursky.authgeartest;

import androidx.annotation.NonNull;

import com.oursky.authgear.net.DefaultHTTPClient;
import com.oursky.authgear.net.HTTPRequest;
import com.oursky.authgear.net.HTTPResponse;

import java.util.ArrayList;

public class MyHTTPClient extends DefaultHTTPClient {
    @NonNull
    @Override
    public HTTPResponse send(@NonNull HTTPRequest request) {
        ArrayList<String> value = new ArrayList<>();
        value.add("42");
        request.getHeaders().put("X-Custom-Header", value);
        return super.send(request);
    }
}
