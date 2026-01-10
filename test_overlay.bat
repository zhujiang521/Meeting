@echo off
echo ====================================
echo 悬浮窗动效测试脚本
echo ====================================
echo.

echo [1] 启动应用...
adb shell am start -n com.lenovo.levoice.caption/.MainActivity
timeout /t 2 /nobreak >nul

echo [2] 启动悬浮窗服务...
adb shell am startservice -n com.lenovo.levoice.caption/.OverlayService
timeout /t 2 /nobreak >nul

echo.
echo [3] 测试淡入淡出动效 (亮橙色)...
adb shell am broadcast -a com.zui.action.SHOW_KINETIC -n com.lenovo.levoice.caption/.KineticBroadcastReceiver --es animation_type "fade" --el duration 3000 --ei background_color -301989666
echo 等待3秒...
timeout /t 4 /nobreak >nul

echo [4] 测试从顶部滑入动效 (亮蓝色)...
adb shell am broadcast -a com.zui.action.SHOW_KINETIC -n com.lenovo.levoice.caption/.KineticBroadcastReceiver --es animation_type "slide" --el duration 3000 --ei background_color -872349952
echo 等待3秒...
timeout /t 4 /nobreak >nul

echo [5] 测试波纹扩散动效 (亮绿色)...
adb shell am broadcast -a com.zui.action.SHOW_KINETIC -n com.lenovo.levoice.caption/.KineticBroadcastReceiver --es animation_type "ripple" --el duration 3000 --ei background_color -855572224
echo 等待3秒...
timeout /t 4 /nobreak >nul

echo.
echo ====================================
echo 测试完成！
echo ====================================
pause