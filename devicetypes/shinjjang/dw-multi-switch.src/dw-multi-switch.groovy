/**
 *
 *
 *  Includes all configuration parameters and ease of advanced configuration.
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
metadata {
	definition(name: "DW Multi Switch", namespace: "ShinJjang", author: "ShinJjang/iquix", mnmn: "SmartThings", ocfDeviceType: "oic.d.thermostat") {  //vid:"generic-temperature"
	capability "Temperature Measurement"
	capability "Relative Humidity Measurement"
	//capability "Tamper Alert"
	capability "Sensor"
	capability "Battery"
	capability "Health Check"
    capability "Refresh"
    capability "Configuration"

    attribute "lastCheckin", "String"
    attribute "configReport", "String"
    attribute "tamper", "String"

    fingerprint mfr:"018C", prod:"0059", model:"0001", deviceJoinName: "DW Switch 3CH"
    fingerprint mfr:"018C", prod:"0058", model:"0001", deviceJoinName: "DW Switch 2CH"
    fingerprint mfr:"018C", prod:"0057", model:"0001", deviceJoinName: "DW Switch 1CH"
    fingerprint mfr:"018C", prod:"0066", model:"0001", deviceJoinName: "DW Switch 3CH"
    fingerprint mfr:"018C", prod:"0065", model:"0001", deviceJoinName: "DW Switch 2CH"
    fingerprint mfr:"018C", prod:"0064", model:"0001", deviceJoinName: "DW Switch 1CH"
    fingerprint mfr:"018C", prod:"0063", model:"0001", deviceJoinName: "DW Switch 3CH"
    fingerprint mfr:"018C", prod:"0062", model:"0001", deviceJoinName: "DW Switch 2CH"
    fingerprint mfr:"018C", prod:"0061", model:"0001", deviceJoinName: "DW Switch 1CH"
    
	}
	simulator {}
    preferences {
		input "reportTime", "enum", title: "온습도 리포팅 주기", defaultValue: 600, options:[60: "1분", 120: "2분", 180: "3분", 300 : "5분", 600: "10분", 900 :"15분", 1200 :"20분", 1800 :"30분", 3600: "60분", 5400: "90분", 7200: "120분", 14400: "240분"], displayDuringSetup: true
        input "minTemp", "enum", title: "리포팅 최소 온도변화량", defaultValue: 20, options:[10: "1℃", 11: "1.1℃", 12: "1.2℃", 13 : "1.3℃", 15: "1.5℃", 18 :"1.8℃", 20 :"2℃", 22 :"2.2℃", 25: "2.5℃", 28: "2.8℃", 30: "3℃"], displayDuringSetup: true
        input "minHumi", "enum", title: "리포팅  최소 습도변화량", defaultValue: 10, options:[5: "5%", 6: "6%", 7: "7%", 8 : "8%", 9: "9%", 18 :"10%", 12 :"12%", 14 :"14%", 15: "15%", 18: "18%", 20: "20%"], displayDuringSetup: true
    }
    tiles(scale: 2) {
        multiAttributeTile(name:"temperature", type:"generic", width:6, height:4) {
            tileAttribute("device.temperature", key:"PRIMARY_CONTROL"){
                attributeState("temperature", label:'${currentValue}°',
					backgroundColors:[
 						[value: 31, color: "#153591"],
 						[value: 44, color: "#1e9cbb"],
 						[value: 59, color: "#90d2a7"],
 						[value: 74, color: "#44b621"],
 						[value: 84, color: "#f1d801"],
 						[value: 95, color: "#d04e00"],
 						[value: 96, color: "#bc2323"]
 					]
                )
            }
            tileAttribute("device.lastCheckin", key: "SECONDARY_CONTROL") {
                attributeState("lastCheckin", label:'${currentValue}' 
                )
            }
			tileAttribute("device.configReport", key: "SECONDARY_CONTROL") {
        		attributeState("configReport", label:'                                          ${currentValue}', defaultState: true)
    		}            
            
        }
        valueTile("temperature2", "device.temperature", inactiveLabel: false) {
        	state "temperature", label:'${currentValue}°', icon: "st.Weather.weather2",
        		backgroundColors:[
 	    			[value: 31, color: "#153591"],
 	    			[value: 44, color: "#1e9cbb"],
 	    			[value: 59, color: "#90d2a7"],
 	    			[value: 74, color: "#44b621"],
 	    			[value: 84, color: "#f1d801"],
 	    			[value: 95, color: "#d04e00"],
 	    			[value: 96, color: "#bc2323"]
 	    		]
            }
        valueTile("humidity", "device.humidity", inactiveLabel: false, width: 2, height: 2) {
            state "humidity", label:'${currentValue}%', unit:"%", icon:"https://raw.githubusercontent.com/bspranger/Xiaomi/master/images/XiaomiHumidity.png",
            backgroundColors:[
                [value: 0, color: "#FFFCDF"],
                [value: 4, color: "#FDF789"],
                [value: 20, color: "#A5CF63"],
                [value: 23, color: "#6FBD7F"],
                [value: 56, color: "#4CA98C"],
                [value: 59, color: "#0072BB"],
                [value: 76, color: "#085396"]
            ]
        }
        valueTile("battery", "device.battery", inactiveLabel: false, width: 2, height: 2) {
            state "battery", label:'${currentValue}%', unit:"%", icon:"https://raw.githubusercontent.com/bspranger/Xiaomi/master/images/XiaomiBattery.png",
            backgroundColors:[
                [value: 10, color: "#bc2323"],
                [value: 26, color: "#f1d801"],
                [value: 51, color: "#44b621"]
            ]
        }
		standardTile("tamper", "device.tamper", height: 2, width: 2, decoration: "flat") {
			state "clear", label: 'Good', action:"", icon:"st.nest.nest-leaf", backgroundColor: "#ffffff", nextState:"detected"
			state "detected", label: '', action:"", icon:"st.thermostat.heat", backgroundColor: "#ff0000", nextState:"clear"
		}
		standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		standardTile("configure", "device.configure", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "configure", label:'', action:"configure", icon:"st.secondary.configure"
		}
        main("temperature2")
        details(["temperature", "humidity", "battery", "tamper", "configure"]) //
    }
}

def parse(String description) {
	def result = []
	def cmd = zwave.parse(description)
	if (cmd) {
		result = zwaveEvent(cmd)
	}
	return result
}

def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd) {
	log.debug "multilevelreport ${cmd}"

    def result = []
	def map = [:]
	switch (cmd.sensorType) {
		case 1:
			map.name = "temperature"
			def cmdScale = cmd.scale == 1 ? "F" : "C"
			map.value = convertTemperatureIfNeeded(cmd.scaledSensorValue, cmdScale, cmd.precision)
			map.unit = getTemperatureScale()
			break
		
		case 5:
			map.name = "humidity"
			map.value = cmd.scaledSensorValue.toInteger()
			map.unit = "%"
			break
		
	}
    log.debug "eventmap: ${map}"
    result << createEvent(map)
    def now = new Date().format("MM-dd HH:mm", location.timeZone)
    result <<  createEvent(name: "lastCheckin", value: now)	
    
	result
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
	log.debug "BatteryReport ${cmd}"
    def result = []
	def map = [name: "battery", unit: "%"]
	if (cmd.batteryLevel == 0xFF) {
		map.value = 50
		map.descriptionText = "$device.displayName 's battery capacity is unknown."
	} else {
	    def batVolt = (cmd.batteryLevel - 10) / 17   // 10-->8 by iquix
    	def batPer = Math.min(100, Math.round(batVolt * 100))   
		map.value = batPer as int
	}
    log.debug "eventmap: ${map}"
    result << createEvent(map)
	result
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
	log.debug "manufacturerId:   ${cmd.manufacturerId}"
	log.debug "productId:        ${cmd.productId}"
	log.debug "productTypeId:    ${cmd.productTypeId}"
	def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
	updateDataValue("MSR", msr)
	createEvent([descriptionText: "$device.displayName MSR: $msr", isStateChange: false])
}

def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd) {
	log.debug "VersionReport"
	log.debug "applicationVersion:		${cmd.applicationVersion}"
	log.debug "applicationSubVersion:	${cmd.applicationSubVersion}"
	log.debug "zWaveLibraryType:		${cmd.zWaveLibraryType}"
	log.debug "zWaveProtocolVersion:	${cmd.zWaveProtocolVersion}"
	log.debug "zWaveProtocolSubVersion: ${cmd.zWaveProtocolSubVersion}"
    def fw = "${cmd.applicationVersion}.${cmd.applicationSubVersion}"
	updateDataValue("fw", fw)
	def text = "$device.displayName: firmware version: $fw, Z-Wave version: ${cmd.zWaveProtocolVersion}.${cmd.zWaveProtocolSubVersion}"
	createEvent(descriptionText: text, isStateChange: false)
}

def zwaveEvent(physicalgraph.zwave.commands.deviceresetlocallyv1.DeviceResetLocallyNotification cmd) {
	log.info "${device.displayName}: received command: $cmd - device has reset itself"
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd, ep = null) {
	log.debug "Security Message Encap ${cmd}"
	def encapsulatedCommand = cmd.encapsulatedCommand()
	if (encapsulatedCommand) {
		zwaveEvent(encapsulatedCommand, null)
	} 
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd){
	log.debug "ConfigurationReport ${cmd}"
    def result = []
	def map = [:]
	switch (cmd.parameterNumber) {
		case 1:
			state.reTime = cmd.scaledConfigurationValue/60 as int
			break
		
		case 2:
			state.minTemp = cmd.scaledConfigurationValue/10
			break
            
		case 3:
			state.minHumi = cmd.scaledConfigurationValue
			break
	}
    log.debug "ConfigurationReport event: ${map}"
    result <<  createEvent(name: "configReport", value: "C/S: ${state.reTime}min, ${state.minTemp}℃, ${state.minHumi}%")	
    
	result    
}

def zwaveEvent(physicalgraph.zwave.commands.notificationv3.NotificationReport cmd, ep = null) {
	log.debug "NotificationReport= ${cmd}" + (ep ? " from endpoint $ep" : "")
    if (cmd.notificationType == 8) {
    def value = cmd.event== 3? "on" : "off"
    ep ? changeSwitch(ep, value) : []
    } else if (cmd.notificationType == 4) {
    	def result = []
		def map = [name: "tamper"]
		if (cmd.event == 2 || cmd.event == 4) {
			map.value = detected
		} else if (cmd.event == 6) {
			map.value = clear
		}
   		log.debug "NotificationReport eventmap: ${map}"
    	result << createEvent(map)
		result
    }
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd, ep = null) {
	log.debug "Multichannel command ${cmd}" + (ep ? " from endpoint $ep" : "")
	def encapsulatedCommand = cmd.encapsulatedCommand()
	zwaveEvent(encapsulatedCommand, cmd.sourceEndPoint as Integer)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd, ep = null) {
	log.debug "Basic ${cmd}:${ep}" + (ep ? " from endpoint $ep" : "")
	def value = cmd.value ? "on" : "off"
	ep ? changeSwitch(ep, value) : []
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd, ep = null) {
	log.debug "Binary ${cmd}" + (ep ? " from endpoint $ep" : "")
	def value = cmd.value ? "on" : "off"
	ep ? changeSwitch(ep, value) : []
}

private changeSwitch(endpoint, value) {
	def result = []
	if(endpoint) {
		String childDni = "${device.deviceNetworkId}:$endpoint"
		def child = childDevices.find { it.deviceNetworkId == childDni }
		log.debug "(name: childswitch${endpoint}, value: ${value})"
		result << child.sendEvent(name: "switch", value: value)
	}
	result
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelEndPointReport cmd, ep = null) {
	if(!childDevices) {
		if (isDW3ch() || isDWK3ch()) {
			addChildSwitches(3)
            log.debug "child 3"
		} else if (isDW2ch() || isDWK2ch()) {
			addChildSwitches(2)
            log.debug "child 2"
		} else if (isDW1ch() || isDWK1ch()) {
			addChildSwitches(1)
            log.debug "child 1"
		} else {
			addChildSwitches(cmd.endPoints)
            log.debug "child ep=$cmd.endPoints"
		}
	}
}

def isDW3ch() {
	zwaveInfo.prod.equals("0066")
}
def isDW2ch() {
	zwaveInfo.prod.equals("0065")
}
def isDW1ch() {
	zwaveInfo.prod.equals("0064")
}
def isDWK3ch() {
	zwaveInfo.prod.equals("0059")||zwaveInfo.prod.equals("0063")
}
def isDWK2ch() {
	zwaveInfo.prod.equals("0058")||zwaveInfo.prod.equals("0062")
}
def isDWK1ch() {
	zwaveInfo.prod.equals("0057")||zwaveInfo.prod.equals("0061")
}

def zwaveEvent(physicalgraph.zwave.Command cmd, ep) {
    log.debug "${device.displayName}: Unhandled ${cmd}" + (ep ? " from endpoint $ep" : "")
}

private onOffCmd(value, endpoint) {
	log.debug "onoffCmd val:${value}, ep:${endpoint}"
	delayBetween([
			secureEncap(zwave.basicV1.basicSet(value: value), endpoint),
		//	secureEncap(zwave.basicV1.basicGet(), endpoint),
			"delay 500"
	], 500)
}

def refresh(endpoint) {
	log.info "refresh()"
	if(endpoint) {
		delayBetween([
			secureEncap(zwave.basicV1.basicGet(), endpoint),
			"delay 500"
		], 500)
    } else {
    	def cmds = []
        cmds << zwave.batteryV1.batteryGet()
		cmds << zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 1)
		cmds << zwave.sensorMultilevelV5.sensorMultilevelGet(sensorType: 5)
        
        sendCommands(cmds,1000)
	}
}

private sendCommands(cmds, delay=1000) {
    sendHubCommand(cmds, delay)
}

private commands(commands, delay=200) {
    delayBetween(commands.collect{ command(it) }, delay)
}

def ping() {
//	refresh()
}

def childOn(deviceNetworkId) {
	childOnOff(deviceNetworkId, 0xFF)
}

def childOff(deviceNetworkId) {
	childOnOff(deviceNetworkId, 0x00)
}

def childOnOff(deviceNetworkId, value) {
	def switchId = getSwitchId(deviceNetworkId)
	if (switchId != null) sendHubCommand onOffCmd(value, switchId)
}

def childRefresh(deviceNetworkId) {
	def switchId = getSwitchId(deviceNetworkId)
	if (switchId != null) sendHubCommand refresh(switchId)
}

def installed() {
	log.debug "Installed ${device.displayName}"
	sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
	refresh()
}

def updated() {
	configure()
}

def configure() {
	log.debug "Configure..."
    	def msrdata = getDataValue("MSR")
        def cmds = []
        cmds << zwave.multiChannelV3.multiChannelEndPointGet()
		if (msrdata == null) cmds << zwave.manufacturerSpecificV2.manufacturerSpecificGet()        
        if (reportTime != null) cmds << zwave.configurationV1.configurationSet(parameterNumber: 1, size: 2, scaledConfigurationValue: reportTime as int)
    	if (minTemp != null) cmds << zwave.configurationV1.configurationSet(parameterNumber: 2, size: 1, scaledConfigurationValue: minTemp as int)
    	if (minHumi != null) cmds << zwave.configurationV1.configurationSet(parameterNumber: 3, size: 1, scaledConfigurationValue: minHumi as int)
	    cmds << zwave.configurationV1.configurationGet(parameterNumber: 1)
	    cmds << zwave.configurationV1.configurationGet(parameterNumber: 2)
	    cmds << zwave.configurationV1.configurationGet(parameterNumber: 3)
    	sendCommands(cmds,1000)    
}

private secure(cmd) {
	if(zwaveInfo.zw.endsWith("s")) {
		zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
	} else {
		cmd.format()
	}
}

private encap(cmd, endpoint = null) {
	if (endpoint) {
		zwave.multiChannelV3.multiChannelCmdEncap(destinationEndPoint:endpoint).encapsulate(cmd)
	} else {
		cmd
	}
}

private secureEncap(cmd, endpoint = null) {
	secure(encap(cmd, endpoint))
}

def getSwitchId(deviceNetworkId) {
	def split = deviceNetworkId?.split(":")
	return (split.length > 1) ? split[1] as Integer : null
}

private addChildSwitches(numberOfSwitches) {
	for(def endpoint : 1..numberOfSwitches) {
		try {
			String childDni = "${device.deviceNetworkId}:$endpoint"
			def componentLabel = device.displayName[0..-1] + "-${endpoint}"
			//addChildDevice("DW Child Device", childDni, device.getHub().getId(), [
            addChildDevice("smartthings", "Child Switch", childDni, device.getHub().getId(), [
					completedSetup: true,
					label         : componentLabel,
					isComponent   : false
			])
		} catch(Exception e) {
			log.debug "Exception: ${e}"
		}
	}
}

