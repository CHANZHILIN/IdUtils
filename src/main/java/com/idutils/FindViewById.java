package com.idutils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  Created by chen on 19-12-8
 *  Introduce: 绑定资源id的注解,
 *  使用:  @FindViewById(R.id.tv_main)
 *         private TextView mTextView;
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FindViewById {
    int value();        //资源id
}
