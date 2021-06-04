/**
 *  Power Trigger Switch 0.3.11
 *	Copyright 2020-2021 Jaewon Park (iquix)
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *	in compliance with the License. You may obtain a copy of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *	on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *	for the specific language governing permissions and limitations under the License.
 *
 */

import groovy.json.JsonOutput

metadata {
	definition (name: "Power Trigger Switch", namespace: "iquix", author: "iquix", mnmn: "SmartThingsCommunity", vid: "20db7582-cf21-33d2-a3e7-9a16e12d9426", ocfDeviceType: "oic.d.switch") { 
		capability "Actuator"
		capability "Configuration"
		capability "Refresh"
		capability "Switch"
		capability "Button"
		capability "Power Meter"
		capability "Health Check"
	}

	preferences {
		input name: "onThreshold", title:"On Threshold Power (W)", type: "number", required: true, defaultValue: 15, range: "1..9999"
		input name: "onDuration", title:"On Threshold Duration (sec)", type: "number", required: true, defaultValue: 15, range: "0..9999"
		input name: "offThreshold", title:"Off Threshold Power (W)", type: "number", required: true, defaultValue: 1, range: "1..9999"
		input name: "offDuration", title:"Off Threshold Duration (sec)", type: "number", required: true, defaultValue: 5, range: "0..9999"
		input name: "eventOptionValue", type: "enum", title: "(Optional) When to fire events that are triggered by On/Off commands?", options:["0": "Only for state changes (Default)" , "1": "Always fire events for every command"], defaultValue: "0"
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', icon:"st.switches.switch.on", backgroundColor:"#00A0DC"
				attributeState "off", label:'${name}', icon:"st.switches.switch.off", backgroundColor:"#ffffff"
			}
			tileAttribute ("power", key: "SECONDARY_CONTROL") {
				attributeState "power", label:'${currentValue} W'
			}
		}
		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		main "switch"
		details(["switch", "refresh"])
	}
}

// Parse incoming device messages to generate events
def parse(String description) {
	//log.debug "description is $description"
	def event = zigbee.getEvent(description)
	if (event) {
		if (event.name == "power") {
			def powerValue
			def div = device.getDataValue("divisor")
			div = div ? (div as int) : 10
			powerValue = (event.value as Integer)/div
			state.power = powerValue
			sendEvent(name: "power", value: powerValue, displayed: true)
			processPower()
		}
		else if (event.name == "switch") {
			"zigbee plug is turned "+event.value
			if (event.value == "off") {
				runIn(1, turnPlugOn)
			}
		}
		else {
			log.debug "sendEvent : " + event
			sendEvent(event)
		}
	}
	/*else {
		log.warn "DID NOT PARSE MESSAGE for description : $description"
		log.debug zigbee.parseDescriptionAsMap(description)
	}*/
}

def processPower() {
	def p = state.power
	def onThresholdVal = onThreshold ?: 15
	def offThresholdVal = offThreshold ?: 1
	def onDurationVal = onDuration ?: 15
	def offDurationVal = offDuration ?: 5
	
	//log.debug "processPower() : Power:{${p}}  Switch:{${state.switch}}  OnThreshold:{${onThresholdVal}} OffThreshold:{${offThresholdVal}}"
	
	if (p >= onThresholdVal && state.switch == "off") {
		log.debug "processPower() OnTrigger : Power{${p}} >= OnThreshold{${onThresholdVal}} at ${now()}. On trigger start time = ${state.onTime}"
		if (state.onTime == 0 && onDurationVal > 1) {
			state.onTime = now()
			if (isPolling) {
				runIn(onDurationVal, powerRefresh, [overwrite: false])
			} else {
				runIn(onDurationVal+1, processPower)
			}
		} else if (now() - state.onTime >= (onDurationVal-1)*1000) {
			log.debug "Setting switch status to on"
			state.switch = "on"
			sendEvent(name: "switch", value: "on", displayed: true)
			state.onTime = 0
			state.offTime = 0
		}
	} else {
		state.onTime = 0
	}

	if (p <= offThresholdVal && state.switch == "on") {
		log.debug "processPower() OffTrigger : Power{${p}} <= OffThreshold{${onThresholdVal}} at ${now()}. Off trigger start time = ${state.offTime}"
		if (state.offTime == 0 && offDurationVal > 1) {
			state.offTime = now()
			if (isPolling) {
				runIn(offDurationVal, powerRefresh, [overwrite: false])
			} else {
				runIn(offDurationVal+1, processPower)
			}
		} else if (now() - state.offTime >= (offDurationVal-1)*1000) {
			log.debug "Setting switch status to off"
			state.switch = "off"
			sendEvent(name: "switch", value: "off", displayed: true)
			state.onTime = 0
			state.offTime = 0
		}
	} else {
		state.offTime = 0
	}
}	

