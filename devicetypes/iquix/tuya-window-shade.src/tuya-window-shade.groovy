/**
 *  Tuya Window Shade (v.0.4.3.1)
 *	Copyright 2020 Jaewon Park (iquix)
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
 */

import groovy.json.JsonOutput

metadata {
	definition(name: "Tuya Window Shade", namespace: "iquix", author: "iquix", ocfDeviceType: "oic.d.blind", vid: "generic-shade") {
		capability "Actuator"
		capability "Configuration"
		capability "Window Shade"
		capability "Window Shade Preset"
		capability "Switch Level"

		command "pause"

		fingerprint profileId: "0104", inClusters: "0000, 000A, 0004, 0005, 00EF", outClusters: "0019", manufacturer: "_TZE200_cowvfni3", model: "TS0601", deviceJoinName: "Tuya Window Treatment" // Zemismart Zigbee Curtain *
		fingerprint profileId: "0104", inClusters: "0000, 000A, 0004, 0005, 00EF", outClusters: "0019", manufacturer: "_TZE200_wmcdj3aq", model: "TS0601", deviceJoinName: "Tuya Window Treatment" // Zemismart Blind *
		fingerprint profileId: "0104", inClusters: "0000, 000A, 0004, 0005, 00EF", outClusters: "0019", manufacturer: "_TZE200_fzo2pocs", model: "TS0601", deviceJoinName: "Tuya Window Treatment" // Zemismart Blind New (Not tested)
		fingerprint profileId: "0104", inClusters: "0000, 000A, 0004, 0005, 00EF", outClusters: "0019", manufacturer: "_TZE200_nogaemzt", model: "TS0601", deviceJoinName: "Tuya Window Treatment" // YS-MT750 *
		fingerprint profileId: "0104", inClusters: "0000, 000A, 0004, 0005, 00EF", outClusters: "0019", manufacturer: "_TZE200_5zbp6j0u", model: "TS0601", deviceJoinName: "Tuya Window Treatment" // YS-MT750 *
		fingerprint profileId: "0104", inClusters: "0000, 000A, 0004, 0005, 00EF", outClusters: "0019", manufacturer: "_TZE200_fdtjuw7u", model: "TS0601", deviceJoinName: "Tuya Window Treatment" // YS-MT750 *
		fingerprint profileId: "0104", inClusters: "0000, 000A, 0004, 0005, 00EF", outClusters: "0019", manufacturer: "_TZE200_zpzndjez", model: "TS0601", deviceJoinName: "Tuya Window Treatment" // DS82 *
		fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006", outClusters: "0019", manufacturer: "_TYST11_cowvfni3", model: "owvfni3", deviceJoinName: "Tuya Window Treatment" // Zemismart Zigbee Curtain *
		fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006", outClusters: "0019", manufacturer: "_TYST11_wmcdj3aq", model: "mcdj3aq", deviceJoinName: "Tuya Window Treatment" // Zemismart Zigbee Blind *
		fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006", outClusters: "0019", manufacturer: "_TYST11_fzo2pocs", model: "zo2pocs", deviceJoinName: "Tuya Window Treatment" // Zemismart Zigbee Blind New (Not tested)
		fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006", outClusters: "0019", manufacturer: "_TYST11_nogaemzt", model: "ogaemzt", deviceJoinName: "Tuya Window Treatment" // YS-MT750
		fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006", outClusters: "0019", manufacturer: "_TYST11_5zbp6j0u", model: "zbp6j0u", deviceJoinName: "Tuya Window Treatment" // YS-MT750
		fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006", outClusters: "0019", manufacturer: "_TYST11_fdtjuw7u", model: "dtjuw7u", deviceJoinName: "Tuya Window Treatment" // YS-MT750
		fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006", outClusters: "0019", manufacturer: "_TYST11_zpzndjez", model: "pzndjez", deviceJoinName: "Tuya Window Treatment" // DS82
	}

	preferences {
		input "preset", "number", title: "Preset position", description: "Set the window shade preset position", defaultValue: 50, range: "0..100", required: false, displayDuringSetup: false
		input "reverse", "enum", title: "Direction", description: "Set direction of curtain motor by open/close app commands. For example, if you send 'open' command from app, but the curtain motor is closing, then set this option to 'Reverse'.", options: ["Forward", "Reverse"], defaultValue: "Forward", required: false, displayDuringSetup: false
		input "fixpercent", "enum", title: "Fix percent", description: "Set 'Fix percent' option unless open is 100% and close is 0%. In Smartthings, 'Open' should be 100% in level and 'Close' should be 0% in level. If it is reversed, then set this option to 'Fix percent'.", options: ["Default", "Fix percent"], defaultValue: "Default", required: false, displayDuringSetup: false
		//input "fixcommand", "enum", title: "Fix command", description: "[Experimental] Set 'Fix command' option if up/down command from RF remote control differs from open/close app command. If you are setting up curtain, please set this option FIRST before setting other options.", options: ["Default", "Fix command"], defaultValue: "Default", required: false, displayDuringSetup: false
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"windowShade", type: "generic", width: 6, height: 4) {
			tileAttribute("device.windowShade", key: "PRIMARY_CONTROL") {
				attributeState "open", label: 'Open', action: "close", icon: "https://objectstorage.ap-seoul-1.oraclecloud.com/n/cnqa4dxui0ha/b/ST/o/window_open.png", backgroundColor: "#00A0DC", nextState: "closing"
				attributeState "closed", label: 'Closed', action: "open", icon: "https://objectstorage.ap-seoul-1.oraclecloud.com/n/cnqa4dxui0ha/b/ST/o/window_close.png", backgroundColor: "#ffffff", nextState: "opening"
				attributeState "partially open", label: 'Partially open', action: "close", icon: "https://objectstorage.ap-seoul-1.oraclecloud.com/n/cnqa4dxui0ha/b/ST/o/window_open.png", backgroundColor: "#d45614", nextState: "closing"
				attributeState "opening", label: 'Opening', action: "pause", icon: "https://objectstorage.ap-seoul-1.oraclecloud.com/n/cnqa4dxui0ha/b/ST/o/window_open.png", backgroundColor: "#00A0DC", nextState: "partially open"
				attributeState "closing", label: 'Closing', action: "pause", icon: "https://objectstorage.ap-seoul-1.oraclecloud.com/n/cnqa4dxui0ha/b/ST/o/window_close.png", backgroundColor: "#ffffff", nextState: "partially open"
			}
		}
		standardTile("contPause", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "pause", label:"", icon:'st.sonos.pause-btn', action:'pause', backgroundColor:"#cccccc"
		}
		standardTile("presetPosition", "device.presetPosition", width: 2, height: 2, decoration: "flat") {
			state "default", label: "Preset", action:"presetPosition", icon:"st.Home.home2"
		}
		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 1) {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		valueTile("shadeLevel", "device.level", width: 4, height: 1) {
			state "level", label: 'Shade is ${currentValue}% up', defaultState: true
		}
		controlTile("levelSliderControl", "device.level", "slider", width:2, height: 1, inactiveLabel: false) {
			state "level", action:"switch level.setLevel"
		}

		main "windowShade"
		details(["windowShade", "contPause", "presetPosition", "shadeLevel", "levelSliderControl", "refresh"])
	}
}

