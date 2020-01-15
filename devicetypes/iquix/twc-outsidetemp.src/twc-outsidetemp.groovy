/**
 *  The Weather Company Outside Temperature
 *  Author: iquix@naver.com
 */
metadata {
	definition (name: "TWC OutsideTemp", namespace: "iquix", author: "iquix") {
		capability "temperatureMeasurement"
		capability "refresh"
		capability "polling"
		capability "sensor"
		attribute "lastFetch", "String"		
	}

	tiles(scale: 2) {

	standardTile("temperature", "device.temperature", width: 6, height: 3, canChangeIcon: false) {
		state "default", label: '${currentValue}º',unit:'${currentValue}', icon: "st.Weather.weather2", backgroundColor:"#999999"
	}

	standardTile("weather", "device.weather", width: 4, height: 2) {
		 state "default", label:'${currentValue}'
	 }
	
	standardTile("refresh", "device.refresh", decoration: "flat", width: 2, height: 2) {
		 state "default", action:"refresh", icon:"st.secondary.refresh"
	 }
	
	standardTile("TWClogo", "device.TWClogo",  width: 1, height: 1,  canChangeIcon: false ) {
		state "default", icon: "https://business.weather.com/img/the-weather-company-logo.png", backgroundColor: "#999999"	  
	}   

	standardTile("lastFetch", "device.lastFetch", decoration: "flat", width: 5, height: 1) {
		state "default", label: 'Last Fetch\n${currentValue}'
	}

	main("TWCOutsideTemp")
	details(["temperature","weather","refresh","TWClogo","lastFetch" ])
	 }
}

def installed() {
	updated()
}

def updated() {
	log.debug "Executing 'updated'"
	unschedule()
	refresh()
	runEvery30Minutes(refresh)
}

def poll() {
	log.debug "Executing 'poll'"
	refresh()
}

def parse(String description) {
	log.debug "Executing 'parse'"
}

def forcepoll() {
	log.debug "Executing 'forcepoll'"
	refresh()
}

def refresh() {
	log.debug "Executing 'refresh'"
	
	def w = getTwcConditions()
	def temp = w.temperature
	def weather = w.wxPhraseMedium
	def temperatureScale = getTemperatureScale()
	
	log.debug "current temperature: ${temp}"
	log.debug "current weather: ${weather}"
	sendEvent(name: "temperature", value: temp, unit: temperatureScale)
	sendEvent(name: "weather", value: weather)
	sendEvent(name: "lastFetch", value: (new Date(now())).format("MMM d HH:mm:ss", location.timeZone))
}