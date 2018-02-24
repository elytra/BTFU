pipeline {
    agent any
	stages {
	    stage("1.7.10") {
	        steps {
	            sh './gradlew :1.7.10:setupCiWorkspace'
	            sh './gradlew :1.7.10:clean'
	            sh './gradlew :1.7.10:build'
	            archive '1.7.10/build/libs/*jar'
	        }
	    }
	    stage("1.8.9") {
	        steps {
	            sh './gradlew :1.8.9:setupCiWorkspace'
	            sh './gradlew :1.8.9:clean'
	            sh './gradlew :1.8.9:build'
	            archive '1.8.9/build/libs/*jar'
	        }
	    }
	    stage("1.9.4") {
	        steps {
	            sh './gradlew :1.9.4:setupCiWorkspace'
	            sh './gradlew :1.9.4:clean'
	            sh './gradlew :1.9.4:build'
	            archive '1.9.4/build/libs/*jar'
	        }
	    }
	    stage("1.11.2") {
	        steps {
	            sh './gradlew :1.11.2:setupCiWorkspace'
	            sh './gradlew :1.11.2:clean'
	            sh './gradlew :1.11.2:build'
	            archive '1.11.2/build/libs/*jar'
	        }
	    }
	    stage("1.12.2") {
	        steps {
	            sh './gradlew :1.12.2:setupCiWorkspace'
	            sh './gradlew :1.12.2:clean'
	            sh './gradlew :1.12.2:build'
	            archive '1.12.2/build/libs/*jar'
	        }
	    }
	}
}