private getCLUSTER_TUYA() { 0xEF00 }
private getSETDATA() { 0x00 }

// tuya DP type
private getDP_TYPE_BOOL() { "01" }
private getDP_TYPE_VALUE() { "02" }
private getDP_TYPE_ENUM() { "04" }


// Parse incoming device messages to generate events
def parse(String description) {
	if (description?.startsWith('catchall:') || description?.startsWith('read attr -')) {
		Map descMap = zigbee.parseDescriptionAsMap(description)		
		log.debug descMap
		if (descMap?.clusterInt==CLUSTER_TUYA) {
			if ( descMap?.command == "01" || descMap?.command == "02" ) {
				def dp = zigbee.convertHexToInt(descMap?.data[2])
				def fncmd = zigbee.convertHexToInt(descMap?.data[6..-1].join(''))
				log.debug "dp=${dp} fncmd=${fncmd}"
				switch (dp) {
					case 0x07: // 0x07: Work state -- Started moving (triggered by RF remote or pulling the curtain)
						if (isZemiCurtain()) {
							if (device.currentValue("level") == 0) {
								log.debug "moving from position 0 : must be opening"
								levelEventMoving(100)
							} else if (device.currentValue("level") == 100) {
								log.debug "moving from position 100 : must be closing"
								levelEventMoving(0)
							}
						} else {
							if (directionVal(fncmd) == 0) {
								log.debug "opening"
								levelEventMoving(100)
							} else if (directionVal(fncmd) == 1) {
								log.debug "closing"
								levelEventMoving(0)
							}
						}
						break
					case 0x01: // 0x01: Control -- Opening/closing/stopping (triggered from Zigbee)
						if (cmdVal(fncmd) == 0) {
							log.debug "opening"
							levelEventMoving(100)
						} else if (cmdVal(fncmd) == 2) {
							log.debug "closing"
							levelEventMoving(0)
						}
						break
					case 0x02: // 0x02: Percent control -- Started moving to position (triggered from Zigbee)
						def pos = levelVal(fncmd)
						log.debug "moving to position: "+pos
						levelEventMoving(pos)
						break
					case 0x03: // 0x03: Percent state -- Arrived at position
						def pos = levelVal(fncmd)
						log.debug "arrived at position: "+pos
						levelEventArrived(pos)
						break
					case 0x67: // 0x03: Reached the limit (YS-MT750 only)
						log.debug "Reached the limit position. the motor has the limit set at this position."
						break
				}
			}
		}
	}
}

