/*
 * Copyright © 2016 - 2018 by GitHub.com/JasonQS
 * anti-recall.qsboy.com
 * All Rights Reserved
 */

package com.qsboy.antirecall.access;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.qsboy.utils.NodesInfo;

import java.util.List;

public class MainService extends AccessibilityService {

    private String TAG = "Main Service";
    private AccessibilityNodeInfo root;
    private String packageName;
    final String pkgTim = "com.tencent.tim";
    final String pkgQQ = "com.tencent.mobileqq";
    final String pkgWX = "com.tencent.mm";
    WXAutoLogin autoLogin;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // TODO: 加一段 服务运行时间段 记录第一次成功启动服务到最后一次收到 event
        if (event.getPackageName() == null) {
            Log.d(TAG, "onAccessibilityEvent: package name is null, return");
            return;
        }
        packageName = event.getPackageName() + "";

//        if (!(packageName.equals(pkgTim) || packageName.equals(pkgQQ) || packageName.equals(pkgWX)))
//            return;

        root = getRootInActiveWindow();
        if (root == null) {
//            Log.d(TAG, "onAccessibilityEvent: root is null, return");
            return;
        }

        int eventType = event.getEventType();
        if (eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            Log.v(TAG, AccessibilityEvent.eventTypeToString(eventType));
        }

        switch (eventType) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                onNotification(event);
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                autoLogin.autoLoginWX();
                onContentChanged(event);
                break;
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                onClick(event);
                break;

        }

    }

    private void onContentChanged(AccessibilityEvent event) {
        if (root == null)
            return;
        // 只需在改变类型为文字时执行添加操作
        // 大部分change type为 CONTENT_CHANGE_TYPE_SUBTREE
        if (event.getContentChangeTypes() != AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT)
            return;
        CharSequence cs = event.getSource().getText();

        switch (packageName) {
            case pkgTim:
                Log.d(TAG, "\nonContentChanged: " + cs);
                new TimClient(this).onContentChanged(root);
                break;
            case pkgQQ:
                Log.d(TAG, "\nonContentChanged: " + cs);
                new QQClient(this).onContentChanged(root);
                break;
        }
    }

    private void onClick(AccessibilityEvent event) {
        Log.i(TAG, "onClick " + event.getText());
//        NodesInfo.show(root, "d");
        switch (packageName) {
            case pkgTim:
                new TimClient(this).findRecalls(root, event);
                break;
            case pkgQQ:
                new QQClient(this).findRecalls(root, event);
                break;
            case pkgWX:
                NodesInfo.show(root, TAG);

        }
    }

    private void onNotification(AccessibilityEvent event) {
        Log.i(TAG, "onNotification: " + packageName);
        List<CharSequence> texts = event.getText();
        if (texts.isEmpty() || texts.size() == 0) {
            NodesInfo.show(root, TAG);
            //微信的登录通知的 text 是 null
            autoLogin.flagEnable();
            return;
        }
        Log.i(TAG, "onNotification: " + packageName + " | " + texts);
        switch (packageName) {
            case pkgTim:
//                new TimClient(this).onContentChanged(root);
                new TimClient(this).onNotificationChanged(event);
                break;
            case pkgQQ:
//                new QQClient(this).onContentChanged(root);
                new QQClient(this).onNotificationChanged(event);
                break;
            case pkgWX:
                new WXClient(this).onNotificationChanged(event);
                break;
        }
    }

    /**
     * 在检测到空通知体的时候 enable flag
     * 在之后的10次 onContentChange 都去检查微信登录
     */
    private class WXAutoLogin {
        private int time = 0;

        public void flagEnable() {
            time = 10;
        }

        private void autoLoginWX() {
            while (time > 0) {
                time--;
                Log.v(TAG, "autoLoginWX");
                if (root.getChildCount() != 1)
                    return;
                AccessibilityNodeInfo node = root.getChild(0);
                if (node.getChildCount() != 5)
                    return;
                //不直接判断字符串是因为多语言适应
                AccessibilityNodeInfo loginBtn = node.getChild(3);
                if (!loginBtn.isClickable())
                    return;
                if (!node.getChild(0).isClickable())
                    return;
                if (!node.getChild(4).isClickable())
                    return;
                loginBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                Log.w(TAG, "autoLoginWX: Perform Click");
                time = 0;
            }
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        autoLogin = new WXAutoLogin();
    }

    @Override
    public void onInterrupt() {

    }
}
