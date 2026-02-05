#include <zephyr/types.h>
#include <stddef.h>
#include <string.h>
#include <errno.h>
#include <zephyr/sys/printk.h>
#include <zephyr/kernel.h>
#include <zephyr/drivers/gpio.h>
#include <zephyr/logging/log.h>
#include <zephyr/drivers/hwinfo.h>
#include <stdio.h>

#include "bluetooth.h"
#include "motor_sim.h"
#include "watchdog.h"
#include "motor.h" // Needed for motor_init

LOG_MODULE_REGISTER(main, LOG_LEVEL_INF);

int main(void)
{
    LOG_INF("Starting Bluetooth Motor Control Application");    

    // 1. Initialize the Motor Data Structures (Safe API)
    motor_init(); 

    // 2. Initialize Bluetooth
    int err = bt_enable(bt_ready);
    if (err) {
        LOG_ERR("Bluetooth init failed (err %d)", err);
        return 0;
    }

    // 3. Register Callbacks & Start Watchdog
    bt_conn_cb_register(&conn_callbacks);
    watchdog_init();

    // 4. Start the Physics Simulation
    motor_sim_init();

    return 0;
}