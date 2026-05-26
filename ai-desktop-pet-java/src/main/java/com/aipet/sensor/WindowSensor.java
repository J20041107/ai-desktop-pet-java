package com.aipet.sensor;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;

public class WindowSensor {
    public String getForegroundWindowTitle() {
        try {
            HWND hwnd = User32.INSTANCE.GetForegroundWindow();
            if (hwnd == null) {
                return "未知窗口";
            }
            char[] buffer = new char[512];
            User32.INSTANCE.GetWindowText(hwnd, buffer, 512);
            String title = new String(buffer).trim();
            return title.isBlank() ? "无标题窗口" : title;
        } catch (Exception e) {
            return "无法读取窗口标题";
        }
    }
}
