import java.util.TimeZone
import groovy.json.JsonOutput

def writeToElastic(String project){
    final String currentTime = currentTime()
    //create empty data
    def data = readJSON text: '{}'
    //take current timestamp
    //data['@timestamp'] = currentTime
    //define key.value list
    def keyValues = [:]
    def build_current_status = ""
    if ( "${currentBuild.previousBuild.result}" == "SUCCESS" && "${currentBuild.currentResult}" == "FAILURE") {
    build_current_status = "broken"
    keyValues.put('job_build_status_delta', "${build_current_status}")
} else if ("${currentBuild.previousBuild.result}" == "FAILURE" && "${currentBuild.currentResult}" == "SUCCESS") {
    build_current_status = "fixed"
    def times_to_fix = time_to_recovery()
    keyValues.put('job_build_status_delta', "${build_current_status}")
    keyValues.put('time_to_fix', "${times_to_fix[0]}")
    keyValues.put('time_to_fix_millis', "${times_to_fix[1]}")
}  else {
    build_current_status = "unchanged"
    keyValues.put('job_build_status_delta', "${build_current_status}")
}
    keyValues.put('job_build_status', "${currentBuild.currentResult}")
    keyValues.put('job_name', "${JOB_NAME}")
    keyValues.put('job_url', "${currentBuild.absoluteUrl}")
    keyValues.put('job_build_number', "${BUILD_NUMBER}")
    keyValues.put('job_build_time_to_finish_readable', "${currentBuild.durationString}")
    keyValues.put('job_duration_milliseconds', "${currentBuild.duration}")
    keyValues.put('job_start_time_in_millis', "${currentBuild.startTimeInMillis}")
    keyValues.put('job_current_fullDisplayName', "${currentBuild.fullDisplayName}")
    keyValues.put('previous_job_build_status', "${currentBuild.previousBuild.result}")
    keyValues.put('previous_job_fullDisplayName', "${currentBuild.previousBuild.fullDisplayName}")
    keyValues.put('previous_job_build_start_time_in_millis', "${currentBuild.previousBuild.startTimeInMillis}")
    //put each Key,Value in the json data
    keyValues.each{ k, v -> data[k as String] = v as String }
    //create jsonString
    def jsonString = JsonOutput.toJson(data)
    echo "Elastic data: ${jsonString}"
    //call writeJsonToElastic to send the curl command
    writeJsonToElastic(jsonString,project)
}

def writeJsonToElastic(jsonString,project) {
  //curl command to send data to elasticsearch . project variable changes basing on the project type! (example: gma, voice etc..)
  sh """
    curl -v -X PUT "http://rlog.prod.inetpsa.com:1200/swx/jenkins/${project}" -H 'Content-Type: application/json' -d '${jsonString}'
  """
}

def currentTime() {
    def now = new Date()
    //Timezone set to Europe/Rome !!!
    final String currentTime = now.format("yyyy-MM-dd'T'HH:mm:ssZZZZ", TimeZone.getTimeZone('Europe/Rome'))
    echo "currentTime : ${currentTime}"
    return currentTime
}

def time_to_recovery() {
    script{
    def timestampString = currentTime()
    def previousStartTimeInMillisString = "${currentBuild.previousBuild.startTimeInMillis}"
    
    def sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
    def timestamp = sdf.parse(timestampString)
    def timestampInMillis = timestamp.time
    
    // Calculate time difference
    def previousStartTimeInMillis = previousStartTimeInMillisString.toLong()
    def timePassedInMillis = timestampInMillis - previousStartTimeInMillis
    // Convert time difference to a human-readable format
    def timePassed = new java.text.SimpleDateFormat("HH 'hours', mm 'minutes', ss 'seconds'").format(new Date(timePassedInMillis))
    println "Time Passed: ${timePassed}"
    return [timePassed,timePassedInMillis]
}
}

return this
