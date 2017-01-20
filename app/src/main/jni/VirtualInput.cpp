#include <iostream>
#include <cassert>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <math.h>
#include <string>
#include <dirent.h>

#include "VirtualInput.h"
#include "logger/phlogger.h"
#include "input.h"
#include "uinput.h"
#include "xsleep.h"

#define ABS_X_MIN       0
#define ABS_X_MAX       4096
#define ABS_Y_MIN       0
#define ABS_Y_MAX       4096

#define SCALE_SIZE_WIDTH    (4096.0)
#define SCALE_SIZE_HEIGHT   (4096.0)
#define SCALE_SIZE          (4096.0)    //目前默认宽度和高度都采用4096

#define FIND_TOUCH_SCREEN_CMD "getevent -il %s"
#define DEVICE_DIR "/dev/input"

/************************************************************************/
/*                                                                      */
/************************************************************************/

bool transformUinputPoint(int x, int y, int width, int height, int& outX, int& outY)
{
	double retx = x;
	retx = retx * SCALE_SIZE / width + ABS_X_MIN + 0.5;
	outX = (int)retx;

	double rety = y;
	rety = rety * SCALE_SIZE / height + ABS_Y_MIN + 0.5;
	outY = (int)rety;

	return true;
}


/*!
 * @brief   全部转成成rotationg==0时的坐标
 * @param 	x	 
 * @param 	y	 
 * @param 	width	 rotationg==0时的宽
 * @param 	height	 rotationg==0时的高
 * @param 	rotation	 
 * @param 	outx	 
 * @param 	outy	 
 * @return  void
 */
inline 
void transformTouchCoordinates(int x, int y,int width,int height, int rotation, int& outx, int& outy)
{
    float scale = SCALE_SIZE;//4096.0;
    int old_x=x,old_y=y;

    int tempx, tempy;
    if (rotation == 0) {
        tempx = x;
        tempy = y;
    } else if (rotation == 90) {
        tempx = width - y;
        tempy = x;
    } else if (rotation == 180) {
        tempx = width - x;
        tempy = height - y;
    } else if (rotation == 270) {
        tempx = y;
        tempy = height - x;
    }

    transformUinputPoint(tempx, tempy, width, height, outx, outy);

    //fprintf(stderr, "point(%d,%d) --> new point(%d,%d), width: %d, height: %d, rotation: %d", old_x, old_y, x, y, width, height, rotation);
    //WriteLog(LOG_INFO, "point(%d,%d) --> new point(%d,%d), width: %d, height: %d, rotation: %d", old_x, old_y, x, y, width, height, rotation);
}


/************************************************************************/
/*                                                                      */
/************************************************************************/
CVirtualInput::CVirtualInput(void)
:m_uinput_fd(0)
//,m_devinput_fd(0)
,m_home_is_keyhome(false)
,m_screen_width(0)
,m_screen_height(0)
,m_track_id(0)
,m_is_mousedowned(false)
,m_is_touchdown(false)
,m_screen_rotation(0)
{
    memset(&m_contacts, 0x00, sizeof(m_contacts));
}

CVirtualInput::~CVirtualInput(void)
{
    RleaseUninput();
    //if (m_devinput_fd > 0) {
    //    close(m_devinput_fd);
    //    m_devinput_fd = 0;
    //}
    if (m_uinput_fd > 0) {
        close(m_uinput_fd);
        m_uinput_fd = 0;
    }
}

