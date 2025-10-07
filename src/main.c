#include <zephyr/types.h>
#include <stddef.h>
#include <string.h>
#include <errno.h>
#include <zephyr/sys/printk.h>
#include <zephyr/sys/byteorder.h>
#include <zephyr/kernel.h>
#include <zephyr/drivers/gpio.h>
#include <zephyr/logging/log.h>

#include <zephyr/bluetooth/bluetooth.h>
#include <zephyr/bluetooth/hci.h>
#include <zephyr/bluetooth/conn.h>
#include <zephyr/bluetooth/uuid.h>
#include <zephyr/bluetooth/gatt.h>

#define DEVICE_NAME CONFIG_BT_DEVICE_NAME
#define DEVICE_NAME_LEN (sizeof(DEVICE_NAME) - 1)
#define ADV_LEN 12

LOG_MODULE_REGISTER(main, LOG_LEVEL_INF);


// UUIDS FOR THE SERVICES AND CHARACTERISTICS
// Custom Service UUID: c52081ba-e90f-40e4-a99f-ccaa4fd11c15
static const struct bt_uuid_128 motor_srv_uuid = BT_UUID_INIT_128(
	BT_UUID_128_ENCODE(0xc52081ba, 0xe90f, 0x40e4, 0xa99f, 0xccaa4fd11c15));

// MOTOR COMMAND CHARACTERISTIC UUID: d10b46cd-412a-4d15-a7bb-092a329eed46
static struct bt_uuid_128 motor_cmd_char_uuid = BT_UUID_INIT_128(
	BT_UUID_128_ENCODE(0xd10b46cd, 0x412a, 0x4d15, 0xa7bb, 0x092a329eed46));

// MOTOR TELEMETRY CHARACTERISTIC UUID: 17da15e5-05b1-42df-8d9d-d7645d6d9293
static struct bt_uuid_128 motor_telemetry_char_uuid = BT_UUID_INIT_128(
	BT_UUID_128_ENCODE(0x17da15e5, 0x05b1, 0x42df, 0x8d9d, 0xd7645d6d9293));

// MOTOR APPLICATION DATA STRUCTURE
struct motor_app_ctx{
	// FLAG TO INDICATE IF NOTIFICATIONS ARE ENABLED FOR USER
	uint8_t notification_enabled; 
	
	// LAST COMMAND RECEIVED FROM USER (0x00 - SHUTDOWN, 0x01 - INIT/Calibrate,
	//  0x02 - Speed Control (Set target RPM), 0x03 - POSITION CONTROL (Set target angle))
	uint8_t last_cmd;  
	
	// LAST COMMAND VALUE (E.G. TARGET RPM OR TARGET ANGLE)
	uint32_t last_target;

	// STATUS OF MOTOR (0x00 - OFF, 0x01 - ON, 0x02 - ERROR)
	uint8_t motor_status;

	// CURRENT SPEED OF MOTOR (RPM)
	uint32_t current_speed;

	// CURRENT POSITION OF MOTOR (Degrees)
	uint32_t current_position;
};

static struct motor_app_ctx motor_ctx;

// FORWARD DECLARATIONS
static void motor_notify_telemetry(void);

// WRITE CALLBACK FOR MOTOR COMMAND CHARACTERISTIC
// EXPECTS 4 BYTES: [command (1 byte)] [value (4 bytes)]
static ssize_t write_motor(struct bt_conn *conn,
			   const struct bt_gatt_attr *attr,
			   const void *buf, uint16_t len,
			   uint16_t offset, uint8_t flags)
{
	if(offset != 0) {
		return BT_GATT_ERR(BT_ATT_ERR_INVALID_OFFSET);
	}
	if(len < 5) { // MINIMUM 5 BYTES REQUIRED
		return BT_GATT_ERR(BT_ATT_ERR_INVALID_ATTRIBUTE_LEN);
	}
	const uint8_t *data = buf;
	// PARSE COMMAND
	motor_ctx.last_cmd = data[0];
	motor_ctx.last_target = sys_get_le32(&data[1]); // 4 BYTES FOR VALUE
	LOG_INF("Received command: 0x%02x, Target value: %d", motor_ctx.last_cmd, motor_ctx.last_target);

	// TODO: IMPLEMENT MOTOR CONTROL LOGIC HERE

	return len;

}

// CALLBACK FOR CLIENT CHARACTERISTIC CONFIGURATION (CCC) CHANGES
static void motor_ccc_cfg_changed(const struct bt_gatt_attr *attr, uint16_t value){
	motor_ctx.notification_enabled = (value == BT_GATT_CCC_NOTIFY);
	LOG_INF("Notifications %s", motor_ctx.notification_enabled ? "enabled" : "disabled");
}

