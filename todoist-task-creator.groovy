 /*
 *  Todoist Task Creator  
 *  Device Handler for Hubitat
 *  Version 1.0 - Stable
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
 *  Based on original device handler by Lazcad / RaveTam
 *  Updates and contributions to code by a4refillpad, bspranger, marcos-mvs, mike-debney, Tiago_Goncalves, and veeceeoh
 *
 * Original version by benevolent
 *
 * Known Bugs: zaro boogs
 */

import groovy.json.JsonSlurper

metadata
{
    definition(name: "Todoist task creator", namespace: "benevolent", author: "benevolent", importUrl: "https://raw.githubusercontent.com/serenewaffles/hubitat-todoist-task-creator/main/todoist-task-creator.groovy")
    {
        capability "Notification"
        attribute "t", "string"
        command "getLabels"
    }
}

preferences
{
    section
    {
        input "password", "password", title: "API Key", required: true
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
    }
}
void updated() {
    state.Usage = "t={task name}[,{flag}={attribute}]"
    state.Usage +="<p><style>table, th, td {border: 1px solid black; border-collapse: collapse;}</style>"
    state.Usage +="<table><tr><th>Flag</th><th>Meaning</th></tr>"
    state.Usage +="<tr><td>t</td><td>task name</td></tr>"
    state.Usage +="<tr><td>p</td><td>project id</td></tr>"
    state.Usage +="<tr><td>ds</td><td>due string (Todoist formatting)</td></tr>"
    state.Usage +="<tr><td>pri</td><td>priority (1 (normal) - 4 (high))</td></tr>"
    state.Usage +="<tr><td>l</td><td>label ID(each gets its own flag)</td></tr>"
    state.Usage +="<tr><td>a</td><td>assignee</td></tr></table>"
    state.Usage +="<p><b>Example:</b>"
    state.Usage +="<br>t=empty trash,p=12345,ds=tomorrow at noon"
    state.Usage +="<br>t=pet dog,ds=every hour,l=1234,l=5678"
}
def logDebug(msg) 
{
    if (logEnable)
    {
        log.debug(msg)
    }
}
def deviceNotification(text)
{
    logDebug("Original text: ${text}")
    def map = [:]
    text.split(",").each {param ->
        def flagAndValue = param.split("=")
        logDebug(flagAndValue)
        switch (flagAndValue[0])
        {
            case "p":
            map["project_id"] = Long.valueOf(flagAndValue[1])
            break
            case "t":
            map["content"] = flagAndValue[1]
            break
            case "ds":
            map["due_string"] = flagAndValue[1]
            break
            case "pri":
            map["priority"] = Long.valueOf(flagAndValue[1])
            break
            case "a":
            map["assignee"] = Long.valueOf(flagAndValue[1])
            break
            case "l":
            if (!map["label_ids"])
            {
                map["label_ids"] = [Long.valueOf(flagAndValue[1])]
            }
            else
            {
                map["label_ids"].add(Long.valueOf(flagAndValue[1]))
            }
            break
            default:
            log.debug("Unknown parameter ${flagAndValue[0]}:${flagAndValue[1]}")
        }
    }
    logDebug(map)
    try
    {
        def postParams = genParamsPre()
        postParams['body'] = map
        httpPostExec(postParams, true)
    }
    catch (Exception e)
    {
        logDebug("Task creation failed")
    }
}

def getBaseURI()
{
    return "https://api.todoist.com/rest/v1/tasks"
}

def genParamsPre()
{
    def params =
        [
            uri: getBaseURI(),
            headers:
            [
                Authorization: "Bearer " + "${password}",
                "X-Request-Id": UUID.randomUUID().toString()
            ],
            requestContentType: 'application/json',
            contentType: 'application/json'
        ]
 
    return params
}

def httpPostExec(params, throwToCaller = false)
{
    logDebug("httpPostExec(${params})")
    
    try
    {
        def result
        httpPost(params)
        { resp ->
            if (resp.data)
            {
                logDebug("resp.data = ${resp.data}")
                result = resp.data
            }
        }
        return result
    }
    catch (Exception e)
    {
        logDebug("httpPostExec() failed: ${e.message}")
        if(throwToCaller)
        {
            throw(e)
        }
    }
}
def getLabels()
{
    def param =
        [
            uri: "https://api.todoist.com/rest/v1/labels",
            headers:
            [
                Authorization: "Bearer " + "${password}",
            ]
        ]
    try
    {
        logDebug("httpPostExec(${param})")
        def jsonSlurper = new JsonSlurper()
        def result
        httpGet(param)
        { resp ->
            if (resp.data)
            {
                logDebug("${resp.data}")
                state.Labels = "<br><table><tr><th>ID</th><th>Name</th></tr>"
                for (i in resp.data)
                {
                    state.Labels +="<tr><td>${i.id}</td><td>${i.name}</td></tr>"
                }
                state.Labels += "</table>"
            }
        }
    }
    catch (Exception e)
    {
        logDebug("httpPostExec() failed: ${e.message}")
        if(throwToCaller)
        {
            throw(e)
        }
    }
    updated()
}
