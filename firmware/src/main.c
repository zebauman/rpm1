#include <zephyr/types.h>
#include <stddef.h>
#include <string.h>
#include <errno.h>
#include <zephyr/sys/printk.h>
#include <zephyr/kernel.h>
#include <zephyr/drivers/gpio.h>
#include <zephyr/logging/log.h>
#include <zephyr/drivers/hwinfo.h>
#include "bluetooth.h"
#include "motor_sim.h"
#include <stdio.h>

LOG_MODULE_REGISTER(main, LOG_LEVEL_INF);

// // WATCHDOG TIMEOUT 2 SECONDS
// #define WATCHDOG_TIMEOUT_MS 2000

// static struct k_work_delayable watchdog_work;

// void watchdog_kick(void){
//     k_work_reschedule(&watchdog_work,)
// }

// // EMERGENCY STOP FUNCTION IF WATCHDOG EXPIRES -> WILL HALT MOTOR
// void watchdog_expired(struct k_work *work){
//     LOG_ERR("Watchdog Timer Expired - Connection Lost - HALTING MOTOR.");

//     motor_ctx.last_target = 0;
//     motor_ctx.motor_status = 0x00;

//     LOG_INF("MOTOR HALTED");
// }

int main(void)
{
    LOG_INF("Starting Bluetooth Motor Control Application");    

    memset(&motor_ctx, 0, sizeof(motor_ctx));

    k_work_init_delayable(&watchdog_work, watchdog_expired);

    int err = bt_enable(bt_ready);
    if (err) {
        LOG_ERR("Bluetooth init failed (err %d)", err);
        return 0;
    }

    bt_conn_cb_register(&conn_callbacks);
    motor_sim_init();

    return 0;
}
