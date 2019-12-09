package com.idutils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.View;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by chen on 19-12-8
 * Introduce:
 */

public class IdUtils {

    public static void inject(Activity activity) {
        inject(new ViewFinder(activity), activity);
    }

    public static void inject(View view) {
        inject(new ViewFinder(view), view);
    }

    public static void inject(View view, Object object) {
        inject(new ViewFinder(view), object);
    }

    /**
     * 兼容上面的三个方法
     *
     * @param viewFinder
     * @param object     反射需要执行的类
     */
    public static void inject(ViewFinder viewFinder, Object object) {
        injectField(viewFinder, object);
        injectEvent(viewFinder, object);
    }

    /**
     * 注入属性
     *
     * @param viewFinder
     * @param object
     */
    private static void injectField(ViewFinder viewFinder, Object object) {
        //1.获取类里面所有的属性
        Class<?> clazz = object.getClass();
        Field[] fields = clazz.getDeclaredFields(); //获取到所有的属性,包括私有和公有的属性
        for (Field field : fields) {
            //2.获取注解FindViewById的里面的value值
            FindViewById findViewById = field.getAnnotation(FindViewById.class);
            if (findViewById != null) {
                //3.利用系统的FindViewById()找到View
                int viewId = findViewById.value();
                View view = viewFinder.findViewById(viewId);
                if (view != null) {
                    //4.动态注入找到View
                    field.setAccessible(true);
                    try {
                        field.set(object, view);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }


    }


    /**
     * 注入事件
     *
     * @param viewFinder
     * @param object
     */
    private static void injectEvent(ViewFinder viewFinder, Object object) {
        //1.获取类里面所有的方法
        Class<?> clazz = object.getClass();
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            OnClick onClick = method.getAnnotation(OnClick.class);
            boolean isCheckNet = method.getAnnotation(CheckNet.class) != null;  //是否需要检查网络
            boolean isAllowDoubleClick = method.getAnnotation(AllowedQucikDoubleClick.class) != null;   //是否允许双击
            if (onClick != null) {
                //2.获取注解OnClick的里面的value值
                int[] viewIds = onClick.value();
                for (int viewId : viewIds) {
                    //3.利用系统的FindViewById()找到View
                    View view = viewFinder.findViewById(viewId);
                    if (view != null) {
                        //4.view.setOnClickListener()
                        view.setOnClickListener(new DeclaredOnClickListener(method, object, isCheckNet, isAllowDoubleClick));
                    }
                }
            }
        }
    }

    private static class DeclaredOnClickListener implements View.OnClickListener {
        private Object mObject;
        private Method mMethod;
        private boolean mIsCheckNet;
        private boolean mIsAllowDoubleClick;    //是否允许快速点击两次

        public DeclaredOnClickListener(Method method, Object object, boolean isCheckNet, boolean isAllowDoubleClick) {
            this.mObject = object;
            this.mMethod = method;
            this.mIsCheckNet = isCheckNet;
            this.mIsAllowDoubleClick = isAllowDoubleClick;
        }

        @Override
        public void onClick(View v) {
            if (mIsCheckNet) {   //需要检查网络
                if (!netWorkAvailable(v.getContext())) {
                    Toast.makeText(v.getContext(), "亲,你的网络开小差了~", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            if (!mIsAllowDoubleClick) {//不允许快速点击两次
                if (isFastDoubleClick()){
                    Toast.makeText(v.getContext(), "亲,你的手速太快了~", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            try {
                //5.反射执行方法
                mMethod.setAccessible(true);
                mMethod.invoke(mObject, v);
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    mMethod.invoke(mObject, null);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        }

    }

    /**
     * 检查当前网络是否可用
     *
     * @param context
     * @return
     */
    private static boolean netWorkAvailable(Context context) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private static final int TIME = 1000;
    private static long lastClickTime = 0;

    /**
     * 处理快速双击，多击事件，在TIME时间内只执行一次事件
     *
     * @return
     */
    public static boolean isFastDoubleClick() {
        long currentTime = System.currentTimeMillis();
        long timeInterval = currentTime - lastClickTime;
        if (0 < timeInterval && timeInterval < TIME) {
            return true;
        }
        lastClickTime = currentTime;
        return false;
    }
}
