/**
 *  Sharp Aquos TV
 *  	Works on Sharp TVs
 *  	Very basic asynchronous, non-polling control of most Sharp TVs made since 2010
 *   	NOTE: PLEASE HARDCODE THE INPUTS YOU WANT TO CONTROL ON LINE 92
 *
 *  	Losely based on: https://github.com/halkeye/sharp.aquos.devicetype.groovy
 *   	and page 36 of this manual http://snpi.dell.com/sna/manuals/A1534250.pdf
 */

preferences {
	input("destIp", "text", title: "IP", description: "The device IP",required:true)
	input("destPort", "number", title: "Port", description: "The port you wish to connect", required:true)
	input("login", "text", title: "Login", description: "The login")
	input("password", "password", title: "Password", description: "The password")
 	input("directTVIP", "text", title: "DirecTV IP", description: "IP addess", required:false)
}

metadata {
	definition (name: "Sharp Aquos", namespace: "KristopherKubicki",
    	author: "kristopher@acm.org") {
        	capability "Actuator"
        	capability "Switch"
        	capability "Music Player"

        	attribute "input", "string"
        	attribute "blocked", "number"

        	command "inputNext"
        	command "appletvOn"
        	command "appletvOff"
        	command "directvOn"
        	command "directvOff"
        	command "inputSelect"
        	command "powerOnDirecTV"
        	command "inputCommand"
      	}

	simulator {
		// TODO-: define status and reply messages here
	}

	tiles(scale:2) {
		standardTile("switch", "device.switch", width: 6, height: 4, canChangeIcon: false, canChangeBackground: true) {
			state "on", label: '${name}', action:"switch.off", backgroundColor: "#79b821", icon:"st.Electronics.electronics18"
			state "off", label: '${name}', action:"switch.on", backgroundColor: "#ffffff", icon:"st.Electronics.electronics18"
		}

		standardTile("mute", "device.mute", width: 2, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
			state "muted", label: '${name}', action:"unmute", backgroundColor: "#79b821", icon:"st.Electronics.electronics13"
			state "unmuted", label: '${name}', action:"mute", backgroundColor: "#ffffff", icon:"st.Electronics.electronics13"
		}

		standardTile("appleTv", "device.appleTv", width: 2, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
			state "off", label: 'Apple TV', action: "appletvOn", backgroundColor: "#FFFFFF", icon:"st.Electronics.electronics18"
			state "on", label: 'Apple TV', action: "appletvOff", backgroundColor: "#53a7c0", icon:"st.Electronics.electronics18"
		}

		standardTile("direcTv", "device.direcTv", width: 2, height: 2, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
			state "off", label: 'Directv', action: "directvOn", backgroundColor: "#FFFFFF", icon:"st.Electronics.electronics18"
			state "on", label: 'Directv', action: "directvOff", backgroundColor: "#53a7c0", icon:"st.Electronics.electronics18"
		}

		controlTile("level", "device.level", "slider", height: 2, width: 6, inactiveLabel: false, range: "(0..60)") {
			state "level", label: '${name}', action:"setLevel"
		}

		main "switch"
        details(["switch","level","mute","direcTv","appleTv"])
	}
}


//  There is no real parser for this device
//  ST cannot interpret the raw packet return, and thus we cannot
//  do anything with the return data.
//  http://community.smartthings.com/t/raw-tcp-socket-communications-with-sendhubcommand/4710/10
//
def parse(String description) {
    log.debug "Parsing '${description}'"
}

def setLevel(val) {
	val = sprintf("%02d",val)
	sendEvent(name: "mute", value: "unmuted")
	sendEvent(name: "level", value: val)
	request("VOLM" + val + "  \r")
}

def appletvOn(){
	log.info("Turn on appletv")
	sendEvent(name: "appleTv", value: "on")
	sendEvent(name: "direcTv", value: "off")
	request('IAVD2   \r')
}

def appletvOff(){
	log.info("Turn off appletv")
	sendEvent(name: "appleTv", value: "off")
	sendEvent(name: "direcTv", value: "on")
	request('IAVD1   \r')
}

def directvOn(){
	log.info("Turn on directv")
	sendEvent(name: "direcTv", value: "on")
	sendEvent(name: "appleTv", value: "off")
	request('IAVD1   \r')
}

def directvOff(){
	log.info("Turn off directv")
	sendEvent(name: "direcTv", value: "off")
	sendEvent(name: "appleTv", value: "on")
	request('IAVD2   \r')
}

def directv(){
	sendEvent(name: "mode", value: "directv")
	powerOnDirecTV(directTVIP)
	request('IAVD1   \r')
}

def mute() {
	sendEvent(name: "mute", value: "muted")
	request('MUTE1   \r')
}

def unmute() {
	sendEvent(name: "mute", value: "unmuted")
	request('MUTE2   \r')
}

def inputNext() {
	def current = device.currentValue("input")
	def selectedInputs = ["1","2","1"]
	def semaphore = 0

	for(selectedInput in selectedInputs) {
		if(semaphore == 1) {
      		return inputSelect(selectedInput)
    	}
  		if(current == selectedInput) {
      		semaphore = 1
    	}
	}

	return inputSelect(selectedInputs[0])
}


//POWER ON DIRECT TV BOX
def powerOnDirecTV(boxIP){

	def ip = "${boxIP}:8080"
	def uri = "/remote/processKey?key=poweron"

	def hubAction = new physicalgraph.device.HubAction(
		method: "GET",
		path: uri,
		headers: [HOST:ip]
	)
	hubAction
	log.debug("Power on Directv Box [http://${ip}${uri}]")
	wakeUpDirectTv(boxIP)
}

def wakeUpDirectTv(boxIP){
	def ip = "${boxIP}:8080"
	def uri = "/remote/processKey?key=exit"

	def hubAction = new physicalgraph.device.HubAction(
		method: "GET",
		path: uri,
		headers: [HOST:ip]
	)
	hubAction
	log.debug("Wake Up Directv Box [http://${ip}${uri}]")
}

def inputCommand(cmd) {
	request("RCKY$cmd   \r")
}

def inputSelect(channel) {
	sendEvent(name: "input", value: channel)
	request("IAVD$channel   \r")
}

// If lastAction is not null, we should probably block
def on() {
	log.debug("Turn on TV")
	directv()
	sendEvent(name: "switch", value: 'on')
	sendEvent(name: "mute", value: "unmuted")
	request("POWR1   \r")
}

def off() {
	log.debug("Turn off TV")
	sendEvent(name: "switch", value: 'off')
	request("RSPW2   \r\rPOWR0   \r")
}

def request(body) {
	def hosthex = convertIPtoHex(destIp)
	def porthex = convertPortToHex(destPort)
	device.deviceNetworkId = "$hosthex:$porthex"

	// sleep up to 9 seconds before issuing the next command
	def cur = new BigDecimal(device.currentValue("blocked") ?: 0)
	def waitTime = 0
	def cmds = []
	def c = new GregorianCalendar()
	if(cur > 0 && cur > c.time.time && cur - c.time.time < 9000) {
		waitTime = cur - c.time.time
		cmds << "delay $waitTime"
	}
	c = new GregorianCalendar()
	sendEvent(name: "blocked", value: c.time.time + 9000 + waitTime)

	def hubAction = new physicalgraph.device.HubAction(body,physicalgraph.device.Protocol.LAN)
	cmds << hubAction

	log.debug cmds

	cmds
}


private String convertIPtoHex(ipAddress) {
	String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02X', it.toInteger() ) }.join()
	return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04X', port.toInteger() )
	return hexport
}
