package com.remotemotorcontroller.ui

object ControlUIState {
    var rpmInput: String = ""
    var angleInput: String = ""

    // CLOCKWISE IS DEFAULT
    var isRpmCcw: Boolean = false
    var isAngleCcw: Boolean = false

    var isMotorRunning = false
}