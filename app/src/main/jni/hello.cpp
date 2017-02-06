#include <android/log.h>

int main (int argc, const char* argv[])
{
    __android_log_print(ANDROID_LOG_INFO, "JNIMsg", "helloword");
    return 0;
}