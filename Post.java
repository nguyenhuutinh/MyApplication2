package com.example.nguyenhuutinh.myapplication;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Post {
    @JsonProperty("id")
    String id;
    @JsonProperty("country_code")
    String country_code;
    @JsonProperty("content")
    String content;
    @JsonProperty("title")
    String title;
    @JsonProperty("visibility")
    String visibility;
    @JsonProperty("friendly_url")
    String friendly_url;
    @JsonProperty("image_url")
    String image_url;
    @JsonProperty("blog_category_id")
    String blog_category_id;

    @JsonProperty("created_at")
    String created_at;
    @JsonProperty("blog_category")
    BlogCategory blog_category;

    @JsonProperty("admin")
    Admin admin;
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCountry_code() {
        return country_code;
    }

    public void setCountry_code(String country_code) {
        this.country_code = country_code;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getFriendly_url() {
        return friendly_url;
    }

    public void setFriendly_url(String friendly_url) {
        this.friendly_url = friendly_url;
    }

    public String getImage_url() {
        return image_url;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }

    public String getBlog_category_id() {
        return blog_category_id;
    }

    public void setBlog_category_id(String blog_category_id) {
        this.blog_category_id = blog_category_id;
    }

    public BlogCategory getBlog_category() {
        return blog_category;
    }

    public void setBlog_category(BlogCategory blog_category) {
        this.blog_category = blog_category;
    }
}
