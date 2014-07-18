/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//      Contributors:      Xu Lijia

package ci.xlj.libs.jenkinsvisitor

import groovy.json.JsonSlurper

import org.apache.http.HttpResponse
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.params.ClientPNames
import org.apache.http.entity.StringEntity
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.protocol.BasicHttpContext
import org.apache.http.util.EntityUtils
import org.apache.log4j.Logger

import ci.xlj.libs.jenkinsvisitor.entity.BuildInfo
import ci.xlj.libs.jenkinsvisitor.entity.Job
import ci.xlj.libs.utils.DateUtils
import ci.xlj.libs.utils.StringUtils

/**
 * This lib provides functionality of manipulating Jenkins Server,<br/>
 * such as creating jobs, reading jobs' data, etc. 
 *
 * @author kfzx-xulj
 */
class JenkinsVisitor {

	private Logger logger = Logger.getLogger(JenkinsVisitor)


	private serverUrl

	private DefaultHttpClient client
	private context

	private get
	private post

	private HttpResponse response
	private entity
	private responseContent=""

	private js=new JsonSlurper()

	private jobNameList
	private viewNameList

	private encoding = "UTF-8"

	private homePageJSON
	
	private int statusCode

	/**
	 * @param serverURL Jenkins Server URL
	 */
	JenkinsVisitor(serverURL) {
		this.serverUrl = StringUtils.transformToUrl(serverURL)
		logger.info("Jenkins Server URL is '$serverUrl'.")

		client = new DefaultHttpClient()
	}

	private void getHomePageJSON() {
		if (!homePageJSON) {
			def homepage="${serverUrl}api/json"
			homePageJSON = doGet(homepage)
		}
	}

	/**
	 * Log on to Jenkins Server
	 *
	 * @param username Jenkins Server username
	 * @param password Jenkins Server password
	 */
	boolean login(username, password) {
		// provide the right credentials
		client.getCredentialsProvider().setCredentials(
				new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
				new UsernamePasswordCredentials(username, password))

		// Generate BASIC scheme object and stick it to the execution context
		def basicAuth = new BasicScheme()

		context = new BasicHttpContext()
		context.setAttribute("preemptive-auth", basicAuth)

		// Add as the first (because of the zero) request interceptor
		// It will first intercept the request and preemptively initialize the
		// authentication scheme if there is not
		client.addRequestInterceptor(new PreemptiveAuth(), 0)

		// validate user login
		getHomePageJSON()

		try {
			def primaryViewUrl = homePageJSON.primaryView.url

			if (StringUtils.isUrl(primaryViewUrl)) {
				return true
			}

		} catch (Exception e) {
			if (responseContent.contains('Failed to login as')) {
				responseContent = 'Invalid username or password. Login failed.'
			} else {
				logger.error('Unexpected error occurred while logging in. Details:\n'
						+ StringUtils.getStrackTrace(e))
			}
		}

		return false
	}

	/**
	 * Create a job on the Jenkins Server
	 *
	 * @param jobName The name of the job to be created
	 * @param config Jenkins job config
	 * @return 200: success
	 */
	int create(String jobName, String config) {
		def createUrl = "${serverUrl}createItem?name=$jobName"

		try {
			doPost(createUrl, config)
		} catch (Exception e) {
			logger.error("Error in creating job '$jobName'. Details:\n"
					+ StringUtils.getStrackTrace(e))
		}

		return response.getStatusLine().getStatusCode()
	}

	/**
	 * Create a job on the Jenkins Server
	 *
	 * @param jobName The name of the job to be created
	 * @param config config.xml file object
	 * @return 200: success
	 */
	int create(String jobName, File configFile)	{

		def config=""
		configFile.eachLine{config<<=it}

		create(jobName, config.toString())

		return response.getStatusLine().getStatusCode()
	}

	/**
	 * Create a job by copying an existing one
	 *
	 * @param srcJobName Source Job Name
	 * @param destJobName Destination Job Name
	 * @return 200: success
	 */
	int copy(srcJobName, destJobName) {
		def copyUrl = "${serverUrl}createItem?name=$destJobName&mode=copy&from=$srcJobName"

		try {
			doPost(copyUrl, null)
		} catch (Exception e) {
			logger.error("Error in copying job '$destJobName' from '$srcJobName'. Details:\n"
					+ StringUtils.getStrackTrace(e))
		}

		return response.getStatusLine().getStatusCode()
	}

	/**
	 * Update a job by posting a config.xml file
	 *
	 * @return 200: success
	 */
	int update(jobName, config) {
		def updateUrl="${serverUrl}jobName/config.xml"

		try {
			doPost(updateUrl, config)
		} catch (Exception e) {
			logger.error("Error in updating config.xml file for job '$jobName'. Details:\n"
					+ StringUtils.getStrackTrace(e))
		}

		return response.getStatusLine().getStatusCode()
	}