def off() {
	sendEvent(name: "switch", value: state.switch)
	if (eventOption != "0" || state.switch=="on") {
		sendEvent(name: "button", value: "held", displayed: false, isStateChange: true)
	}
}

def on() {
	sendEvent(name: "switch", value: state.switch)
	if (eventOption != "0" || state.switch=="off") {
		sendEvent(name: "button", value: "pushed", displayed: false, isStateChange: true)
	}
}

def refresh() {
	log.debug "refresh() called"
	Integer reportIntervalMinutes = 5
	def cmds = zigbee.onOffRefresh() + zigbee.simpleMeteringPowerRefresh() + zigbee.electricMeasurementPowerRefresh()
	if (device.getDataValue("manufacturer") == "Jasco Products") {
		// Some versions of hub firmware will incorrectly remove this binding causing manual control of switch to stop working
		// This needs to be the first binding table entry because the device will automatically write this entry each time it restarts
		cmds += ["zdo bind 0x${device.deviceNetworkId} 2 1 0x0006 {${device.zigbeeId}} {${device.zigbeeId}}", "delay 2000"]
	}
	if (device.getDataValue("divisor") == "1") {
		cmds + zigbee.onOffConfig(0, reportIntervalMinutes * 60) + zigbee.simpleMeteringPowerConfig(1, 600, 0x02) + zigbee.electricMeasurementPowerConfig(1, 600, 0x0002)
	} else {
		cmds + zigbee.onOffConfig(0, reportIntervalMinutes * 60) + zigbee.simpleMeteringPowerConfig(1, 600, 0x14) + zigbee.electricMeasurementPowerConfig(1, 600, 0x0014)
	}
}

def configure() {
	log.debug "in configure()"
	if ((device.getDataValue("manufacturer") == "Develco Products A/S") || (device.getDataValue("manufacturer") == "Aurora"))  {
		device.updateDataValue("divisor", "1")
	}
	if ((device.getDataValue("manufacturer") == "SALUS") || (device.getDataValue("manufacturer") == "DAWON_DNS") || (device.getDataValue("model") == "TS0121") || (device.getDataValue("model") == "TS011F"))  {
		device.updateDataValue("divisor", "1")
	}
	if ((device.getDataValue("manufacturer") == "LDS") || (device.getDataValue("manufacturer") == "REXENSE") || (device.getDataValue("manufacturer") == "frient A/S"))  {
		device.updateDataValue("divisor", "1")
	}
	if (isPolling) {
		unschedule()
		runEvery1Minute(powerRefresh)
	}
	return configureHealthCheck()
}

def configureHealthCheck() {
	Integer hcIntervalMinutes = 12
	sendEvent(name: "checkInterval", value: hcIntervalMinutes * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
	return refresh()
}

def installed() {
	log.debug "in installed()"
	state.onTime = 0
	state.offTime = 0
	state.switch="off"
	sendEvent(name: "switch", value: "off", displayed: false)
	sendEvent(name: "button", value: "pushed", displayed: false, isStateChange: false)
	sendEvent(name: "supportedButtonValues", value: ["pushed", "held"].encodeAsJSON(), displayed: false)
}


def updated() {
	log.debug "in updated()"
	// updated() doesn't have it's return value processed as hub commands, so we have to send them explicitly
	def cmds = configure()
	cmds.each{ sendHubCommand(new physicalgraph.device.HubAction(it)) }
}

def ping() {
	return zigbee.onOffRefresh() + zigbee.simpleMeteringPowerRefresh() + zigbee.electricMeasurementPowerRefresh()
}

def turnPlugOn() {
	log.debug "turning on the zigbee plug"
	def cmds = zigbee.on()
	cmds.each{ sendHubCommand(new physicalgraph.device.HubAction(it)) } 
}

def powerRefresh() {
	def cmds = zigbee.electricMeasurementPowerRefresh()
	cmds.each{ sendHubCommand(new physicalgraph.device.HubAction(it)) }
}

private getIsPolling() {
	return (device.getDataValue("model") == "TS0121" && device.getDataValue("manufacturer") != "_TZ3000_8nkb7mof")
}

private getEventOption() {
	return eventOptionValue ?: "0"
}