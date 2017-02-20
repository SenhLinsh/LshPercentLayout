package com.linsh.lshpercentlayout;


import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.v4.view.MarginLayoutParamsCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.TextView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PercentLayoutHelper {
    private static final String TAG = "PercentLayout";
    private static final String KEY_BASE_SCREEN_WIDTH = "BASE_SCREEN_WIDTH";
    private static final String KEY_BASE_SCREEN_HEIGHT = "BASE_SCREEN_HEIGHT";
    private static final String KEY_DEVICE_SCREEN_WIDTH = "DEVICE_SCREEN_WIDTH";
    private static final String KEY_DEVICE_SCREEN_HEIGHT = "DEVICE_SCREEN_HEIGHT";

    private final ViewGroup mHost;
    // 当前屏幕宽高
    private static int mScreenWidth;
    private static int mScreenHeight;
    // 写布局时用于作对比的基础屏幕宽高
    private static int mBaseScreenWidth;
    private static int mBaseScreenHeight;
    // 用于在预览时匹配模拟机型的的预览设备屏幕宽高
    // (预览时无法获取屏幕宽高, 导致预览时使用默认屏幕宽高, 而与当前模拟设备屏幕宽高不一致, 导致布局整体大小出现偏差)
    private static int mDeviceScreenWidth;
    private static int mDeviceScreenHeight;

    public PercentLayoutHelper(ViewGroup host) {
        mHost = host;
        checkBaseScreenSize();
        getScreenSize();
    }

    // 通过Application配置基础屏幕宽高
    public static void initBaseScreenSize(int baseScreenWidth, int baseScreenHeight) {
        mBaseScreenWidth = baseScreenWidth;
        mBaseScreenHeight = baseScreenHeight;
    }

    // 通过Application配置预览设备屏幕宽高
    public static void initDeviceScreenSize(int deviceScreenWidth, int deviceScreenHeight) {
        mDeviceScreenWidth = deviceScreenWidth;
        mDeviceScreenHeight = deviceScreenHeight;
    }

    // 检查并获取基础屏幕宽高, 如果没有配置, 则使用默认宽高: 1080*1920
    private void checkBaseScreenSize() {
        if (mBaseScreenWidth == 0 && mBaseScreenHeight == 0) {
            Log.v("LshLog", "LshPercentLayout: 获取清单文件的BaseScreenSize");
            getMetaBaseScreenSize();
            getMetaDeviceScreenSize();
        }
        if (mBaseScreenWidth == 0 && mBaseScreenHeight == 0) {
            Log.v("LshLog", "LshPercentLayout: 设置默认BaseScreenSize: 1080*1920");
            mBaseScreenWidth = 1080;
            mBaseScreenHeight = 1920;
        }
    }

    // 获取清单文件基础屏幕宽高
    private void getMetaBaseScreenSize() {
        try {
            ApplicationInfo aiApplicationInfo = mHost.getContext().getPackageManager().getApplicationInfo(
                    mHost.getContext().getPackageName(), PackageManager.GET_META_DATA);
            if (null != aiApplicationInfo) {
                if (null != aiApplicationInfo.metaData) {
                    mBaseScreenWidth = aiApplicationInfo.metaData.getInt(KEY_BASE_SCREEN_WIDTH);
                    mBaseScreenHeight = aiApplicationInfo.metaData.getInt(KEY_BASE_SCREEN_HEIGHT);
                    Log.v("LshLog", "LshPercentLayout: 获取清单文件的BaseScreenSize---" + "width:" + mBaseScreenWidth + " height:" + mBaseScreenHeight);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    // 获取清单文件预览设备屏幕宽高
    private void getMetaDeviceScreenSize() {
        try {
            ApplicationInfo aiApplicationInfo = mHost.getContext().getPackageManager().getApplicationInfo(
                    mHost.getContext().getPackageName(), PackageManager.GET_META_DATA);
            if (null != aiApplicationInfo) {
                if (null != aiApplicationInfo.metaData) {
                    mDeviceScreenWidth = aiApplicationInfo.metaData.getInt(KEY_DEVICE_SCREEN_WIDTH);
                    mDeviceScreenHeight = aiApplicationInfo.metaData.getInt(KEY_DEVICE_SCREEN_HEIGHT);
                    Log.v("LshLog", "LshPercentLayout: 获取清单文件的DeviceScreenSize---" + "width:" + mDeviceScreenWidth + " height:" + mDeviceScreenHeight);
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    // 获取屏幕尺寸, 在布局预览时, 无法获取屏幕宽高, 可配置预览设备屏幕宽高
    private void getScreenSize() {
        WindowManager wm = (WindowManager) mHost.getContext().getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(outMetrics);
        mScreenWidth = outMetrics.widthPixels;
        mScreenHeight = outMetrics.heightPixels;

        Log.v("LshLog", "LshPercentLayout: mScreenWidth=" + mScreenWidth + " mScreenHeight=" + mScreenHeight);
        // 预览时使用全屏由于没有context获取不了屏幕宽高，导致无法正确预览（宽高为0）， 所以下面设置默认的宽高以方便在布局中预览
        if (mScreenWidth == 0 && mScreenHeight == 0) {
            Log.e("LshLog", "LshPercentLayout: 无法获取屏幕宽高!!!!");
            if (mDeviceScreenWidth != 0 || mDeviceScreenHeight != 0) {
                mScreenWidth = mDeviceScreenWidth;
                mScreenHeight = mDeviceScreenHeight;
            } else {
                // 默认宽高 1080 * 1920
                mScreenWidth = mBaseScreenWidth;
                mScreenHeight = mBaseScreenHeight;
            }
        }
    }

    /**
     * Helper method to be called from {@link ViewGroup.LayoutParams#setBaseAttributes} override
     * that reads layout_width and layout_height attribute values without throwing an exception if
     * they aren't present.
     */
    public static void fetchWidthAndHeight(ViewGroup.LayoutParams params, TypedArray array,
                                           int widthAttr, int heightAttr) {
        params.width = array.getLayoutDimension(widthAttr, 0);
        params.height = array.getLayoutDimension(heightAttr, 0);
    }

    // 根据百分比改变自身宽高
    public void adjustMyself(int widthMeasureSpec, int heightMeasureSpec, PercentLayoutHelper.PercentLayoutInfo percentLayoutInfo) {
        ViewParent parent = mHost.getParent();
        if (parent == null || parent instanceof PercentFrameLayout
                || parent instanceof PercentLinearLayout || parent instanceof PercentRelativeLayout) {
            return;
        }
        int widthHint = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightHint = View.MeasureSpec.getSize(heightMeasureSpec);
        // 获取LayoutParams并根据百分比参数修改LayoutParams
        ViewGroup.LayoutParams params = mHost.getLayoutParams();

        if (percentLayoutInfo != null) {
            supportTextSize(widthHint, heightHint, mHost, percentLayoutInfo);
            supportPadding(widthHint, heightHint, mHost, percentLayoutInfo);
            supportMinOrMaxDimesion(widthHint, heightHint, mHost, percentLayoutInfo);

            if (params instanceof ViewGroup.MarginLayoutParams) {
                percentLayoutInfo.fillMarginLayoutParams((ViewGroup.MarginLayoutParams) params,
                        widthHint, heightHint);
            } else {
                percentLayoutInfo.fillLayoutParams(params, widthHint, heightHint);
            }
        }
    }

    /**
     * 遍历所有子控件, 并根据百分比改变他们的宽高
     */
    public void adjustChildren(int widthMeasureSpec, int heightMeasureSpec) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "adjustChildren: " + mHost + " widthMeasureSpec: "
                    + View.MeasureSpec.toString(widthMeasureSpec) + " heightMeasureSpec: "
                    + View.MeasureSpec.toString(heightMeasureSpec));
        }
        int widthHint = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightHint = View.MeasureSpec.getSize(heightMeasureSpec);

        if (Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, "widthHint = " + widthHint + " , heightHint = " + heightHint);

        for (int i = 0, N = mHost.getChildCount(); i < N; i++) {
            View view = mHost.getChildAt(i);
            ViewGroup.LayoutParams params = view.getLayoutParams();

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "should adjust " + view + " " + params);
            }

            if (params instanceof PercentLayoutParams) {
                PercentLayoutInfo info =
                        ((PercentLayoutParams) params).getPercentLayoutInfo();
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "using " + info);
                }
                if (info != null) {
                    supportTextSize(widthHint, heightHint, view, info);
                    supportPadding(widthHint, heightHint, view, info);
                    supportMinOrMaxDimesion(widthHint, heightHint, view, info);

                    if (params instanceof ViewGroup.MarginLayoutParams) {
                        info.fillMarginLayoutParams((ViewGroup.MarginLayoutParams) params,
                                widthHint, heightHint);
                    } else {
                        info.fillLayoutParams(params, widthHint, heightHint);
                    }
                }
            }
        }
    }

    // 修改子控件的padding
    private void supportPadding(int widthHint, int heightHint, View view, PercentLayoutInfo info) {
        int left = view.getPaddingLeft(), right = view.getPaddingRight(), top = view.getPaddingTop(), bottom = view.getPaddingBottom();
        PercentLayoutInfo.PercentVal percentVal = info.paddingLeftPercent;
        if (percentVal != null) {
            int base = getBaseByModeAndVal(widthHint, heightHint, percentVal.basemode);
            left = (int) (base * percentVal.percent);
        }
        percentVal = info.paddingRightPercent;
        if (percentVal != null) {
            int base = getBaseByModeAndVal(widthHint, heightHint, percentVal.basemode);
            right = (int) (base * percentVal.percent);
        }

        percentVal = info.paddingTopPercent;
        if (percentVal != null) {
            int base = getBaseByModeAndVal(widthHint, heightHint, percentVal.basemode);
            top = (int) (base * percentVal.percent);
        }

        percentVal = info.paddingBottomPercent;
        if (percentVal != null) {
            int base = getBaseByModeAndVal(widthHint, heightHint, percentVal.basemode);
            bottom = (int) (base * percentVal.percent);
        }
        view.setPadding(left, top, right, bottom);
    }

    private void supportMinOrMaxDimesion(int widthHint, int heightHint, View view, PercentLayoutInfo info) {
        try {
            Class clazz = view.getClass();
            invokeMethod("setMaxWidth", widthHint, heightHint, view, clazz, info.maxWidthPercent);
            invokeMethod("setMaxHeight", widthHint, heightHint, view, clazz, info.maxHeightPercent);
            invokeMethod("setMinWidth", widthHint, heightHint, view, clazz, info.minWidthPercent);
            invokeMethod("setMinHeight", widthHint, heightHint, view, clazz, info.minHeightPercent);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void invokeMethod(String methodName, int widthHint, int heightHint, View view, Class clazz, PercentLayoutInfo.PercentVal percentVal) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (Log.isLoggable(TAG, Log.DEBUG))
            Log.d(TAG, methodName + " ==> " + percentVal);
        if (percentVal != null) {
            Method setMaxWidthMethod = clazz.getMethod(methodName, int.class);
            setMaxWidthMethod.setAccessible(true);
            int base = getBaseByModeAndVal(widthHint, heightHint, percentVal.basemode);
            setMaxWidthMethod.invoke(view, (int) (base * percentVal.percent));
        }
    }

    // 修改子控件的textSize
    private void supportTextSize(int widthHint, int heightHint, View view, PercentLayoutInfo info) {
        PercentLayoutInfo.PercentVal textSizePercent = info.textSizePercent;
        if (textSizePercent == null) return;

        int base = getBaseByModeAndVal(widthHint, heightHint, textSizePercent.basemode);
        float textSize = (int) (base * textSizePercent.percent);

        //Button 和 EditText 是TextView的子类
        if (view instanceof TextView) {
            ((TextView) view).setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        }
    }

    private static int getBaseByModeAndVal(int widthHint, int heightHint, PercentLayoutInfo.BASEMODE basemode) {
        switch (basemode) {
            case BASE_HEIGHT:
                return heightHint;
            case BASE_WIDTH:
                return widthHint;
            case BASE_SCREEN_WIDTH:
                return mScreenWidth;
            case BASE_SCREEN_HEIGHT:
                return mScreenHeight;
        }
        return 0;
    }

    /**
     * 通过View的attrs参数构建百分比布局信息
     * 在LayoutParams(Context c, AttributeSet attrs)构造中调用
     */
    public static PercentLayoutInfo getPercentLayoutInfo(Context context,
                                                         AttributeSet attrs) {
        PercentLayoutInfo info = null;
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.PercentLayout_Layout);

        info = setWidthAndHeightVal(array, info);
        info = setMarginRelatedVal(array, info);
        info = setTextSizeSupportVal(array, info);
        info = setMinMaxWidthHeightRelatedVal(array, info);
        info = setPaddingRelatedVal(array, info);

        array.recycle();
        return info;
    }

    // 设置宽高百分比属性
    private static PercentLayoutInfo setWidthAndHeightVal(TypedArray array, PercentLayoutInfo info) {
        // 设置宽
        PercentLayoutInfo.PercentVal percentVal = getPercentVal(array, R.styleable.PercentLayout_Layout_layout_widthPercent, true);
        if (percentVal != null) {
            info = checkForInfoExists(info);
            info.widthPercent = percentVal;
        }
        // 设置高
        percentVal = getPercentVal(array, R.styleable.PercentLayout_Layout_layout_heightPercent, false);
        if (percentVal != null) {
            info = checkForInfoExists(info);
            info.heightPercent = percentVal;
        }
        return info;
    }

    // 设置字体大小百分比属性
    private static PercentLayoutInfo setTextSizeSupportVal(TypedArray array, PercentLayoutInfo info) {
        //textSizePercent 默认以高度作为基准
        PercentLayoutInfo.PercentVal percentVal = getPercentVal(array, R.styleable.PercentLayout_Layout_layout_textSizePercent, false);
        if (percentVal != null) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "percent text size: " + percentVal.percent);
            }
            info = checkForInfoExists(info);
            info.textSizePercent = percentVal;
        }

        return info;
    }

    // 设置最小最大宽高百分比属性
    private static PercentLayoutInfo setMinMaxWidthHeightRelatedVal(TypedArray array, PercentLayoutInfo info) {
        //maxWidth
        PercentLayoutInfo.PercentVal percentVal = getPercentVal(array,
                R.styleable.PercentLayout_Layout_layout_maxWidthPercent,
                true);
        if (percentVal != null) {
            info = checkForInfoExists(info);
            info.maxWidthPercent = percentVal;
        }
        //maxHeight
        percentVal = getPercentVal(array,
                R.styleable.PercentLayout_Layout_layout_maxHeightPercent,
                false);
        if (percentVal != null) {
            info = checkForInfoExists(info);
            info.maxHeightPercent = percentVal;
        }
        //minWidth
        percentVal = getPercentVal(array,
                R.styleable.PercentLayout_Layout_layout_minWidthPercent,
                true);
        if (percentVal != null) {
            info = checkForInfoExists(info);
            info.minWidthPercent = percentVal;
        }
        //minHeight
        percentVal = getPercentVal(array,
                R.styleable.PercentLayout_Layout_layout_minHeightPercent,
                false);
        if (percentVal != null) {
            info = checkForInfoExists(info);
            info.minHeightPercent = percentVal;
        }

        return info;
    }

    // 设置margin百分比属性
    private static PercentLayoutInfo setMarginRelatedVal(TypedArray array, PercentLayoutInfo info) {
        //默认margin参考宽度
        PercentLayoutInfo.PercentVal percentVal =
                getPercentVal(array,
                        R.styleable.PercentLayout_Layout_layout_marginPercent,
                        true);

        if (percentVal != null) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "percent margin: " + percentVal.percent);
            }
            info = checkForInfoExists(info);
            info.leftMarginPercent = percentVal;
            info.topMarginPercent = percentVal;
            info.rightMarginPercent = percentVal;
            info.bottomMarginPercent = percentVal;
        }

        percentVal = getPercentVal(array, R.styleable.PercentLayout_Layout_layout_marginLeftPercent, true);
        if (percentVal != null) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "percent left margin: " + percentVal.percent);
            }
            info = checkForInfoExists(info);
            info.leftMarginPercent = percentVal;
        }

        percentVal = getPercentVal(array, R.styleable.PercentLayout_Layout_layout_marginTopPercent, false);
        if (percentVal != null) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "percent top margin: " + percentVal.percent);
            }
            info = checkForInfoExists(info);
            info.topMarginPercent = percentVal;
        }

        percentVal = getPercentVal(array, R.styleable.PercentLayout_Layout_layout_marginRightPercent, true);
        if (percentVal != null) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "percent right margin: " + percentVal.percent);
            }
            info = checkForInfoExists(info);
            info.rightMarginPercent = percentVal;
        }

        percentVal = getPercentVal(array, R.styleable.PercentLayout_Layout_layout_marginBottomPercent, false);
        if (percentVal != null) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "percent bottom margin: " + percentVal.percent);
            }
            info = checkForInfoExists(info);
            info.bottomMarginPercent = percentVal;
        }
        percentVal = getPercentVal(array, R.styleable.PercentLayout_Layout_layout_marginStartPercent, true);
        if (percentVal != null) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "percent start margin: " + percentVal.percent);
            }
            info = checkForInfoExists(info);
            info.startMarginPercent = percentVal;
        }

        percentVal = getPercentVal(array, R.styleable.PercentLayout_Layout_layout_marginEndPercent, true);
        if (percentVal != null) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "percent end margin: " + percentVal.percent);
            }
            info = checkForInfoExists(info);
            info.endMarginPercent = percentVal;
        }

        return info;
    }

    // 设置padding百分比属性
    private static PercentLayoutInfo setPaddingRelatedVal(TypedArray array, PercentLayoutInfo info) {
        //默认padding以宽度为标准
        PercentLayoutInfo.PercentVal percentVal = getPercentVal(array,
                R.styleable.PercentLayout_Layout_layout_paddingPercent,
                true);
        if (percentVal != null) {
            info = checkForInfoExists(info);
            info.paddingLeftPercent = percentVal;
            info.paddingRightPercent = percentVal;
            info.paddingBottomPercent = percentVal;
            info.paddingTopPercent = percentVal;
        }


        percentVal = getPercentVal(array,
                R.styleable.PercentLayout_Layout_layout_paddingLeftPercent,
                true);
        if (percentVal != null) {
            info = checkForInfoExists(info);
            info.paddingLeftPercent = percentVal;
        }

        percentVal = getPercentVal(array,
                R.styleable.PercentLayout_Layout_layout_paddingRightPercent,
                true);
        if (percentVal != null) {
            info = checkForInfoExists(info);
            info.paddingRightPercent = percentVal;
        }

        percentVal = getPercentVal(array,
                R.styleable.PercentLayout_Layout_layout_paddingTopPercent,
                true);
        if (percentVal != null) {
            info = checkForInfoExists(info);
            info.paddingTopPercent = percentVal;
        }

        percentVal = getPercentVal(array,
                R.styleable.PercentLayout_Layout_layout_paddingBottomPercent,
                true);
        if (percentVal != null) {
            info = checkForInfoExists(info);
            info.paddingBottomPercent = percentVal;
        }

        return info;
    }

    // 通过attr获取百分比属性
    private static PercentLayoutInfo.PercentVal getPercentVal(TypedArray array, int index, boolean baseWidth) {
        String sizeStr = array.getString(index);
        PercentLayoutInfo.PercentVal percentVal = getPercentVal(sizeStr, baseWidth);
        return percentVal;
    }


    @NonNull
    private static PercentLayoutInfo checkForInfoExists(PercentLayoutInfo info) {
        info = info != null ? info : new PercentLayoutInfo();
        return info;
    }

    // 默认百分比布局的正则
    private static final String REGEX_PERCENT = "^(([0-9]+)([.]([0-9]+))?|([.]([0-9]+))?)%([s]?[wh]?)$";
    // 为了避免要计算分数而设置的相对百分比的正则
    private static final String REGEX_RELATIVE_PERCENT = "^([0-9]+)([s]?[wh])$";

    /**
     * 将布局中获取的自定义attr转换成具体的百分比属性
     */
    private static PercentLayoutInfo.PercentVal getPercentVal(String percentStr, boolean isOnWidth) {
        if (percentStr == null) {
            return null;
        }
        // 配置正则
        Pattern p = Pattern.compile(REGEX_PERCENT);
        Matcher matcher = p.matcher(percentStr);
        if (!matcher.matches()) {
            // 如果不匹配默认的百分比规则, 则继续匹配相对百分比正则
            return getRelativePercentVal(percentStr);
        }
        int len = percentStr.length();
        String floatVal = matcher.group(1);
        String lastAlpha = percentStr.substring(len - 1);
        // 百分比值
        float percent = Float.parseFloat(floatVal) / 100f;
        // 创建,设置并返回百分比属性
        PercentLayoutInfo.PercentVal percentVal = new PercentLayoutInfo.PercentVal();
        percentVal.percent = percent;
        if (percentStr.endsWith(PercentLayoutInfo.BASEMODE.SW)) {
            // 基于屏幕宽度
            percentVal.basemode = PercentLayoutInfo.BASEMODE.BASE_SCREEN_WIDTH;
        } else if (percentStr.endsWith(PercentLayoutInfo.BASEMODE.SH)) {
            // 基于屏幕高度
            percentVal.basemode = PercentLayoutInfo.BASEMODE.BASE_SCREEN_HEIGHT;
        } else if (percentStr.endsWith(PercentLayoutInfo.BASEMODE.PERCENT)) {
            // 未指定, 根据当前属性判断应该是基于父控件宽度还是高度
            if (isOnWidth) {
                percentVal.basemode = PercentLayoutInfo.BASEMODE.BASE_WIDTH;
            } else {
                percentVal.basemode = PercentLayoutInfo.BASEMODE.BASE_HEIGHT;
            }
        } else if (percentStr.endsWith(PercentLayoutInfo.BASEMODE.W)) {
            // 基于父控件宽度
            percentVal.basemode = PercentLayoutInfo.BASEMODE.BASE_WIDTH;
        } else if (percentStr.endsWith(PercentLayoutInfo.BASEMODE.H)) {
            // 基于父控件高度
            percentVal.basemode = PercentLayoutInfo.BASEMODE.BASE_HEIGHT;
        } else {
            throw new IllegalArgumentException("the " + percentStr + " must be endWith [%|w|h|sw|sh]");
        }

        return percentVal;
    }

    // 匹配自定义的相对百分比
    private static PercentLayoutInfo.PercentVal getRelativePercentVal(String percentStr) {
        Pattern p = Pattern.compile(REGEX_RELATIVE_PERCENT);
        Matcher matcher = p.matcher(percentStr);
        if (!matcher.matches()) {
            // 不匹配则抛出异常
            throw new RuntimeException("the value of layout_xxxPercent invalid! ==>" + percentStr);
        }
        String intVal = matcher.group(1);

        PercentLayoutInfo.PercentVal percentVal = new PercentLayoutInfo.PercentVal();
        if (percentStr.endsWith(PercentLayoutInfo.BASEMODE.W)) {
            // 基于屏幕宽度
            percentVal.basemode = PercentLayoutInfo.BASEMODE.BASE_SCREEN_WIDTH;
            percentVal.percent = 1f * Integer.parseInt(intVal) / mBaseScreenWidth;
        } else if (percentStr.endsWith(PercentLayoutInfo.BASEMODE.H)) {
            // 基于屏幕高度
            percentVal.basemode = PercentLayoutInfo.BASEMODE.BASE_SCREEN_HEIGHT;
            percentVal.percent = 1f * Integer.parseInt(intVal) / mBaseScreenHeight;
        } else {
            throw new IllegalArgumentException("the " + percentStr + " must be endWith [%|w|h|sw|sh]");
        }
        return percentVal;
    }

    /**
     * Iterates over children and restores their original dimensions that were changed for
     * percentage values. Calling this method only makes sense if you previously called
     * {@link PercentLayoutHelper#adjustChildren(int, int)}.
     */

    public void restoreOriginalParams() {
        for (int i = 0, N = mHost.getChildCount(); i < N; i++) {
            View view = mHost.getChildAt(i);
            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "should restore " + view + " " + params);
            }
            if (params instanceof PercentLayoutParams) {
                PercentLayoutInfo info =
                        ((PercentLayoutParams) params).getPercentLayoutInfo();
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "using " + info);
                }
                if (info != null) {
                    if (params instanceof ViewGroup.MarginLayoutParams) {
                        info.restoreMarginLayoutParams((ViewGroup.MarginLayoutParams) params);
                    } else {
                        info.restoreLayoutParams(params);
                    }
                }
            }
        }
    }

    /**
     * Iterates over children and checks if any of them would like to get more space than it
     * received through the percentage dimension.
     * <p/>
     * If you are building a layout that supports percentage dimensions you are encouraged to take
     * advantage of this method. The developer should be able to specify that a child should be
     * remeasured by adding normal dimension attribute with {@code wrap_content} value. For example
     * he might specify child's attributes as {@code app:layout_widthPercent="60%p"} and
     * {@code android:layout_width="wrap_content"}. In this case if the child receives too little
     * space, it will be remeasured with width set to {@code WRAP_CONTENT}.
     *
     * @return True if the measure phase needs to be rerun because one of the children would like
     * to receive more space.
     */
    public boolean handleMeasuredStateTooSmall() {
        boolean needsSecondMeasure = false;
        for (int i = 0, N = mHost.getChildCount(); i < N; i++) {
            View view = mHost.getChildAt(i);
            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "should handle measured state too small " + view + " " + params);
            }
            if (params instanceof PercentLayoutParams) {
                PercentLayoutInfo info =
                        ((PercentLayoutParams) params).getPercentLayoutInfo();
                if (info != null) {
                    if (shouldHandleMeasuredWidthTooSmall(view, info)) {
                        needsSecondMeasure = true;
                        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    }
                    if (shouldHandleMeasuredHeightTooSmall(view, info)) {
                        needsSecondMeasure = true;
                        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    }
                }
            }
        }
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "should trigger second measure pass: " + needsSecondMeasure);
        }
        return needsSecondMeasure;
    }

    private static boolean shouldHandleMeasuredWidthTooSmall(View view, PercentLayoutInfo info) {
        int state = ViewCompat.getMeasuredWidthAndState(view) & ViewCompat.MEASURED_STATE_MASK;
        if (info == null || info.widthPercent == null) {
            return false;
        }
        return state == ViewCompat.MEASURED_STATE_TOO_SMALL && info.widthPercent.percent >= 0 &&
                info.mPreservedParams.width == ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    private static boolean shouldHandleMeasuredHeightTooSmall(View view, PercentLayoutInfo info) {
        int state = ViewCompat.getMeasuredHeightAndState(view) & ViewCompat.MEASURED_STATE_MASK;
        if (info == null || info.heightPercent == null) {
            return false;
        }
        return state == ViewCompat.MEASURED_STATE_TOO_SMALL && info.heightPercent.percent >= 0 &&
                info.mPreservedParams.height == ViewGroup.LayoutParams.WRAP_CONTENT;
    }


    /**
     * 百分比信息
     */
    public static class PercentLayoutInfo {

        // 基准模式
        private enum BASEMODE {

            BASE_WIDTH, BASE_HEIGHT, BASE_SCREEN_WIDTH, BASE_SCREEN_HEIGHT;

            // 未指定
            public static final String PERCENT = "%";
            // 父控件宽度
            public static final String W = "w";
            // 父控件高度
            public static final String H = "h";
            // 屏幕宽度
            public static final String SW = "sw";
            // 屏幕高度
            public static final String SH = "sh";
        }

        // 百分比属性(包括基准模式和百分比值)
        public static class PercentVal {
            public float percent = -1;
            public BASEMODE basemode;

            public PercentVal() {
            }

            public PercentVal(float percent, BASEMODE baseMode) {
                this.percent = percent;
                this.basemode = baseMode;
            }

            @Override
            public String toString() {
                return "PercentVal{" +
                        "percent=" + percent +
                        ", basemode=" + basemode.name() +
                        '}';
            }
        }

        public PercentVal widthPercent;
        public PercentVal heightPercent;

        public PercentVal leftMarginPercent;
        public PercentVal topMarginPercent;
        public PercentVal rightMarginPercent;
        public PercentVal bottomMarginPercent;
        public PercentVal startMarginPercent;
        public PercentVal endMarginPercent;

        public PercentVal textSizePercent;

        public PercentVal maxWidthPercent;
        public PercentVal maxHeightPercent;
        public PercentVal minWidthPercent;
        public PercentVal minHeightPercent;

        public PercentVal paddingLeftPercent;
        public PercentVal paddingRightPercent;
        public PercentVal paddingTopPercent;
        public PercentVal paddingBottomPercent;

        final ViewGroup.MarginLayoutParams mPreservedParams;


        public PercentLayoutInfo() {
            mPreservedParams = new ViewGroup.MarginLayoutParams(0, 0);
        }

        /**
         * 将LayoutParams填充成百分比模式
         */
        public void fillLayoutParams(ViewGroup.LayoutParams params, int widthHint,
                                     int heightHint) {
            // 先将原始的布局参数保存起来
            mPreservedParams.width = params.width;
            mPreservedParams.height = params.height;
            // 根据百分比属性设置布局宽高
            if (widthPercent != null) {
                int base = getBaseByModeAndVal(widthHint, heightHint, widthPercent.basemode);
                params.width = (int) (base * widthPercent.percent);
            }
            if (heightPercent != null) {
                int base = getBaseByModeAndVal(widthHint, heightHint, heightPercent.basemode);
                params.height = (int) (base * heightPercent.percent);
            }

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "after fillLayoutParams: (" + params.width + ", " + params.height + ")");
            }
        }

        /**
         * 将带Margin的LayoutParams填充成百分比模式
         */
        public void fillMarginLayoutParams(ViewGroup.MarginLayoutParams params, int widthHint,
                                           int heightHint) {
            // 填充宽高
            fillLayoutParams(params, widthHint, heightHint);

            // 预存原始的margin, 再赋值百分比margin
            mPreservedParams.leftMargin = params.leftMargin;
            mPreservedParams.topMargin = params.topMargin;
            mPreservedParams.rightMargin = params.rightMargin;
            mPreservedParams.bottomMargin = params.bottomMargin;
            MarginLayoutParamsCompat.setMarginStart(mPreservedParams,
                    MarginLayoutParamsCompat.getMarginStart(params));
            MarginLayoutParamsCompat.setMarginEnd(mPreservedParams,
                    MarginLayoutParamsCompat.getMarginEnd(params));

            if (leftMarginPercent != null) {
                int base = getBaseByModeAndVal(widthHint, heightHint, leftMarginPercent.basemode);
                params.leftMargin = (int) (base * leftMarginPercent.percent);
            }
            if (topMarginPercent != null) {
                int base = getBaseByModeAndVal(widthHint, heightHint, topMarginPercent.basemode);
                params.topMargin = (int) (base * topMarginPercent.percent);
            }
            if (rightMarginPercent != null) {
                int base = getBaseByModeAndVal(widthHint, heightHint, rightMarginPercent.basemode);
                params.rightMargin = (int) (base * rightMarginPercent.percent);
            }
            if (bottomMarginPercent != null) {
                int base = getBaseByModeAndVal(widthHint, heightHint, bottomMarginPercent.basemode);
                params.bottomMargin = (int) (base * bottomMarginPercent.percent);
            }
            if (startMarginPercent != null) {
                int base = getBaseByModeAndVal(widthHint, heightHint, startMarginPercent.basemode);
                MarginLayoutParamsCompat.setMarginStart(params,
                        (int) (base * startMarginPercent.percent));
            }
            if (endMarginPercent != null) {
                int base = getBaseByModeAndVal(widthHint, heightHint, endMarginPercent.basemode);
                MarginLayoutParamsCompat.setMarginEnd(params,
                        (int) (base * endMarginPercent.percent));
            }
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "after fillMarginLayoutParams: (" + params.width + ", " + params.height
                        + ")");
            }
        }

        // 恢复原始的带Margin布局参数
        public void restoreMarginLayoutParams(ViewGroup.MarginLayoutParams params) {
            restoreLayoutParams(params);
            params.leftMargin = mPreservedParams.leftMargin;
            params.topMargin = mPreservedParams.topMargin;
            params.rightMargin = mPreservedParams.rightMargin;
            params.bottomMargin = mPreservedParams.bottomMargin;
            MarginLayoutParamsCompat.setMarginStart(params,
                    MarginLayoutParamsCompat.getMarginStart(mPreservedParams));
            MarginLayoutParamsCompat.setMarginEnd(params,
                    MarginLayoutParamsCompat.getMarginEnd(mPreservedParams));
        }

        // 恢复原始的布局参数
        public void restoreLayoutParams(ViewGroup.LayoutParams params) {
            params.width = mPreservedParams.width;
            params.height = mPreservedParams.height;
        }
    }

    /**
     * 如果你想在代码中修改某一个控件的百分比布局参数, 确保它的父控件是LshPercentLayout, 通过调用getPercentLayoutInfo()来获取百分比布局参数
     */
    public interface PercentLayoutParams {
        PercentLayoutInfo getPercentLayoutInfo();
    }
}
