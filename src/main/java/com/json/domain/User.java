package com.json.domain;


import lombok.Data;

import java.util.Date;



@Data
public class User {
    private Long id;
    private String name;
    private String trueName;
    private Integer age;
    private String sex;
    private Date createTime;
}