bool CVirtualInput::Init()
{
    int original_errno = 0;
    int uinput_fd = -1;
    //fprintf(stderr, "[debug] call %s at [%s:%d]\n", __FUNCTION__, __FILE__, __LINE__);
    if (m_uinput_fd > 0) return true;

    struct uinput_user_dev dev;
    uinput_fd = open("/dev/uinput", O_WRONLY | O_NDELAY);
    if (uinput_fd <= 0) {
        perror("Error opening the uinput device\n");
        return false;
    }

	memset(&dev, 0, sizeof(dev));
    //采用之前版本的设置值
	dev.id.bustype = BUS_I2C; //0x18
    dev.id.product = 0;
    dev.id.vendor  = 0;
	dev.id.version = 0;
	strcpy(dev.name, "oray virtual IME");

    //minor tweak to support ABSolute events
    dev.absmin[ABS_X] = ABS_X_MIN; 
    dev.absmax[ABS_X] = ABS_X_MAX;

    dev.absmin[ABS_Y] = ABS_Y_MIN; 
    dev.absmax[ABS_Y] = ABS_Y_MAX;

	int ret = write(uinput_fd, &dev, sizeof(dev));
	if(ret < sizeof(dev)) {
      fprintf(stderr, "write event failed , %s\n", "dsfds");
      goto err;
    }
	
	// key event
	if (ioctl(uinput_fd, UI_SET_EVBIT, EV_KEY) == -1)
        goto err;

	if (ioctl(uinput_fd, UI_SET_KEYBIT, BTN_MOUSE) == -1)
        goto err;
    if (ioctl(uinput_fd, UI_SET_KEYBIT, BTN_TOUCH) == -1)
        goto err;

    if (ioctl(uinput_fd, UI_SET_KEYBIT, BTN_LEFT) == -1)
        goto err;
    if (ioctl(uinput_fd, UI_SET_KEYBIT, BTN_MIDDLE) == -1)
        goto err;
    if (ioctl(uinput_fd, UI_SET_KEYBIT, BTN_RIGHT) == -1)
        goto err;
    if (ioctl(uinput_fd, UI_SET_KEYBIT, BTN_FORWARD) == -1)
        goto err;
    if (ioctl(uinput_fd, UI_SET_KEYBIT, BTN_BACK) == -1)
        goto err;

    if (ioctl(uinput_fd, UI_SET_KEYBIT, BTN_TOOL_FINGER) == -1)
        goto err;

    //android 被控端不屏蔽键盘，使用手机端的键盘来进行输入
    /* Configure device to handle all keys, see linux/input.h. */
    //for (i = 0; i < KEY_MAX; i++) {
    //    ioctl(m_uinput_fd, UI_SET_KEYBIT, i);
    //}

    // function key (android special)
    if (ioctl(uinput_fd, UI_SET_KEYBIT, KEY_HOME) == -1)
        goto err;
    if (ioctl(uinput_fd, UI_SET_KEYBIT, KEY_HOMEPAGE) == -1)
        goto err;
    if (ioctl(uinput_fd, UI_SET_KEYBIT, KEY_BACK) == -1)
        goto err;
    if (ioctl(uinput_fd, UI_SET_KEYBIT, KEY_MENU) == -1)
        goto err;
    if (ioctl(uinput_fd, UI_SET_KEYBIT, KEY_POWER) == -1)
        goto err;
    if (ioctl(uinput_fd, UI_SET_KEYBIT, KEY_VOLUMEUP) == -1)
        goto err;
    if (ioctl(uinput_fd, UI_SET_KEYBIT, KEY_VOLUMEDOWN) == -1)
        goto err;
	
	// abs event
    if (ioctl(uinput_fd, UI_SET_EVBIT, EV_ABS) == -1)
        goto err;
    if (ioctl(uinput_fd, UI_SET_ABSBIT, ABS_X) == -1)
        goto err;
    if (ioctl(uinput_fd, UI_SET_ABSBIT, ABS_Y) == -1)
        goto err;

	// syn and msc_scan event
    if (ioctl(uinput_fd, UI_SET_EVBIT, EV_SYN) == -1)
        goto err;

	// set input props
    if (ioctl(uinput_fd, _IOW(UINPUT_IOCTL_BASE, 110, int), 1) == -1)
        goto err;
	
	/* Register the device with linux's input subsystem */
	if (ioctl(uinput_fd, UI_DEV_CREATE, 0) == -1)
        goto err; 

    m_uinput_fd = uinput_fd;
    return m_uinput_fd;

err:
    original_errno = errno;
    close(uinput_fd);
    errno = original_errno;

	return -1;
}

void CVirtualInput::SetScreenDimension(int width, int height, int offset_x, int offset_y, int orientation)
{
    //save screen width/height
    m_screen_width    = width;
    m_screen_height   = height;
    m_screen_rotation = orientation;
}


void CVirtualInput::SetRotation(int orientation)
{
    m_screen_rotation = orientation;
}

