package com.example.traveltdea;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {

    @GET("posts")
    Call<List<Post>> getPosts();

}