	/**
	 * Build a certain job
	 *
	 * @param jobName The name of the job to be built
	 * @return 200: success
	 */
	int build(jobName) {
		def buildUrl = "${serverUrl}job/${jobName}/build"

		try {
			doPost(buildUrl, null)
		} catch (Exception e) {
			logger.error("Error in building job '$jobName'. Details:\n"
					+ StringUtils.getStrackTrace(e))
		}

		return response.getStatusLine().getStatusCode()
	}

	/**
	 * Delete a certain job
	 *
	 * @param jobName The name of the job to be deleted
	 * @return 302: success
	 */
	int delete(jobName) {
		def deleteUrl = "${serverUrl}job/${jobName}/doDelete"

		try {
			doPost(deleteUrl, null)
		} catch (Exception e) {
			logger.error("Error in deleting job '$jobName'. Details:\n"
					+ StringUtils.getStrackTrace(e))
		}

		return response.getStatusLine().getStatusCode()
	}

	/**
	 * Disable a certain job
	 *
	 * @param jobName The name of the job to be disabled
	 * @return 302: success
	 */
	int disable(jobName) {
		def disableUrl = "${serverUrl}job/${jobName}/disable"

		try {
			doPost(disableUrl, null)
		} catch (Exception e) {
			logger.error("Error in disabling job '$jobName'. Details:\n"
					+ StringUtils.getStrackTrace(e))
		}

		return response.getStatusLine().getStatusCode()
	}

	/**
	 * Enable a certain job
	 *
	 * @param jobName The name of the job to be enabled
	 * @return 302: success
	 */
	int enable(jobName) {
		def enableUrl = "${serverUrl}job/${jobName}/enable"

		try {
			doPost(enableUrl, null)
		} catch (Exception e) {
			logger.error("Error in enabling job '$jobName'. Details:\n"
					+ StringUtils.getStrackTrace(e))
		}

		return response.getStatusLine().getStatusCode()
	}

	void doPost(postUrl, postContent) {
		post = new HttpPost(postUrl)
		post.setHeader("Content-Type", "text/xml;charset=UTF-8")

		if (postContent) {
			def configEntity = new StringEntity(postContent)
			post.setEntity(configEntity)
		}

		response = client.execute(post, context)
		statusCode=response.getStatusLine().getStatusCode()
		entity = response.getEntity()
		responseContent = getHtmlFromHttpEntity()

		EntityUtils.consume(entity)
	}

	/**
	 * Retrieve job name list<br>
	 */
	List<String> getJobNameList() {
		if (!homePageJSON) {
			getHomePageJSON()
		}

		jobNameList = []

		for(def j in homePageJSON.jobs){
			jobNameList<<j.name
		}

		return jobNameList
	}

	boolean checkExistence(jobName) {
		getHomePageJSON()

		homePageJSON.jobs.each{
			if(it.name==jobName){
				return true
			}
		}
	}

	/**
	 * Retrieve the data of a certain job
	 *
	 * @param jobName The name of the job
	 * @return Job object or null
	 */
	Job getJobDetails(jobName) {
		def jobUrl = "${serverUrl}job/$jobName"

		def job = new Job(jobName)
		job.url=jobUrl

		def jobJSON = doGet("$jobUrl/api/json")
		job.buildable=jobJSON.buildable
		job.color=jobJSON.color
		job.concurrentBuild=jobJSON.concurrentBuild
		job.nextBuildNum=jobJSON.nextBuildNumber

		// get build number
		job.firstBuildNum=jobJSON.firstBuild ? jobJSON.firstBuild.number :-1
		job.lastBuildNum=jobJSON.lastBuild? jobJSON.lastBuild.number:-1
		job.lastCompletedBuildNum=jobJSON.lastCompletedBuild?jobJSON.lastCompletedBuild.number:-1
		job.lastFailedBuildNum=jobJSON.lastFailedBuild?jobJSON.lastFailedBuild.number:-1
		job.lastStableBuildNum=jobJSON.lastStableBuild? jobJSON.lastStableBuild.number:-1
		job.lastUnstableBuildNum=jobJSON.lastUnstableBuild? jobJSON.lastUnstableBuild.number:-1
		job.lastSuccessfulBuildNum=jobJSON.lastSuccessfulBuild ?jobJSON.lastSuccessfulBuild.number :-1
		job.lastUnsuccessfulBuildNum=jobJSON.lastUnsuccessfulBuild?jobJSON.lastUnsuccessfulBuild.number :-1

		// get detailed build info
		jobJSON.builds.each{
			def buildInfo = new BuildInfo()
			buildInfo.currentBuildNum=it.number
			buildInfo.currentBuildUrl=it.url

			def buildInfoJSON = doGet("${it.url}api/json")
			buildInfo.duration=buildInfoJSON.duration
			buildInfo.dateTime=DateUtils.toString(DateUtils.toDate(buildInfoJSON.id))
			buildInfo.result=buildInfoJSON.result
			buildInfo.builtOnSlave=buildInfoJSON.builtOn

			job.addBuildInfo(it.number, buildInfo)
		}

		return job
	}

