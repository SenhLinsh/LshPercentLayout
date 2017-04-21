# LshPercentLayout

>基于[https://github.com/hongyangAndroid/android-percent-support-extend](https://github.com/hongyangAndroid/android-percent-support-extend)进行开发,
对其进行了功能的增进和完善, 使其更方便于开发和使用.

## 使用方法:
### 原库的使用方法请见:
[https://github.com/hongyangAndroid/android-percent-support-extend](https://github.com/hongyangAndroid/android-percent-support-extend)

### 添加依赖
在项目根目录build.gradle中添加自定义maven

```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

在module的build.gradle中添加依赖

```
	dependencies {
	        compile 'com.github.SenhLinsh:LshPercentLayout:v1.0'
	}
```

## 扩展

1. PercentLayout可以直接设置layout_percent** 属性 (原版只能给PercentLayout的子View制定百分比属性)
2. 可以在预览中显示xx%sw和xx%sh (原版无法预览, 因为预览中无法获取屏幕宽高)
3. 新增相对百分比, 基于设计图和屏幕宽高, 只要配置好设计图的宽高就可以直接在布局中设置与设计图相同的尺寸, 免于计算百分比

### 如何在预览中正确显示
在清单文件的application节点中设置预览设备的屏幕尺寸, 然后在预览中选择配置好的尺寸设备, 即可完美接合

```
   <!--百分比布局用于作为预览的预览屏幕宽高-->
    <meta-data
        android:name="DEVICE_SCREEN_WIDTH"
        android:value="1080"/>
    <meta-data
        android:name="DEVICE_SCREEN_HEIGHT"
        android:value="1920"/>
```

### 如何设置设计图的宽高
在清单文件的application节点中配置如下meta-data (将数值改为设计图实际尺寸)

```
    <!--百分比布局用于作为对比的基础屏幕宽高-->
    <meta-data
        android:name="BASE_SCREEN_WIDTH"
        android:value="1920"/>
    <meta-data
        android:name="BASE_SCREEN_HEIGHT"
        android:value="1080"/>
```

### 如何设置布局属性

#### 支持的属性
* layout_heightPercent
* layout_widthPercent
* layout_marginBottomPercent
* layout_marginEndPercent
* layout_marginLeftPercent
* layout_marginPercent
* layout_marginRightPercent
* layout_marginStartPercent
* layout_marginTopPercent
* layout_textSizePercent
* layout_maxWidthPercent
* layout_maxHeightPercent
* layout_minWidthPercent
* layout_minHeightPercent
* layout_paddingPercent
* layout_paddingTopPercent
* layout_paddingBottomPercent
* layout_paddingLeftPercent
* layout_paddingRightPercent

对于值可以取：10%w , 10%h , 10% , 10%sw , 10%sh, 100w, 100sw, 100h, 100sh<br/>
注意: 100w和100sw, 100h和100sh的效果是一样的, 都是基于设计图的屏幕宽或者高