private levelEventMoving(currentLevel) {
	def lastLevel = device.currentValue("level")
	log.debug "levelEventMoving - currentLevel: ${currentLevel} lastLevel: ${lastLevel}"
	if (lastLevel == "undefined" || currentLevel == lastLevel) { //Ignore invalid reports
		log.debug "Ignore invalid reports"
	} else {
		if (lastLevel < currentLevel) {
			sendEvent([name:"windowShade", value: "opening", displayed: true])
		} else if (lastLevel > currentLevel) {
			sendEvent([name:"windowShade", value: "closing", displayed: true])
		}
		state.levelRestoreValue = lastLevel
		runIn(90, "levelRestore", [overwrite:true])
	}
}

private levelEventArrived(level) {
	state.levelRestoreValue = null
	if (level == 0) {
		sendEvent(name: "windowShade", value: "closed", displayed: true)
	} else if (level == 100) {
		sendEvent(name: "windowShade", value: "open", displayed: true)
	} else if (level > 0 && level < 100) {
		sendEvent(name: "windowShade", value: "partially open", displayed: true)
	} else {
		log.debug "Position value error (${level}) : Please remove the device from Smartthings, and setup limit of the curtain before pairing."
		sendEvent(name: "windowShade", value: "unknown", displayed: true)
		sendEvent(name: "level", value: 50, displayed: true)
		return
	}
	sendEvent(name: "level", value: (level), displayed: true)
}

def close() {
	log.info "close()"
	def currentLevel = device.currentValue("level")
	if (currentLevel == 0) {
		sendEvent(name: "windowShade", value: "closed", displayed: true)
	}
	sendTuyaCommand("01", DP_TYPE_ENUM, zigbee.convertToHexString(cmdVal(2)))
}

def open() {
	log.info "open()"
	def currentLevel = device.currentValue("level")
	if (currentLevel == 100) {
		sendEvent(name: "windowShade", value: "open", displayed: true)
	}
	sendTuyaCommand("01", DP_TYPE_ENUM, zigbee.convertToHexString(cmdVal(0)))
}

def pause() {
	log.info "pause()"
	sendEvent(name: "windowShade", value: device.currentValue("windowShade"), displayed: false)
	sendTuyaCommand("01", DP_TYPE_ENUM, "01")
}

def setLevel(data, rate = null) {
	log.info "setLevel(${data})"
	def currentLevel = device.currentValue("level")
	if (currentLevel == data) {
		sendEvent(name: "level", value: currentLevel, displayed: true)
	}
	runIn(10, "levelSet", [overwrite:true])
	sendTuyaCommand("02", DP_TYPE_VALUE, zigbee.convertToHexString(levelVal(data), 8))
}


