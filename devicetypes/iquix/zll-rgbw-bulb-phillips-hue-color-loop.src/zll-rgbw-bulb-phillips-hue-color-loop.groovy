/**
 *  Copyright 2017 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

import physicalgraph.zigbee.zcl.DataType

metadata {
	definition (name: "ZLL RGBW Bulb (Phillips Hue Color Loop)", namespace: "iquix", author: "iquix", ocfDeviceType: "oic.d.light", vid: "generic-rgbw-color-bulb-2000K-6500K") {

		capability "Actuator"
		capability "Color Control"
		capability "Color Temperature"
		capability "Configuration"
		capability "Polling"
		capability "Refresh"
		capability "Switch"
		capability "Switch Level"
		capability "Health Check"
		capability "Light"

		command "startLoop"
		command "stopLoop"
		command "setLoopTime"
		command "setDirection"
		
		attribute "loopActive", "string"
		attribute "loopDirection", "string"
		attribute "loopTime", "number"

		attribute "colorName", "string"

		// Philips Hue
		fingerprint profileId: "C05E", inClusters: "0000,0003,0004,0005,0006,0008,0300,1000", outClusters: "0019", manufacturer: "Philips", model: "LCT001", deviceJoinName: "Philips Hue A19"
		fingerprint profileId: "C05E", inClusters: "0000,0003,0004,0005,0006,0008,0300,1000", outClusters: "0019", manufacturer: "Philips", model: "LCT002", deviceJoinName: "Philips Hue BR30"
		fingerprint profileId: "C05E", inClusters: "0000,0003,0004,0005,0006,0008,0300,1000", outClusters: "0019", manufacturer: "Philips", model: "LCT003", deviceJoinName: "Philips Hue GU10"
		fingerprint profileId: "C05E", inClusters: "0000,0003,0004,0005,0006,0008,0300,1000", outClusters: "0019", manufacturer: "Philips", model: "LCT007", deviceJoinName: "Philips Hue A19"
		fingerprint profileId: "C05E", inClusters: "0000,0003,0004,0005,0006,0008,0300,1000", outClusters: "0019", manufacturer: "Philips", model: "LCT010", deviceJoinName: "Philips Hue A19"
		fingerprint profileId: "C05E", inClusters: "0000,0003,0004,0005,0006,0008,0300,1000", outClusters: "0019", manufacturer: "Philips", model: "LCT011", deviceJoinName: "Philips Hue BR30"
		fingerprint profileId: "C05E", inClusters: "0000,0003,0004,0005,0006,0008,0300,1000", outClusters: "0019", manufacturer: "Philips", model: "LCT012", deviceJoinName: "Philips Hue Candle"
		fingerprint profileId: "C05E", inClusters: "0000,0003,0004,0005,0006,0008,0300,1000", outClusters: "0019", manufacturer: "Philips", model: "LCT014", deviceJoinName: "Philips Hue A19"
		fingerprint profileId: "C05E", inClusters: "0000,0003,0004,0005,0006,0008,0300,1000", outClusters: "0019", manufacturer: "Philips", model: "LCT015", deviceJoinName: "Philips Hue A19"
		fingerprint profileId: "C05E", inClusters: "0000,0003,0004,0005,0006,0008,0300,1000", outClusters: "0019", manufacturer: "Philips", model: "LCT016", deviceJoinName: "Philips Hue A19"
		fingerprint profileId: "C05E", inClusters: "0000,0003,0004,0005,0006,0008,0300,1000", outClusters: "0019", manufacturer: "Philips", model: "LST001", deviceJoinName: "Philips Hue Lightstrip"
		fingerprint profileId: "C05E", inClusters: "0000,0003,0004,0005,0006,0008,0300,1000", outClusters: "0019", manufacturer: "Philips", model: "LST002", deviceJoinName: "Philips Hue Lightstrip"
		
	}

	// UI tile definitions
	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00A0DC", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00A0DC", nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
			}
			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"switch level.setLevel"
			}
			tileAttribute ("device.color", key: "COLOR_CONTROL") {
				attributeState "color", action:"color control.setColor"
			}
		}
		controlTile("colorTempSliderControl", "device.colorTemperature", "slider", width: 2, height: 2, inactiveLabel: false, range:"(2000..6500)") {
			state "colorTemperature", action:"color temperature.setColorTemperature"
		}
		valueTile("colorName", "device.colorName", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "colorName", label: '${currentValue}'
		}
		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		standardTile("loop", "device.loopActive", height: 2, width: 2, inactiveLabel: false, decoration: "flat") {
			state "Active", label:'Color Loop ${currentValue}', action: "stopLoop", backgroundColor: "#79b821", nextState: "stoppingLoop"
			state "startingLoop", label: "Starting Loop", action: "stopLoop", backgroundColor: "#79b821", nextState: "stoppingLoop"
			state "Inactive", label:'Color Loop ${currentValue}', action: "startLoop", backgroundColor: "#ffffff", nextState: "startingLoop"
			state "stoppingLoop", label: "Stopping Loop", action: "startLoop", backgroundColor: "#ffffff", nextState: "startingLoop"
		}
		controlTile("loopTimeControl", "device.loopTime", "slider", height: 2, width: 2, range: "(1..60)", inactiveLabel: false) {
			state "loopTime", action: "setLoopTime"
		}
		standardTile("loopDir", "device.loopDirection", height: 2, width: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label: 'Color Loop Direction ${currentValue}', action: "setDirection"
		}

		main(["switch"])
		details(["switch", "colorTempSliderControl", "colorName", "refresh", "loop", "loopTimeControl", "loopDir"])
	}
}

//Globals
private getATTRIBUTE_HUE() { 0x0000 }
private getATTRIBUTE_SATURATION() { 0x0001 }
private getHUE_COMMAND() { 0x00 }
private getSATURATION_COMMAND() { 0x03 }
private getMOVE_TO_HUE_AND_SATURATION_COMMAND() { 0x06 }
private getCOLOR_CONTROL_CLUSTER() { 0x0300 }
private getATTRIBUTE_COLOR_TEMPERATURE() { 0x0007 }
private getMOVE_TO_COLOR_TEMPERATURE_COMMAND() { 0x0A }
private getCOLOR_LOOP_SET_COMMAND() { 0x44 }
private getATTRIBUTE_COLOR_LOOP_ACTIVE() { 0x4002 }

// Parse incoming device messages to generate events
def parse(String description) {
	log.debug "description is $description"

	def event = zigbee.getEvent(description)
	if (event) {
		log.debug event
		if (event.name == "level" && event.value == 0) {}
		else {
			if (event.name == "colorTemperature") {
				event.value = Math.max(Math.min(6500, event.value),2000)
				setGenericName(event.value)
			}
			sendEvent(event)
		}
	}
	else {
		def zigbeeMap = zigbee.parseDescriptionAsMap(description)
		log.trace "zigbeeMap : $zigbeeMap"

		if (zigbeeMap?.clusterInt == COLOR_CONTROL_CLUSTER && zigbeeMap.value != null) {
			if(zigbeeMap.attrInt == ATTRIBUTE_HUE){  //Hue Attribute
				def hueValue = Math.round(zigbee.convertHexToInt(zigbeeMap.value) / 0xfe * 100)
				sendEvent(name: "hue", value: hueValue, displayed:false)
			}
			else if(zigbeeMap.attrInt == ATTRIBUTE_SATURATION){ //Saturation Attribute
				def saturationValue = Math.round(zigbee.convertHexToInt(zigbeeMap.value) / 0xfe * 100)
				sendEvent(name: "saturation", value: saturationValue, displayed:false)
			}
			else if(zigbeeMap.attrInt == ATTRIBUTE_COLOR_LOOP_ACTIVE ){ // Color Loop
			   	sendEvent(name: "loopActive", value: (zigbeeMap.value == "00") ? "Inactive" : "Active")
			}
		}
		else {
			log.info "DID NOT PARSE MESSAGE for description : $description"
		}
	}
}

def on() {
	zigbee.on() + ["delay 1500"] + zigbee.onOffRefresh()
}

def off() {
	zigbee.off() + ["delay 1500"] + zigbee.onOffRefresh() + zigbee.readAttribute(COLOR_CONTROL_CLUSTER, ATTRIBUTE_COLOR_LOOP_ACTIVE)
}

def refresh() {
	refreshAttributes() + configureAttributes()
}

def poll() {
	configureHealthCheck()

	refreshAttributes()
}

def ping() {
	refreshAttributes()
}

def healthPoll() {
	log.debug "healthPoll()"
	def cmds = refreshAttributes()
	cmds.each{ sendHubCommand(new physicalgraph.device.HubAction(it))}
}

def configureHealthCheck() {
	if (!state.hasConfiguredHealthCheck) {
		log.debug "Configuring Health Check, Reporting"
		unschedule("healthPoll", [forceForLocallyExecuting: true])
		runEvery5Minutes("healthPoll", [forceForLocallyExecuting: true])
		state.hasConfiguredHealthCheck = true
	}
}

def configure() {
	log.debug "Configuring Reporting and Bindings."
	configureAttributes() + refreshAttributes()

	sendEvent(name: "loopActive", value: "Inactive")
	sendEvent(name: "loopDirection", value: "Up")
	sendEvent(name: "loopTime", value: "20")
}

def configureAttributes() {
	zigbee.onOffConfig() +
	zigbee.levelConfig()
}

def refreshAttributes() {
	zigbee.onOffRefresh() +
	zigbee.levelRefresh() +
	zigbee.colorTemperatureRefresh() +
	zigbee.readAttribute(COLOR_CONTROL_CLUSTER, ATTRIBUTE_HUE) +
	zigbee.readAttribute(COLOR_CONTROL_CLUSTER, ATTRIBUTE_SATURATION) +
	zigbee.readAttribute(COLOR_CONTROL_CLUSTER, ATTRIBUTE_COLOR_LOOP_ACTIVE)
}

def updated() {
	sendEvent(name: "checkInterval", value: 2 * 10 * 60 + 1 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
	configureHealthCheck()
}

def installed() {
	sendEvent(name: "checkInterval", value: 2 * 10 * 60 + 1 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
	configureHealthCheck()
}

def setColorTemperature(value) {
	value = value as Integer
	def tempInMired = Math.round(1000000 / value)
	def finalHex = zigbee.swapEndianHex(zigbee.convertToHexString(tempInMired, 4))
	
	def cmds = []
	cmds += zigbee.command(COLOR_CONTROL_CLUSTER, MOVE_TO_COLOR_TEMPERATURE_COMMAND, "$finalHex 1800") +	
		   ["delay 1500"] +
		   zigbee.readAttribute(COLOR_CONTROL_CLUSTER, ATTRIBUTE_COLOR_TEMPERATURE)
	if (device.currentValue("loopActive") == "Active") {
		cmds = zigbee.command(COLOR_CONTROL_CLUSTER, COLOR_LOOP_SET_COMMAND, "01 00 00 0000 0000") + cmds + zigbee.readAttribute(COLOR_CONTROL_CLUSTER, ATTRIBUTE_COLOR_LOOP_ACTIVE)
	}
	cmds
}

def setLevel(value, rate = null) {
	zigbee.setLevel(value) + zigbee.onOffRefresh() + zigbee.levelRefresh() //adding refresh because of ZLL bulb not conforming to send-me-a-report
}

private getScaledHue(value) {
	zigbee.convertToHexString(Math.round(value * 0xfe / 100.0), 2)
}

private getScaledSaturation(value) {
	zigbee.convertToHexString(Math.round(value * 0xfe / 100.0), 2)
}

def setColor(value){
	log.trace "setColor($value)"

	def cmds = []
	cmds += zigbee.command(COLOR_CONTROL_CLUSTER, MOVE_TO_HUE_AND_SATURATION_COMMAND,
		  getScaledHue(value.hue), getScaledSaturation(value.saturation), "1800") +
		  zigbee.readAttribute(COLOR_CONTROL_CLUSTER, ATTRIBUTE_HUE) +
		  zigbee.readAttribute(COLOR_CONTROL_CLUSTER, ATTRIBUTE_SATURATION) +
		  zigbee.onOffRefresh()
	if (device.currentValue("loopActive") == "Active") {
		cmds = zigbee.command(COLOR_CONTROL_CLUSTER, COLOR_LOOP_SET_COMMAND, "01 00 00 0000 0000") + cmds + zigbee.readAttribute(COLOR_CONTROL_CLUSTER, ATTRIBUTE_COLOR_LOOP_ACTIVE)
	}
	cmds
}

//Naming based on the wiki article here: http://en.wikipedia.org/wiki/Color_temperature
def setGenericName(value){
	if (value != null) {
		def genericName = "White"
		if (value < 3300) {
			genericName = "Soft White"
		} else if (value < 4150) {
			genericName = "Moonlight"
		} else if (value <= 5000) {
			genericName = "Cool White"
		} else if (value >= 5000) {
			genericName = "Daylight"
		}
		sendEvent(name: "colorName", value: genericName)
	}
}

def setHue(value) {
	//payload-> hue value, direction (00-> shortest distance), transition time (1/10th second)
	zigbee.command(COLOR_CONTROL_CLUSTER, HUE_COMMAND, getScaledHue(value), "00", "1800") +
	zigbee.readAttribute(COLOR_CONTROL_CLUSTER, ATTRIBUTE_HUE)
}

def setSaturation(value) {
	//payload-> sat value, transition time
	zigbee.command(COLOR_CONTROL_CLUSTER, SATURATION_COMMAND, getScaledSaturation(value), "1800") +
	zigbee.readAttribute(COLOR_CONTROL_CLUSTER, ATTRIBUTE_SATURATION)
}

//-- Loop functions---------------------------------

def setDirection() {
	def direction = (device.currentValue("loopDirection") == "Down" ? "Up" : "Down")
	sendEvent(name: "loopDirection", value: direction)
	if (device.currentValue("loopActive") == "Active") {
		def dirHex = (direction == "Down" ? "00" : "01")
		zigbee.command(COLOR_CONTROL_CLUSTER, COLOR_LOOP_SET_COMMAND, "02 01 ${dirHex} 0000 0000")
	}
}

def setLoopTime(value) {
	sendEvent(name:"loopTime", value: value)
	if (device.currentValue("loopActive") == "Active") {
		def finTime = zigbee.swapEndianHex(zigbee.convertToHexString(value, 4))
		zigbee.command(COLOR_CONTROL_CLUSTER, COLOR_LOOP_SET_COMMAND, "04 01 00 ${finTime} 0000")
	}
}

def startLoop(Map params) {
	// direction either increments or decrements the hue value: "Up" will increment, "Down" will decrement
	def direction = (params?.direction != null) ? params.direction : device.currentValue("loopDirection")
    sendEvent(name: "loopDirection", value: direction)
    def dirHex = direction == "Down" ? "00" : "01"
	
	// time parameter is the time in seconds for a full loop
	def cycle = (params?.time != null) ? params.time : device.currentValue("loopTime")
    sendEvent(name:"loopTime", value: cycle)
	def finTime = zigbee.swapEndianHex(zigbee.convertToHexString(cycle, 4))
	
	def cmds = []
    
	cmds += zigbee.on()	
	if (params?.hue != null) {  
    	log.debug "activating color loop from specified hue"
		def sHue = Math.min(Math.round(params.hue * 255 / 100), 255)
		def finHue = zigbee.swapEndianHex(zigbee.convertToHexString(sHue, 4))
		cmds += zigbee.command(COLOR_CONTROL_CLUSTER, COLOR_LOOP_SET_COMMAND, "0F 01 ${dirHex} ${finTime} ${finHue}")
	} else {
		log.debug "activating color loop from current hue"
		cmds += zigbee.command(COLOR_CONTROL_CLUSTER, COLOR_LOOP_SET_COMMAND, "07 02 ${dirHex} ${finTime} 0000")
	}
	cmds += zigbee.readAttribute(COLOR_CONTROL_CLUSTER, ATTRIBUTE_COLOR_LOOP_ACTIVE)+ zigbee.onOffRefresh()
	cmds
}

def stopLoop() {
	log.debug "deactivating color loop"
	
	zigbee.command(COLOR_CONTROL_CLUSTER, COLOR_LOOP_SET_COMMAND, "01 00 00 0000 0000") + 
	zigbee.readAttribute(COLOR_CONTROL_CLUSTER, ATTRIBUTE_COLOR_LOOP_ACTIVE) + 
	zigbee.onOffRefresh() + 
	zigbee.readAttribute(COLOR_CONTROL_CLUSTER, ATTRIBUTE_HUE) + zigbee.readAttribute(COLOR_CONTROL_CLUSTER, ATTRIBUTE_SATURATION)
}