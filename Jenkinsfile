pipeline {
  agent any
  environment {
    IMAGE_BASE = "vuphongle23/movie-ticket-booking-be"
    COMMIT     = "${env.GIT_COMMIT ?: 'local'}"
    TAG_LATEST = "${IMAGE_BASE}:latest"
    TAG_BUILD  = "${IMAGE_BASE}:${env.BUILD_NUMBER}"
  }
  options {
    timestamps()
  }
  stages {
    stage('Checkout') {
      steps {
        git branch: 'dev',
            url: 'https://github.com/vuphongle/movie-ticket-booking-be.git'
      }
    }

    stage('Build image') {
      steps {
        script {
          docker.build("${TAG_BUILD}")
        }
      }
    }

    stage('Tag latest') {
      steps {
        sh 'docker tag ${TAG_BUILD} ${TAG_LATEST}'
      }
    }

    stage('Push to Docker Hub') {
      steps {
        withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
          sh '''
            echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
            docker push ${TAG_BUILD}
            docker push ${TAG_LATEST}
            docker logout
          '''
        }
      }
    }
  }
}
