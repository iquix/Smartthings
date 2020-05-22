/**
 *  The Weather Company Lowest Temperature
 *  Author: iquix@naver.com 
 */
metadata {
	definition (name: "TWC LowestTemp", namespace: "iquix", author: "iquix") {
		capability "temperatureMeasurement"
		capability "sensor"
		attribute "lastFetch", "String"
	}

	tiles(scale: 2) {

	standardTile("Lowest temperature", "device.temperature", width: 6, height: 3, canChangeIcon: false) {
		state "default", label: '${currentValue}º',unit:'${currentValue}', icon: "st.Weather.weather2", backgroundColor:"#999999"}  

	standardTile("TWClogo", "device.TWClogo",  width: 1, height: 1,  canChangeIcon: false ) {
		state "default", icon: "https://business.weather.com/img/the-weather-company-logo.png", backgroundColor: "#999999"	  }   
		
	standardTile("lastFetch", "device.lastFetch", decoration: "flat", width: 5, height: 1) {
		state "default", label: 'Last Fetch\n${currentValue}'} 
	
	main("LowestTemp")
	details(["Lowest temperature", "TWClogo","lastFetch"])
	}
}

def installed() {
	updated()
}

def updated() {
	def crontext1 = rand(60) + " " + rand(30) + " 18 * * ?"  // refersh once between 18:00~18:30 every day
	def crontext2 = rand(60) + " " + rand(30) + " 2 * * ?"  // refersh once more between 2:00~2:30 every day
	log.debug "Executing 'updated'. adding cron: ${crontext1} and ${crontext2}"
	unschedule()
	schedule(crontext1, refresh)
	schedule(crontext2, _refresh)
	refresh()
}

def parse(String description) {
	log.debug "Executing 'parse'"
}

def rand(n) {
	return (new Random().nextInt(n))
}

def _refresh() {
	refresh()
}

def refresh() {
	log.debug "Executing 'refresh'"
	
	def w = getTwcForecast()
		
	def tempMin = w.temperatureMin[0]
	def tempMax = w.temperatureMax[0]
	def temperatureScale = getTemperatureScale()
	
	log.debug "response lowest temp: ${w.temperatureMin}"
	log.debug "response highest temp: ${w.temperatureMax}"
	sendEvent(name: "temperature", value: tempMin, unit: temperatureScale, displayed: true)
	sendEvent(name: "lastFetch", value: (new Date(now())).format("MMM d HH:mm:ss", location.timeZone))
}