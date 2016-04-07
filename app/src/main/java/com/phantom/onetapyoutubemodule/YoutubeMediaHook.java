package com.phantom.onetapyoutubemodule;

import android.content.Context;
import android.content.Intent;

import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class YoutubeMediaHook implements IXposedHookLoadPackage {
    private static final String ACTION_SAVE_YOUTUBE_VIDEO = "com.phantom.onetapvideodownload.action.saveyoutubeurl";
    private static final String ACTION_SEND_NOTIFICATION_FOR_EMAIL = "com.phantom.onetapvideodownload.action.sendemail";
    private static final String ONE_TAP_PACKAGE_NAME = "com.phantom.onetapvideodownload";
    private static final String PACKAGE_NAME = "com.google.android.youtube";
    private static final String CLASS_NAME = "com.phantom.onetapvideodownload.IpcService";
    private static final String EXTRA_PARAM_STRING = "com.phantom.onetapvideodownload.extra.url";
    private static final String EXTRA_NOTIFICATION_TITLE = PACKAGE_NAME + ".extra.notification_title";
    private static final String EXTRA_NOTIFICATION_BODY = PACKAGE_NAME + ".extra.notification_body";
    private static final String EXTRA_EMAIL_SUBJECT = PACKAGE_NAME + ".extra.email_subject";
    private static final String EXTRA_EMAIL_BODY = PACKAGE_NAME + ".extra.email_body";
    private static final String ORIGINAL_MAIN_CLASS_NAME = "com.google.android.libraries.youtube.innertube.model.media.FormatStreamModel";
    private static final String ORIGINAL_METHOD_CLASS_NAME = "com.google.android.libraries.youtube.proto.nano.InnerTubeApi.FormatStream";
    private static final HashMap<Integer, YouTubePackage> applicationMap = new HashMap<>();
    private static long lastVideoTime = System.currentTimeMillis();
    static {
        applicationMap.put(0, new YouTubePackage(ORIGINAL_MAIN_CLASS_NAME, ORIGINAL_METHOD_CLASS_NAME));
        applicationMap.put(108358, new YouTubePackage("jci", "noo"));
        applicationMap.put(107756, new YouTubePackage("ipz", "myv"));
        applicationMap.put(108754, new YouTubePackage("knr", "pkl"));
        applicationMap.put(108959, new YouTubePackage("kqo", "pqh"));
        applicationMap.put(107858, new YouTubePackage("irs", "nax"));
        applicationMap.put(102857, new YouTubePackage("hwt", "lqk"));
        applicationMap.put(108553, new YouTubePackage("jmv", "oae"));
        applicationMap.put(102555, new YouTubePackage("hux", "lkq"));
        applicationMap.put(103457, new YouTubePackage("idv", "mpj"));
        applicationMap.put(108058, new YouTubePackage("jcw", "noc"));
        applicationMap.put(102952, new YouTubePackage("hxz", "lsy"));
        applicationMap.put(103351, new YouTubePackage("ift", "mkx"));
        applicationMap.put(110354, new YouTubePackage("lkn", "qcf"));
        applicationMap.put(101855, new YouTubePackage("gmg", "jlb"));
        applicationMap.put(103155, new YouTubePackage("ibx", "map"));
        applicationMap.put(101653, new YouTubePackage("gin", "jda"));
        applicationMap.put(101253, new YouTubePackage("gfl", "iur"));
        applicationMap.put(100203, new YouTubePackage("fmo", "hoi"));
        applicationMap.put(110153, new YouTubePackage("kts", "pgj"));
        applicationMap.put(102455, new YouTubePackage("hfz", "ktr"));
        applicationMap.put(100506, new YouTubePackage("fwc", "ibn"));
        applicationMap.put(100305, new YouTubePackage("foe", "hrf"));
        applicationMap.put(100405, new YouTubePackage("ftp", "hxs"));
        applicationMap.put(103553, new YouTubePackage("lkn", "qcf"));
        applicationMap.put(100852, new YouTubePackage("geg", "iny"));
        applicationMap.put(108957, new YouTubePackage("kqo", "pqh"));
        applicationMap.put(110759, new YouTubePackage("lxg", "qub"));
        applicationMap.put(111060, new YouTubePackage("mdn", "rgd"));
    }

    public Context getContext() {
        Class activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", null);
        Object activityThread = XposedHelpers.callStaticMethod(activityThreadClass, "currentActivityThread");
        return (Context) XposedHelpers.callMethod(activityThread, "getSystemContext");
    }

    public boolean findClass(ClassLoader loader, String className) {
        try {
            loader.loadClass(className);
            return true;
        } catch( ClassNotFoundException e ) {
            return false;
        }
    }

    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(PACKAGE_NAME)) {
            return;
        }

        // Method Signature
        // public MainClass(MethodParameterClass paramJlb, String paramString, long paramLong)

        final Context context = getContext();
        final ClassLoader loader = lpparam.classLoader;
        boolean isNotObfuscated = findClass(loader, ORIGINAL_MAIN_CLASS_NAME);

        final XC_MethodHook methodHook = new XC_MethodHook() {
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam hookParams) throws Throwable {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastVideoTime < 1200L) {
                    return;
                }

                lastVideoTime = currentTime;
                String paramString = (String)hookParams.args[1];
                XposedBridge.log(paramString);

                Intent intent = new Intent(ACTION_SAVE_YOUTUBE_VIDEO);
                intent.setClassName(ONE_TAP_PACKAGE_NAME, CLASS_NAME);
                intent.putExtra(EXTRA_PARAM_STRING, paramString);
                context.startService(intent);
            }
        };

        int packageVersion = context.getPackageManager()
                .getPackageInfo(lpparam.packageName, 0).versionCode;

        XposedBridge.log("OneTapVideoDownload : Youtube Package version : " + packageVersion);

        YouTubePackage currentPackage;
        if (isNotObfuscated) {
            currentPackage = applicationMap.get(0);
        } else {
            currentPackage = applicationMap.get(getSignificantDigits(packageVersion));
        }

        if (currentPackage == null) {
            XposedBridge.log("One Tap Video Download : Trying bruteforcing");
            boolean successful = false;
            for (Map.Entry<Integer, YouTubePackage> pair : applicationMap.entrySet()) {
                String mainClassName = pair.getValue().getMainClass();
                String parameterClassName = pair.getValue().getMethodParameterClass();
                successful = hookYoutube(lpparam.classLoader, methodHook, mainClassName,
                        parameterClassName);
                if (successful) {
                    break;
                }
            }

            if (!successful) {
                XposedBridge.log("OneTapVideoDownload : Class names not found for this youtube version. " +
                        "Please contact developer to get support for this package");
                Intent intent = new Intent(ACTION_SEND_NOTIFICATION_FOR_EMAIL);
                intent.setClassName(ONE_TAP_PACKAGE_NAME, CLASS_NAME);
                intent.putExtra(EXTRA_NOTIFICATION_TITLE, "Youtube Module not supported");
                intent.putExtra(EXTRA_NOTIFICATION_BODY, "Please click to send this information to the developer");
                intent.putExtra(EXTRA_EMAIL_SUBJECT, "Youtube module not supported");
                intent.putExtra(EXTRA_EMAIL_BODY, "Youtube module is not supported. Youtube version : " + packageVersion);
                context.startService(intent);
            }
        } else {
            String mainClassName = currentPackage.getMainClass();
            String parameterClassName = currentPackage.getMethodParameterClass();
            hookYoutube(lpparam.classLoader, methodHook, mainClassName, parameterClassName);
        }
    }

    private boolean hookYoutube(ClassLoader classLoader, XC_MethodHook methodHook,
                                String mainClassName, String parameterClassName) {
        try {
            Class mainClass = XposedHelpers.findClass(mainClassName, classLoader);
            Object[] objects = new Object[]{
                    XposedHelpers.findClass(parameterClassName, classLoader),
                    String.class,
                    Long.TYPE,
                    methodHook
            };
            XposedHelpers.findAndHookConstructor(mainClass, objects);
        } catch (Exception e) {
            return false;
        }
        XposedBridge.log("OneTapVideoDownload : Successful Hooking : " + mainClassName);
        return true;
    }

    private int getSignificantDigits(int version) {
        return version / (int)Math.pow(10, (int)Math.log10(version) - 5);
    }
}