void CVirtualInput::MouseMove(int x, int y)
{
	assert(IsValid());
    if (m_is_mousedowned) {
        int uinput_x, uinput_y;
        ConvertToUinputDot(x, y, uinput_x, uinput_y);
        //WriteLog(LOG_INFO, "[debug] MouseMove (%d,%d -- %d,%d) >>>>>> >>>>>>", x, y, uinput_x, uinput_y);

    	SendEventToUinput(EV_ABS, ABS_X, uinput_x);
    	SendEventToUinput(EV_ABS, ABS_Y, uinput_y);
        SendEventToUinput(EV_SYN, SYN_REPORT, 0x0);
    }
}

void CVirtualInput::MouseLButtonDown(int x, int y)
{
    assert(IsValid());
    int uinput_x, uinput_y;
    ConvertToUinputDot(x, y, uinput_x, uinput_y);

    m_is_mousedowned = true;
    //WriteLog(LOG_INFO, "[debug] MouseLButtonDown (%d,%d -- %d,%d) !!!!!! !!!!!!", x, y, uinput_x, uinput_y);

    SendEventToUinput(EV_KEY, BTN_TOUCH, 0x1);
    SendEventToUinput(EV_KEY, BTN_TOOL_FINGER, 0x1);
    SendEventToUinput(EV_ABS, ABS_X, uinput_x);
    SendEventToUinput(EV_ABS, ABS_Y, uinput_y);
    SendEventToUinput(EV_SYN, SYN_REPORT, 0x0);
}

void CVirtualInput::MouseLButtonUp(int x, int y)
{
    assert(IsValid());
    m_is_mousedowned = false;
    //WriteLog(LOG_INFO, "[debug] MouseLButtonUp ^^^^^^ ^^^^^^");

	SendEventToUinput(EV_KEY, BTN_TOUCH, 0x0);
	SendEventToUinput(EV_KEY, BTN_TOOL_FINGER, 0x0);
	SendEventToUinput(EV_SYN, SYN_REPORT, 0x0);
}

void CVirtualInput::MouseLButtonClick(void)
{
	assert(IsValid());
}

void CVirtualInput::MouseRButtonDown(void)
{
	assert(IsValid());
}

void CVirtualInput::MouseRButtonUp(void)
{
	assert(IsValid());
}

void CVirtualInput::MouseRButtonClick(void)
{
	assert(IsValid());
}

void CVirtualInput::MouseMButtonDown(void)
{
	assert(IsValid());
}

void CVirtualInput::MouseMButtonUp(void)
{
	assert(IsValid());
}

void CVirtualInput::MouseMButtonClick(void)
{
	assert(IsValid());
}

void CVirtualInput::MouseWheelUp(void)
{
	assert(IsValid());
}

void CVirtualInput::MouseWheelDown(void)
{
	assert(IsValid());
}

bool CVirtualInput::KeyBoardDown(int keycode)
{
	assert(IsValid());

    SendEventToUinput(EV_KEY, keycode, 0x1);
    SendEventToUinput(EV_SYN, SYN_REPORT, 0x0);
	return true;
}

bool CVirtualInput::KeyBoardUp(int keycode)
{
	assert(IsValid());
    SendEventToUinput(EV_KEY, keycode, 0x0);
    SendEventToUinput(EV_SYN, SYN_REPORT, 0x0);
	return true;
}

bool CVirtualInput::KeyBoardClick(int keycode)
{
    KeyBoardDown(keycode);
    KeyBoardUp(keycode);
	return true;
}

int CVirtualInput::SendEventToUinput(int evtype, int evcode, int evvalue)
{
    return SendEventToKernel(m_uinput_fd, evtype, evcode, evvalue);
}

bool CVirtualInput::ConvertToUinputDot(int x, int y, int& outX, int& outY)
{
    if (m_screen_rotation ==0 || m_screen_rotation == 180)
        transformTouchCoordinates(x, y, m_screen_width, m_screen_height, m_screen_rotation, outX, outY);
    else
        transformTouchCoordinates(x, y, m_screen_height, m_screen_width, m_screen_rotation, outX, outY);
	return true;
}

bool CVirtualInput::ConvertToUinputDot2(int x, int y, int width, int height, int& outX, int& outY)
{
    if (m_screen_rotation ==0 || m_screen_rotation == 180)
        transformTouchCoordinates(x, y, width, height, m_screen_rotation, outX, outY);
    else
        transformTouchCoordinates(x, y, height, width, m_screen_rotation, outX, outY);
    return true;
}

