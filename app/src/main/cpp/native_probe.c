#include <jni.h>

#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <string.h>
#include <sys/types.h>
#include <unistd.h>

static void append_line(char *output, size_t output_size, const char *name,
                        const char *value) {
    size_t used = strlen(output);
    if (used >= output_size) {
        return;
    }
    snprintf(output + used, output_size - used, "%s=%s\n", name, value);
}

static void append_access(char *output, size_t output_size, const char *name,
                          const char *path, int flags) {
    errno = 0;
    int fd = open(path, flags | O_CLOEXEC);
    int saved_errno = errno;
    if (fd >= 0) {
        close(fd);
    }

    size_t used = strlen(output);
    if (used >= output_size) {
        return;
    }
    snprintf(output + used, output_size - used, "%s=%s errno=%d\n", name,
             fd >= 0 ? "ok" : "denied", saved_errno);
}

static void append_context(char *output, size_t output_size) {
    char context[256] = "unknown";
    int fd = open("/proc/self/attr/current", O_RDONLY | O_CLOEXEC);
    if (fd >= 0) {
        ssize_t count = read(fd, context, sizeof(context) - 1);
        close(fd);
        if (count > 0) {
            context[count] = '\0';
            context[strcspn(context, "\r\n")] = '\0';
        }
    }
    append_line(output, output_size, "selinux_context", context);
}

JNIEXPORT jstring JNICALL
Java_org_sroot_app_NativeProbe_run(JNIEnv *env, jobject thiz) {
    (void)thiz;

    char output[4096] = {0};
    char line[128];
    snprintf(line, sizeof(line), "%u", getuid());
    append_line(output, sizeof(output), "uid", line);
    snprintf(line, sizeof(line), "%u", geteuid());
    append_line(output, sizeof(output), "euid", line);
    snprintf(line, sizeof(line), "%u", getgid());
    append_line(output, sizeof(output), "gid", line);
    snprintf(line, sizeof(line), "%u", getegid());
    append_line(output, sizeof(output), "egid", line);
    snprintf(line, sizeof(line), "%ld", sysconf(_SC_PAGESIZE));
    append_line(output, sizeof(output), "page_size", line);

    append_context(output, sizeof(output));
    append_access(output, sizeof(output), "tracefs_control",
                  "/sys/kernel/tracing/tracing_on", O_RDWR);
    append_access(output, sizeof(output), "tracefs_event",
                  "/sys/kernel/tracing/events/workqueue/workqueue_execute_start/enable",
                  O_RDWR);
    append_access(output, sizeof(output), "ashmem", "/dev/ashmem", O_RDWR);
    append_access(output, sizeof(output), "boot_id",
                  "/proc/sys/kernel/random/boot_id", O_RDONLY);
    append_access(output, sizeof(output), "proc_self_status",
                  "/proc/self/status", O_RDONLY);

    return (*env)->NewStringUTF(env, output);
}
