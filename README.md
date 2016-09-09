[![Download](https://api.bintray.com/packages/byteam/maven/delta/images/download.svg)](https://bintray.com/byteam/maven/delta/_latestVersion) [![API](https://img.shields.io/badge/API-8%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=8) [![License](https://img.shields.io/badge/License-Apache%202.0-orange.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

[中文wiki](https://github.com/byteam/delta/wiki)

![logo](arts/logo_huakang.png)

> Delta offers your android app the hot fix and incremental upgrade powers.

## Principle

Inspired by wechat's [Tinker](http://bugly.qq.com/bbs/forum.php?mod=viewthread&tid=1264&extra=page%3D1), please check this page for more information.

![Flowchart](arts/patch_flowchart.png)

## Features

1. Kind to developers and easy to use.
2. Hot fix and incremental upgrade(on the schedule).
3. Supports all platform versions.
4. Fine compatibility.
5. Negligible performance cost.

## To be improved

1. Verify patch file.
2. Handle exception.
3. Incremental upgrade.
4. It's up to you.

## How to get

In your project's `build.gradle`

```
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:x.y.z'
        classpath 'org.byteam.delta:delta-gradle-plugin:x.y.z'
        ...
    }
}
```

In your application module's `build.gradle`

```
apply plugin: 'com.android.application'
apply plugin: 'delta'
...
delta {
    enable true
    autoPushPatchToDevice true // for test
}
```

## Configurations

Avaliable options:

|    option     |     type     |     usage     |     default     |
|:---:|:---:|:---:|:---:|
|enable|boolean|enable delta or not|false|
|versionCode|int|specify application's version code|variant's version code|
|deltaVersion|String|specify delta library's version|same as plugin version|
|maxNumberOfIdxPerDex|int|max number of methods in per dex|0xFF00|
|mapping|String|specify the mapping file|null|
|autoPushPatchToDevice|boolean|auto push patch to device rom, used for test|fale|

## Generate patch dex

The delta gradle plugin will create some tasks:

![tasks](arts/tasks.png)

1. Execute backup task to backup the dex and mapping files for future usage.
2. Find bugs, fix it(change some code).
3. Execute patch task, this task will apply existing mapping or specifed mapping and generate a new dex, then produce the final patch.

The final patch directory looks like this:

![patch_dir](arts/patch_dir.png)


## How to use

In your own `Application`:

```
public class App extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        // Install existing patch
        Delta.install(this);
    }

    ...
}
```

```
/**
 * 从手机内存中应用patch,一般用于测试,切勿在正式环境中使用.
 *
 * @param context Context
 */
Delta.applyPatchFromDevice(Context context);

/**
 * 应用指定的patch. 该操作为耗时操作,建议放到非UI线程执行.
 *
 * @param context  Context
 * @param patchDex patch文件
 */
Delta.applyPatch(Context context, File patchDex);

/**
 * 删除所有补丁.
 *
 * @param context  Context
 */
Delta.clean(Context context;

```

## Partners

*Looking forward to your join!*

![dangdang](arts/partners/dangdang.png)

## Thanks to

1. [MultiDex](https://developer.android.com/studio/build/multidex.html)
2. [微信热补丁实践之路](http://bugly.qq.com/bbs/forum.php?mod=viewthread&tid=1264&extra=page%3D1)
3. [Binary diff/patch utility](http://www.daemonology.net/bsdiff/)
4. [安卓App热补丁动态修复技术介绍](http://mp.weixin.qq.com/s?__biz=MzI1MTA1MzM2Nw==&mid=400118620&idx=1&sn=b4fdd5055731290eef12ad0d17f39d4a&scene=1&srcid=1031x2ljgSF4xJGlH1xMCJxO&uin=MjAyNzY1NTU=&key=04dce534b3b035ef58d8714d714d36bcc6cc7e136bbd64850522b491d143aafceb62c46421c5965e18876433791d16ec&devicetype=iMac%20MacBookPro12,1%20OSX%20OSX%2010.10.5%20build%2814F27%29&version=11020201&lang=zh_CN&pass_ticket=7O/VfztuLjqu23ED2WEkvy1SJstQD4eLRqX%2b%2bbCY3uE=)
5. [Instant Run: How Does it Work?](https://medium.com/google-developers/instant-run-how-does-it-work-294a1633367f#.ft7r6je52)

## License

[Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)
