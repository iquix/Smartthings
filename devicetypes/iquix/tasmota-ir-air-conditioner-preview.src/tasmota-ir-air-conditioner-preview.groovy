/**
 *  Tasmota IR Air Conditioner 0.0.1.1 (alpha preview)
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
 *
 */
metadata {
	definition (name: "Tasmota IR Air Conditioner (Preview)", namespace: "iquix", author: "iquix", ocfDeviceType: "oic.d.airconditioner") { 
		capability "Actuator"
		capability "Configuration"
		capability "Refresh"
		capability "Switch"
		capability "Thermostat Cooling Setpoint"
		capability "Air Conditioner Mode"
		capability "Air Conditioner Fan Mode"
		capability "Health Check"
		//attribute "power", "number"
	}

	//fingerprint profileId: "0104", deviceId: "0051", inClusters: "0000 0003 0004 0006 0009 0702 0B04", outClusters: "0000 0003 0004 0006 0009 0702 0B04", manufacturer: "Heiman", model: "SmartPlug", deviceJoinName: "에어컨" // fingerprint of Heiman 16A plug

	preferences {
		input name: "TasmotaIP", title:"local IP address of Tasmota IR", type: "string", required: true
		input name: "username", title:"Username of Tasmota IR", type: "string"
		input name: "password", title:"Password of Tasmota IR", type: "string"
		input name: "ACvendor", title:"Vendor string of Air Conditioner", options: ["SAMSUNG_AC", "LG", "LG2", "COOLIX", "DAIKIN", "KELVINATOR", "MITSUBISHI_AC", "GREE", "ARGO", "TROTEC", "TOSHIBA_AC", "FUJITSU_AC", "MIDEA", "HAIER_AC", "HITACHI_AC", "HAIER_AC_YRW02", "WHIRLPOOL_AC", "ELECTRA_AC", "PANASONIC_AC", "DAIKIN2", "VESTEL_AC", "TECO", "TCL112AC", "MITSUBISHI_HEAVY_88", "MITSUBISHI_HEAVY_152", "DAIKIN216", "SHARP_AC", "GOODWEATHER", "DAIKIN160", "NEOCLIMA", "DAIKIN176", "DAIKIN128"], type: "enum", required: true, defaultValue: "SAMSUNG_AC"
		input name: "offThreshold", title:"Off Threshold Power (W)", type: "number", required: true, defaultValue: 3.9
		input name: "onThreshold", title:"On Threshold Power (W)", type: "number", required: true, defaultValue: 4
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
	log.debug "description is $description"
	def event = zigbee.getEvent(description)
	if (event) {
		if (event.name == "power") {
			def powerValue
			def div = device.getDataValue("divisor")
			div = div ? (div as int) : 10
			powerValue = (event.value as Integer)/div
			//sendEvent(name: "power", value: powerValue, displayed: false)
			processPower(powerValue)
		}
		else if (event.name == "switch") {
			log.debug "zigbee plug is turned "+event.value
			if (event.value == "off") {
				runIn(1, turnPlugOn)
			}
		}
		else {
			log.debug "sendEvent : " + event
			sendEvent(event)
		}
	}
	else {
		log.warn "DID NOT PARSE MESSAGE for description : $description"
		log.debug zigbee.parseDescriptionAsMap(description)
	}
}

def processPower(p) {
	def onThresholdVal = (onThreshold==null)? 4 : onThreshold
	def offThresholdVal = (offThreshold==null)? 3.9 : offThreshold
	
	log.debug "--processPower() : Power:{${p}} OnThreshold:{${onThresholdVal}} OffThreshold:{${offThresholdVal}}"
	
	if (p >= onThresholdVal && state.switch == "off") {
		log.debug "	switch off-->on"
		sendEvent(name: "switch", value: "on", displayed: true)
		state.switch="on"
		sendEvent(name: "airConditionerMode", value: "auto", displayed: true)
	}
	if (p <= offThresholdVal && state.switch == "on") {
		log.debug "	switch on-->off"
		sendEvent(name: "switch", value: "off", displayed: true)
		
		state.switch="off"
	}
}	

def off() {
	sendTasmota('IRhvac {"Vendor":"'+VENDOR+'", "Power":"Off"}')
}

def on() {
	//sendTasmota('IRhvac {"Vendor":"'+VENDOR+'", "Power":"On","Mode":"'+device.currentValue("airConditionerMode")+'","FanSpeed":"Auto","Temp":"'+device.currentValue("coolingSetpoint")+'"}')
	sendTasmota('IRhvac {"Vendor":"'+VENDOR+'", "Power":"On","Mode":"Auto","FanSpeed":"'+FANMODE+'","Temp":"'+device.currentValue("coolingSetpoint")+'"}')
	sendEvent(name: "airConditionerMode", value: "auto", displayed: true)
}

def setCoolingSetpoint(temperature){
	if (state.switch=="on") {
		sendTasmota('IRhvac {"Vendor":"'+VENDOR+'", "Power":"On","Mode":"Cool","FanSpeed":"'+FANMODE+'","Temp":"'+temperature+'"}')
		sendEvent(name: "airConditionerMode", value: "cool", displayed: true)
	}
	sendEvent(name: "coolingSetpoint", value: temperature as int, unit: "C", displayed: true)
}

def setAirConditionerMode(mode) {
	if (mode!="cool" && mode!="auto" && mode!="fanOnly" && mode!="dry") {
		sendEvent(name: "airConditionerMode", value: "auto", displayed: true)
		return
	}
	if (state.switch=="on") {
		sendTasmota('IRhvac {"Vendor":"'+VENDOR+'", "Power":"On","Mode":"'+mode+'","FanSpeed":"'+FANMODE+'","Temp":"'+device.currentValue("coolingSetpoint")+'"}')
	}
	sendEvent(name: "airConditionerMode", value: mode, displayed: true)
}

def setFanMode(mode) {
	if (mode!="low" && mode!="medium" && mode!="high" && mode!="auto") {
		sendEvent(name: "fanMode", value: "auto", displayed: true)
	} else {
		sendEvent(name: "fanMode", value: mode, displayed: true)
	}
	if (state.switch=="on") {
		sendTasmota('IRhvac {"Vendor":"'+VENDOR+'", "Power":"On","Mode":"'+device.currentValue("airConditionerMode")+'","FanSpeed":"'+FANMODE+'","Temp":"'+device.currentValue("coolingSetpoint")+'"}')
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
		cmds += zigbee.onOffConfig(0, reportIntervalMinutes * 60) + zigbee.simpleMeteringPowerConfig(1, 600, 0x01) + zigbee.electricMeasurementPowerConfig(1, 600, 0x0001)
	} else {
		cmds += zigbee.onOffConfig(0, reportIntervalMinutes * 60) + zigbee.simpleMeteringPowerConfig() + zigbee.electricMeasurementPowerConfig()
	}
	log.debug cmds
	return cmds
}

def configure() {
	log.debug "in configure()"
	if ((device.getDataValue("manufacturer") == "Develco Products A/S") || (device.getDataValue("manufacturer") == "Aurora"))  {
		device.updateDataValue("divisor", "1")
	}
	if ((device.getDataValue("manufacturer") == "SALUS") || (device.getDataValue("manufacturer") == "DAWON_DNS"))  {
		device.updateDataValue("divisor", "1")
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
	state.switch="off"
	sendEvent(name: "switch", value: "off", displayed: true)
	sendEvent(name: "coolingSetpoint", value: 27, unit: "C")
	sendEvent(name: "supportedAcModes", value:["auto", "cool","dry","fanOnly"])
	sendEvent(name: "supportedAcFanModes", value:["auto", "low", "medium", "high", "turbo"])
	sendEvent(name: "airConditionerMode", value: "auto", displayed: false)
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
	log.debug "Automatically turning on the zigbee plug"
	def cmds = zigbee.on()
	cmds.each{ sendHubCommand(new physicalgraph.device.HubAction(it)) } 
}

def sendTasmota(command) {
	def options = [
		method: "GET",
		headers: [HOST: settings.TasmotaIP+":80"],
		path: "/cm?user=" + (settings.username ?: "") + "&password=" + (settings.password ?: "") + "&cmnd=" + URLEncoder.encode(command, "UTF-8").replaceAll(/\+/,'%20')
	]
	log.debug options
	def hubAction = new physicalgraph.device.HubAction(options, null)
	sendHubCommand(hubAction)
}

def getVENDOR() {
	return (settings.ACvendor ?: "SAMSUNG_AC")
}

def getFANMODE() {
	return (device.currentValue("fanMode") == "turbo" ? "max" : device.currentValue("fanMode"))
}