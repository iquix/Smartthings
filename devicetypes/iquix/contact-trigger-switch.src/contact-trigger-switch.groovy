/**
 *  Contact Trigger Switch 0.0.5
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

import physicalgraph.zigbee.clusters.iaszone.ZoneStatus
import physicalgraph.zigbee.zcl.DataType
import groovy.json.JsonOutput

metadata {
	definition (name: "Contact Trigger Switch", namespace: "iquix", author: "iquix", mnmn: "SmartThingsCommunity", vid: "1404f634-52a1-31b6-a6aa-09d114a92980", ocfDeviceType: "oic.d.switch") { 
		capability "Actuator"
		capability "Configuration"
		capability "Refresh"
		capability "Switch"
		capability "Contact Sensor"
		capability "Battery"
		capability "Button"
		capability "Health Check"
	}

	preferences {
		input name: "openDuration", title:"Open Threshold Duration (sec)", type: "number", required: true, defaultValue: 0, range: "0..9999"
		input name: "closeDuration", title:"Close Threshold Duration (sec)", type: "number", required: true, defaultValue: 0, range: "0..9999"
		input name: "eventOptionValue", type: "enum", title: "(Optional) When to fire events that are triggered by On/Off commands?", options:["0": "Only for state changes (Default)" , "1": "Always fire events for every command"], defaultValue: "0"
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', icon:"st.switches.switch.on", backgroundColor:"#00A0DC"
				attributeState "off", label:'${name}', icon:"st.switches.switch.off", backgroundColor:"#ffffff"
			}
		}
		standardTile("contact", "device.contact", width: 2, height: 2) {
			state "open", label: '${name}', icon: "st.contact.contact.open", backgroundColor: "#e86d13"
			state "closed", label: '${name}', icon: "st.contact.contact.closed", backgroundColor: "#00A0DC"
		}
		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		main "switch"
		details(["switch", "contact", "refresh"])
	}
}

private getIAS_ZONE_TYPE_ATTRIBUTE() { 0x0001 }
private getPOLL_CONTROL_CLUSTER() { 0x0020 }
private getCHECK_IN_INTERVAL_ATTRIBUTE() { 0x0000 }
private getFAST_POLL_TIMEOUT_ATTRIBUTE() { 0x0003 }
private getSET_LONG_POLL_INTERVAL_CMD() { 0x02 }
private getSET_SHORT_POLL_INTERVAL_CMD() { 0x03 }

// Parse incoming device messages to generate events
def parse(String description) {
	Map map = null
	if (device.getDataValue("manufacturer") == "LUMI") {
		if (description?.startsWith('catchall:')) {
			map = lumi_catchall(description)
		}
		if (map == null) {
			map = zigbee.getEvent(description)
		}
	   	if (map?.name == "switch") {
			map = [name: 'contact', value: event.value == 'off' ? 'closed' : 'open'] 
		}
	} else {
		map = zigbee.getEvent(description)
	}
	if (!map) {
		if (description?.startsWith('zone status') || description?.startsWith('zone report')) {
			map = parseIasMessage(description)
		} else {
			Map descMap = zigbee.parseDescriptionAsMap(description)
			if (descMap?.clusterInt == zigbee.POWER_CONFIGURATION_CLUSTER && descMap.commandInt != 0x07 && descMap?.value) {
				List<Map> descMaps = collectAttributes(descMap)
				if (device.getDataValue("manufacturer") == "Samjin") {
					def battMap = descMaps.find { it.attrInt == 0x0021 }
					if (battMap) {
						map = getBatteryPercentageResult(Integer.parseInt(battMap.value, 16))
					}
				} else {
					def battMap = descMaps.find { it.attrInt == 0x0020 }
					if (battMap) {
						map = getBatteryResult(Integer.parseInt(battMap.value, 16))
					}
				}
			} else if (descMap?.clusterInt == zigbee.IAS_ZONE_CLUSTER && descMap.attrInt == zigbee.ATTRIBUTE_IAS_ZONE_STATUS) {
				def zs = new ZoneStatus(zigbee.convertToInt(descMap.value, 16))
				map = getContactResult(zs.isAlarm1Set() ? "open" : "closed")
			}
		}
	}
	if (map?.name == 'contact') {
		state.contact = map.value
		processContact()
	}
	def result = map ? createEvent(map) : [:]

	if (description?.startsWith('enroll request')) {
		List cmds = zigbee.enrollResponse()
		log.debug "enroll response: ${cmds}"
		result = cmds?.collect { new physicalgraph.device.HubAction(it) }
	}
	return result
}

private List<Map> collectAttributes(Map descMap) {
	List<Map> descMaps = new ArrayList<Map>()

	descMaps.add(descMap)

	if (descMap.additionalAttrs) {
		descMaps.addAll(descMap.additionalAttrs)
	}

	return  descMaps
}

private Map parseIasMessage(String description) {
	ZoneStatus zs = zigbee.parseZoneStatus(description)
	return zs.isAlarm1Set() ? getContactResult('open') : getContactResult('closed')
}

private Map getBatteryResult(rawValue) {
	log.debug "Battery rawValue = ${rawValue}"
	def result = [:]

	if (!(rawValue == 0 || rawValue == 255)) {
		result.name = 'battery'
		result.translatable = true
		if (device.getDataValue("manufacturer") == "SmartThings") {
			volts = rawValue // For the batteryMap to work the key needs to be an int
			def batteryMap = [28: 100, 27: 100, 26: 100, 25: 90, 24: 90, 23: 70,
							  22: 70, 21: 50, 20: 50, 19: 30, 18: 30, 17: 15, 16: 1, 15: 0]
			def minVolts = 15
			def maxVolts = 28

			if (volts < minVolts)
				volts = minVolts
			else if (volts > maxVolts)
				volts = maxVolts
			def pct = batteryMap[volts]
			result.value = pct
		} else {
			def volts = rawValue / 10
			def minVolts = isFrientSensor() ? 2.3 : 2.1
			def maxVolts = 3.0
			def pct = (volts - minVolts) / (maxVolts - minVolts)
			def roundedPct = Math.round(pct * 100)
			if (roundedPct <= 0)
				roundedPct = 1
			result.value = Math.min(100, roundedPct)
		}
		result.descriptionText = "{{ device.displayName }} battery was {{ value }}%"		
	}

	return result
}

private Map getBatteryPercentageResult(rawValue) {
	log.debug "Battery Percentage rawValue = ${rawValue} -> ${rawValue / 2}%"
	def result = [:]

	if (0 <= rawValue && rawValue <= 200) {
		result.name = 'battery'
		result.translatable = true
		result.descriptionText = "{{ device.displayName }} battery was {{ value }}%"
		result.value = Math.round(rawValue / 2)
	}

	return result
}

Map lumi_catchall(String description) {
	Map result = [:]
	def catchall = zigbee.parse(description)

	if (catchall.clusterId == 0x0000) {
		def length = catchall.data.size()
		if ((catchall.data.get(0) == 0x01 || catchall.data.get(0) == 0x02 ) && ( catchall.data.get(1) == 0xFF)) {
			for (int i = 4; i < (length - 3); i++) {
				if (catchall.data.get(i) == 0x21) {
					def rawvolts = ((catchall.data.get(i+2) << 8) + catchall.data.get(i+1)) / 1000
					def minvolts = 2.7
					def maxvolts = 3.2
					def percent = Math.min(100, Math.round(100.0 * (rawvolts - minvolts) / (maxvolts - minvolts))) 
					return [ name: 'battery', value: percent, unit: '%']
				}
			}
		}
	}
	return result
}


private Map getContactResult(value) {
	log.debug "Contact Status : ${value}"
	def linkText = getLinkText(device)
	def descriptionText = "${linkText} was ${value == 'open' ? 'opened' : 'closed'}"
	return [
		name		   : 'contact',
		value		  : value,
		descriptionText: descriptionText
	]
}

def ping() {
	zigbee.readAttribute(zigbee.IAS_ZONE_CLUSTER, zigbee.ATTRIBUTE_IAS_ZONE_STATUS)
}

def refresh() {
	log.debug "Refreshing Values "
	def refreshCmds = []

	if (device.getDataValue("manufacturer") == "Samjin") {
		refreshCmds += zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, 0x0021)
	} else {
		refreshCmds += zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, 0x0020)
	}
	refreshCmds += zigbee.readAttribute(zigbee.TEMPERATURE_MEASUREMENT_CLUSTER, 0x0000) +
		zigbee.readAttribute(zigbee.IAS_ZONE_CLUSTER, zigbee.ATTRIBUTE_IAS_ZONE_STATUS) +
		zigbee.enrollResponse()

	return refreshCmds
}

def configure() {
	if (device.getDataValue("manufacturer") == "LUMI") {
		sendEvent(name: 'checkInterval', value: 86400, displayed: false, data: [ protocol: 'zigbee', hubHardwareId: device.hub.hardwareID ])
		return
	}
	sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 1 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])

	log.debug "Configuring Reporting, IAS CIE, and Bindings."
	def batteryAttr = device.getDataValue("manufacturer") == "Samjin" ? 0x0021 : 0x0020
	def cmds = refresh() +
		zigbee.configureReporting(zigbee.IAS_ZONE_CLUSTER, zigbee.ATTRIBUTE_IAS_ZONE_STATUS, DataType.BITMAP16, 30, 60 * 5, null) +
		zigbee.configureReporting(zigbee.POWER_CONFIGURATION_CLUSTER, batteryAttr, DataType.UINT8, 30, 21600, 0x01) +
		zigbee.enrollResponse()
	if (isEcolink()) {
		cmds += configureEcolink()
	} else if (isBoschRadionMultiSensor()) {
		cmds += zigbee.readAttribute(zigbee.IAS_ZONE_CLUSTER, IAS_ZONE_TYPE_ATTRIBUTE)
	} else if (isFrientSensor()) {
		cmds += zigbee.configureReporting(zigbee.TEMPERATURE_MEASUREMENT_CLUSTER, 0x0000, DataType.INT16, 30, 60 * 30, 0x64, [destEndpoint: 0x26])
	}
	return cmds
}

private configureEcolink() {
	sendEvent(name: "checkInterval", value: 60 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])

	def enrollCmds = zigbee.writeAttribute(POLL_CONTROL_CLUSTER, CHECK_IN_INTERVAL_ATTRIBUTE, DataType.UINT32, 0x00001C20) + zigbee.command(POLL_CONTROL_CLUSTER, SET_SHORT_POLL_INTERVAL_CMD, "0200") +
		zigbee.writeAttribute(POLL_CONTROL_CLUSTER, FAST_POLL_TIMEOUT_ATTRIBUTE, DataType.UINT16, 0x0028) + zigbee.command(POLL_CONTROL_CLUSTER, SET_LONG_POLL_INTERVAL_CMD, "B1040000")

	return zigbee.addBinding(POLL_CONTROL_CLUSTER) + refresh() + enrollCmds
}

private Boolean isEcolink() {
	device.getDataValue("manufacturer") == "Ecolink"
}

private Boolean isBoschRadionMultiSensor() {
	device.getDataValue("manufacturer") == "Bosch" && device.getDataValue("model") == "RFMS-ZBMS"
}

private Boolean isFrientSensor() {
	device.getDataValue("manufacturer") == "frient A/S"
}

//------------------------------------------------


def processContact() {
	def contact = state.contact
	def openDurationVal = openDuration ?: 0
	def closeDurationVal = closeDuration ?: 0
	if (contact == "open" && state.switch == "off") {
		log.debug "processContact() OnTrigger : Contact: Open at ${now()}. On trigger start time = ${state.onTime}"
		if (state.onTime == 0 && openDurationVal > 1) {
			state.onTime = now()
			runIn(openDurationVal+1, processContact)
		} else if (now() - state.onTime >= (openDurationVal-1)*1000) {
			log.debug "Setting switch status to on"
			state.switch = "on"
			sendEvent(name: "switch", value: "on", displayed: true)
			state.oTime = 0
			state.offTime = 0
		}
	} else {
		state.onTime = 0
	}

	if (contact == "closed" && state.switch == "on") {
		log.debug "processContact() OffTrigger : Contact: Closed at ${now()}. Off trigger start time = ${state.offTime}"
		if (state.offTime == 0 && closeDurationVal > 1) {
			state.offTime = now()
			runIn(closeDurationVal+1, processContact)
		} else if (now() - state.offTime >= (closeDurationVal-1)*1000) {
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

def installed() {
	log.debug "in installed()"
	state.onTime = 0
	state.offTime = 0
	state.contact="closed"
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

private getEventOption() {
	return eventOptionValue ?: "0"
}