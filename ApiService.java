package com.example.nguyenhuutinh.myapplication;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

interface ApiService {
    @GET("blog_posts")
    Call<String> getBlogs(@Query("country_code")String countrycode, @Query("limit")String limit);


}
