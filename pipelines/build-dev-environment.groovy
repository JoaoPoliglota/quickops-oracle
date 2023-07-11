pipeline {
    agent any 

    options {
        disableConcurrentBuilds()
        disableResume()
    }

    parameters {
        string name: 'ENVIRONMENT_NAME', trim: true     
        password defaultValue: '123456789', description: 'Password to use for MySQL container - root user', name: 'MYSQL_PASSWORD'
        string name: 'MYSQL_PORT', trim: true  

        booleanParam(name: 'SKIP_STEP_1', defaultValue: false, description: 'STEP 1 - RE-CREATE DOCKER IMAGE')        
    }
    environment {
      EVN = params.ENVIRONMENT_NAME.toLowerCase()
    }
  
    stages {
        stage('Checkout GIT repository') {
            steps {     
              script {
                git branch: 'main',
                credentialsId: 'root',
                url: 'git@gitlab.com:joaopoliglota/quickops.git'
              }
            }
        }
        stage('Create latest Docker image') {
            steps {
              script {
                if (!params.SKIP_STEP_1){    
                    echo 'Creating docker image with name $params.ENVIRONMENT_NAME using port: $params.MYSQL_PORT'

                    // fixed PASSOWRD to PASSWORD in template && Hiding password output
                    sh """
                    set +x
                    sed 's/<PASSWORD>/$params.MYSQL_PASSWORD/g' pipelines/include/create_developer.template > pipelines/include/create_developer.sql
                    """

                    // lowercase container image name
                    sh """
                    docker build pipelines/ -t "${EVN}":latest
                    """

                }else{
                    echo "Skipping STEP1"
                }
              }
            }
        }
        stage('Start new container using latest image and create user') {
            steps {     
              script {
                
                def dateTime = (sh(script: "date +%Y%m%d%H%M%S", returnStdout: true).trim())
                def containerName = "${EVN}_${dateTime}"

                // lowercase container image name && Hiding password output
                sh """
                set +x
                docker run -d --name ${containerName} --rm -e MYSQL_ROOT_PASSWORD="$params.MYSQL_PASSWORD" -p $params.MYSQL_PORT:3306 "${EVN}":latest
                """

                sh "sleep 30" // Sleep time to up database
                
                //  Hiding password output
                sh """
                set +x
                docker exec  ${containerName} /bin/bash -c 'mysql --user="root" --password="$params.MYSQL_PASSWORD" < /scripts/create_developer.sql'
                """

                echo "Docker container created: $containerName"

              }
            }
        }
    }

}
