/**
 *  Copyright 2017, 2021 SmartThings, iquix
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
import physicalgraph.zigbee.zcl.DataType

metadata {
	definition (name: "ZLL RGBW Bulb (Phillips Hue Transition Patched)", namespace: "iquix", author: "SmartThings/iquix", ocfDeviceType: "oic.d.light", mnmn: "SmartThings", vid: "SmartThings-smartthings-ZLL_RGBW_Bulb") {

		capability "Actuator"
		capability "Color Control"
		capability "Color Temperature"
		capability "Configuration"
		capability "Refresh"
		capability "Switch"
		capability "Switch Level"
		capability "Health Check"
		capability "Light"

		attribute "colorName", "string"
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
		controlTile("colorTempSliderControl", "device.colorTemperature", "slider", width: 4, height: 2, inactiveLabel: false, range:"(2700..6500)") {
			state "colorTemperature", action:"color temperature.setColorTemperature"
		}
		valueTile("colorName", "device.colorName", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "colorName", label: '${currentValue}'
		}
		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		main(["switch"])
		details(["switch", "colorTempSliderControl", "colorName", "refresh"])
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
private getMOVE_LEVEL_ONOFF_COMMAND() { 0x04 }

// Parse incoming device messages to generate events
def parse(String description) {
	log.debug "description is $description"

	def event = zigbee.getEvent(description)
	if (event) {
		log.debug event
		if (event.name == "level" && event.value == 0) {}
		else {
			if (event.name == "colorTemperature") {
				setGenericName(event.value)
			}
			sendEvent(event)
		}
	}
	else {
		def zigbeeMap = zigbee.parseDescriptionAsMap(description)
		def cluster = zigbee.parse(description)

		if (zigbeeMap?.clusterInt == COLOR_CONTROL_CLUSTER) {
			if(zigbeeMap.attrInt == ATTRIBUTE_HUE){  //Hue Attribute
				state.hueValue = Math.round(zigbee.convertHexToInt(zigbeeMap.value) / 0xfe * 100)
				runIn(5, updateColor, [overwrite: true])
			}
			else if(zigbeeMap.attrInt == ATTRIBUTE_SATURATION){ //Saturation Attribute
				state.saturationValue = Math.round(zigbee.convertHexToInt(zigbeeMap.value) / 0xfe * 100)
				runIn(5, updateColor, [overwrite: true])
			}
		}
		else if (cluster && cluster.clusterId == 0x0006 && cluster.command == 0x07) {
			if (cluster.data[0] == 0x00){
				log.debug "ON/OFF REPORTING CONFIG RESPONSE: " + cluster
				sendEvent(name: "checkInterval", value: 60 * 12, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
			}
			else {
				log.warn "ON/OFF REPORTING CONFIG FAILED- error code:${cluster.data[0]}"
			}
		}
		else {
			log.info "DID NOT PARSE MESSAGE for description : $description"
			log.debug zigbeeMap
		}
	}
}

def updateColor() {
	sendEvent(name: "hue", value: state.hueValue, descriptionText: "Color has changed")
	sendEvent(name: "saturation", value: state.saturationValue, descriptionText: "Color has changed", displayed: false)
}

def on() {
	zigbee.command(zigbee.LEVEL_CONTROL_CLUSTER, MOVE_LEVEL_ONOFF_COMMAND, zigbee.convertToHexString(((state.lastLevel?:100)/100*0xff) as Integer, 2), "1E00") + ["delay 1500"] + zigbee.onOffRefresh() + ["delay 2500"] + zigbee.levelRefresh()
}

def off() {
	state.lastLevel = device.currentValue("level")
	if (state.lastLevel == 1) {
		zigbee.off() + ["delay 1500"] + zigbee.onOffRefresh()
	} else {
		zigbee.command(zigbee.LEVEL_CONTROL_CLUSTER, MOVE_LEVEL_ONOFF_COMMAND, "00", "4000") + ["delay 7400"] + zigbee.onOffRefresh()
	}
}
/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
	return zigbee.onOffRefresh()
}

def refresh() {
	zigbee.onOffRefresh() +
	zigbee.levelRefresh() +
	zigbee.colorTemperatureRefresh() +
	zigbee.hueSaturationRefresh()
}

def configure() {
	log.debug "Configuring Reporting and Bindings."
	// Device-Watch allows 2 check-in misses from device + ping (plus 1 min lag time)
	// enrolls with default periodic reporting until newer 5 min interval is confirmed
	sendEvent(name: "checkInterval", value: 2 * 10 * 60 + 1 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
	state.lastLevel = 100

	def cmds = []
	if(device.currentState("colorTemperature")?.value == null) {
		cmds +=	zigbee.setColorTemperature(5000)
	}

	cmds += refresh() +
	// OnOff, level minReportTime 2 seconds, maxReportTime 5 min. Reporting interval if no activity
	zigbee.onOffConfig() +
	zigbee.levelConfig(2, 3600, 0x01)  // +
	//zigbee.writeAttribute(zigbee.LEVEL_CONTROL_CLUSTER, 0x0010, DataType.UINT16, 0x001E) + zigbee.writeAttribute(zigbee.LEVEL_CONTROL_CLUSTER, 0x0012, DataType.UINT16, 0x001E) + zigbee.writeAttribute(zigbee.LEVEL_CONTROL_CLUSTER, 0x0013, DataType.UINT16, 0x001E)
	cmds
}

def setColorTemperature(value) {
	value = value as Integer
	def tempInMired = Math.round(1000000 / value)
	def finalHex = zigbee.swapEndianHex(zigbee.convertToHexString(tempInMired, 4))

	zigbee.command(COLOR_CONTROL_CLUSTER, MOVE_TO_COLOR_TEMPERATURE_COMMAND, finalHex, "1E00") + 
	["delay 4000"] + zigbee.colorTemperatureRefresh()
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

def setLevel(value, rate = null) {
	zigbee.command(zigbee.LEVEL_CONTROL_CLUSTER, MOVE_LEVEL_ONOFF_COMMAND, zigbee.convertToHexString((value/100*0xff) as Integer, 2), "1E00") + ["delay 4000"] + zigbee.onOffRefresh() + zigbee.levelRefresh()
}

private getScaledHue(value) {
	zigbee.convertToHexString(Math.round(value * 0xfe / 100.0), 2)
}

private getScaledSaturation(value) {
	zigbee.convertToHexString(Math.round(value * 0xfe / 100.0), 2)
}

def setColor(value){
	log.trace "setColor($value)"
	zigbee.on() +
	zigbee.command(COLOR_CONTROL_CLUSTER, MOVE_TO_HUE_AND_SATURATION_COMMAND,
		getScaledHue(value.hue), getScaledSaturation(value.saturation), "1E00") +
	["delay 4000"] + zigbee.hueSaturationRefresh()
}

def setHue(value) {
	zigbee.command(COLOR_CONTROL_CLUSTER, HUE_COMMAND, getScaledHue(value), "00", "1E00") +
	["delay 4000"] + zigbee.readAttribute(COLOR_CONTROL_CLUSTER, ATTRIBUTE_HUE)
}

def setSaturation(value) {
	zigbee.command(COLOR_CONTROL_CLUSTER, SATURATION_COMMAND, getScaledSaturation(value), "1E00") +
	["delay 4000"] + zigbee.readAttribute(COLOR_CONTROL_CLUSTER, ATTRIBUTE_SATURATION)
}

def installed() {
	if (((device.getDataValue("manufacturer") == "MRVL") && (device.getDataValue("model") == "MZ100")) || (device.getDataValue("manufacturer") == "OSRAM SYLVANIA") || (device.getDataValue("manufacturer") == "OSRAM")) {
		if ((device.currentState("level")?.value == null) || (device.currentState("level")?.value == 0)) {
			sendEvent(name: "level", value: 100)
		}
	} else if (isTintBulb()) {
		sendHubCommand(zigbee.command(COLOR_CONTROL_CLUSTER, MOVE_TO_HUE_AND_SATURATION_COMMAND, getScaledHue(0), getScaledSaturation(0), "0000"))
	}
}

private boolean isTintBulb() {
	device.getDataValue("model") == "ZBT-ExtendedColor"
}