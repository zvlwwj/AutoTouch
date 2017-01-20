#ifndef CVIRTUALINPUT_H
#define CVIRTUALINPUT_H

#include <sys/types.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <stdarg.h>
#include <unistd.h>
#include <iostream>
#include "protocol.h"

// 功能1, 代表android的Home键 
#define FUN_HOME        "fun_home"

// 功能2, 代表android的Back键 
#define FUN_BACK        "fun_back"

// 功能3, 代表android的Menu键 
#define FUN_MENU        "fun_menu"

// 功能4, 代表android的通知栏键 
#define FUN_NOTIFY      "fun_notify"

// 功能5, 代表android的电源键 
#define FUN_POWER       "fun_power"

// 功能6, 代表android的音量+
#define FUN_VOLUMEUP    "fun_volumeup"

// 功能7, 代表android的音量-
#define FUN_VOLUMEDOWN "fun_volumedown"

// 功能7, 代表android的下拉
#define FUN_PULLDOWN    "fun_pulldown"

// 功能7, 代表android的上拉
#define FUN_PULLUP      "fun_pullup"

struct RawTouchEvent {
    int x;
    int y;
    int isdown;
    int index;
};


/**
 * @brief 虚拟输入器(Android版)
 * @remark 用来模拟键盘鼠标的输入
 */
class CVirtualInput 
{
public:
	CVirtualInput(void);
	~CVirtualInput(void);

public:
	/*
	 * @brief 初始化
	 */
    bool Init();

    void SetScreenDimension(int width, int height, int offset_x=0, int offset_y=0, int orientation=0);

    void SetRotation(int orientation);
	/*
	 * @brief 是否可用
	 */
	bool IsValid() const {
        return m_uinput_fd > 0;
    }

    bool IsOpen() const {
        return m_uinput_fd > 0;
    }

	/*
	 * @brief 鼠标移动
	 */
	void MouseMove(int x, int y);

	/*
	 * @brief 鼠标左键按下
	 */
	void MouseLButtonDown(int x, int y);

	/*
	 * @brief 鼠标左键抬起
	 */
	void MouseLButtonUp(int x, int y);

	/*
	 * @brief 鼠标左键点击
	 */
	void MouseLButtonClick(void);

	/*
	 * @brief 鼠标右键按下
	 */
	void MouseRButtonDown(void);

	/*
	 * @brief 鼠标右键抬起
	 */
	void MouseRButtonUp(void);

	/*
	 * @brief 鼠标右键点击
	 */
	void MouseRButtonClick(void);

	/*
	 * @brief 鼠标中键按下
	 */
	void MouseMButtonDown(void);

	/*
	 * @brief 鼠标中键抬起
	 */
	void MouseMButtonUp(void);

	/*
	 * @brief 鼠标中键点击
	 */
	void MouseMButtonClick(void);

	/*
	 * @brief 鼠标滚轮上滑
	 */
	void MouseWheelUp(void);

	/*
	 * @brief 鼠标滚轮下滑
	 */
	void MouseWheelDown(void);

	/*
	 * @brief 键盘按键按下
	 * @return 是否成功，如果失败则是键盘的按键值不支持
	 */
	bool KeyBoardDown(int keycode);

	/*
	 * @brief 键盘按键抬起
	 * @return 是否成功，如果失败则是键盘的按键值不支持
	 */
	bool KeyBoardUp(int keycode);

	/*
	 * @brief 键盘按键点击
	 * @return 是否成功，如果失败则是键盘的按键值不支持
	 */
	bool KeyBoardClick(int keycode);

	/*
	 * @brief 功能键按下
	 * @return 是否成功，如果失败则是参数错误
	 */
    bool FunctionKeyDown(const char* funkey);

    /*
	 * @brief 功能键抬起
	 * @return 是否成功，如果失败则是参数错误
	 */
    bool FunctionKeyUp(const char* funkey);

    int SingleTouch(int trackid, int x, int y, int isdown, int srcimage_w, int srcimage_h);

    int MultiTouch(TOUCH_RAWDATA data);

    bool HomePageKeyDownUp(bool is_down);

private:
    /*
     * 发送事件, 到uinput设备文件
     */
    int SendEventToUinput(int evtype, int evcode, int evvalue);

    /*
     * 把像素点转换成uinput对应的点
     */
    bool ConvertToUinputDot(int x, int y, int& outX, int& outY);

    /*
     * 把像素点转换成uinput对应的点
     */
    bool ConvertToUinputDot2(int x, int y, int w, int h, int& outX, int& outY);

    /*
     * 发送事件, 到对应的设备文件
     */
    int SendEventToKernel(int device_fd, int evtype, int evcode, int evvalue);

    /*
     * 释放uinput设备文件
     */
    int RleaseUninput();

    /*
     * 功能键处理, is_down == true表示按下键
     */
    bool FunctionKeyDownUp(const char * funkey, bool is_down);

    /*
     * HOME键的处理
     */
    //bool HomeKeyDownUp(bool is_down);

    /**
     * 查找触摸屏驱动设备文件
     * return 空字符串, 查找失败; 非空，查找成功
     */
    std::string FindTouchDeviceFile();

    /**
     * 判断设备文件是否是触摸驱动文件
     * return true, 是触摸驱动文件
     */
    static bool IsTouchDevice(const char* device, bool& is_keyhome);

    int SingleTouchDown(int index, int x, int y, int isdown, int srcimage_w, int srcimage_h);

    int SingleTouchUp(int index, int x, int y, int isdown, int srcimage_w, int srcimage_h);

    int SignleTouchMove(int index, int x, int y, int isdown, int srcimage_w, int srcimage_h);

    void CalcTouchData(TOUCH_RAWDATA data);

    int MultiTouchImpl();

    int write_event(int evtype, int evcode, int evvalue);

public:
    bool simulate_pulldown();

    bool simulate_pullup();

private:
	int m_uinput_fd;
    //int m_devinput_fd;

	// 标示, Home键是否是通过发KEY_HOME到设备文件实现的.
	bool m_home_is_keyhome;
    int m_screen_width;     //!< 被控端录像或录屏的宽度，此值可与被控端屏幕的宽度不同
	int m_screen_height;    //!< 被控端录像或录屏的高度，此值可与被控端屏幕的高度不同
    int m_screen_rotation;  //!< 屏幕旋转方向
    int m_track_id;         //!< 事件的id

    bool m_is_mousedowned;  //!< 标示是否产生了mouse down事件 
    bool m_is_touchdown;    //!< 标示是否产生了touch down事件
    bool m_first_mtouchdown; //!< 标示是否产生了multi touch down事件

    struct MTouchState{
        uint32_t enabled;
        uint32_t trackid;
        uint32_t devicex;
        uint32_t devicey;
    };

    MTouchState m_contacts[2];
};

#endif