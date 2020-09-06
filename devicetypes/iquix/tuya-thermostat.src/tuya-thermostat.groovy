/**
 *  Tuya Thermostat (v.0.0.1.0) - Preview Alpha
 *	Copyright 2020 Jaewon Park
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
	definition(name: "Tuya Thermostat", namespace: "iquix", author: "iquix", vid: "generic-radiator-thermostat") {
		capability "Thermostat"
		capability "Temperature Measurement"        
		capability "Refresh"
		capability "Actuator"
		capability "Sensor"

		fingerprint profileId: "0104", inClusters: "0000 0004 0005 00EF", outClusters: "0019 000A", manufacturer: "_TZE200_aoclfnxz", model: "TS0601", deviceJoinName: "Tuya Thermostat"
	}

	preferences {
	}

	tiles {
		multiAttributeTile(name:"switch", type: "generic", width: 6, height: 4){
			tileAttribute ("device.thermostatMode", key: "PRIMARY_CONTROL") {
				attributeState "heat", label:'${name}', action:"off", icon:"st.thermostat.heat", backgroundColor:"#e86d13", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"heat", icon:"st.thermostat.heating-cooling-off", backgroundColor:"#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"off", icon:"st.thermostat.heat", backgroundColor:"#e86d13", nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"heat", icon:"st.thermostat.heating-cooling-off", backgroundColor:"#ffffff", nextState:"turningOn"
			}
			tileAttribute("device.lastCheckin", key: "SECONDARY_CONTROL") {
				attributeState("default", label:'Last Update: ${currentValue}',icon: "st.Health & Wellness.health9")
			}
		}
		controlTile("temperatureControl", "device.heatingSetpoint", "slider", sliderType: "HEATING", range:"(5..40)", height: 2, width: 3) {
			state "default", action:"setHeatingSetpoint", backgroundColor: "#E86D13"
		}
		valueTile("curTemp_label", "", decoration: "flat", width: 3, height: 1) {
			state "default", label:'Current\nTemp'
		}
		valueTile("temperature", "device.temperature", decoration: "flat", width: 3, height: 1) {
			state "default", label:'${currentValue}'
		}
		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 3, height: 2) {
			state "default", label:"", action:"refresh", icon:"st.secondary.refresh"
		}
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
		if ((descMap?.clusterInt==CLUSTER_TUYA) && (descMap?.command == "01" || descMap?.command == "02")) {
			def dp = zigbee.convertHexToInt(descMap?.data[2])
			def fncmd = ""
			for (def i = 0; i < zigbee.convertHexToInt(descMap?.data[4]+descMap?.data[5]); i++) {
				fncmd += descMap?.data[i + 6]
			}
			if (dp == state.old_dp && fncmd == state.old_fncmd) {
				return
			}
			state.old_dp = dp
			state.old_fncmd = fncmd
			//log.debug descMap
			log.debug "dp=" + dp + "  fncmd=" + fncmd
			switch (dp) {
				case 0x01: // 0x01: Heat / Off
					if (fncmd == "00") {
						log.debug "mode: off"
						sendEvent(name: "thermostatMode", value: "off", displayed: true)
					} else if (fncmd == "01") {
						log.debug "mode: heat"
						sendEvent(name: "thermostatMode", value: "heat", displayed: true)
					}
					break
				case 0x10: // 0x10: Target Temperature
					def setpointValue = zigbee.convertHexToInt(fncmd)
					log.debug "target temp: "+setpointValue
					sendEvent(name: "heatingSetpoint", value: setpointValue as int, unit: "C", displayed: true)
					sendEvent(name: "coolingSetpoint", value: setpointValue as int, unit: "C", displayed: false)
					break
				case 0x18: // 0x18 : Current Temperature
					def currentTemperatureValue = zigbee.convertHexToInt(fncmd)/10
					log.debug "current temp: "+currentTemperatureValue
					sendEvent(name: "temperature", value: currentTemperatureValue, unit: "C", displayed: true)
					break
				case 0x02: // 0x02 : Scheduled Mode
					//if (fncmd == "01") {
					//	runIn(1, setManualMode)
					//}
					break
			}
		} else {
			log.debug "not parsed : "+descMap
		}
	}
}

def setThermostatMode(mode){
	log.debug "setThermostatMode("+mode+")"
	sendTuyaCommand("01", DP_TYPE_BOOL, (mode=="heat")?"01":"00")
}

def setHeatingSetpoint(temperature){
	log.debug "setHeatingSetpoint("+temperature+")"
	def settemp = temperature as int 
	settemp += (settemp != temperature && temperature > device.currentValue("heatingSetpoint")) ? 1 : 0
	log.debug "change setpoint to "+settemp
	sendTuyaCommand("10", DP_TYPE_VALUE, zigbee.convertToHexString(settemp as int, 8))
}

def setCoolingSetpoint(temperature){
	setHeatingSetpoint(temperature)
}

def heat(){
	setThermostatMode("heat")
}

def off(){
	setThermostatMode("off")
}

def on() {
	heat()
}

def setManualMode() {
	log.debug "setManualMode()"
	def cmds = sendTuyaCommand("02", DP_TYPE_ENUM, "00") + sendTuyaCommand("03", DP_TYPE_ENUM, "01")
	cmds.each{ sendHubCommand(new physicalgraph.device.HubAction(it)) }	 
}

def installed() {
	log.info "installed()"
	sendEvent(name: "supportedThermostatModes", value: JsonOutput.toJson(["heat", "off"]), displayed: false)
	sendEvent(name: "thermostatMode", value: "off", displayed: false)
	sendEvent(name: "heatingSetpoint", value: 0, unit: "C", displayed: false)
	sendEvent(name: "coolingSetpoint", value: 0, unit: "C", displayed: false)
	sendEvent(name: "temperature", value: 0, unit: "C", displayed: false)
}

def updated() {
	log.info "updated()"
}

private sendTuyaCommand(dp, dp_type, fncmd) {
	zigbee.command(CLUSTER_TUYA, SETDATA, "00" + PACKET_ID + dp + dp_type + zigbee.convertToHexString(fncmd.length()/2, 4) + fncmd )
}

private getPACKET_ID() {
	state.packetID = ((state.packetID ?: 0) + 1 ) % 256
	return zigbee.convertToHexString(state.packetID)
}