int CVirtualInput::RleaseUninput()
{
    if ( m_uinput_fd <= 0) {
        return -1;
    }
    
	// close 
	ioctl(m_uinput_fd, UI_DEV_DESTROY);
	close(m_uinput_fd);
	m_uinput_fd = 0;

	return 0;
}

bool CVirtualInput::FunctionKeyDown(const char* fun_key)
{
	return FunctionKeyDownUp(fun_key, true);
}

bool CVirtualInput::FunctionKeyUp(const char* fun_key)
{
    return FunctionKeyDownUp(fun_key, false);
}

bool CVirtualInput::FunctionKeyDownUp(const char* fun_key, bool is_down)
{
	assert(IsValid());

    int keycode = -1;
    if (0 == strcmp(FUN_HOME, fun_key)) {
        //return HomeKeyDownUp(is_down);
        keycode = KEY_HOME;
    } else if (0 == strcmp(FUN_BACK, fun_key)) {
        keycode = KEY_BACK;
    } else if (0 == strcmp(FUN_MENU, fun_key)) {
        keycode = KEY_MENU;
    } else if (0 == strcmp(FUN_NOTIFY, fun_key)) {
		// 通知栏，暂时无法实现 
        return simulate_pulldown();
    } else if (0 == strcmp(FUN_POWER, fun_key)) {
        keycode = KEY_POWER;
    } else if (0 == strcmp(FUN_VOLUMEUP, fun_key)) {
        keycode = KEY_VOLUMEUP;
    } else if (0 == strcmp(FUN_VOLUMEDOWN, fun_key)) {
        keycode = KEY_VOLUMEDOWN;
    } else if (0 == strcmp(FUN_PULLDOWN, fun_key)) {
        return simulate_pulldown();
    } else if (0 == strcmp(FUN_PULLUP, fun_key)) {
        return simulate_pullup();
    } else {
        // view blew
    }
    
    if (-1 != keycode) {
        int ev_value = is_down ? 0x1 : 0x0;
        SendEventToUinput(EV_KEY, keycode, ev_value);
        SendEventToUinput(EV_SYN, SYN_REPORT, 0x0);
        return true;
    } else {
        return false;
    }
}

//bool CVirtualInput::HomeKeyDownUp(bool is_down)
//{
//	int fd = m_devinput_fd;
//	if (fd <= 0) {
//        std::string device = FindTouchDeviceFile();
//        fd = open(device.c_str(), O_RDWR);
//        if(fd <= 0) {
//            fprintf(stderr, "could not open %s\n", device.c_str());
//        }
//        printf("find touch device is %s\n", device.c_str());
//	}
//
//    if (fd > 0) 
//    {
//        m_devinput_fd = fd;
//        //WriteLog(LOG_INFO, "home key: %s", m_home_is_keyhome?"KEY_HOME":"KEY_HOMEPAGE");
//		int home_key = m_home_is_keyhome ? KEY_HOME : KEY_HOMEPAGE;
//        int ev_value = is_down ? 0x1 : 0x0;
//        SendEventToUinput(EV_KEY, home_key, ev_value);
//        SendEventToUinput(EV_SYN, SYN_REPORT, 0x0);
//        return true;
//    }
//    return false;
//}

bool CVirtualInput::HomePageKeyDownUp(bool is_down)
{
	int home_key =  KEY_HOMEPAGE;
    int ev_value = is_down ? 0x1 : 0x0;
    SendEventToUinput(EV_KEY, home_key, ev_value);
    SendEventToUinput(EV_SYN, SYN_REPORT, 0x0);
    return true;
}

std::string CVirtualInput::FindTouchDeviceFile()
{
	const char* dirname = DEVICE_DIR;
    char devname[1024];
	memset(devname, 0, sizeof(devname));
    char *filename;
    DIR *dir;
    struct dirent *de;
    dir = opendir(dirname);
    if(dir == NULL)
    {
    	printf("open dir(%s) fail\n", dirname);
        return "";
    }

    strcpy(devname, dirname);
    filename = devname + strlen(devname);
    *filename++ = '/';
    std::string device_file;
    while((de = readdir(dir))) {
        if(de->d_name[0] == '.' &&
           (de->d_name[1] == '\0' ||
            (de->d_name[1] == '.' && de->d_name[2] == '\0')))
            continue;
        strcpy(filename, de->d_name);
        bool istouch = IsTouchDevice(devname, m_home_is_keyhome);
        if (istouch) {
        	device_file = devname;
			break;
		}
    }
    closedir(dir);
    return device_file;
}

