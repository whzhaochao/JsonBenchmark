package com.json.domain;

import lombok.Data;

import java.util.Date;

@Data
public class Article {
    private Long id;
    private String author;
    private Long tenantId;
    private String title;
    private String subTitle;
    private String htmlContent;
    private Date publishTime;
}
