# RPM1 – Android BLE Motor Controller

A Kotlin-based Android application for real-time Bluetooth Low Energy (BLE) control and monitoring of a brushless DC motor using an STM32WB55 microcontroller.


**Real-time control • Low-latency BLE communication • Live RPM & Position analytics**

---

## Overview

This Android application provides a wireless control interface for the **DC Motor Rotation Control System**, enabling users to:

- Set motor **speed (RPM)**
- Command motor **position (angle)**
- Perform **initialization and calibration**
- View **real-time telemetry** (RPM + shaft angle)
- Monitor **connection status** and device information
- Configure **BLE scanning & performance options**

The application communicates with an **STM32WB55** running a custom BLE GATT service.  
A compact command/telemetry protocol enables **low-latency, bidirectional communication** suitable for real-time motor control.

---

## Features

### Bluetooth Low Energy
- BLE scanning with device filtering
- Automatic reconnection
- Full GATT connection + service/characteristic discovery
- Configurable BLE performance modes:
    - Balanced
    - High-Performance
    - Low-Power

### Motor Control
- Start/stop controls
- Set speed (RPM)
- Set position (angle)
- Safe shutdown command
- Calibration mode

### Live Analytics
- Real-time RPM & angle graph
- Adjustable maximum point count
- Background analytics (graph persists between UI screens)

### User Interface
- Persistent top header with live telemetry
- Navigation bar: **Scan / Control / Analytics / Settings**
- Clean, modern UI built with Kotlin + XML
- Robust error handling for:
    - Disconnects
    - Malformed packets
    - Lost notifications

---

## BLE Protocol Summary

The app interfaces with a **custom BLE GATT service** defined on the STM32WB55 microcontroller.

---

### Service UUID
SERVICE_MOTOR = `0000ff00-0000-1000-8000-00805f9b34fb`

---

## Command Characteristic (Write)
CHAR_CMD = `0000ff01-0000-1000-8000-00805f9b34fb`

### Payload Structure
| Byte | Meaning              |
|------|----------------------|
| 0    | Command ID           |
| 1–4  | 32-bit parameter (LE)|

### Supported Commands
| Command      | ID      | Description                        |
|--------------|---------|------------------------------------|
| Calibrate    | `0x01`  | Motor initialization & zeroing     |
| Set RPM      | `0x02`  | Sets target speed                  |
| Set Position | `0x03`  | Sets target angle                  |
| Shutdown     | `0x04`  | Stops motor immediately            |

---

## Telemetry Characteristic (Notify)
CHAR_TELEM = `0000ff02-0000-1000-8000-00805f9b34fb`

### Telemetry Payload
| Field | Type   | Description                 |
|-------|--------|-----------------------------|
| RPM   | int32  | Current motor speed (RPM)   |
| Angle | int32  | Current shaft angle         |
| Status | uint8 (optional) | Mode/fault flags |

---