	/**
	 * Retrieve report info from a specific plugin of a certain job
	 *
	 * @param jobName Job name
	 * @param buildTag build number or build tag
	 * @param pluginUrlFragment plugin url fragment
	 */
	def getJobReportData(jobName, buildTag, pluginUrlFragment) throws Exception {
		return doGet("${serverUrl}job/$jobName/$buildTag/$pluginUrlFragment/api/json")
	}

	/**
	 * Get Upstream Job Names
	 */
	List<String> getUpStreamJobNameList(jobName) {
		def jobJSON = doGet("${serverUrl}job/$jobName/api/json")

		jobNameList = []

		jobJSON.upstreamProjects.each{ jobNameList<<it.name }

		return jobNameList
	}

	/**
	 * Get Downstream Job Names
	 */
	List<String> getDownStreamJobNameList(jobName) {
		def jobJSON = doGet("${serverUrl}job/$jobName/api/json")

		jobNameList = []
		
		for(def project:jobJSON.downstreamProjects){ 
			jobNameList<<project.name
		}

		return jobNameList
	}

	int getServerMode() {
		if (!homePageJSON) {
			getHomePageJSON()
		}

		return homePageJSON.mode
	}

	String getServerDescription() {
		if (!homePageJSON) {
			getHomePageJSON()
		}

		return homePageJSON.nodeDescription
	}

	int getServerExecutors() {
		if (!homePageJSON) {
			getHomePageJSON()
		}

		return homePageJSON.numExecutors
	}

	List<String> getViewNameList() {
		if (!homePageJSON) {
			getHomePageJSON()
		}

		viewNameList = []

		for(def v in homePageJSON.views){
			viewNameList<<v.name
		}

		return viewNameList
	}

	List<String> getViewJobList(viewName) {
		def viewJSON = doGet("${serverUrl}view/$viewName/api/json")

		def jobList = []

		viewJSON.jobs.each{ jobList<<it.name }

		return jobList
	}

	List<String> getNodeNameList() {
		def nodesJSON = doGet("${serverUrl}computer/api/json")

		def nodeList = []

		nodesJSON.computer.each{ nodeList<<it.displayName }

		return nodeList
	}

	boolean getNodeStatus(nodeName) {
		def nodeJSON = doGet("${serverUrl}computer/${nodeName=='master'? '(master)' : nodeName}/api/json")

		return nodeJSON.offline
	}

	/**
	 * Restart Jenkins Server
	 * @return 200:success
	 */
	int restartJenkins() {
		client.getParams().setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS,
				true)

		def restartUrl = "${serverUrl}quietDown"

		try {
			doGet(restartUrl)
		} catch (Exception e) {
			logger.error("Error in restarting server '$serverUrl'. Details:\n"
					+ StringUtils.getStrackTrace(e))
		}

		return response.getStatusLine().getStatusCode()
	}

	/**
	 * Cancel restarting Jenkins Server
	 * @return 200: success
	 */
	int cancelRestartJenkins() {
		client.getParams().setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS,
				true)

		def cancelRestartUrl = "${serverUrl}cancelQuietDown"

		try {
			doGet(cancelRestartUrl)
		} catch (Exception e) {
			logger.error("Error in canceling restarting server '$serverUrl'. Details:\n"
					+ StringUtils.getStrackTrace(e))
		}

		return response.getStatusLine().getStatusCode()
	}

	private doGet(String url) {
		get = new HttpGet(url)

		response = client.execute(get, context)
		entity = response.getEntity()

		def result = js.parse(new InputStreamReader(entity.getContent(), encoding))
		EntityUtils.consume(entity)

		return result
	}
	
	int getResponseStatusCode(){
		return statusCode
	}
	
	String getResponseContent(){
		return responseContent
	}
	
	/**
	 * Parse HTTP Entity
	 */
	private getHtmlFromHttpEntity() {
		def htmlContent = ""

		try {
			def reader = new BufferedReader(new InputStreamReader(
					entity.getContent(), encoding))

			reader.eachLine { htmlContent<<=it }

		} catch (Exception e) {
			logger.error("Error in transforming HTTP Entity into String. Details:\n"
					+ StringUtils.getStrackTrace(e))
		}

		return htmlContent.toString()
	}
}
