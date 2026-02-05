#ifndef BLUETOOTH_H_
#define BLUETOOTH_H_

#include <zephyr/types.h>
#include <zephyr/bluetooth/bluetooth.h>
#include <zephyr/bluetooth/conn.h>
#include <zephyr/bluetooth/gatt.h>
#include <zephyr/bluetooth/uuid.h>
#include <zephyr/logging/log.h>

#define MY_COMPANY_ID 0x706D

// MOTOR SERVICE UUID
#define BT_UUID_MOTOR_SERVICE_VAL \
	BT_UUID_128_ENCODE(0xc52081ba, 0xe90f, 0x40e4, 0xa99f, 0xccaa4fd11c15)

// COMMAND CHARACTERISTIC UUID
#define BT_UUID_MOTOR_CMD_VAL \
	BT_UUID_128_ENCODE(0xd10b46cd, 0x412a, 0x4d15, 0xa7bb, 0x092a329eed46)

// MOTOR TELEMETRY UUID
#define BT_UUID_MOTOR_TELEMETRY_VAL \
	BT_UUID_128_ENCODE(0x17da15e5, 0x05b1, 0x42df, 0x8d9d, 0xd7645d6d9293)

// MOTOR HEARTBEAT UUID
#define BT_UUID_MOTOR_HEARTBEAT_VAL \
	BT_UUID_128_ENCODE(0x2215d558, 0xc569, 0x4bd1, 0x8947, 0xb4fd5f9432a0)


enum motor_cmds{
	MOTOR_MODE_OFF = 0x00,
	MOTOR_MODE_INIT = 0x01,
	MOTOR_MODE_SPEED = 0x02,
	MOTOR_MODE_POSITION = 0x03
};

// MOTOR APPLICATION DATA STRUCTURE
struct motor_app_ctx{
	// FLAG TO INDICATE IF NOTIFICATIONS ARE ENABLED FOR USER
	bool notification_enabled; 
	
	// HEARTBEAT VALUE -> CONFIRMS BLE SYNCHRONIZATION
	uint8_t heartbeat_val;
};

// PUBLIC API GETTERS FOR BLUETOOTH STATS

/** @brief Get the latest heartbeat counter value (received from the phone via BLE)*/
uint8_t bt_get_heartbeat(void);
/** @brief Check if the android device has subscribed to notifications*/
uint8_t bt_is_notify_enabled(void);

void bt_ready(int err);

void motor_notify_telemetry(void);

// MAIN.c has to register the bluetooth
extern struct bt_conn_cb conn_callbacks;

#endif /* BLUETOOTH_H_ */