bool CVirtualInput::IsTouchDevice(const char* device, bool& is_keyhome)
{
	//建立管道 
	char buffer[1024];
	memset(buffer, 0, sizeof(buffer));
	sprintf(buffer, FIND_TOUCH_SCREEN_CMD, device);

	FILE *pp = popen(buffer, "r");
	if (!pp) {
		std::cout << "exec getevent fail" << std::endl;
		return false;
	}
	int line_count = 0;
	while (fgets(buffer, sizeof(buffer), pp) != NULL) {
		if ( NULL != strstr(buffer, " KEY_HOME ")) {
			line_count ++;
			is_keyhome = true;
			break;
		} else if (NULL != strstr(buffer, " KEY_HOMEPAGE ")) {
			line_count ++;
			is_keyhome = false;
			break;
		} else {
			continue;
		}
	}
	return line_count > 0;
}

int CVirtualInput::SendEventToKernel(int device_fd, int evtype, int evcode, int evvalue)
{
    if(device_fd <= 0) {
        fprintf(stderr, "could not open dev file");
        return 1;
    }

    struct input_event event;
    memset(&event, 0, sizeof(event));
    event.type = evtype;
    event.code = evcode;
    event.value = evvalue;
    int ret = write(device_fd, &event, sizeof(event));
    if(ret < sizeof(event)) {
        fprintf(stderr, "write event failed\n");
        return -1;
    }
    return 0;
}

int CVirtualInput::SingleTouch(int index, int x, int y, int isdown, int srcimage_w, int srcimage_h)
{
    //fprintf(stderr, "[debug] call %s at [%s:%d]\n", __FUNCTION__, __FILE__, __LINE__);
    if (m_uinput_fd <= 0) return -1;

    //fprintf(stderr, "[debug] call %s at [%s:%d]\n", __FUNCTION__, __FILE__, __LINE__);
    if (isdown == 1) {
        return SingleTouchDown(index, x, y, isdown, srcimage_w, srcimage_h);
    }
    else {
        return SingleTouchUp(index, x, y, isdown, srcimage_w, srcimage_h);
    }

    return 0;
}

int CVirtualInput::SingleTouchDown(int index, int x, int y, int isdown, int srcimage_w, int srcimage_h)
{
    if (m_uinput_fd <= 0) return -1;

    if (!m_is_touchdown) {
        int uinput_x, uinput_y;
        ConvertToUinputDot2(x, y, srcimage_w, srcimage_h, uinput_x, uinput_y);

        //fprintf(stderr, "[debug] SingleTouchDown (%d,%d -- %d,%d) !!!!!! !!!!!!\n", x, y, uinput_x, uinput_y);
        //WriteLog(LOG_INFO, "[debug] SingleTouchDown (%d,%d -- %d,%d) !!!!!! !!!!!!", x, y, uinput_x, uinput_y);
        m_is_touchdown = true;

        SendEventToUinput(EV_KEY, BTN_TOUCH, 0x01);
        SendEventToUinput(EV_KEY, BTN_TOOL_FINGER, 0x1);
        SendEventToUinput(EV_ABS, ABS_X, uinput_x);
        SendEventToUinput(EV_ABS, ABS_Y, uinput_y);
        SendEventToUinput(EV_SYN, SYN_REPORT, 0);

    } else {
        SignleTouchMove(index, x, y, isdown, srcimage_w, srcimage_h);
    }
    return 0;
}

int CVirtualInput::SingleTouchUp(int index, int x, int y, int isdown, int srcimage_w, int srcimage_h)
{
    if (m_uinput_fd <= 0) return -1;
    if (!m_is_touchdown) return -1;

    int uinput_x, uinput_y;
    ConvertToUinputDot2(x, y, srcimage_w, srcimage_h, uinput_x, uinput_y);

    //fprintf(stderr, "[debug] SingleTouchUp (%d,%d -- %d,%d) ^^^^^^ ^^^^^^\n", x, y, uinput_x, uinput_y);
    //WriteLog(LOG_INFO, "[debug] SingleTouchUp (%d,%d -- %d,%d) ^^^^^^ ^^^^^^", x, y, uinput_x, uinput_y);
    m_is_touchdown = false;

    SendEventToUinput(EV_KEY, BTN_TOUCH, 0x00);
    SendEventToUinput(EV_KEY, BTN_TOOL_FINGER, 0);
    SendEventToUinput(EV_SYN, SYN_REPORT, 0x00);

    return 0;
}

