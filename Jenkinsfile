pipeline {
    agent any

    tools {
        jdk 'jdk25'          // Jenkins Global Tool Configuration 이름과 일치시킬 것
    }

    environment {
        APP_NAME   = 'ilog'
        IMAGE_TAG  = "${env.BUILD_NUMBER}-${env.GIT_COMMIT?.take(7)}"
        ECR_REPO   = credentials('ecr-repo-uri')
        AWS_REGION = 'ap-northeast-2'
    }

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }

        stage('Lint') {
            steps { sh './gradlew spotlessCheck --no-daemon' }
        }

        stage('Test') {
            steps { sh './gradlew test --no-daemon' }   // Testcontainers 사용 → 에이전트에 Docker 필요
            post {
                always {
                    junit 'build/test-results/test/*.xml'
                    recordCoverage(tools: [[parser: 'JACOCO', pattern: 'build/reports/jacoco/test/jacocoTestReport.xml']])
                }
            }
        }

        stage('Coverage Gate') {
            steps { sh './gradlew jacocoTestCoverageVerification --no-daemon' }
        }

        stage('Build Jar') {
            steps { sh './gradlew bootJar -x test --no-daemon' }
        }

        stage('Docker Build & Push') {
            when { anyOf { branch 'develop'; branch 'main' } }
            steps {
                sh '''
                  aws ecr get-login-password --region $AWS_REGION \
                    | docker login --username AWS --password-stdin $ECR_REPO
                  docker build -t $ECR_REPO:$IMAGE_TAG .
                  docker push $ECR_REPO:$IMAGE_TAG
                '''
            }
        }

        stage('Deploy dev') {
            when { branch 'develop' }
            steps {
                // TODO: 배포 대상 확정 후 교체 (EC2 + docker compose / ECS / Elastic Beanstalk)
                sh './scripts/deploy.sh dev $IMAGE_TAG'
            }
        }

        stage('Approve prod') {
            when { branch 'main' }
            steps { input message: '운영 배포를 진행할까요?' }
        }

        stage('Deploy prod') {
            when { branch 'main' }
            steps { sh './scripts/deploy.sh prod $IMAGE_TAG' }
        }

        stage('Health Check') {
            when { anyOf { branch 'develop'; branch 'main' } }
            steps {
                // 대상 환경 URL은 Jenkins Credentials/파라미터로 주입
                sh './scripts/health-check.sh $BRANCH_NAME'
            }
        }
    }

    post {
        failure { echo '빌드 실패 — 알림 채널 연동 시 여기에 추가' }
        always  { cleanWs() }
    }
}
