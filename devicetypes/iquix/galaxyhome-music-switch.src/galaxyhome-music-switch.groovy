/**
 *  Galaxy Home Music Switch ver 0.2.0
 *  Copyright 2020 Jaewon Park
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
 
metadata {
	definition (name: "GalaxyHome Music Switch", namespace: "iquix", author: "iquix", ocfDeviceType: "oic.d.switch") {
		capability "Switch"
		capability "Actuator"
		command "playURI", ["string"]
	}
	preferences {
		input name: "galaxyHomeAddr", title:"local IP address of Galaxy Home", type: "string"
		input name: "mediauri", title:"URI of mp3", type: "string"
        input name: "repeat", title:"repeat", type: "enum", options: ["repeat", "play once"]
	}
	tiles {
		multiAttributeTile(name:"switch", type: "generic", width: 6, height: 4){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label:'${name}', action:"off", icon:"st.switches.light.on", backgroundColor:"#00a0dc", nextState:"turningOff"
				attributeState "off", label:'${name}', action:"on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
				attributeState "turningOn", label:'${name}', action:"off", icon:"st.switches.light.on", backgroundColor:"#00a0dc", nextState:"turningOff"
				attributeState "turningOff", label:'${name}', action:"on", icon:"st.switches.light.off", backgroundColor:"#ffffff", nextState:"turningOn"
			}
		}
	}
}

def parse(description) {
	def msg = parseLanMessage(description)
    def node = msg?.xml?.property?.LastChange
    if (node) {
		if (node?.text()?.size()>40  && node?.text()?.contains("</") ) {
			def xml1 = parseXml(node.text())
			def currentStatus = xml1?.InstanceID?.TransportState?.'@val'?.text()
            if (currentStatus && currentStatus != state.lastStatus) {
            	state.lastStatus = currentStatus
            	log.debug " - state : " + currentStatus
            }
        }
	}
}

def on() {
	if (settings.mediauri != null) {
		playURI(settings.mediauri)
	} else {
		log.error "mediauri is not set. Please go to settings and setup mediauri"
	}	
	sendEvent(name: "switch", value: "on")
	runIn(1, off)
}

def off() {
	sendEvent(name: "switch", value: "off")
}


def installed() {
	log.debug "installed()"
}

def configure() {
	log.debug "configure()"
	sendEvent(name: "switch", value: "off")
}

def updated() {
	log.debug "updated()"
}

def playURI(u) {
	if (settings.galaxyHomeAddr != null) {
    	u = URLEncoder.encode(u, "UTF-8").replaceAll(/\+/,'%20').replace('%3A',':').replace('%2F','/')replace('%40','@').replace('%25','%').replace('%3F','?')
        state.currentURI = u
		send("SetAVTransportURI", u)
		send("Play", null)
        unschedule()
        nextURIRepeat()
        runEvery1Minute(nextURIRepeat)
	} else {
		log.debug "galaxyHomeAddr is not set. Please go to settings and setup galaxyHomeAddr"
	}
}

private send(s, u) {
	def action
	def data
	if (s == "Play") {
		action = "\"urn:schemas-upnp-org:service:AVTransport:1#Play\""
		data = "<?xml version=\"1.0\" encoding=\"utf-8\"?><s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body><u:Play xmlns:u=\"urn:schemas-upnp-org:service:AVTransport:1\"><InstanceID>0</InstanceID><Speed>1</Speed></u:Play></s:Body></s:Envelope>"
	} else if (s == "SetAVTransportURI") {
		action = "\"urn:schemas-upnp-org:service:AVTransport:1#SetAVTransportURI\""
		data = "<?xml version=\"1.0\" encoding=\"utf-8\"?><s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body><u:SetAVTransportURI xmlns:u=\"urn:schemas-upnp-org:service:AVTransport:1\"><InstanceID>0</InstanceID><CurrentURI>"+u+"</CurrentURI><CurrentURIMetaData></CurrentURIMetaData></u:SetAVTransportURI></s:Body></s:Envelope>"
	} else if (s == "SetNextAVTransportURI") {
		action = "\"urn:schemas-upnp-org:service:AVTransport:1#SetNextAVTransportURI\""
		data = "<?xml version=\"1.0\" encoding=\"utf-8\"?><s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body><u:SetNextAVTransportURI xmlns:u=\"urn:schemas-upnp-org:service:AVTransport:1\"><InstanceID>0</InstanceID><NextURI>"+u+"</NextURI><NextURIMetaData></NextURIMetaData></u:SetNextAVTransportURI></s:Body></s:Envelope>"
	} else if (s == "SetPlayMode") {
		action = "\"urn:schemas-upnp-org:service:AVTransport:1#SetPlayMode\""
		data = "<?xml version=\"1.0\" encoding=\"utf-8\"?><s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body><u:SetPlayMode xmlns:u=\"urn:schemas-upnp-org:service:AVTransport:1\"><InstanceID>0</InstanceID><NewPlayMode>"+u+"</NewPlayMode></u:SetPlayMode></s:Body></s:Envelope>"
	} else if (s == "GetMediaInfo") {
		action = "\"urn:schemas-upnp-org:service:AVTransport:1#GetMediaInfo\""
		data = "<?xml version=\"1.0\" encoding=\"utf-8\"?><s:Envelope s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body><u:GetMediaInfo xmlns:u=\"urn:schemas-upnp-org:service:AVTransport:1\"><InstanceID>0</InstanceID></u:GetMediaInfo></s:Body></s:Envelope>"
	} else {
    	return
    }
	def options = [
		"method": "POST",
		"path": "/upnp/control/AVTransport1",
		"headers": [
			"HOST": (settings.galaxyHomeAddr+":9197"),
			"Content-Type": "text/xml; charset=utf-8",
			"SOAPAction": action
		],
		"body": data
	]
	log.debug options
	def myhubAction = new physicalgraph.device.HubAction(options, (s=="GetMediaInfo") ? callback : null)
	sendHubCommand(myhubAction)
}

//--------------------------------------------

private getCallBackAddress()
{
	device.hub.getDataValue("localIP") + ":" + device.hub.getDataValue("localSrvPortTCP")
}

private subscribeAction(path) {
	def address = getCallBackAddress()
	def result = new physicalgraph.device.HubAction(
		method: "SUBSCRIBE",
		path: path,
		headers: [
			HOST: (settings.galaxyHomeAddr+":9197"),
			CALLBACK: "<http://${address}/notify>",
			NT: "upnp:event",
			TIMEOUT: "Second-300"])
    log.debug result
	sendHubCommand(result)
}

def subscribeEvent() {
	subscribeAction("/upnp/event/AVTransport1")
}

def nextURIRepeat() {
	//send("SetNextAVTransportURI", (settings.repeat == "repeat") ? state.currentURI : "")
    send("GetMediaInfo", callback)
}

def callback(physicalgraph.device.HubResponse hubResponse){
	def msg
    try {
        msg = parseLanMessage(hubResponse.description)
        log.debug msg
    } catch (e) {
        log.error "Exception caught while parsing data: "+e;
    }
}