int CVirtualInput::SignleTouchMove(int index, int x, int y, int isdown, int srcimage_w, int srcimage_h)
{
	assert(IsValid());
    if (m_is_touchdown) {
        int uinput_x, uinput_y;
        ConvertToUinputDot2(x, y, srcimage_w, srcimage_h, uinput_x, uinput_y);

        //fprintf(stderr, "[debug] SignleTouchMove (%d,%d -- %d,%d) >>>>>> >>>>>>\n", x, y, uinput_x, uinput_y);
        //WriteLog(LOG_INFO, "[debug] SignleTouchMove (%d,%d -- %d,%d) >>>>>> >>>>>>", x, y, uinput_x, uinput_y);

    	SendEventToUinput(EV_ABS, ABS_X, uinput_x);
    	SendEventToUinput(EV_ABS, ABS_Y, uinput_y);
        SendEventToUinput(EV_SYN, SYN_REPORT, 0);
    }
    return 0;
}

int CVirtualInput::MultiTouch(TOUCH_RAWDATA data) 
{
    //if (data.count == 2) {
    //    fprintf(stderr, "[debug] point1(%d,%d), point2(%d,%d), rect(%d,%d --%d,%d)\n", 
    //        data.item[0].x, data.item[0].y, data.item[1].x, data.item[1].y,
    //        data.item[0].srcimage_w, data.item[0].srcimage_h, data.item[1].srcimage_w, data.item[1].srcimage_h);
    //}
#if 0
    if (m_uinput_fd <= 0) return -1;

    CalcTouchData(data);
    MultiTouchImpl();

    if (data.count == 1) 
    {
        bool bfound = false;
        for (int i=0; i<2; i++) {
            if (m_contacts[i].enabled != 0) {
                bfound = true;
            }
        }
        if (!bfound)
            m_first_mtouchdown = false;
    }
#endif
    return 0;
}

void CVirtualInput::CalcTouchData(TOUCH_RAWDATA data)
{
    if (!m_first_mtouchdown) {
        for (int i=0; i<2; i++) {
            int uinput_x, uinput_y;
            ConvertToUinputDot2(data.item[i].x, data.item[i].y, data.item[i].srcimage_w, data.item[i].srcimage_h, uinput_x, uinput_y);

            m_contacts[i].enabled = 1;
            m_contacts[i].trackid = data.item[i].index;
            m_contacts[i].devicex = uinput_x;
            m_contacts[i].devicey = uinput_y;
            //fprintf(stderr, "[debug] id=%d, point(%d,%d)\n", m_contacts[i].trackid, m_contacts[i].devicex, m_contacts[i].devicey);
        }
        m_first_mtouchdown = true;
    }
    else {
        if (data.count == 2) {
            for (int i=0; i<2; i++) {
                for (int j=0; j<2; j++) {
                    if (data.item[j].index == m_contacts[i].trackid) {
                        int uinput_x, uinput_y;
                        ConvertToUinputDot2(data.item[j].x, data.item[j].y, data.item[j].srcimage_w, data.item[j].srcimage_h, uinput_x, uinput_y);

                        m_contacts[i].enabled = data.item[j].isdown?2:3;
                        m_contacts[i].trackid = data.item[j].index;
                        m_contacts[i].devicex = uinput_x;
                        m_contacts[i].devicey = uinput_y;

                        //fprintf(stderr, "[debug] 2 enabled=%d, point(%d,%d)\n", m_contacts[i].enabled, m_contacts[i].devicex, m_contacts[i].devicey);
                    }
                }
            }
        } else {
            for (int i=0; i<2; i++) {
                if (data.item[0].index == m_contacts[i].trackid) {
                    int uinput_x, uinput_y;
                    ConvertToUinputDot2(data.item[0].x, data.item[0].y, data.item[0].srcimage_w, data.item[0].srcimage_h, uinput_x, uinput_y);

                    m_contacts[i].enabled = data.item[0].isdown?2:3;
                    m_contacts[i].trackid = data.item[0].index;
                    m_contacts[i].devicex = uinput_x;
                    m_contacts[i].devicey = uinput_y;
                    //fprintf(stderr, "[debug] 1 enabled=%d, point(%d,%d)\n", m_contacts[i].enabled, m_contacts[i].devicex, m_contacts[i].devicey);
                }
            }
        }
    }
}

