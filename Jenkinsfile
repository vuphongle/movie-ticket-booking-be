pipeline {
  agent any
  environment {
    IMAGE_BASE = "vuphongle23/movie-ticket-booking-be"
    COMMIT     = "${env.GIT_COMMIT ?: 'local'}"
    TAG_LATEST = "${IMAGE_BASE}:latest"
    TAG_BUILD  = "${IMAGE_BASE}:${env.BUILD_NUMBER}"
    
    // VPS Configuration
    VPS_HOST = "104.248.157.211"
    VPS_USER = "root"
    DEPLOY_PATH = "/opt/movie-ticket-booking-be"
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

    stage('Clean Docker Environment') {
      steps {
        script {
          // Remove any previous build artifacts and dangling images
          sh """
            docker system prune -f || true
            docker image rm ${TAG_BUILD} || true
            docker image rm ${TAG_LATEST} || true
          """
        }
      }
    }

    stage('Build Image') {
      steps {
        script {
          // Build with --no-cache and --pull to ensure fresh build
          docker.build("${TAG_BUILD}", "--no-cache --pull .")
        }
      }
    }

    stage('Tag Latest') {
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

    stage('Deploy to VPS') {
      steps {
        script {
          withCredentials([sshUserPrivateKey(credentialsId: 'vps-ssh-key', keyFileVariable: 'SSH_KEY', usernameVariable: 'SSH_USER')]) {
            sh """
              ssh -i \${SSH_KEY} -o StrictHostKeyChecking=no ${VPS_USER}@${VPS_HOST} '
                cd ${DEPLOY_PATH} && \
                docker compose pull backend && \
                docker compose up -d backend && \
                docker compose ps
              '
            """
          }
        }
      }
    }
  }
  
  post {
    success {
      echo "✅ Build, push và deploy thành công!"
      echo "Image: ${TAG_BUILD}"
      echo "Latest: ${TAG_LATEST}"
      echo "Deployed to: ${VPS_HOST}"
    }
    failure {
      echo "❌ Pipeline thất bại!"
      // Clean up failed build artifacts
      sh """
        docker image rm ${TAG_BUILD} || true
      """
    }
    always {
      // Clean up dangling images
      sh """
        docker system prune -f || true
      """
    }
  }
}