// HELPER TO SEND TELEMETRY NOTIFICATIONS TO PHONE
static void motor_notify_telemetry(void)
{
	if (!motor_ctx.notification_enabled) {
		return;
	}

	// TELEMETRY DATA FORMAT
	// [motor_status (1 byte - 4 bits)] [current_speed (4 bytes - 32 bits)] [current_position (4 bytes - 32 bits)]
	uint8_t telemetry_data[9];
	telemetry_data[0] = motor_ctx.motor_status; // FIRST BYTE IS MOTOR STATUS [0]
	sys_put_le32(motor_ctx.current_speed, &telemetry_data[1]); // NEXT 4 BYTES ARE CURRENT SPEED [1-4] - 32 BITS INT
	sys_put_le32(motor_ctx.current_position, &telemetry_data[5]); // LAST 4 BYTES ARE CURRENT POSITION [5-8] - 32 BITS INT

	// SEND NOTIFICATION
	int err = bt_gatt_notify(NULL, &motor_telemetry_char_uuid.uuid,
				 telemetry_data, sizeof(telemetry_data));
	if (err) {
		LOG_ERR("Failed to send notification (err %d)", err);
	} else {
		LOG_INF("Telemetry notification sent");
	}
}

// DEFINE GATT CHARACTERISTICS AND SERVICES
// DEFINE THE motor_svc SERVICE
BT_GATT_SERVICE_DEFINE(motor_svc, BT_GATT_PRIMARY_SERVICE(&motor_srv_uuid),
	// MOTOR COMMAND CHARACTERISTIC - WRITE ONLY
	// FIRST is UUID, SECOND is PROPERTIES, THIRD is PERMISSIONS,
	// FOURTH is READ CALLBACK (NULL IF NOT READABLE), FIFTH is WRITE CALLBACK
	// SIXTH is USER DATA (NULL IF NONE)
	BT_GATT_CHARACTERISTIC(&motor_cmd_char_uuid.uuid,
				   BT_GATT_CHRC_WRITE,
			       BT_GATT_PERM_WRITE,
			       NULL, write_motor, NULL), 
	// MOTOR TELEMETRY CHARACTERISTIC - NOTIFY ONLY
	BT_GATT_CHARACTERISTIC(&motor_telemetry_char_uuid.uuid,
				   BT_GATT_CHRC_NOTIFY,
			       BT_GATT_PERM_NONE,
			       NULL, NULL, NULL),
	// CLIENT CHARACTERISTIC CONFIGURATION (CCC) - FOR ENABLING/DISABLING NOTIFICATIONS
	BT_GATT_CCC(motor_ccc_cfg_changed,
		    BT_GATT_PERM_READ | BT_GATT_PERM_WRITE)
	);
	



// BLUETOOTH INIT & ADVERTISING
static void bt_ready(int err)
{
	if (err) {
		LOG_ERR("Bluetooth init failed (err %d)", err);
		return;
	}

	LOG_INF("Bluetooth initialized");

	// START ADVERTISING
	struct bt_le_adv_param adv_param = {
		.options = BT_LE_ADV_OPT_CONNECTABLE,
		.interval_min = 0x20,
		.interval_max = 0x40,
		.peer = NULL,
	};
	// ADVERTISING DATA
	const struct bt_data ad[] = {
		BT_DATA(BT_DATA_NAME_COMPLETE, DEVICE_NAME, DEVICE_NAME_LEN),
	};

	err = bt_le_adv_start(&adv_param, ad, ARRAY_SIZE(ad), NULL, 0);
	if (err) {
		LOG_ERR("Advertising failed to start (err %d)", err);
		return;
	}

	LOG_INF("Advertising successfully started");
}

static void connected(struct bt_conn *conn, uint8_t err)
{
	if (err) {
		LOG_ERR("Connection failed (err %u)", err);
	} else {
		LOG_INF("Connected");
	}
}

static void disconnected(struct bt_conn *conn, uint8_t reason)
{
	LOG_INF("Disconnected (reason %u)", reason);
}

static struct bt_conn_cb conn_callbacks = {
	.connected = connected,
	.disconnected = disconnected,
};

int main(void)
{
    int err;

    LOG_INF("Starting Bluetooth Motor Control Application");

    // Initialize context values
    memset(&motor_ctx, 0, sizeof(motor_ctx));

    err = bt_enable(bt_ready);
    if (err) {
        LOG_ERR("Bluetooth init failed (err %d)", err);
        return 0;
    }

    bt_conn_cb_register(&conn_callbacks);

    // Simulate telemetry updates
    while (1) {
        // Example: simulate changing speed/position
        motor_ctx.current_speed = (motor_ctx.current_speed + 10) % 1000;
        motor_ctx.current_position = (motor_ctx.current_position + 5) % 360;
        motor_ctx.motor_status = 0x01; // ON

        motor_notify_telemetry();
        k_sleep(K_MSEC(1000));
    }

    return 0;
}