int CVirtualInput::MultiTouchImpl()
{
  int found_any = 0;
#if 0
  for (int i = 0; i < 2; i++)
  {
    switch (m_contacts[i].enabled)
    {
      case 1: // DOWN
        //fprintf(stderr, "[debug] multi touch down, point(%d,%d)\n", m_contacts[i].devicex, m_contacts[i].devicey);
        found_any = 1;

        // report down
        write_event(EV_ABS, ABS_MT_TRACKING_ID, m_contacts[i].trackid);
        write_event(EV_KEY, BTN_TOUCH, 1);
        write_event(EV_KEY, BTN_TOOL_FINGER, 1);
        // 
        write_event(EV_ABS, ABS_MT_TOUCH_MAJOR, 0x00000006);
        write_event(EV_ABS, ABS_MT_WIDTH_MAJOR, 0x00000004);
        // report point
        write_event(EV_ABS, ABS_MT_POSITION_X, m_contacts[i].devicex);
        write_event(EV_ABS, ABS_MT_POSITION_Y, m_contacts[i].devicey);
        // report one point
        write_event(EV_SYN, SYN_MT_REPORT, 0);
        m_contacts[i].enabled = 2;
        break;
      case 2: // MOVED
        //fprintf(stderr, "[debug] multi touch move, point(%d,%d)\n", m_contacts[i].devicex, m_contacts[i].devicey);
        found_any = 1;
        //
        write_event(EV_ABS, ABS_MT_TRACKING_ID, m_contacts[i].trackid);
        //
        write_event(EV_ABS, ABS_MT_TOUCH_MAJOR, 0x00000006);
        write_event(EV_ABS, ABS_MT_WIDTH_MAJOR, 0x00000004);
        //
        write_event(EV_ABS, ABS_MT_POSITION_X, m_contacts[i].devicex);
        write_event(EV_ABS, ABS_MT_POSITION_Y, m_contacts[i].devicey);
        //
        write_event(EV_SYN, SYN_MT_REPORT, 0);
        break;
      case 3: // UP
        //fprintf(stderr, "[debug] multi touch up, point(%d,%d)\n", m_contacts[i].devicex, m_contacts[i].devicey);
        found_any = 1;

        write_event(EV_ABS, ABS_MT_TRACKING_ID, m_contacts[i].trackid);
        write_event(EV_KEY, BTN_TOUCH, 0);
        write_event(EV_KEY, BTN_TOOL_FINGER, 0);
        write_event(EV_SYN, SYN_MT_REPORT, 0);
        //
        m_contacts[i].enabled = 0;
        break;
      default:
        fprintf(stderr, "[debug] unuse point(%d,%d)\n", m_contacts[i].devicex, m_contacts[i].devicey);
        break;
    }
  }

  //if (found_any)
    write_event(EV_SYN, SYN_REPORT, 0);
#endif

  return 1;
}

int CVirtualInput::write_event(int evtype, int evcode, int evvalue)
{
    return SendEventToKernel(m_uinput_fd, evtype, evcode, evvalue);
}

bool CVirtualInput::simulate_pulldown()
{
    int dx = 200;
    int dy = 20;
    
    for (int i=0; i<10; i++) {  
        SingleTouch(0, dx, dy+20*i, true, m_screen_width, m_screen_height);  
        xsleep(1);  
    }  
    SingleTouchUp(0, dx, dy, false, m_screen_width, m_screen_height);
    return true;
}

bool CVirtualInput::simulate_pullup()
{
    int dx = 200;
    int dy = SCALE_SIZE-20;
    
    for (int i=0; i<10; i++) {  
        SingleTouch(0, dx, dy-20*i, true, m_screen_width, m_screen_height);  
        xsleep(1);  
    }  
    SingleTouchUp(0, dx, dy, false, m_screen_width, m_screen_height);
    return true;
}
