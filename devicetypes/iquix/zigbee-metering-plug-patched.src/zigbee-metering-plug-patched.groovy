/**
 *  Copyright 2019-2022 SmartThings/iquix
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
    definition (name: "Zigbee Metering Plug (Patched)", namespace: "iquix", author: "SmartThings/iquix", ocfDeviceType: "oic.d.smartplug", mnmn: "SmartThings",  vid: "generic-switch-power-energy") {
        capability "Energy Meter"
        capability "Power Meter"
        capability "Actuator"
        capability "Switch"
        capability "Refresh"
        capability "Health Check"
        capability "Sensor"
        capability "Configuration"

        fingerprint profileId: "0104", inClusters: "0000, 0004, 0005, 0006, 0702, 0B04", outClusters: "0019, 000A", model: "TS0121",  deviceJoinName: "Tuya Outlet" // Tuya Smart Plug
        fingerprint profileId: "0104", inClusters: "0000, 0004, 0005, 0006, 0702, 0B04, E000, E001", outClusters: "0019, 000A", model: "TS011F",  deviceJoinName: "Tuya Outlet" //Tuya Smart Plug
        fingerprint profileId: "0104", inClusters: "0003, 0004, 0005, 0006, 0702, 0B04, E000, E001", outClusters: "0019, 000A", model: "TS011F",  deviceJoinName: "Tuya Outlet" // Tuya Smart Plug
    }

    preferences {
        input "powerPollingValue", "enum", title: "Power Polling Preference", options: ["0": "Automatic", "1": "Force Enable Power Polling", "2": "Force Disable Power Polling"], defaultValue: "0", required: false, displayDuringSetup: false
    }

    tiles(scale: 2){
        multiAttributeTile(name:"switch", type: "generic", width: 6, height: 4, canChangeIcon: true){
            tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
                attributeState("on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00a0dc")
                attributeState("off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff")
            }
        }
        valueTile("power", "device.power", decoration: "flat", width: 2, height: 2) {
            state "default", label:'${currentValue} W'
        }
        valueTile("energy", "device.energy", decoration: "flat", width: 2, height: 2) {
            state "default", label:'${currentValue} kWh'
        }
        standardTile("refresh", "device.power", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        standardTile("reset", "device.energy", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:'reset kWh', action:"reset"
        }

        main(["switch"])
        details(["switch","power","energy","refresh","reset"])
    }
}

def getATTRIBUTE_READING_INFO_SET() { 0x0000 }
def getATTRIBUTE_HISTORICAL_CONSUMPTION() { 0x0400 }

def parse(String description) {
    log.debug "description is $description"
    def event = zigbee.getEvent(description)
    def powerDiv = 1
    def energyDiv = 100

    if (event) {
        log.info "event enter:$event"
        if (event.name== "power") {
            event.value = event.value/powerDiv
            event.unit = "W"
        } else if (event.name== "energy") {
            event.value = event.value/energyDiv
            event.unit = "kWh"
        }
        log.info "event outer:$event"
        sendEvent(event)
    } else {
        List result = []
        def descMap = zigbee.parseDescriptionAsMap(description)
        log.debug "Desc Map: $descMap"

        List attrData = [[clusterInt: descMap.clusterInt ,attrInt: descMap.attrInt, value: descMap.value]]
        descMap.additionalAttrs.each {
            attrData << [clusterInt: descMap.clusterInt, attrInt: it.attrInt, value: it.value]
        }

        attrData.each {
            def map = [:]
            if (it.value && it.clusterInt == zigbee.SIMPLE_METERING_CLUSTER && it.attrInt == ATTRIBUTE_HISTORICAL_CONSUMPTION) {
                log.debug "power"
                map.name = "power"
                map.value = zigbee.convertHexToInt(it.value)/powerDiv
                map.unit = "W"
            }
            else if (it.value && it.clusterInt == zigbee.SIMPLE_METERING_CLUSTER && it.attrInt == ATTRIBUTE_READING_INFO_SET) {
                log.debug "energy"
                map.name = "energy"
                map.value = zigbee.convertHexToInt(it.value)/energyDiv
                map.unit = "kWh"
            }

            if (map) {
                result << createEvent(map)
            }
            log.debug "Parse returned $map"
        }
        return result
    }
}

def off() {
    def cmds = zigbee.off()
    if (device.getDataValue("model") == "HY0105") {
        cmds += zigbee.command(zigbee.ONOFF_CLUSTER, 0x00, "", [destEndpoint: 0x02])
    }
    if (isPolling) {
        cmds += ["delay 1500"] + zigbee.electricMeasurementPowerRefresh()
    }
    return cmds
}


def on() {
    def cmds = zigbee.on()
    if (device.getDataValue("model") == "HY0105") {
        cmds += zigbee.command(zigbee.ONOFF_CLUSTER, 0x01, "", [destEndpoint: 0x02])
    }
    if (isPolling) {
        cmds += ["delay 1500"] + zigbee.electricMeasurementPowerRefresh()
    }
    return cmds
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
    return refresh()
}

def refresh() {
    log.debug "refresh"
    zigbee.onOffRefresh() +
        zigbee.electricMeasurementPowerRefresh() +
        zigbee.readAttribute(zigbee.SIMPLE_METERING_CLUSTER, ATTRIBUTE_READING_INFO_SET)
}

def installed() {
    log.debug "installed()"
    return configure()
}

def updated() {
    log.debug "updated()"
    if (powerPolling != state.prevPowerPolling) {
        state.prevPowerPolling = powerPolling
        setPolling()
    }
}

def configure() {
    log.debug "configure()"
    // this device will send instantaneous demand and current summation delivered every 1 minute
    sendEvent(name: "checkInterval", value: 2 * 60 + 10 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])

    if ((device.getDataValue("manufacturer") == "Develco Products A/S") || (device.getDataValue("manufacturer") == "Aurora"))  {
        device.updateDataValue("divisor", "1")
    }
    if ((device.getDataValue("manufacturer") == "SALUS") || (device.getDataValue("manufacturer") == "DAWON_DNS") || (device.getDataValue("model") == "TS0121") || (device.getDataValue("model") == "TS011F"))  {
        device.updateDataValue("divisor", "1")
    }
    if ((device.getDataValue("manufacturer") == "LDS") || (device.getDataValue("manufacturer") == "REXENSE") || (device.getDataValue("manufacturer") == "frient A/S"))  {
        device.updateDataValue("divisor", "1")
    }
    
    setPolling()
    
    return refresh() +
        zigbee.onOffConfig() +
        zigbee.configureReporting(zigbee.SIMPLE_METERING_CLUSTER, ATTRIBUTE_READING_INFO_SET, DataType.UINT48, 1, 600, 1) +
        zigbee.electricMeasurementPowerConfig(1, 600, 1) 
}

def powerRefresh() {
    def cmds = zigbee.electricMeasurementPowerRefresh()
    cmds.each{ sendHubCommand(new physicalgraph.device.HubAction(it)) }
}

private setPolling() {
    unschedule()
    if (isPolling) {
    	log.debug "setPolling() : Scheduling power polling every 1 minute."
        runEvery1Minute(powerRefresh)
    } else {
        log.debug "setPolling() : Power polling disabled. Power will be reported only if the plug supports real time power reporting."
    }
}

private getIsPolling() {
    def pollingTS011FAppVers = ["45", "44", "41", "40"]
    def pushTS0121Devices = ["_TZ3000_8nkb7mof"]
    def manufacturer = device.getDataValue("manufacturer")
    def model = device.getDataValue("model")
    def appVer = device.getDataValue("application")
    return (powerPolling != "2") && ((model == "TS0121" && (pushTS0121Devices.findIndexOf{ it == manufacturer } == -1) ) || (model == "TS011F" && pollingTS011FAppVers.findIndexOf{ it == appVer } != -1) || powerPolling == "1")
}

private getPowerPolling() { powerPollingValue ?: "0" }
