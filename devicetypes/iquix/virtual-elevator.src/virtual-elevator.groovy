/**
 *  Virtual Elevator
 *   - iquix@naver.com
 *  Copyright 2021
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
	definition (name: "Virtual Elevator", namespace: "iquix", author: "iquix", ocfDeviceType: "x.com.st.d.elevator", mnmn: "SmartThingsCommunity", vid: "16a4a521-d35d-3000-a466-fd55d30d01ce") {
		capability "Elevator Call"
	}
}

def parse(String description) {
	log.debug "Parsing '${description}'"
}

def call(){
	log.debug "call()"
	sendEvent(name: "callStatus", value: "called")
	runIn(2, setStandby)
}

def setStandby() {
	log.debug "set standby mode"
	sendEvent(name: "callStatus", value: "standby")
}

def installed() {
	sendEvent(name: "callStatus", value: "standby")
}