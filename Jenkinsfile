pipeline {
  agent {
    docker {
      image 'ubuntu'
    }

  }
  stages {
    stage('checkout') {
      steps {
        echo 'Hello'
      }
    }
    stage('build') {
      parallel {
        stage('build') {
          steps {
            sh 'echo "hello"'
          }
        }
        stage('deploy') {
          steps {
            sh 'Hello'
          }
        }
      }
    }
  }
  environment {
    stage = 'DEVELOPMENT'
  }
}