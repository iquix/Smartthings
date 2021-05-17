/**
 *  Stateless Tuya Window Shade (v.0.0.1)
 *	Copyright 2021 Jaewon Park (iquix)
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
	definition(name: "Stateless Tuya Window Shade", namespace: "iquix", author: "iquix", ocfDeviceType: "oic.d.blind") {
		capability "Actuator"
		capability "Configuration"
		capability "Window Shade"

		command "pause"

		fingerprint profileId: "0104", manufacturer: "_TZE200_iossyxra", model: "TS0601", deviceJoinName: "Stateless Tuya Window Treatment" // Zemismart Zigbee Roller
	}

	preferences {
		input "reverse", "enum", title: "Direction", description: "Set direction of curtain motor by open/close app commands. For example, if you send 'open' command from app, but the curtain motor is closing, then set this option to 'Reverse'.", options: ["Forward", "Reverse"], defaultValue: "Forward", required: false, displayDuringSetup: false
	}
}

private getCLUSTER_TUYA() { 0xEF00 }
private getSETDATA() { 0x00 }
private getINIT_DEVICE() { 0x03 }

// tuya DP type
private getDP_TYPE_BOOL() { "01" }
private getDP_TYPE_VALUE() { "02" }
private getDP_TYPE_ENUM() { "04" }


// Parse incoming device messages to generate events
def parse(String description) {
	// do nothing
}

def close() {
	log.info "close()"
	sendEvent(name: "windowShade", value: "unknown", displayed: false)
	sendTuyaCommand("01", DP_TYPE_ENUM, zigbee.convertToHexString(2))
}

def open() {
	log.info "open()"
	sendEvent(name: "windowShade", value: "unknown", displayed: false)
	sendTuyaCommand("01", DP_TYPE_ENUM, zigbee.convertToHexString(0))
}

def pause() {
	log.info "pause()"
	sendEvent(name: "windowShade", value: "unknown", displayed: false)
	sendTuyaCommand("01", DP_TYPE_ENUM, "01")
}

def setLevel(data) {
	if (data < 50) {
		close()
	} else {
		open()
	}
}

def installed() {
	log.info "installed()"
	sendEvent(name: "supportedWindowShadeCommands", value: JsonOutput.toJson(["open", "close", "pause"]), displayed: false)
	sendEvent(name: "windowShade", value: "unknown", displayed: false)
	return
} 

def updated() {
	log.info "updated()"
	def cmds = sendTuyaCommand("05", DP_TYPE_ENUM, (reverse == "Reverse") ? "01" : "00")
	cmds.each{ sendHubCommand(new physicalgraph.device.HubAction(it)) }	
}

private sendTuyaCommand(dp, dp_type, fncmd) {
	zigbee.command(CLUSTER_TUYA, SETDATA, PACKET_ID + dp + dp_type + zigbee.convertToHexString(fncmd.length()/2, 4) + fncmd )
}

private getPACKET_ID() {
	state.packetID = ((state.packetID ?: 0) + 1 ) % 65536
	zigbee.convertToHexString(state.packetID, 4)
}