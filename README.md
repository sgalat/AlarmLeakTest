# Alarm Leak POC App

## Goal

Demonstrate what appears to be a reference leak issue in Android AlarmManager service.

## Description

The [API 24 AlarmManager.setExact()](https://developer.android.com/reference/android/app/AlarmManager#setExact(int,%20long,%20java.lang.String,%20android.app.AlarmManager.OnAlarmListener,%20android.os.Handler)) method appears to accumulate global references in the AlarmManager service when invoked repeatedly.

The accumulated references don't seem to be ever released, which leads to evenutual exhaustion of resources in ART and a crash of the AlarmManager system process.

The issue has been observed on Android 11, and the same test doesn't demonstrate the issue on Android 8.1. The Android versions between 8.1 and 11 haven't been tested.

## Details

The easiest way to reproduce the leak issue is to schedule/cancel the alarm in a loop using the same listener object, which is implemented in this POC app.

However, the issue also reproduces with a more comprehensive test where the app waits for the alarm to trigger before rescheduling. It takes longer to accumulate references this way, and appears to be a less stable approach in general to reproduce the overflow issue.

## Instructions

Press the Start button in the app and wait.

The following values can be modified in the source code as needed:
* Modify the value of `NUM_BATCH_ALARMS` to increase number of alarms scheduled in one batch
* Modify the value of `SCHEDULING_PERIOD_MS` to modify the period at which a new batch of alarms is scheduled

If the issue is reproduced, the Android system server will eventually crash.

## Tombstone Example

Below are snippets from a tombstone after the service crash on Pixel 5a running AOSP 11:
```
signal 6 (SIGABRT), code -1 (SI_QUEUE), fault addr --------
Abort message: 'JNI ERROR (app bug): global reference table overflow (max=51200)global reference table dump:
  Last 10 entries (of 2000):
     1999: 0x15efe770 com.android.server.AlarmManagerService$2
```
Backtrace:
```
backtrace:
      #00 pc 000000000004e178  /apex/com.android.runtime/lib64/bionic/libc.so (abort+168) (BuildId: bca874ad82277777df5c95ca3b0f6e6f)
      #01 pc 0000000000552300  /apex/com.android.art/lib64/libart.so (art::Runtime::Abort(char const*)+2260) (BuildId: 654611f4b82732632c62ea0b86e40f78)
      #02 pc 0000000000013990  /system/lib64/libbase.so (android::base::SetAborter(std::__1::function<void (char const*)>&&)::$_3::__invoke(char const*)+76) (BuildId: 46870759aa3cab9f96ce2152bf079214)
      #03 pc 0000000000012fb4  /system/lib64/libbase.so (android::base::LogMessage::~LogMessage()+320) (BuildId: 46870759aa3cab9f96ce2152bf079214)
      #04 pc 00000000003843a8  /apex/com.android.art/lib64/libart.so (art::JavaVMExt::AddGlobalRef(art::Thread*, art::ObjPtr<art::mirror::Object>)+268) (BuildId: 654611f4b82732632c62ea0b86e40f78)
      #05 pc 00000000003969b8  /apex/com.android.art/lib64/libart.so (art::JNI<false>::NewGlobalRef(_JNIEnv*, _jobject*)+632) (BuildId: 654611f4b82732632c62ea0b86e40f78)
      #06 pc 000000000012404c  /system/lib64/libandroid_runtime.so (JavaDeathRecipient::JavaDeathRecipient(_JNIEnv*, _jobject*, android::sp<DeathRecipientList> const&)+140) (BuildId: 6dabcbee3fe1aee01da091e87e94c772)
      #07 pc 0000000000123bc8  /system/lib64/libandroid_runtime.so (android_os_BinderProxy_linkToDeath(_JNIEnv*, _jobject*, _jobject*, int)+180) (BuildId: 6dabcbee3fe1aee01da091e87e94c772)
      #08 pc 00000000002041d0  /system/framework/arm64/boot-framework.oat (art_jni_trampoline+160) (BuildId: 49c2c610bbbb06b4c2f583f1e3c6fb7cd0976b2a)
      #09 pc 0000000001851f70  /system/framework/oat/arm64/services.odex (com.android.server.AlarmManagerService.setImpl+304) (BuildId: 2c4d1de11976c08b2cbbfaa9e70020145750a6c1)
      #10 pc 00000000015afb94  /system/framework/oat/arm64/services.odex (com.android.server.AlarmManagerService$4.set+708) (BuildId: 2c4d1de11976c08b2cbbfaa9e70020145750a6c1)
```

See the full tombstone file in `logs/aosp_11_pixel5a_tombstone`.
