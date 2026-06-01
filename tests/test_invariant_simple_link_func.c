#include <check.h>
#include <stdlib.h>
#include <string.h>

/* Define the buffer size that g_SSID uses (typically 33 bytes: 32 + null) */
#define G_SSID_SIZE 33

/* Access the global buffer from the production code */
extern char g_SSID[G_SSID_SIZE];

/* We need to simulate calling the WLAN event handler with crafted events.
   Include the production source directly to test the actual code path. */
#include "03_lab_exercises/CC3100_workspace/websrv/simple_link_func.c"

START_TEST(test_ssid_copy_no_overflow)
{
    /* Invariant: Buffer reads/writes to g_SSID never exceed G_SSID_SIZE bytes */
    const char *payloads[] = {
        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", /* 68 chars - 2x overflow */
        "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB", /* 10x overflow */
        "CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC", /* exactly 32 - boundary */
        "ValidSSID",                        /* normal valid input */
    };
    int num_payloads = sizeof(payloads) / sizeof(payloads[0]);

    for (int i = 0; i < num_payloads; i++) {
        /* Clear buffer with a sentinel pattern to detect overflow */
        char sentinel_buf[G_SSID_SIZE + 16];
        memset(sentinel_buf, 0xAA, sizeof(sentinel_buf));
        memcpy(g_SSID, sentinel_buf, G_SSID_SIZE);

        /* Construct a fake WLAN event */
        SlWlanEvent_t event;
        memset(&event, 0, sizeof(event));
        event.Event = SL_WLAN_CONNECT_EVENT;
        size_t copy_len = strlen(payloads[i]);
        if (copy_len > sizeof(event.EventData.STAandP2PModeWlanConnected.ssid_name))
            copy_len = sizeof(event.EventData.STAandP2PModeWlanConnected.ssid_name);
        memcpy(event.EventData.STAandP2PModeWlanConnected.ssid_name, payloads[i], copy_len);
        event.EventData.STAandP2PModeWlanConnected.ssid_len = copy_len;

        SimpleLinkWlanEventHandler(&event);

        /* Assert: g_SSID must be null-terminated within bounds */
        size_t actual_len = strnlen(g_SSID, G_SSID_SIZE);
        ck_assert_msg(actual_len < G_SSID_SIZE,
            "g_SSID overflow: written %zu bytes (max %d) with payload %d",
            actual_len, G_SSID_SIZE - 1, i);
    }
}
END_TEST

Suite *security_suite(void)
{
    Suite *s;
    TCase *tc_core;

    s = suite_create("Security");
    tc_core = tcase_create("Core");

    tcase_add_test(tc_core, test_ssid_copy_no_overflow);
    suite_add_tcase(s, tc_core);

    return s;
}

int main(void)
{
    int number_failed;
    Suite *s;
    SRunner *sr;

    s = security_suite();
    sr = srunner_create(s);

    srunner_run_all(sr, CK_NORMAL);
    number_failed = srunner_ntests_failed(sr);
    srunner_free(sr);

    return (number_failed == 0) ? EXIT_SUCCESS : EXIT_FAILURE;
}