#include "motor.h"

#include <string.h>
#include <stdbool.h>

static struct motor_stats m_stats;

void motor_init(void){
    memset(&m_stats, 0, sizeof(m_stats)); // WIPE ALL THE DATA TO ZERO (EVEN PRE-EXISTING DATA)

    motor_set_state(MOTOR_STATE_STOPPED);

    motor_set_sync_warning(false);
    motor_set_overheat_warning(false);
}

void motor_set_speed(int32_t rpm){
    m_stats.current_speed = rpm;    // SHOULD BE CORRECT VALUE SINCE PASSED DIRECTLY FROM MOTOR LOGIC
}

void motor_set_position(int32_t degrees){
    m_stats.current_position = degrees; // SHOULD BE CORRECT VALUE SINCE PASSED DIRECTLY FROM MOTOR LOGIC
}

void motor_set_state(uint8_t new_state){
    m_stats.motor_status = (m_stats.motor_status & MOTOR_FLAG_MASK) | (new_state & MOTOR_STATE_MASK);
}

void motor_set_flag(uint8_t flag, bool active){
    if(active){
        m_stats.motor_status |= (flag & MOTOR_FLAG_MASK);
    } else{
        m_stats.motor_status &= ~(flag & MOTOR_FLAG_MASK);
    }
}

void motor_set_sync_warning(bool active){
    motor_set_flag(MOTOR_FLAG_SYNC_BAD, active);
}

void motor_set_overheat_warning(bool active){
    motor_set_flag(MOTOR_FLAG_OVERHEAT, active);
}

void motor_set_target_state(uint8_t new_state){
    m_stats.target_state = new_state & MOTOR_STATE_MASK;
}

void motor_set_target_speed(int32_t rpm){
    if(rpm > RPM_MAX) rpm = RPM_MAX;
    if(rpm < RPM_MIN) rpm = RPM_MIN;

    m_stats.target_speed = rpm;
}

void motor_set_target_position(int32_t degrees){
    m_stats.current_position = degrees % 360;
}


// GETTERS

uint8_t motor_get_full_status(void){
    return m_stats.motor_status;
}

bool motor_is_sync_bad(void){
    return m_stats.motor_status & MOTOR_FLAG_SYNC_BAD;
}

bool motor_is_overheated(void){
    return m_stats.motor_status & MOTOR_FLAG_OVERHEAT;
}

int32_t motor_get_speed(void){
    return m_stats.current_speed;
}

int32_t motor_get_position(void){
    return m_stats.current_position;
}

uint8_t motor_get_target_state(void){
    return m_stats.target_state;
}

int32_t motor_get_target_speed(void){
    return m_stats.target_speed;
}

int32_t motor_get_target_position(void){
    return m_stats.target_position;
}