/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License") you may not use this file except in compliance with
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

package ci.xlj.libs.jenkinsvisitor.entity


final class Job {

	private String name

	private String url

	private boolean buildable

	private String color

	private boolean concurrentBuild

	private int nextBuildNum

	private int firstBuildNum

	private int lastBuildNum

	private int lastCompletedBuildNum

	private int lastFailedBuildNum

	private int lastStableBuildNum

	private int lastUnstableBuildNum

	private int lastSuccessfulBuildNum

	private int lastUnsuccessfulBuildNum

	private Map<Integer, BuildInfo> buildHistory = new HashMap<Integer, BuildInfo>()

	Job(name) {
		this.name = name
	}

	String getName(){
		return name
	}

	String getColor(){
		return color
	}

	def getFirstBuildInfo() {
		if (firstBuildNum != -1) {
			return buildHistory.get(firstBuildNum)
		} else {
			return null
		}
	}

	def getLastBuildInfo() {
		if (lastBuildNum != -1) {
			return buildHistory.get(lastBuildNum)
		} else {
			return null
		}
	}

	def getLastCompletedBuildInfo() {
		if (lastCompletedBuildNum != -1) {
			return buildHistory.get(lastCompletedBuildNum)
		} else {
			return null
		}
	}

	def getLastFailedBuildInfo() {
		if (lastFailedBuildNum != -1) {
			return buildHistory.get(lastFailedBuildNum)
		} else {
			return null
		}
	}

	def getLastStableBuildInfo() {
		if (lastStableBuildNum != -1) {
			return buildHistory.get(lastStableBuildNum)
		} else {
			return null
		}
	}

	def getLastSuccessfulBuildInfo() {
		if (lastSuccessfulBuildNum != -1) {
			return buildHistory.get(lastSuccessfulBuildNum)
		} else {
			return null
		}
	}

	def getLastUnstableBuildInfo() {
		if (lastUnstableBuildNum != -1) {
			return buildHistory.get(lastUnstableBuildNum)
		} else {
			return null
		}
	}

	def getLastUnsuccessfulBuildInfo() {
		if (lastUnsuccessfulBuildNum != -1) {
			return buildHistory.get(lastUnsuccessfulBuildNum)
		} else {
			return null
		}
	}

	void addBuildInfo(buildNum, buildInfo) {
		this.buildHistory.put(buildNum, buildInfo)
	}
}
