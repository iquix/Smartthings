/**
 *  Tuya Window Shade (v.0.3.2.0)
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
	definition(name: "Tuya Window Shade (Legacy)", namespace: "iquix", author: "iquix", ocfDeviceType: "oic.d.blind", vid: "generic-shade") {
		capability "Actuator"
		capability "Configuration"
		capability "Window Shade"
		capability "Window Shade Preset"
		capability "Switch Level"

		command "pause"

		fingerprint profileId: "0104", inClusters: "0000, 000A, 0004, 0005, 00EF", outClusters: "0019", manufacturer: "_TZE200_cowvfni3", model: "TS0601", deviceJoinName: "Tuya Window Treatment"
		fingerprint profileId: "0104", inClusters: "0000, 000A, 0004, 0005, 00EF", outClusters: "0019", manufacturer: "_TZE200_wmcdj3aq", model: "TS0601", deviceJoinName: "Tuya Window Treatment"
		fingerprint profileId: "0104", inClusters: "0000, 000A, 0004, 0005, 00EF", outClusters: "0019", manufacturer: "_TZE200_nogaemzt", model: "TS0601", deviceJoinName: "Tuya Window Treatment"
		fingerprint profileId: "0104", inClusters: "0000, 000A, 0004, 0005, 00EF", outClusters: "0019", manufacturer: "_TZE200_5zbp6j0u", model: "TS0601", deviceJoinName: "Tuya Window Treatment"
		fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006", outClusters: "0019", manufacturer: "_TYST11_cowvfni3", model: "owvfni3", deviceJoinName: "Tuya Window Treatment"
		fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006", outClusters: "0019", manufacturer: "_TYST11_wmcdj3aq", model: "mcdj3aq", deviceJoinName: "Tuya Window Treatment"
		fingerprint profileId: "0104", inClusters: "0000, 0003, 0004, 0005, 0006", outClusters: "0019", manufacturer: "_TYST11_fdtjuw7u", model: "dtjuw7u", deviceJoinName: "Tuya Window Treatment"
	}

	preferences {
		input "preset", "number", title: "Preset position", description: "Set the window shade preset position", defaultValue: 50, range: "0..100", required: false, displayDuringSetup: false
		input "reverse", "enum", title: "Direction", description: "Set direction of curtain motor by open/close app commands. [WARNING!! Please set curtain position to 50% before changing this option.]", options: ["Forward", "Reverse"], defaultValue: "Forward", required: false, displayDuringSetup: false
		input "fixpercent", "enum", title: "Fix percent", description: "Set 'Fix percent' option unless open is 100% and close is 0%. [WARNING: Please set curtain position to 50% before changing this option.]", options: ["Default", "Fix percent"], defaultValue: "Default", required: false, displayDuringSetup: false
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
				def fncmd = descMap?.data[6..-1]
				log.debug "dp=" + dp + "  fncmd=" + fncmd
				switch (dp) {
					case 0x07: // 0x07: Work state -- Started moving (triggered by transmitter or pulling on curtain)
						if (device.currentValue("level") == 0) {
							log.debug "moving from position 0 : must be opening"
							levelEventMoving(100)
						} else if (device.currentValue("level") == 100) {
							log.debug "moving from position 100 : must be closing"
							levelEventMoving(0)
						}
						break
					case 0x01: // 0x01: Control -- Opening/closing/stopping (triggered from Zigbee)
						if (fncmd[0] == "02") {
							log.debug "opening"
							levelEventMoving(100)
						} else if (fncmd[0] == "00") {
							log.debug "closing"
							levelEventMoving(0)
						}
						break
					case 0x02: // 0x02: Percent control -- Started moving to position (triggered from Zigbee)
						def pos = levelVal(zigbee.convertHexToInt(fncmd[3]))
						log.debug "moving to position: "+pos
						levelEventMoving(pos)
						break
					case 0x03: // 0x03: Percent state -- Arrived at position
						def pos = levelVal(zigbee.convertHexToInt(fncmd[3]))
						log.debug "arrived at position: "+pos
						levelEventArrived(pos)
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
	sendTuyaCommand("01", DP_TYPE_ENUM, "00")
}

def open() {
	log.info "open()"
	def currentLevel = device.currentValue("level")
	if (currentLevel == 100) {
		sendEvent(name: "windowShade", value: "open", displayed: true)
	}
	sendTuyaCommand("01", DP_TYPE_ENUM, "02")
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
	state.preferences = null
	sendEvent(name: "supportedWindowShadeCommands", value: JsonOutput.toJson(["open", "close", "pause"]), displayed: false)
}

def updated() {
	log.info "updated()"
	def cmds = setDirection()
	if (state.preferences != "|${reverse}|${fixpercent}|")  {
		state.preferences = "|${reverse}|${fixpercent}|"
		cmds += setLevel(50)
	}
	cmds.each{ sendHubCommand(new physicalgraph.device.HubAction(it)) }	 
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

private setDirection() {
	log.info "setDirection()"
	sendTuyaCommand("05", DP_TYPE_ENUM, (reverse == "Reverse") ? "01" : "00")
}

private sendTuyaCommand(dp, dp_type, fncmd) {
	zigbee.command(CLUSTER_TUYA, SETDATA, PACKET_ID + dp + dp_type + zigbee.convertToHexString(fncmd.length()/2, 4) + fncmd )
}

private getPACKET_ID() {
	state.packetID = ((state.packetID ?: 0) + 1 ) % 65536
	return zigbee.convertToHexString(state.packetID, 4)
}

private levelVal(n) {
	return (int)((fixpercent == "Fix percent") ? 100-n : n)
}