def presetPosition() {
	setLevel(preset ?: 50)
}

def installed() {
	log.info "installed()"
	state.preferences = "|${reverse}|${fixpercent}|${fixcommand}|"
	state.default_fix_percent == null
	sendEvent(name: "supportedWindowShadeCommands", value: JsonOutput.toJson(["open", "close", "pause"]), displayed: false)
	def cmds = sendTuyaCommand("02", DP_TYPE_VALUE, zigbee.convertToHexString(50, 8))
	cmds.each{ sendHubCommand(new physicalgraph.device.HubAction(it)) }
}

def updated() {
	log.info "updated()"
	calcDefaultFixpercent()
	if (state.preferences != "|${reverse}|${fixpercent}|${fixcommand}|") {
		state.preferences = "|${reverse}|${fixpercent}|${fixcommand}|"
		def cmds = sendTuyaCommand("02", DP_TYPE_VALUE, zigbee.convertToHexString(50, 8))
		cmds.each{ sendHubCommand(new physicalgraph.device.HubAction(it)) }
		runIn(2, "setDirection")
	} else {
		setDirection()
	}
}

def levelSet() {
	if (state.levelRestoreValue != null) {
		log.debug "Position data not received yet. Setting position to previous position (${state.levelRestoreValue}) to prevent app error."
		sendEvent(name: "level", value: state.levelRestoreValue, displayed: false)
	}
}

def levelRestore() {
	if (state.levelRestoreValue != null) {
		log.debug "Position data finally not received until timeout. Restoring previous state and position(${state.levelRestoreValue})."
		levelEventArrived(state.levelRestoreValue)
	}
}

def setDirection() {
	log.info "setDirection()"
	def cmds = sendTuyaCommand("05", DP_TYPE_ENUM, (reverse == "Reverse") ? "01" : "00")
	cmds.each{ sendHubCommand(new physicalgraph.device.HubAction(it)) }	
}

private doLimitTravel() {
	log.info "doLimitTravel()"
	sendTuyaCommand("06", DP_TYPE_BOOL, "01")
}

private sendTuyaCommand(dp, dp_type, fncmd) {
	zigbee.command(CLUSTER_TUYA, SETDATA, PACKET_ID + dp + dp_type + zigbee.convertToHexString(fncmd.length()/2, 4) + fncmd )
}

private getPACKET_ID() {
	state.packetID = ((state.packetID ?: 0) + 1 ) % 65536
	zigbee.convertToHexString(state.packetID, 4)
}

private levelVal(n) {
	if (state.default_fix_percent == null) {
		calcDefaultFixpercent()
	}
	def pct = n & 0xFF
	//extremly awkward percent packet in "ogaemzt" device. special thanks to 경기PA팬텀
	if (state.default_fix_percent == "ogaemzt") {
		return (int)(((fixpercent == "Fix percent") ^ (n == pct)) ? 100 - pct : pct)
	} else {
		return (int)(((fixpercent == "Fix percent") ^ state.default_fix_percent) ? 100 - pct : pct)	
	}
	
}

private cmdVal(c) {
	//return (fixcommand == "Fix command") ? 2 - c : c
	return c
}

private directionVal(c) {
	//return ( (isZemiBlind() && (reverse != "Reverse")) ^ (fixcommand == "Fix command") ) ? 1 - c : c
	return (isZemiBlind() && (reverse != "Reverse")) ? 1 - c : c
}

private calcDefaultFixpercent() {
	def fixpercent_devices = ["owvfni3", "zbp6j0u", "pzndjez"]
	def dev = fixpercent_devices.find { productId == it }
	state.default_fix_percent = isOgaemzt() ? "ogaemzt" : (dev != null)
	log.debug "default fixpercent for this device is set to ${state.default_fix_percent}"
}

private getProductId() {
	return device.getDataValue("manufacturer")[-7..-1]
}

private isZemiCurtain() {
	return (productId == "owvfni3")
}

private isZemiBlind() {
	return (productId == "mcdj3aq" || productId == "zo2pocs")
}

private isOgaemzt() {
	return (productId == "ogaemzt")
}